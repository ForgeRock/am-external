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
 * Copyright 2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.helpers;

import static com.sun.identity.shared.Constants.AM_LOCALE;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Helper class to deal with localized messages.
 */
public class LocalizationHelper {

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
    public LocalizationHelper(@Assisted Realm realm,
                              LocaleSelector localeSelector,
                              @Named("iPlanetAMAuthService") OpenAMSettings authSettings) {
        this.localeSelector = localeSelector;
        this.realm = realm;
        this.authSettings = authSettings;
    }

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
     * @param context the context of the tree authentication.
     * @param bundleClass the bundle class.
     * @param localizations mapping of localisations to localised text.
     * @param defaultMessageKey default message key in the event we do not locate an appropriate locale.
     * @return a possibly null String containing the localized text.
     */
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
