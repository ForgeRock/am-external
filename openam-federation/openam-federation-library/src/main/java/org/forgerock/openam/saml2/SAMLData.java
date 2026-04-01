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
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * SAMLData provides the raw SAML data objects that will be used internally within AM. SAMLScriptedBindingObject provides the
 * data that is safe to expose to scripts.
 */
public interface SAMLData {

    /**
     * Get the {@link AuthnRequest}.
     *
     * @return The {@link AuthnRequest} object.
     */
    AuthnRequest getRawAuthnRequest();

    /**
     * Get the {@link Subject}.
     *
     * @return The {@link Subject} object.
     */
    Subject getRawSubject();

    /**
     * Get the session index.
     *
     * @return The session index.
     */
    String getSessionIndex();

    /**
     * Get the {@link ResponseInfo}.
     *
     * @return The {@link ResponseInfo} object.
     */
    ResponseInfo getRawResponseInfo();

    /**
     * Get the {@link Assertion}.
     *
     * @return The {@link Assertion} object.
     */
    Assertion getRawAssertion();
}
