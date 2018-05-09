/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.PersistentCookieDecisionNode;

/**
 * A PersistentCookieDecisionNode builder.
 */
public class PersistentCookieDecisionBuilder extends AbstractNodeBuilder implements
        PersistentCookieDecisionNode.Config {

    private char[] hmacKey;
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
    public PersistentCookieDecisionBuilder hmacKey(char[] hmacKey) {
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
    public char[] hmacSigningKey() {
        return this.hmacKey;
    }

    @Override
    public boolean useSecureCookie() {
        return this.useSecureCookie;
    }
}
