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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.net.URI;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.annotations.Supported;

/**
 * This handler interface allows authentication nodes to suspend authentication and send a unique ID out of band to the
 * end-user. Once the user provides this token ID to the authentication framework, the authentication process will be
 * resumed and the currently executing node will be able to continue processing the authentication request.
 */
@FunctionalInterface
@Supported
public interface SuspensionHandler {

    /**
     * Handles a suspension request by sending the suspension ID out of band to the end-user to allow them to continue
     * the authentication flow.
     *
     * @param resumeURI the URI that will be used to resume authentication.
     * @return The {@link Callback} describes the outcome having enacted the out of bounds action.
     * @throws NodeProcessException If there was an unrecoverable error occurring while sending the suspension ID to the
     * end-user. This will result in a failed authentication from the user's perspective.
     */
    Callback handle(URI resumeURI) throws NodeProcessException;

}
