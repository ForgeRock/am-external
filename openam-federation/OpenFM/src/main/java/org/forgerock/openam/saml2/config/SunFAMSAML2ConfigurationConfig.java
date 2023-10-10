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
 * Copyright 2020-2023 ForgeRock AS.
 */

package org.forgerock.openam.saml2.config;

import static org.forgerock.openam.annotations.sm.Config.Scope.GLOBAL;
import static org.forgerock.openam.annotations.sm.Config.Scope.SERVICE;

import org.forgerock.am.config.GlobalConfiguration;
import org.forgerock.am.config.ServiceComponentConfig;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;
import org.forgerock.openam.annotations.sm.I18nKey;
import org.forgerock.openam.sm.annotations.TagSwappableDefault;
import org.forgerock.openam.sm.annotations.adapters.NumberRange;

/**
 * Service interface for configuring saml2 configuration.
 */
@Config(scope = SERVICE,
        name = "sunFAMSAML2Configuration",
        i18nFile = "famSAML2Configuration",
        resourceName = "saml2",
        descriptionKey = "sunFAMSAML2Configuration")
public interface SunFAMSAML2ConfigurationConfig extends ServiceComponentConfig,
        GlobalConfiguration<SunFAMSAML2ConfigurationConfig.Global> {

    /**
     * Global config interface holding the config for the saml2 service attributes.
     */
    @Config(scope = GLOBAL)
    interface Global {

        @Attribute(order = 100,
                name = "CacheCleanupInterval",
                resourceName = "cacheCleanupInterval",
                i18nKey = "a100")
        @NumberRange(rangeStart = 300)
        default Integer cacheCleanupInterval() {
            return 600;
        }

        @Attribute(order = 200,
                name = "NameIDInfoAttribute",
                resourceName = "nameIDInfoAttribute",
                i18nKey = "a101")
        default String nameIDInfoAttribute() {
            return "sun-fm-saml2-nameid-info";
        }

        @Attribute(order = 300,
                name = "NameIDInfoKeyAttribute",
                resourceName = "nameIDInfoKeyAttribute",
                i18nKey = "a102")
        default String nameIDInfoKeyAttribute() {
            return "sun-fm-saml2-nameid-infokey";
        }

        @Attribute(order = 400,
                name = "IDPDiscoveryCookieDomain",
                resourceName = "idpDiscoveryCookieDomain",
                i18nKey = "a103")
        @TagSwappableDefault("@COOKIE_DOMAIN@")
        String idpDiscoveryCookieDomain();

        @Attribute(order = 500,
                name = "IDPDiscoveryCookieType",
                resourceName = "idpDiscoveryCookieType",
                i18nKey = "a104")
        default SunFAMSAML2ConfigurationConfig.IdpDiscoveryCookieTypeChoice idpDiscoveryCookieType() {
            return SunFAMSAML2ConfigurationConfig.IdpDiscoveryCookieTypeChoice.PERSISTENT;
        }

        @Attribute(order = 600,
                name = "IDPDiscoveryURLScheme",
                resourceName = "idpDiscoveryUrlSchema",
                i18nKey = "a105")
        default SunFAMSAML2ConfigurationConfig.IdpDiscoveryUrlSchemaChoice idpDiscoveryUrlSchema() {
            return SunFAMSAML2ConfigurationConfig.IdpDiscoveryUrlSchemaChoice.HTTPS;
        }

        @Attribute(order = 700,
                name = "XMLEncryptionClass",
                resourceName = "xmlEncryptionClass",
                i18nKey = "a106")
        default String xmlEncryptionClass() {
            return "com.sun.identity.saml2.xmlenc.FMEncProvider";
        }

        @Attribute(order = 800,
                name = "EncryptedKeyInKeyInfo",
                resourceName = "encryptedKeyInKeyInfo",
                i18nKey = "a107")
        default boolean encryptedKeyInKeyInfo() {
            return true;
        }

        @Attribute(order = 900,
                name = "XMLSigningClass",
                resourceName = "xmlSigningClass",
                i18nKey = "a108")
        default String xmlSigningClass() {
            return "com.sun.identity.saml2.xmlsig.FMSigProvider";
        }

        @Attribute(order = 1000,
                name = "SigningCertValidation",
                resourceName = "signingCertValidation",
                i18nKey = "a109")
        default boolean signingCertValidation() {
            return false;
        }

        @Attribute(order = 1100,
                name = "CACertValidation",
                resourceName = "caCertValidation",
                i18nKey = "a110")
        default boolean caCertValidation() {
            return false;
        }

        @Attribute(order = 1300,
                name = "bufferLength",
                i18nKey = "a112")
        default Integer bufferLength() {
            return 2048;
        }
    }

    /**
     * Choice values for Idp Discovery Cookie.
     */
    enum IdpDiscoveryCookieTypeChoice {
        @I18nKey("PERSISTENT")
        PERSISTENT("PERSISTENT"),
        @I18nKey("SESSION")
        SESSION("SESSION");
        private final String name;

        IdpDiscoveryCookieTypeChoice(String name) {
            this.name = name;
        }
    }

    /**
     * Choice values for Idp Discovery Schema.
     */
    enum IdpDiscoveryUrlSchemaChoice {
        @I18nKey("http")
        HTTP("http"),
        @I18nKey("https")
        HTTPS("https");
        private final String name;

        IdpDiscoveryUrlSchemaChoice(String name) {
            this.name = name;
        }
    }
    }
