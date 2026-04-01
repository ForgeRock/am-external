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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.rest;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TREES_SERVICE_NAME;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.Resources.newHandler;

import java.util.Set;
import java.util.function.Predicate;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.ServiceRoutes;
import org.forgerock.openam.core.rest.sms.routing.ServiceHandler;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTree;
import org.forgerock.util.Reject;

import com.google.auto.service.AutoService;
import jakarta.inject.Inject;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Service handler for the authentication trees service.
 * <p>
 * This service handler is responsible for registering the authentication trees service and its routes.
 */
@AutoService(ServiceHandler.class)
public class AuthTreeHandler implements ServiceHandler {
    private final AuthTreesCollectionProviderFactory authTreesCollectionProviderFactory;
    private final CoreWrapper coreWrapper;
    private static final Predicate<String> AUTHENTICATION_TREES_HANDLES_FUNCTION =
            AUTH_TREES_SERVICE_NAME::equals;

    @Inject
    AuthTreeHandler(AuthTreesCollectionProviderFactory authTreesCollectionProviderFactory, CoreWrapper coreWrapper) {
        this.authTreesCollectionProviderFactory = authTreesCollectionProviderFactory;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Set<SchemaType> getApplicableSchemaTypes() {
        return Set.of(SchemaType.ORGANIZATION);
    }

    @Override
    public boolean handles(String serviceName) {
        return AUTHENTICATION_TREES_HANDLES_FUNCTION.test(serviceName);
    }

    @Override
    public ServiceRoutes registerService(SmsRouteTree tree, SchemaType schemaType, String serviceName)
            throws SMSException, SSOException {
        Reject.unless(handles(serviceName), "Service handler does not handle service: " + serviceName);
        ServiceSchemaManager ssm =
                coreWrapper.getServiceSchemaManager(AUTH_TREES_SERVICE_NAME, coreWrapper.getAdminToken());

        ServiceSchema treeSchema = ssm.getOrganizationSchema().getSubSchema("tree");

        SmsRouteTree treesHandlingRouteTree = tree.handles(AUTH_TREES_SERVICE_NAME);
        SmsRouteTree.Route route = treesHandlingRouteTree.addRoute(STARTS_WITH, "trees",
                newHandler(authTreesCollectionProviderFactory.create(treeSchema)));
        var serviceRoutes = new ServiceRoutes();
        serviceRoutes.addRoute(treesHandlingRouteTree, route.matcher);
        return serviceRoutes;
    }
}
