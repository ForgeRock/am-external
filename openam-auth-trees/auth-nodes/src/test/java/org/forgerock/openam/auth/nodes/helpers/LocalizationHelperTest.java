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

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.OpenAMSettings;
import org.mockito.Mock;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test the Localization Helper.
 */
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
    private Realm realm;
    private LocaleSelector localeSelector;

    @Test
    public void shouldGetCorrectDefaultLocale() throws NodeProcessException {
        initialiseMockConfig();

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isNotNull();
        assertThat(message).isEqualTo("CustomCanadaMessage");
    }

    @Test
    public void shouldGetCorrectCustomLocale() throws NodeProcessException {
        initialiseMockConfig(Locale.CHINESE);

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isNotNull();
        assertThat(message).isEqualTo("CustomChineseMessage");
    }

    @Test
    public void shouldGetNoMessageLocaleDoesNotExist() throws NodeProcessException {
        initialiseMockConfig(Locale.ITALY);

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test
    public void shouldGetNoMessageMissingBundleClass() throws NodeProcessException {
        initialiseMockConfig(Locale.ITALY);

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), null,
                CUSTOM_MESSAGE_MAP, DEFAULT_KEY);

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test
    public void shouldGetNoMessageMissingDefaultKey() throws NodeProcessException {
        initialiseMockConfig(Locale.ITALY);

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                CUSTOM_MESSAGE_MAP, null);

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test(expectedExceptions = MissingResourceException.class)
    public void shouldThrowMissingResourceExceptionWhenNoLocalizationsAndNotInResourceBundle()
            throws NodeProcessException {
        initialiseMockConfig();

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

        //WHEN
        String message = localizationHelper.getLocalizedMessage(getContext(), LocalizationHelper.class,
                null, "test.not.present.in.file");

        //THEN
        assertThat(message).isEqualTo(null);
    }

    @Test
    public void shouldReturnDefaultValueWhenNoLocalizationsAndNotInResourceBundle()
            throws NodeProcessException {
        initialiseMockConfig();
        String defaultValue = "test.default.value";

        //GIVEN
        LocalizationHelper localizationHelper = new LocalizationHelper(realm, localeSelector, settings);

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

    private void initialiseMockConfig() {
        initialiseMockConfig(DEFAULT_LOCALE);
    }

    private void initialiseMockConfig(Locale defaultLocale) {
        settings = mock(OpenAMSettings.class);
        realm = mock(Realm.class);
        localeSelector = mock(LocaleSelector.class);
        given(localeSelector.getBestLocale(any(), any())).willReturn(defaultLocale);

        try {
            given(settings.getStringSetting(realm.asPath(), "iplanet-am-auth-locale"))
                    .willReturn(defaultLocale.toString());
        } catch (SSOException | SMSException e) {
            fail("Failed to initialise mock");
        }
    }

}
