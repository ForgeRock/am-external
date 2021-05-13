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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.am.saml2.impl;

/**
 * Constants used by the SAML2 Client implementation.
 *
 * @since AM 7.0.0
 */
public final class Saml2ClientConstants {

    /**
     * The name of the SAML2 Client resource bundle.
     */
    public static final String SAML2_CLIENT_BUNDLE_NAME = "Saml2Client";
    /**
     * Name of the cookie used to hold the current AM auth location.
     */
    public static final String AM_LOCATION_COOKIE = "authenticationStep";
    /**
     * Key for looking up the response key in the http query string.
     */
    public static final String RESPONSE_KEY = "responsekey";
    /**
     * Key for looking up the boolean error state from the http query string.
     */
    public static final String ERROR_PARAM_KEY = "error";
    /**
     * Key for looking up the error type from the http query string.
     */
    public static final String ERROR_CODE_PARAM_KEY = "errorCode";
    /**
     * Key for looking up the error message from the http query string.
     */
    public static final String ERROR_MESSAGE_PARAM_KEY = "errorMessage";
    /**
     * SAML failover error localisation key.
     */
    public static final String SAML_FAILOVER_ERROR_CODE = "samlFailoverError";
    /**
     * SAML verification error localisation key.
     */
    public static final String SAML_VERIFY_ERROR_CODE = "samlVerify";

    private Saml2ClientConstants() {
        throw new AssertionError("Should not be instantiated");
    }
}
