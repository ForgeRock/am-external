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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.openam.annotations.sm.Attribute;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_ISSUER;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_BG_COLOR;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_GENERATE_RECOVERY_CODES;

/**
 * Common Config interface for MFA nodes.
 */
public interface MultiFactorCommonConfig {

    /**
     * Specifies the name of the issuer.
     *
     * @return issuer name as string.
     */
    @Attribute(order = 10, validators = {RequiredValueValidator.class})
    default String issuer() {
        return DEFAULT_ISSUER;
    }

    /**
     * Specifies the attribute to be used as account name.
     *
     * @return account name as string.
     */
    @Attribute(order = 20)
    default AbstractMultiFactorNode.UserAttributeToAccountNameMapping accountName() {
        return AbstractMultiFactorNode.UserAttributeToAccountNameMapping.USERNAME;
    }

    /**
     * Background color of entry in ForgeRock Authenticator app.
     *
     * @return the hexadecimal color value as string.
     */
    @Attribute(order = 30)
    default String bgColor() {
        return DEFAULT_BG_COLOR;
    }

    /**
     * URL of a logo image resource associated with the Issuer.
     *
     * @return the URL of the logo image resource.
     */
    @Attribute(order = 40)
    default String imgUrl() {
        return "";
    }

    /**
     * Specifies whether to generate recovery codes and store them in the device profile.
     *
     * @return true if the codes are to be generated.
     */
    @Attribute(order = 50)
    default boolean generateRecoveryCodes() {
        return DEFAULT_GENERATE_RECOVERY_CODES;
    }

    /**
     * The message to displayed to user to scan the QR code.
     * @return The mapping of locales to scan QR code messages.
     */
    @Attribute(order = 60)
    default Map<Locale, String> scanQRCodeMessage() {
        return Collections.emptyMap();
    }

}
