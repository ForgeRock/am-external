/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TREES_SERVICE_NAME;
import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TREES_SERVICE_VERSION;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.guava.common.cache.CacheLoader;
import org.forgerock.guava.common.cache.LoadingCache;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.impl.RealmImpl;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.ConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.ServiceListeners;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.datastruct.ValueNotFoundException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;

/**
 * This class is responsible for translating SMS config for authentication trees into {@link AuthTree} instances.
 *
 * @since AM 5.5.0
 */
@Singleton
public class AuthTreeService {

    private final Logger logger = LoggerFactory.getLogger("amAuth");

    private final ServiceConfigManagerFactory scmFactory;
    private final NodeFactory nodeFactory;
    private final NodeRegistry nodeRegistry;
    private final LoadingCache<Realm, Set<String>> trees;

    /**
     * Constructs new AuthTreeService.
     *
     * @param scmFactory SCM factory
     * @param nodeFactory A node factory.
     * @param nodeRegistry The registry of authentication node services.
     * @param serviceListeners The service listeners creation API.
     */
    @Inject
    public AuthTreeService(ServiceConfigManagerFactory scmFactory, NodeFactory nodeFactory,
            NodeRegistry nodeRegistry, ServiceListeners serviceListeners) {
        this.scmFactory = scmFactory;
        this.nodeFactory = nodeFactory;
        this.nodeRegistry = nodeRegistry;
        this.trees = CacheBuilder.newBuilder()
                .maximumSize(SystemProperties.getAsInt("org.forgerock.openam.trees.ids.cache.size", 50))
                .build(CacheLoader.from(this::loadTreeIds));
        serviceListeners.forService(AUTH_TREES_SERVICE_NAME)
                .onRealmChange(trees::invalidate).listen();
    }

    /**
     * Check to see if the tree exists in the realm.
     * @param realm The realm.
     * @param treeId The tree ID.
     * @return {@literal true} if the tree is present in the realm.
     */
    public boolean hasTree(Realm realm, String treeId) {
        return trees.getUnchecked(realm).contains(treeId);
    }

    private Set<String> loadTreeIds(Realm realm) {
        try {
            ServiceConfig realmConfig = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION)
                    .getOrganizationConfig(realm.asDN(), null);
            ServiceConfig treesContainer = realmConfig.getSubConfig("treesContainer");
            return treesContainer.getSubConfigNames();
        } catch (SMSException | SSOException e) {
            throw new IllegalStateException("Could not load tree service for realm " + realm);
        }
    }

    /**
     * Loads an authentication tree from it's configuration in the SMS. This method should only be called having
     * asserted the tree is expected to exist.
     *
     * @param realm the {@link RealmImpl} where the authentication tree exists
     * @param treeId the authetication tree id
     * @return An optional of the {@link AuthTree} representing the tree or empty if the tree cannot be loaded.
     */
    public Optional<AuthTree> getTree(Realm realm, String treeId) {
        try {
            try {
                ServiceConfig realmConfig = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION)
                        .getOrganizationConfig(realm.asDN(), null);
                ServiceConfig treesContainer = realmConfig.getSubConfig("treesContainer");
                return getAuthTree(treeId, treesContainer, realm);
            } catch (RuntimeException excp) {
                logger.error("Error getting the tree from ServiceConfig", excp);
                throw excp.getCause();
            }
        } catch (ValueNotFoundException e) {
            logger.error("Tree does not conform to correct schema", e);
        } catch (Throwable e) {
            logger.error("Could not get SMS service: " + AUTH_TREES_SERVICE_NAME, e);
        }
        return Optional.empty();
    }

    private Set<AuthTree> getAllTrees(Realm realm) {
        try {
            ServiceConfig realmConfig = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION)
                    .getOrganizationConfig(realm.asDN(), null);
            ServiceConfig treesContainer = realmConfig.getSubConfig("treesContainer");
            Set<String> treeNames = treesContainer.getSubConfigNames();
            if (treeNames == null || treeNames.isEmpty()) {
                return Collections.emptySet();
            }
            return trees.getUnchecked(realm).stream()
                    .map(treeName -> getAuthTree(treeName, treesContainer, realm))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toSet());

        } catch (SMSException | SSOException | RuntimeException e) {
            logger.error("Could not get SMS service: " + AUTH_TREES_SERVICE_NAME, e);
        }
        return Collections.emptySet();
    }

    /**
     * Returns a set of authentication trees whose authentication
     * level equals to or greater than the specified authLevel. If no such
     * tree exists, an empty set will be returned.
     *
     * @param authLevel authentication level.
     * @param realm Realm of the tree.
     * @return Set of authentication trees whose authentication level
     *         equals to or greater that the specified authentication level.
     */
    public Set<String> getAuthTrees(int authLevel, Realm realm) {
        Set<AuthTree> authTrees = getAllTrees(realm);
        Set<String> set = new HashSet<>();
        for (AuthTree t : authTrees) {
            try {
                Optional<Integer> maxAuthLevel = t.getMaxAuthLevel();
                if (maxAuthLevel.isPresent() && maxAuthLevel.get() >= authLevel) {
                    String name = t.getName();
                    set.add(name);
                }
            } catch (NodeProcessException e) {
                logger.error("Authentication level of the tree could not be calculated: " + t.getName(), e);
            }
        }
        return set;
    }

    /**
     * Returns a set of authentication trees available within the realm.
     * If no trees exists, an empty set will be returned.
     *
     * @param realm Realm of the tree.
     * @return Set of authentication trees.
     */
    public Set<String> getAllAuthTrees(Realm realm) {
        return getAllTrees(realm).stream().map(AuthTree::getName).collect(Collectors.toSet());
    }

    private Optional<AuthTree> getAuthTree(String treeId, ServiceConfig treesContainer, Realm realm) {
        try {
            ServiceConfig treeConfig = treesContainer.getSubConfig(treeId);
            if (treeConfig == null) {
                return Optional.empty();
            }

            AuthTree.Builder builder = AuthTree.builder(nodeFactory, nodeRegistry).realm(realm);
            builder.entryNodeId(UUID.fromString(CollectionHelper.getMapAttrThrows(treeConfig.getAttributes(),
                    "entryNodeId")));
            builder.name(treeId);
            for (String nodeIdString : treeConfig.getSubConfigNames()) {
                ServiceConfig nodeConfig = treeConfig.getSubConfig(nodeIdString);
                UUID nodeId = UUID.fromString(nodeIdString);
                String nodeType = ConfigHelper.readValue(nodeConfig, "nodeType");
                String displayName = ConfigHelper.readValue(nodeConfig, "displayName");
                AuthTree.ConnectionBuilder connectionBuilder = builder.node(nodeId, nodeType, displayName);
                for (String outcomeId : nodeConfig.getSubConfigNames()) {
                    ServiceConfig connectionConfig = nodeConfig.getSubConfig(outcomeId);
                    String destinationNodeId = CollectionHelper.getMapAttrThrows(connectionConfig.getAttributes(),
                            "destinationNodeId");
                    connectionBuilder.connect(outcomeId, UUID.fromString(destinationNodeId));
                }
            }
            return Optional.of(builder.build());
        } catch (Exception excp) {
            throw new RuntimeException(excp);
        }
    }
}
