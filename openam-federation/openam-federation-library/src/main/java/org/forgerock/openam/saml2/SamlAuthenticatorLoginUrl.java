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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.saml2;

import static org.forgerock.openam.entitlement.EntitlementConditionConstants.TRANSACTION_CONDITION_ADVICE;

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.transactions.core.TransactionService;

import com.sun.identity.saml2.common.SAML2Constants;

/**
 * Class to create the login URL which is where the SAML flows redirects the user to authenticate after the initial SAML
 * flow is initiated.
 */
public class SamlAuthenticatorLoginUrl {

    private final URIBuilder uriBuilder;

    /**
     * Construct a SamlAuthenticatorLoginUrl with an initial URL.
     *
     * @param url Initial URL
     * @throws URISyntaxException If the URI is invalid
     */
    public SamlAuthenticatorLoginUrl(String url) throws URISyntaxException {
        this.uriBuilder = new URIBuilder(url);
    }

    /**
     * Adds the supplied params to the URL.
     *
     * @param params A map of params.
     * @return the builder for continuing.
     */
    public SamlAuthenticatorLoginUrl addParams(Map<String, String> params) {
        if(params != null) {
            params.forEach(this::addParam);
        }
        return this;
    }

    /**
     * Adds the supplied entity to the URL with key {@value SAML2Constants#SPENTITYID}.
     *
     * @param spEntityID Entity to add.
     * @return the builder for continuing.
     */
    public SamlAuthenticatorLoginUrl addSpEntityId(String spEntityID) {
        if (spEntityID != null) {
            uriBuilder.addParameter(SAML2Constants.SPENTITYID, spEntityID);
        }
        return this;
    }

    /**
     * If supplied argument is true, then adds the "ForceAuth" parameter to the URL.
     *
     * @param isForceAuth Value of force auth.
     * @return the builder for continuing.
     */
    public SamlAuthenticatorLoginUrl addForceAuth(boolean isForceAuth) {
        if (isForceAuth) {
            uriBuilder.addParameter("ForceAuth", "true");
        }
        return this;
    }

    /**
     * Adds a "goto" param to the URL.
     *
     * @param url URL to redirect to after authentication
     * @return the builder for continuing.
     */
    public SamlAuthenticatorLoginUrl addGoto(String url) {
        uriBuilder.addParameter("goto", url);
        return this;
    }

    /**
     * Adds a transaction ID to the URL.
     *
     * @param transactionId Transaction ID to add.
     * @return the builder for continuing.
     */
    public SamlAuthenticatorLoginUrl addTransaction(String transactionId) {
        uriBuilder.addParameter("authIndexType", "composite_advice");
        uriBuilder.addParameter("authIndexValue", TransactionService
                .getAdviceString(TRANSACTION_CONDITION_ADVICE, transactionId));
        return this;
    }

    /**
     * Adds a parameter to the URL.
     *
     * @param key   Parameter key
     * @param value Parameter value
     */
    public SamlAuthenticatorLoginUrl addParam(String key, String value) {
        uriBuilder.addParameter(key, value);
        return this;
    }

    /**
     * Returns the constructed URL.
     *
     * @return the constructed URL
     */
    public String getUrl() {
        return uriBuilder.toString();
    }
}
