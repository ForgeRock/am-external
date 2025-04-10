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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.events;

import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event submitted to the event bus when a radius request is accepted and an ACCESS-ACCEPT message has been returned to
 * the client.
 */
public class AuthRequestAcceptedEvent extends AcceptedRadiusEvent {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRequestAcceptedEvent.class);

    /**
     * Constructor.
     *
     * @param request the request associated with the event
     * @param response the response to the request (if available), null if not.
     * @param context the context in which the request was received.
     */
    public AuthRequestAcceptedEvent(RadiusRequest request, RadiusResponse response, RadiusRequestContext context) {
        super(request, response, context);
        LOG.debug("Constructed AuthRequestAcceptedEvent.AuthRequestAcceptedEvent()");

    }

}
