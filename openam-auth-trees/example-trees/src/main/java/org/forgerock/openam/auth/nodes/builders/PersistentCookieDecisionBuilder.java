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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import java.util.Optional;

import org.forgerock.openam.auth.nodes.PersistentCookieDecisionNode;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.SigningKey;

/**
 * A PersistentCookieDecisionNode builder.
 */
public class PersistentCookieDecisionBuilder extends AbstractNodeBuilder implements
        PersistentCookieDecisionNode.Config {

    private Optional<char[]> hmacKey;
    private boolean useSecureCookie;

    /**
     * A PersistentCookieDecisionBuilder constructor.
     */
    public PersistentCookieDecisionBuilder() {
        super("Persistent Cookie Decision", PersistentCookieDecisionNode.class);
    }

    /**
     * Sets the hmac key, used for signing. Must be 256 bit base64 encoded.
     *
     * @param hmacKey the hmac key.
     * @return this builder.
     */
    public PersistentCookieDecisionBuilder hmacKey(Optional<char[]> hmacKey) {
        this.hmacKey = hmacKey;
        return this;
    }

    /**
     * Sets the use secure cookie flag.
     *
     * @param useSecureCookie if true, the connection must be secure to use the cookie.
     * @return this builder.
     */
    public PersistentCookieDecisionBuilder useSecureCookie(boolean useSecureCookie) {
        this.useSecureCookie = useSecureCookie;
        return this;
    }

    @Override
    public Optional<char[]> hmacSigningKey() {
        return this.hmacKey;
    }

    @Override
    public Optional<Purpose<SigningKey>> signingKeyPurpose() {
        return Optional.empty();
    }

    @Override
    public boolean useSecureCookie() {
        return this.useSecureCookie;
    }
}
