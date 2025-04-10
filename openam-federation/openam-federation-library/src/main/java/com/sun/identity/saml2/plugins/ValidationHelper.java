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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import static com.sun.identity.saml2.common.SAML2SDKUtils.bundle;

import com.sun.identity.saml2.common.SAML2Exception;

public class ValidationHelper {
    /**
     * Validates that the provided realm is not null. <br>
     *
     * @param realm the realm
     * @throws SAML2Exception if the realm is null
     */
    public void validateRealm(String realm) throws SAML2Exception {
        if (realm == null) {
            throw new SAML2Exception(bundle.getString("nullRealm"));
        }
    }

    /**
     * Validates that the provided remote entityID is not null. <br>
     *
     * @param remoteEntity the Remote Entity
     * @throws SAML2Exception if the remoteEntity is null
     */
    public void validateRemoteEntity(String remoteEntity) throws SAML2Exception {
        if (remoteEntity == null) {
            throw new SAML2Exception(bundle.getString("nullRemoteEntityID"));
        }
    }

    /**
     * Validates that the provided hosted entityID is not null. <br>
     *
     * @param hostedEntity the Hosted Entity
     * @throws SAML2Exception if the hostedEntity is null
     */
    public void validateHostedEntity(String hostedEntity) throws SAML2Exception {
        if (hostedEntity == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }
    }

    /**
     * Validates that the provided session is not null. <br>
     *
     * @param session the session
     * @throws SAML2Exception if the session is null
     */
    public void validateSession(Object session) throws SAML2Exception {
        if (session == null) {
            throw new SAML2Exception(bundle.getString("nullSSOToken"));
        }
    }

}
