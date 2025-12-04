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

import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.SigningKey;

/**
 * A SetPersistentCookieNode builder.
 */
public class SetPersistentCookieBuilder extends AbstractNodeBuilder implements SetPersistentCookieNode.Config {

    private char[] hmacKey;
    private boolean useSecureCookie;

    /**
     * A SetPersistentCookieBuilder constructor.
     */
    public SetPersistentCookieBuilder() {
        super("Set Persistent Cookie", SetPersistentCookieNode.class);
    }

    /**
     * Sets the hmac key. Must be 256 bit, base 64 encoded value.
     *
     * @param hmacKey the hmac key.
     * @return this builder.
     */
    public SetPersistentCookieBuilder hmacKey(char[] hmacKey) {
        this.hmacKey = hmacKey;
        return this;
    }

    /**
     * Sets the use secure cookie boolean value. If set to true, it requests that the browser only uses the cookie
     * on secure connections.
     *
     * @param useSecureCookie the use secure cookie boolean value.
     * @return this builder.
     */
    public SetPersistentCookieBuilder useSecureCookie(boolean useSecureCookie) {
        this.useSecureCookie = useSecureCookie;
        return this;
    }

    @Override
    public boolean useSecureCookie() {
        return this.useSecureCookie;
    }

    @Override
    public Optional<char[]> hmacSigningKey() {
        return Optional.ofNullable(this.hmacKey);
    }

    @Override
    public Optional<Purpose<SigningKey>> signingKeyPurpose() {
        return Optional.empty();
    }
}
