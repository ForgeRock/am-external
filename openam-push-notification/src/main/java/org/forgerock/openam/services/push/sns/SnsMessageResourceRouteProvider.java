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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.services.push.sns;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.openam.rest.Routers.none;

import javax.inject.Inject;

import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.openam.rest.RestRouteProvider;

/**
 * A {@link RestRouteProvider} that adds routes for the SNS message endpoint.
 *
 * @since 13.5.0
 */
public class SnsMessageResourceRouteProvider implements RestRouteProvider {

    /**
     * The path on which the endpoint produced will be accessible.
     */
    public static final String ROUTE = "push/sns/message";

    private SnsMessageResource snsMessageResource;

    /**
     * Inject the SNS Message Resource.
     *
     * @param snsMessageResource The Push Message Resource.
     */
    @Inject
    public void setMessageResource(SnsMessageResource snsMessageResource) {
        this.snsMessageResource = snsMessageResource;
    }

    @Override
    public void addResourceRoutes(ResourceRouter rootRouter, ResourceRouter realmRouter) {
        realmRouter
                .route(ROUTE)
                .auditAs(Component.PUSH)
                .authenticateWith(none())
                .forVersion(1)
                .toRequestHandler(EQUALS, newHandler(snsMessageResource));
    }
}
