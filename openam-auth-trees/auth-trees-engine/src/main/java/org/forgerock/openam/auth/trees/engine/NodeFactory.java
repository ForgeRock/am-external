/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.Node.Metadata;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Types;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.ServiceSchemaManagerFactory;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating {@link Node} instances by type and id.
 *
 * <p>The available node types are stored as global config items in the SMS.</p>
 *
 * @since AM 5.5.0
 */
@Singleton
public class NodeFactory {
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final Injector injector;
    private final NodeRegistry nodeRegistry;
    private final AnnotatedServiceRegistry serviceRegistry;
    private final LoadingCache<Class<? extends Node>, NodeProvider> nodeProviderCache;
    private final LoadingCache<Class<? extends OutcomeProvider>, OutcomeProviderProvider> outcomeProviderProviderCache;
    private final SmsJsonConverterFactory smsJsonConverterFactory;
    private final ServiceConfigManagerFactory serviceConfigManagerFactory;
    private final ServiceSchemaManagerFactory serviceSchemaManagerFactory;

    /**
     * Guice constructor.
     * @param injector The injector.
     * @param nodeRegistry The registry of node services.
     * @param serviceRegistry The annotated service registry.
     * @param smsJsonConverterFactory The factory for SMS JSON conversion.
     * @param serviceConfigManagerFactory A factory for ServiceConfigManager instances.
     * @param serviceSchemaManagerFactory A factory for ServiceSchemaManager instances.
     */
    @Inject
    public NodeFactory(Injector injector, NodeRegistry nodeRegistry, AnnotatedServiceRegistry serviceRegistry,
            SmsJsonConverterFactory smsJsonConverterFactory, ServiceConfigManagerFactory serviceConfigManagerFactory,
            ServiceSchemaManagerFactory serviceSchemaManagerFactory) {
        this.injector = injector;
        this.nodeRegistry = nodeRegistry;
        this.serviceRegistry = serviceRegistry;

        this.nodeProviderCache = CacheBuilder.newBuilder()
                .build(CacheLoader.from(nodeClass -> {
                    Class<?> configClass = nodeClass.getAnnotation(Node.Metadata.class).configClass();
                    TypeLiteral<NodeProvider<?>> typeLiteral = getNodeProviderTypeLiteral(configClass);
                    Injector childInjector = injector.createChildInjector(new FactoryModuleBuilder()
                            .implement(Node.class, nodeClass)
                            .build(typeLiteral));
                    return childInjector.getInstance(Key.get(typeLiteral));
                }));
        this.outcomeProviderProviderCache = CacheBuilder.newBuilder()
                .build(CacheLoader.from(outcomeProviderClass -> {
                    Injector childInjector = injector.createChildInjector(new FactoryModuleBuilder()
                            .implement(OutcomeProvider.class, outcomeProviderClass)
                            .build(OutcomeProviderProvider.class));
                    return childInjector.getInstance(Key.get(OutcomeProviderProvider.class));
                }));
        this.smsJsonConverterFactory = smsJsonConverterFactory;
        this.serviceConfigManagerFactory = serviceConfigManagerFactory;
        this.serviceSchemaManagerFactory = serviceSchemaManagerFactory;
    }

    /**
     * Constructs a {@link Node} instance that matches the given type and node ID.
     *
     * @param type node type
     * @param id node UUID
     * @param realm realm where the tree belongs to
     * @param tree tree where the node belongs to
     * @return an instance of a concrete {@link Node}.
     * @throws NodeProcessException If the node can't be created for some SMS reason.
     */
    @SuppressWarnings("unchecked")
    public Node createNode(String type, final UUID id, final Realm realm, AuthTree tree) throws NodeProcessException {
        if (!nodeRegistry.isNodeService(type)) {
            logger.error("Unsupported node type " + type);
            throw new IllegalArgumentException("Unsupported node type " + type);
        }
        final Class<? extends Node> clazz = nodeRegistry.getNodeType(type);
        try {
            NodeProvider provider = nodeProviderCache.get(clazz);
            Object config = getConfigForNode(type, realm, id);
            return provider.create(realm, id, tree, config);
        } catch (ExecutionException | RuntimeException e) {
            logger.error("Could not create {} node with id {} in realm {}", type, id, realm, e);
            throw new NodeProcessException("Failed to create node", e);
        }
    }

