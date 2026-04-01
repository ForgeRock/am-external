/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static com.sun.identity.sm.SchemaType.ORGANIZATION;
import static java.util.Objects.requireNonNull;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.openam.sm.DefaultAnnotatedServiceRegistry.INSTANCE_SUB_SCHEMA_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.api.enums.Stability;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.GeneralActionsHandler;
import org.forgerock.openam.core.rest.sms.PassThroughSubordinateHandler;
import org.forgerock.openam.core.rest.sms.ServiceRoutes;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptorFactory;
import org.forgerock.openam.core.rest.sms.routing.ServiceHandler;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTree;
import org.forgerock.util.Reject;

import com.google.auto.service.AutoService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Base class for node service handlers.
 * <p>
 * To implement a custom node service handler, create a class that extends {@link NodeServiceHandler}
 * and annotate it with {@link AutoService} for {@link ServiceHandler}. It will also need to be registered in the
 * set of {@link NodeServiceHandler}s in a guice module, for example:
 * <pre>
 *     {@code
 *         var customNodeHandlers = Multibinder.newSetBinder(binder(), NodeServiceHandler.class);
 *         customNodeHandlers.addBinding().to(MyCustomNodeHandler.class);
 *     }
 * </pre>
 * </p>
 */
public abstract class NodeServiceHandler implements ServiceHandler {

    private final CoreWrapper coreWrapper;
    private final NodeVersionProviderFactory nodeVersionProviderFactory;
    private final NodeResourceVersionRouterFactory nodeResourceVersionRouterFactory;
    private final NodeRegistry nodeRegistry;
    private final SubordinateActionsInterceptorFactory subordinateActionsInterceptorFactory;
    private final GeneralActionsHandler generalActionsHandler;

    /**
     * Constructor for BaseNodeServiceHandler.
     *
     * @param coreWrapper                      the core wrapper
     * @param nodeVersionProviderFactory       the node version provider factory
     * @param nodeResourceVersionRouterFactory the node resource version router factory
     * @param nodeRegistry                     the node registry
     * @param subordinateActionsInterceptorFactory the factory for subordinate actions interceptor
     */
    protected NodeServiceHandler(CoreWrapper coreWrapper, NodeVersionProviderFactory nodeVersionProviderFactory,
            NodeResourceVersionRouterFactory nodeResourceVersionRouterFactory, NodeRegistry nodeRegistry,
            SubordinateActionsInterceptorFactory subordinateActionsInterceptorFactory) {
        this.coreWrapper = requireNonNull(coreWrapper);
        this.nodeVersionProviderFactory = requireNonNull(nodeVersionProviderFactory);
        this.nodeResourceVersionRouterFactory = requireNonNull(nodeResourceVersionRouterFactory);
        this.nodeRegistry = requireNonNull(nodeRegistry);
        this.subordinateActionsInterceptorFactory = requireNonNull(subordinateActionsInterceptorFactory);
        this.generalActionsHandler = new GeneralActionsHandler();
    }

    @Override
    public Set<SchemaType> getApplicableSchemaTypes() {
        return Set.of(ORGANIZATION);
    }

    @Override
    public ServiceRoutes registerService(SmsRouteTree tree, SchemaType schemaType, String serviceName)
            throws SMSException, SSOException {
        Reject.unless(handles(serviceName), "Service handler does not handle service: " + serviceName);
        return addNodeHandler(tree, serviceName);
    }

    private ServiceRoutes addNodeHandler(SmsRouteTree tree, String nodeName) throws SMSException, SSOException {
        SmsRouteTree routeTree = tree.handles(nodeName);
        SSOToken adminToken = coreWrapper.getAdminToken();
        List<String> versions = nodeRegistry.getVersions(nodeName)
                .stream()
                .map(i -> String.format("%d.0", i))
                .toList();
        ServiceSchemaManager firstVersionSsm = coreWrapper.getServiceSchemaManager(nodeName, versions.get(0),
                adminToken);
        ServiceSchema rootSchema = firstVersionSsm.getOrganizationSchema();
        ServiceSchema instanceSchema = rootSchema.getSubSchema(INSTANCE_SUB_SCHEMA_NAME);
        SmsCollectionProvider legacyProvider = getProvider(nodeName, instanceSchema, rootSchema, Stability.DEPRECATED);
        NodeVersionProvider versionsProvider = getVersionsProvider(nodeName, versions, adminToken);
        ServiceRoutes serviceRoutes = new ServiceRoutes();
        RequestHandler handler = newHandler(nodeResourceVersionRouterFactory.create(
                subordinateActionsInterceptorFactory.create(legacyProvider, generalActionsHandler), versionsProvider));
        SmsRouteTree.Route route = routeTree.addRoute(STARTS_WITH, nodeName, handler, false,
                new PassThroughSubordinateHandler());
        serviceRoutes.addRoute(routeTree, route.matcher);
        return serviceRoutes;
    }

    private NodeVersionProvider getVersionsProvider(String nodeName, List<String> versions, SSOToken adminToken)
            throws SSOException, SMSException {
        ServiceSchema rootSchema;
        ServiceSchema instanceSchema;
        List<SmsCollectionProvider> versionProviders = new ArrayList<>();
        for (String version : versions) {
            ServiceSchemaManager ssm = coreWrapper.getServiceSchemaManager(nodeName, version, adminToken);
            rootSchema = ssm.getOrganizationSchema();
            instanceSchema = rootSchema.getSubSchema(INSTANCE_SUB_SCHEMA_NAME);
            versionProviders.add(getProvider(nodeName, instanceSchema, rootSchema, Stability.EVOLVING));
        }
        return nodeVersionProviderFactory.create(versionProviders, generalActionsHandler);
    }

    /**
     * Get the collection provider for the given node name.
     *
     * @param nodeName the name of the node.
     * @param instanceSchema the instance schema for the node.
     * @param rootSchema the root schema for the node.
     * @param stability the stability of the service API.
     * @return the collection provider for the node.
     */
    protected abstract SmsCollectionProvider getProvider(String nodeName, ServiceSchema instanceSchema,
            ServiceSchema rootSchema, Stability stability);
}
