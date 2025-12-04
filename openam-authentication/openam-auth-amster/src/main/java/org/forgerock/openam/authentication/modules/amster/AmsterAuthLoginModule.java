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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.amster;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.authentication.modules.common.AuthLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * The {@link AuthLoginModule} for the {@link Amster} login module.
 * <p>
 *     This auth module supports authentication using established SSH keys, which involves signing a JWT that
 *     contains subject and expiration claims. The subject claim is interpreted as the username of the principal. This
 *     works as follows:
 * </p>
 * <ul>
 *     <li>
 *         The Amster client signs the JWT using a local private key, and the server verifies the signature using the
 *         list of public keys in the {@code $BASE_DIR/authorized_keys} (or other configured location) file for the OS
 *         user that is running OpenAM, by finding a key that matches the JWT's {@code kid} claim. If the entry in the
 *         authorized keys file contains a {@code from} parameter, then only connections that originate from a
 *         qualifying host/address will be permitted.
 *     </li>
 * </ul>
 */
public class AmsterAuthLoginModule extends AuthLoginModule {

    private static final String AUTHORIZED_KEYS_ATTR = "forgerock-am-auth-amster-authorized-keys";
    /** The attribute name for the Amster module's enabled setting. */
    public static final String ENABLED_ATTR = "forgerock-am-auth-amster-enabled";

    private final Logger debug;
    private Set<Key> keys;
    private String validatedUserId;
    private AuthorizedKeys authorizedKeys;
    private String nonce;
    private boolean enabled;

    /** Constructs an instance. Used by the {@link Amster} module in a server deployment environment. */
    AmsterAuthLoginModule() {
        this(LoggerFactory.getLogger(AmsterAuthLoginModule.class), new AuthorizedKeys());
    }

    /**
     * Constructs an instance. Used in a unit test environment.
     *
     * @param debug The debug instance to log to.
     */
    @VisibleForTesting
    AmsterAuthLoginModule(Logger debug, AuthorizedKeys authorizedKeys) {
        this.debug = debug;
        this.authorizedKeys = authorizedKeys;
    }

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        enabled = CollectionHelper.getBooleanMapAttr(options, ENABLED_ATTR, false);
        if (!enabled) {
            return;
        }
        String authorizedKeysFilename = CollectionHelper.getMapAttr(options, AUTHORIZED_KEYS_ATTR);
        try (InputStream authorizedKeys = authorizedKeysStream(authorizedKeysFilename)) {
            this.keys = this.authorizedKeys.read(authorizedKeys);
        } catch (IOException e) {
            debug.error("AmsterAuthLoginModule#init: Exception handing " + authorizedKeysFilename, e);
        }
        nonce = UUID.randomUUID().toString();
    }

    private InputStream authorizedKeysStream(String keysFile) {
        if (keysFile == null) {
            debug.error("AmsterAuthLoginModule#init: No value for " + AUTHORIZED_KEYS_ATTR);
        } else {
            try {
                return new BufferedInputStream(new FileInputStream(keysFile));
            } catch (IOException e) {
                debug.error("AmsterAuthLoginModule#init: Could not read authorized keys file " + keysFile, e);
            }
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        if (!enabled) {
            throw new LoginException("Login disabled");
        }
        if (callbacks != null && callbacks.length == 1 && (callbacks[0] instanceof HiddenValueCallback)) {
            String jwtString = ((HiddenValueCallback) callbacks[0]).getValue();
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
            String nonce = this.nonce;
            this.nonce = null;
            if (nonce.equals(jwt.getClaimsSet().get("nonce").asString())) {
                for (Key key : keys) {
                    if (key.isValid(jwt, getHttpServletRequest())) {
                        this.validatedUserId = jwt.getClaimsSet().getSubject();
                        return ISAuthConstants.LOGIN_SUCCEED;
                    }
                }
            }
        }
        throw new LoginException("Not authenticated");
    }

    @Override
    public Principal getPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return validatedUserId;
            }
        };
    }

    String getNonce() {
        return nonce;
    }
}