    /**
     * Returns the config instance for a given node.
     *
     * @param type the node type.
     * @param realm the realm.
     * @param nodeId the id of the node instance.
     * @return the config.
     * @throws NodeProcessException if config is not found.
     */
    public Object getConfigForNode(String type, Realm realm, UUID nodeId) throws NodeProcessException {
        if (!nodeRegistry.isNodeService(type)) {
            throw new IllegalArgumentException("Unsupported node type " + type);
        }
        Class<?> configClass = nodeRegistry.getConfigType(type);
        try {
            Optional<?> realmInstance = serviceRegistry.getRealmInstance(configClass, realm, nodeId.toString());
            return realmInstance.orElseThrow(() -> new NodeProcessException("Node did not exist"));
        } catch (SMSException | SSOException | RuntimeException e) {
            logger.error("Could not get Config for type {} node with id {} in realm {}", type, nodeId, realm, e);
            throw new NodeProcessException("Failed to get node config", e);
        }
    }

    @SuppressWarnings("unchecked")
    private TypeLiteral<NodeProvider<?>> getNodeProviderTypeLiteral(Class<?> configClass) {
        Type type = Types.newParameterizedTypeWithOwner(NodeFactory.class, NodeProvider.class, configClass);
        return (TypeLiteral<NodeProvider<?>>) TypeLiteral.get(type);
    }

    /**
     * Gets the outcome provider associated with a given node type.
     * @param realm the realm the node is in
     * @param type node type
     * @return an instance of a concrete {@link OutcomeProvider}.
     * @throws NodeProcessException if the outcome provider cannot be obtained.
     */
    public OutcomeProvider getOutcomeProvider(Realm realm, String type) throws NodeProcessException {
        if (!nodeRegistry.isNodeService(type)) {
            throw new IllegalArgumentException("Unsupported node type " + type);
        }
        Metadata annotation = nodeRegistry.getNodeType(type).getAnnotation(Metadata.class);
        Class<? extends OutcomeProvider> providerClass = annotation.outcomeProvider();
        try {
            return outcomeProviderProviderCache.get(providerClass).create(realm);
        } catch (ExecutionException e) {
            logger.error("Could not create {} node outcome provider in realm {}", type, realm, e);
            throw new NodeProcessException("Failed to obtain outcome provider", e);
        }
    }

    /**
     * Get the node outcomes for a given node.
     *
     * @param preferredLocales The locales to get the outcome names in.
     * @param realm The realm.
     * @param nodeId The ID of the node.
     * @param nodeType The type of the node.
     * @return The list of outcomes for the configured node.
     * @throws NodeProcessException In the event of a failure in the outcome provider.
     * @throws SSOException If the config for the node cannot be loaded.
     * @throws SMSException If the config for the node cannot be loaded.
     */
    public List<OutcomeProvider.Outcome> getNodeOutcomes(PreferredLocales preferredLocales, Realm realm, String nodeId,
            String nodeType) throws NodeProcessException, SSOException, SMSException {
        OutcomeProvider provider = getOutcomeProvider(realm, nodeType);
        ServiceSchemaManager ssm = serviceSchemaManagerFactory.build(nodeType);
        ServiceConfigManager scm = serviceConfigManagerFactory.create(nodeType, "1.0");
        ServiceConfig nodeConfig = scm.getOrganizationConfig(realm.asDN(), null).getSubConfig(nodeId);
        ServiceSchema nodeOrgSchema = ssm.getOrganizationSchema();
        ServiceSchema nodeSchema = nodeOrgSchema.getSubSchema(nodeOrgSchema.getSubSchemaNames().iterator().next());
        JsonValue nodeAttributes = smsJsonConverterFactory.create(nodeSchema)
                .toJson(realm.asPath(), nodeConfig.getAttributes(), false);
        try {
            return provider.getOutcomes(preferredLocales, nodeAttributes);
        } catch (NodeProcessException | RuntimeException e) {
            logger.error("The node outcomes cannot be fetched", e);
            throw e;
        }
    }

    private interface NodeProvider<T> {
        Node create(Realm realm, UUID nodeId, AuthTree authTree, T config)
                throws NodeProcessException;
    }

    private interface OutcomeProviderProvider {
        OutcomeProvider create(Realm realm);
    }
}
