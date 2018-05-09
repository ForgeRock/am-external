/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.forgerock.util.i18n.PreferredLocales;

import com.amazonaws.util.StringUtils;

/**
 * Used when overriding properties files using configuration.
 */
public class LocaleSelector {
    /**
     * This is used for picking the best locale from a list of candidates.
     *
     * @param preferredLocales The users preferred locales.
     * @param candidates       The locales available to the system.
     * @return The best match. Returns null if there is no appropriate match.
     */
    public Locale getBestLocale(PreferredLocales preferredLocales, Collection<Locale> candidates) {
        List<Locale> preferred = preferredLocales.getLocales();
        for (Locale locale : preferredLocales.getLocales()) {
            if (candidates.contains(locale)) {
                return locale;
            }

            if (!StringUtils.isNullOrEmpty(locale.getVariant())) {
                locale = new Locale(locale.getLanguage(), locale.getCountry(), "");
                if (!preferred.contains(locale) && candidates.contains(locale)) {
                    return locale;
                }
            }

            if (!StringUtils.isNullOrEmpty(locale.getCountry())) {
                locale = new Locale(locale.getLanguage(), "", "");
                if (!preferred.contains(locale) && candidates.contains(locale)) {
                    return locale;
                }
            }
        }

        return null;
    }
}
