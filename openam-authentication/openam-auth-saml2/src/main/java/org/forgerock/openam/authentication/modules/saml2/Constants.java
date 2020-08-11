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
 * Copyright 2015-2019 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.saml2;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_ATTR_PREFIX_FORGEROCK;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_START;

import javax.security.auth.callback.TextOutputCallback;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

/**
 * Constants for the SAML2 SP SSO Auth Module.
 */
final class Constants {

    /**
     * The Name of the SAML 2 authentication module for debug logging purposes.
     */
    static final String AM_AUTH_SAML2 = "amAuthSAML2";

    /**
     * Arbitrary number of the max callbacks expected on an auth module's single step.
     */
    static final int MAX_CALLBACKS_INJECTED = 10;

    /**
     * Used for IdP-initiated SLO - whether the NameIdFormat was transient.
     */
    public static final String IS_TRANSIENT = "isTransient";

    /**
     * Used to store the key to the assertion and response for the post SSO work.
     */
    public static final String CACHE_KEY = "cacheKey";

    /**
     * Used for determining if the request went through an IdP Proxy.
     */
    public static final String REQUEST_ID = "requestId";

    /*
     * Auth Module States.
     */
    /** Auth Module state - starting state. */
    static final int START = LOGIN_START;
    /** Auth Module state - returning from redirect. */
    static final int REDIRECT = 2;
    /** Auth Module state - Performing local login. */
    static final int LOGIN_STEP = 3;
    /** Auth Module state - Error. */
    static final int STATE_ERROR = 4;

    /**
     * Auth Module Callback indexes.
     */
    static final int REDIRECT_CALLBACK = 0;

    /*
     * Auth Module Configuration XML Names.
     */
    /** Entity Name Module Configuration XML Name. */
    static final String ENTITY_NAME = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-entity-name";
    /** Meta Alias Module Configuration XML Name. */
    static final String META_ALIAS = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-meta-alias";
    /** Allow Create Module Configuration XML Name. */
    static final String ALLOW_CREATE = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-allow-create";
    /** Auth Comparison Module Configuration XML Name. */
    static final String AUTH_COMPARISON = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-auth-comparison";
    /** Authentication Context Class Reference Module Configuration XML Name. */
    static final String AUTHN_CONTEXT_CLASS_REF = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-authn-context-class-ref";
    /** Authentication Context Declaration Reference Module Configuration XML Name. */
    static final String AUTHN_CONTEXT_DECL_REF = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-authn-context-decl-ref";
    /** SAML2 Binding Module Configuration XML Name. */
    static final String BINDING = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-binding";
    /** Force Authentication Module Configuration XML Name. */
    static final String FORCE_AUTHN = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-force-authn";
    /** Is Passive Module Configuration XML Name. */
    static final String IS_PASSIVE = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-is-passive";
    /** Name ID Format Module Configuration XML Name. */
    static final String NAME_ID_FORMAT = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-name-id-format";
    /** Request Binding Module Configuration XML Name. */
    static final String REQ_BINDING = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-req-binding";
    /** Login Chain Module Configuration XML Name. */
    static final String LOCAL_CHAIN = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-login-chain";
    /** Single Log Out Enabled Module Configuration XML Name. */
    static final String SLO_ENABLED = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-slo-enabled";
    /** Single Logout Redirect Location Module Configuration XML Name. */
    static final String SLO_RELAY_STATE = AUTH_ATTR_PREFIX_FORGEROCK + "saml2-slo-relay";

    /**
     * Default Callback.
     */
    static final TextOutputCallback DEFAULT_CALLBACK = new ScriptTextOutputCallback("PLACEHOLDER");

    /**
     * Do not construct util classes.
     */
    private Constants() {
    }
}
