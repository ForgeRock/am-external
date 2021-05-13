/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AuthnRequestInfo.java,v 1.2 2008/06/25 05:47:53 qcheng Exp $
 *
 * Portions Copyrighted 2015-2019 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * This class stores information about the request made to
 * the Service Provider.
 */
public class AuthnRequestInfo extends CacheObject {

    private final AuthnRequest authnRequest;
    private final String relayState;

    /**
     * Constructor creates the AuthnRequest Info for a request.
     * @param authnRequest the Authentication Request Object
     * @param relayState the Redirection URL on completion of Request.
     */

    public AuthnRequestInfo(AuthnRequest authnRequest, String relayState) {
        this.authnRequest = authnRequest;
        this.relayState = relayState;
        time = currentTimeMillis();
    }

    /**
     * Returns the <code>AuthnRequest</code> Object.
     *
     * @return the <code>AuthnRequest</code> Object.
     */
    public AuthnRequest getAuthnRequest() {
        return authnRequest;
    }

    /**
     * Returns the <code>RelayState</code> parameter value.
     *
     * @return the RelayState parameter value.
     */
    protected String getRelayState() {
        return relayState;
    }
}
