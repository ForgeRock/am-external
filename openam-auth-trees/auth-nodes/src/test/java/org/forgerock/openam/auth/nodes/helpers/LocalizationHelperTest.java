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

package org.forgerock.openam.auth.nodes.helpers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.OpenAMSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test the Localization Helper.
 */
@ExtendWith(MockitoExtension.class)
public class LocalizationHelperTest {

    private static final String DEFAULT_KEY = "default.key";

    private static final Map<Locale, String> CUSTOM_MESSAGE_MAP = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSMessage");
        put(Locale.CHINESE, "CustomChineseMessage");
        put(Locale.GERMANY, "CustomGermanyMessage");
        put(Locale.CANADA, "CustomCanadaMessage");
    }};

    private static final Locale DEFAULT_LOCALE = Locale.CANADA;

    @Mock
    private OpenAMSettings settings;
    @Mock
    private Realm realm;
    @Mock
    private LocaleSelector localeSelector;

    @Test
    void shouldGetCorrectDefaultLocale() throws NodeProcessException {
        given(localeSelector.getBestLocale(any(), any())).willReturn(DEFAULT_LOCALE);

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isNotNull();
        assertThat(message).isEqualTo("CustomCanadaMessage");
    }

    @Test
    void shouldGetCorrectCustomLocale() throws NodeProcessException {
        given(localeSelector.getBestLocale(any(), any())).willReturn(Locale.CHINESE);

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isNotNull();
        assertThat(message).isEqualTo("CustomChineseMessage");
    }

    @Test
    void shouldGetNoMessageLocaleDoesNotExist() throws NodeProcessException {
        given(localeSelector.getBestLocale(any(), any())).willReturn(Locale.ITALY);

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test
    void shouldGetNoMessageMissingBundleClass() throws NodeProcessException {
        given(localeSelector.getBestLocale(any(), any())).willReturn(Locale.ITALY);

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), null,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test
    void shouldGetNoMessageMissingDefaultKey() throws NodeProcessException {
        given(localeSelector.getBestLocale(any(), any())).willReturn(Locale.ITALY);

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, null);

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test
    void shouldThrowMissingResourceExceptionWhenNoLocalizationsAndNotInResourceBundle()
            throws NodeProcessException {

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        assertThatThrownBy(() -> {
            localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                    null, "test.not.present.in.file");
        })
                //THEN
                .isInstanceOf(MissingResourceException.class)
                .hasMessage("Can't find bundle for base name "
                        + "org.forgerock.openam.auth.nodes.helpers.LocalizationHelper, locale ");
    }

    @Test
    void shouldReturnDefaultValueWhenNoLocalizationsAndNotInResourceBundle()
            throws NodeProcessException {
        String defaultValue = "test.default.value";

        //GIVEN
        LocalizedMessageProvider localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessageWithDefault(getContext(), LocalizationHelper.class,
                null, "test.not.present.in.file", defaultValue);

        //THEN
        assertThat(message).isEqualTo(defaultValue);
    }

    private TreeContext getContext() {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

}
