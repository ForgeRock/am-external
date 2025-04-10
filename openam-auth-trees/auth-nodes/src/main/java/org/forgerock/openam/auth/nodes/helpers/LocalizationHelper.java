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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.helpers;

import static com.sun.identity.shared.Constants.AM_LOCALE;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Helper class to deal with localized messages.
 */
public class LocalizationHelper implements LocalizedMessageProvider {

    private static final Logger logger = LoggerFactory.getLogger(LocalizationHelper.class);
    private static final String AM_AUTH_LOCALE = "iplanet-am-auth-locale";

    private final LocaleSelector localeSelector;
    private final OpenAMSettings authSettings;
    private final Realm realm;

    /**
     * The constructor.
     *
     * @param realm the realm.
     * @param localeSelector the locale selector support class.
     * @param authSettings an accessor for the AM auth settings.
     */
    @Inject
    LocalizationHelper(@Assisted Realm realm,
                              LocaleSelector localeSelector,
                              @Named("iPlanetAMAuthService") OpenAMSettings authSettings) {
        this.localeSelector = localeSelector;
        this.realm = realm;
        this.authSettings = authSettings;
    }

    @Override
    public String getLocalizedMessage(TreeContext context,
            Class<?> bundleClass,
            Map<Locale, String> localizations,
            String defaultMessageKey) {
        PreferredLocales preferredLocales = context.request.locales;
        if (localizations != null) {
            Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localizations.keySet());
            if (bestLocale != null) {
                return localizations.get(bestLocale);
            } else if (localizations.size() > 0) {
                List<Locale> systemLocales = new ArrayList<>();

                addRealmConfiguredLocaleIfPresent(systemLocales);
                addServerConfiguredLocaleIfPresent(systemLocales);

                PreferredLocales preferredSystemLocales = new PreferredLocales(systemLocales);
                Locale bestSystemLocale = localeSelector.getBestLocale(preferredSystemLocales, localizations.keySet());
                if (bestSystemLocale != null) {
                    return localizations.get(bestSystemLocale);
                }

                return localizations.get(localizations.keySet().iterator().next());
            }
        }

        if (bundleClass != null && StringUtils.isNotEmpty(defaultMessageKey)) {
            ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(bundleClass.getName(),
                    bundleClass.getClassLoader());
            return bundle.getString(defaultMessageKey);
        }

        return null;
    }

    @Override
    public String getLocalizedMessageWithDefault(TreeContext context, Class<?> bundleClass,
            Map<Locale, String> localizations, String defaultMessageKey, String defaultMessageValue) {
        try {
            return getLocalizedMessage(context, bundleClass, localizations, defaultMessageKey);
        } catch (MissingResourceException e) {
            logger.debug("No localized value found for key {}. Using default text, {}. {}",
                    defaultMessageKey, defaultMessageValue, e.getMessage());
            return defaultMessageValue;
        }
    }

    private void addRealmConfiguredLocaleIfPresent(List<Locale> systemLocales) {
        Locale realmLocale = null;
        try {
            String localeString = authSettings.getStringSetting(realm.asPath(), AM_AUTH_LOCALE);
            if (localeString != null && !localeString.equals("")) {
                realmLocale = com.sun.identity.shared.locale.Locale.getLocale(localeString);
            }
        } catch (SSOException | SMSException e) {
            logger.warn("Error attempting to retrieve the realm/global default locale");
        }
        if (realmLocale != null) {
            systemLocales.add(realmLocale);
        }
    }

    private void addServerConfiguredLocaleIfPresent(List<Locale> systemLocales) {
        String globalLocaleString = SystemProperties.get(AM_LOCALE);
        if (globalLocaleString != null) {
            Locale globalLocale = Locale.forLanguageTag(globalLocaleString.replace("_", "-"));
            if (globalLocale != null) {
                systemLocales.add(globalLocale);
            }
        }
    }

}
