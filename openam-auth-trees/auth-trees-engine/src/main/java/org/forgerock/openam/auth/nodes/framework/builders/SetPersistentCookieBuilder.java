/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;

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
    public char[] hmacSigningKey() {
        return this.hmacKey;
    }
}
