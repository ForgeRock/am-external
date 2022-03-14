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
 * Copyright 2018-2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.OpenAMSettings;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Test the message node.
 */
public class MessageNodeTest {

    private static final int DEFAULT_CHOICE_INDEX = 1;

    private static final LocaleSelector LOCALE_SELECTOR = new LocaleSelector();
    private static final Map<Locale, String> CUSTOM_YES = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSYes");
        put(Locale.GERMANY, "CustomGermanyYes");
        put(Locale.CANADA, "CustomCanadaYes");
        put(Locale.CHINESE, "CustomChineseYes");
    }};
    private static final Map<Locale, String> CUSTOM_NO = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSNo");
        put(Locale.CANADA, "CustomCanadaNo");
        put(Locale.CHINESE, "CustomChineseNo");
        put(Locale.GERMANY, "CustomGermanyNo");
    }};
    private static final Map<Locale, String> CUSTOM_MESSAGE = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSMessage");
        put(Locale.CHINESE, "CustomChineseMessage");
        put(Locale.GERMANY, "CustomGermanyMessage");
        put(Locale.CANADA, "CustomCanadaMessage");
    }};

    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final String CONFIRMATION_VARIABLE = "confirmationVariable";
    private static final Map<Locale, String> EMPTY_MAP = new HashMap<>() {{
        put(Locale.US, "");
    }};

    @Mock
    private MessageNode.Config config;
    private OpenAMSettings settings;
    private Realm realm;

    @DataProvider(name = "confirmations")
    public Object[][] confirmations() {
        return new Object[][]{
                {0, "true"},
                {1, "false"}
        };
    }

    @Test
    public void processSetResultInSharedStateWhenNoError() throws Exception {
        initialiseMockNodeConfig();

        //GIVEN
        when(config.stateField()).thenReturn(Optional.of("stateField"));

        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        Action action = messageNode.process(getContext());

        //THEN
        Assert.assertTrue(config.stateField().isPresent());
        Assert.assertTrue(action.sharedState.isDefined(config.stateField().get()));
        Assert.assertNotEquals(action.sharedState.get(config.stateField().get()), Optional.empty());

    }

    @Test
    public void processSetResultWithoutSharedStateWhenNoError() throws Exception {
        initialiseMockNodeConfig("");

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        Action action = messageNode.process(getContext());

        //THEN
        assertThat(action.outcome).isEqualTo(null);
    }

    @Test(dataProvider = "confirmations")
    public void shouldGetCorrectOutcomeForChoiceIndex(int index, String response) throws Exception {
        initialiseMockNodeConfig();

        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION,
                CUSTOM_MESSAGE.getOrDefault(Locale.US, ""));
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[]{CUSTOM_YES.getOrDefault(Locale.US, ""),
                        CUSTOM_NO.getOrDefault(Locale.US, "")}, DEFAULT_CHOICE_INDEX);
        confirmationCallback.setSelectedIndex(index);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        Action action = messageNode.process(getContext(arrayList));

        assertThat(action.outcome).isEqualTo(response);
        Assert.assertTrue(config.stateField().isPresent());
        assertThat(action.sharedState.get(config.stateField().get()).asInteger()).isEqualTo(index);
    }

    @Test(dataProvider = "confirmations")
    public void shouldGetCorrectOutcomeForChoiceIndexWithoutSharedState(int index, String response) throws Exception {
        initialiseMockNodeConfig("");

        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION,
                CUSTOM_MESSAGE.getOrDefault(Locale.US, ""));
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[]{CUSTOM_YES.getOrDefault(Locale.US, ""),
                        CUSTOM_NO.getOrDefault(Locale.US, "")}, DEFAULT_CHOICE_INDEX);
        confirmationCallback.setSelectedIndex(index);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        Action action = messageNode.process(getContext(arrayList));

        assertThat(action.outcome).isEqualTo(response);
    }

    @Test
    public void processNoException() throws Exception {
        initialiseMockNodeConfig();

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //no exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processThrowExceptionWhenEmptyMessage() throws Exception {
        initialiseMockNodeConfig(EMPTY_MAP);

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //throw an exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processThrowExceptionWhenEmptyYesMessage() throws Exception {
        initialiseMockNodeConfig(getNonEmptyMap("CustomYes"), EMPTY_MAP);

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //throw an exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void processThrowExceptionWhenEmptyNoMessage() throws Exception {
        initialiseMockNodeConfig(EMPTY_MAP, getNonEmptyMap("CustomNo"));

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //throw an exception
    }

    @Test
    public void processNoExceptionWhenEmptyStateField() throws Exception {
        initialiseMockNodeConfig("");

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //no exception
    }

    @Test
    public void shouldGetCorrectOutcomeForDefaultLocale() throws NodeProcessException {
        initialiseMockNodeConfig(Locale.GERMANY);

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        Action action = messageNode.process(getContext());

        //THEN
        assertThat(action.outcome).isEqualTo(null);
        assertThat(((TextOutputCallback) action.callbacks.get(0)).getMessage()).isEqualTo("CustomGermanyMessage");
    }

    @Test
    public void shouldGetCorrectOutcomeIfDefaultLocaleDoesNotExist() throws NodeProcessException {
        initialiseMockNodeConfig(Locale.ITALY);

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, LOCALE_SELECTOR, settings);

        //WHEN
        messageNode.process(getContext());

        //THEN
        // No exception thrown.
        // Cannot consistently predict outcome of default node as it picks the first element in an unordered map
    }

    private TreeContext getContext() {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    private TreeContext getContext(List<? extends Callback> callbacks) {
        return getContext(callbacks, json(object()));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), callbacks, Optional.empty());
    }

    private Map<Locale, String> getNonEmptyMap(String nonDefaultValue) {
        return new HashMap<>() {{
            put(Locale.US, nonDefaultValue);
        }};
    }

    private void initialiseMockNodeConfig(String confirmationVariable) {
        initialiseMockNodeConfig(CUSTOM_MESSAGE, CUSTOM_YES, CUSTOM_NO, confirmationVariable, DEFAULT_LOCALE);
    }

    private void initialiseMockNodeConfig() {
        initialiseMockNodeConfig(CUSTOM_MESSAGE, CUSTOM_YES, CUSTOM_NO, CONFIRMATION_VARIABLE, DEFAULT_LOCALE);
    }

    private void initialiseMockNodeConfig(Map<Locale, String> customMessage) {
        initialiseMockNodeConfig(customMessage, CUSTOM_YES, CUSTOM_NO, CONFIRMATION_VARIABLE, DEFAULT_LOCALE);
    }

    private void initialiseMockNodeConfig(Map<Locale, String> customYes, Map<Locale, String> customNo) {
        initialiseMockNodeConfig(CUSTOM_MESSAGE, customYes, customNo, CONFIRMATION_VARIABLE, DEFAULT_LOCALE);
    }

    private void initialiseMockNodeConfig(Locale defaultLocale) {
        initialiseMockNodeConfig(CUSTOM_MESSAGE, CUSTOM_YES, CUSTOM_NO, CONFIRMATION_VARIABLE, defaultLocale);
    }

    private void initialiseMockNodeConfig(
            Map<Locale, String> customMessage,
            Map<Locale, String> customYes,
            Map<Locale, String> customNo,
            String confirmationVariable,
            Locale defaultLocale
    ) {
        settings = mock(OpenAMSettings.class);
        config = mock(MessageNode.Config.class);
        realm = mock(Realm.class);
        given(config.message()).willReturn(customMessage);
        given(config.messageYes()).willReturn(customYes);
        given(config.messageNo()).willReturn(customNo);
        given(config.stateField()).willReturn(Optional.of(confirmationVariable));
        try {
            given(settings.getStringSetting(realm.asPath(), "iplanet-am-auth-locale"))
                    .willReturn(defaultLocale.toString());
        } catch (SSOException | SMSException e) {
            fail("Failed to initialise mock");
        }
    }
}