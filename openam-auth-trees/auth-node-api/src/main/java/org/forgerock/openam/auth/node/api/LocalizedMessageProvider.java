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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.util.Locale;
import java.util.Map;

import org.forgerock.openam.core.realms.Realm;

/**
 * Provider class to deal with localization of messages, use {@link LocalizedMessageProviderFactory} to create
 * instances of this interface.
 */
public interface LocalizedMessageProvider {
    /**
     * Resolve the localized message value based on the available localizations and preferences.
     * <p>
     * This method will use the following order of precedence for the resolution of the locale
     * to use for a given message:
     * <ol>
     *     <li>Ordered list of Locales provided by the {@link TreeContext#request}</li>
     *     <li>Locales defined in the Realm Authentication Settings</li>
     *     <li>Locales defined at the Server level</li>
     *     <li>Catch all Default localisation key</li>
     * </ol>
     *
     * @param context           the context of the tree authentication.
     * @param bundleClass       the bundle class.
     * @param localizations     mapping of localisations to localised text.
     * @param defaultMessageKey default message key in the event we do not locate an appropriate locale.
     * @return a possibly null String containing the localized text.
     */
    String getLocalizedMessage(TreeContext context, Class<?> bundleClass, Map<Locale, String> localizations,
            String defaultMessageKey);

    /**
     * Resolve the localized message value based on the available localizations and preferences.
     * If no localizations exist and no key is found in any of the bundles, then return a default value.
     * <p>
     * This method will use the following order of precedence for the resolution of the locale
     * to use for a given message:
     * <ol>
     *     <li>Ordered list of Locales provided by the {@link TreeContext#request}</li>
     *     <li>Locales defined in the Realm Authentication Settings</li>
     *     <li>Locales defined at the Server level</li>
     *     <li>Default localisation key</li>
     * </ol>
     * and from the following sources, in order:
     * <ol>
     *     <li>The list of supplied localizations</li>
     *     <li>The resource bundle file</li>
     *     <li>The default message value</li>
     * </ol>
     *
     * @param context             the context of the tree authentication.
     * @param bundleClass         the bundle class.
     * @param localizations       mapping of localisations to localised text.
     * @param defaultMessageKey   default message key in the event we do not locate an appropriate locale.
     * @param defaultMessageValue default message value, in the event that no match is found for the key.
     * @return a String containing the localized text or default message value.
     */
    String getLocalizedMessageWithDefault(TreeContext context, Class<?> bundleClass,
            Map<Locale, String> localizations, String defaultMessageKey, String defaultMessageValue);

    /**
     * Factory interface for creating instances of {@link LocalizedMessageProvider}.
     */
    interface LocalizedMessageProviderFactory {
        /**
         * Create a new instance of {@link LocalizedMessageProvider}.
         *
         * @param realm the realm to use for the provider.
         * @return a new instance of {@link LocalizedMessageProvider}.
         */
        LocalizedMessageProvider create(Realm realm);
    }
}
