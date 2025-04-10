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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.ProtocolFactory;

import java.io.Serializable;

/**
 * This class represents a copy of a AuthnRequestInfo in the service provider and
 * is used when in SAML2 failover mode to track AuthnRequest's between multiple instances
 * of OpenAM.
 * The key difference between AuthnRequestInfo and AuthnRequestInfoCopy is 
 * AuthnRequestInfoCopy only keeps those objects that can be serialized.
 * 
 * @author Mark de Reeper mark.dereeper@forgerock.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthnRequestInfoCopy implements Serializable {

    private String authnRequest;
    private String relayState;

    /**
     * Default constructor for deserialization.
     */
    public AuthnRequestInfoCopy() {
    }

    public AuthnRequestInfoCopy(AuthnRequestInfo info) throws SAML2Exception {
        // We store the XML representation of the AuthnRequest as it may not be serializable.
        this.authnRequest = info.getAuthnRequest().toXMLString(true, true);
        this.relayState = info.getRelayState();
    }

    public AuthnRequestInfo getAuthnRequestInfo() throws SAML2Exception {
        return new AuthnRequestInfo(ProtocolFactory.getInstance().createAuthnRequest(authnRequest), relayState);
    }    
}
