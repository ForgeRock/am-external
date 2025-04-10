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
 * Copyright 2025 ForgeRock AS.
 */
/*
 *  Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.ReCaptchaNode;

/**
 * The ReCaptchaNode builder.
 */
@Deprecated
public class ReCaptchaNodeBuilder extends AbstractNodeBuilder implements ReCaptchaNode.Config {

    private String siteKey;
    private String secretKey;
    private String recaptchaUri;

    /**
     * The ReCaptchaNodeBuilder constructor.
     */
    public ReCaptchaNodeBuilder() {
        super("Legacy CAPTCHA", ReCaptchaNode.class);
    }

    /**
     * Sets the siteKey.
     * @param siteKey siteKey for reCAPTCHA.
     * @return the siteKey
     */
    public ReCaptchaNodeBuilder siteKey(String siteKey) {
        this.siteKey = siteKey;
        return this;
    }

    /**
     * Sets the secretKey.
     * @param secretKey the secretKey for reCAPTCHA.
     * @return the secretKey.
     */
    public ReCaptchaNodeBuilder secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    /**
     * Sets the verification reCaptchaUri.
     *
     * @param recaptchaUri the URI to verify the reCAPTCHA response.
     * @return the uri.
     */
    public ReCaptchaNodeBuilder reCaptchaUri(String recaptchaUri) {
        this.recaptchaUri = recaptchaUri;
        return this;
    }

    @Override
    public String siteKey() {
        return siteKey;
    }

    @Override
    public String secretKey() {
        return secretKey;
    }

    @Override
    public String reCaptchaUri() {
        return recaptchaUri;
    }
}
