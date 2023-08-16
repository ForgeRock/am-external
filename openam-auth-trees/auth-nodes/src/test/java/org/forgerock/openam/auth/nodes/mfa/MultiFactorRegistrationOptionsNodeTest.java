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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.MFA_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.PUSH_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_GET_APP;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_GET_APP_OPTION_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_MANDATORY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_MESSAGE_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_OPT_OUT_OPTION_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_REGISTER_DEVICE_OPTION_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.DEFAULT_SKIP_STEP_OPTION_KEY;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.OutcomeProvider.GET_APP_OUTCOME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.OutcomeProvider.OPT_OUT_OUTCOME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode.OutcomeProvider.SKIP_OUTCOME;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

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
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MultiFactorRegistrationOptionsNodeTest {

    static final int DEFAULT_CHOICE_INDEX = 0;

    static final Map<Locale, String> MAP_DEFAULT_MESSAGE = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_MESSAGE_KEY);
    }};
    static final Map<Locale, String> MAP_DEFAULT_REGISTER_DEVICE_OPTION = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_REGISTER_DEVICE_OPTION_KEY);
    }};
    static final Map<Locale, String> MAP_DEFAULT_GET_APP_OPTION = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_GET_APP_OPTION_KEY);
    }};
    static final Map<Locale, String> MAP_DEFAULT_SKIP_STEP_OPTION = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_SKIP_STEP_OPTION_KEY);
    }};
    static final Map<Locale, String> MAP_DEFAULT_OPT_OUT_OPTION = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_OPT_OUT_OPTION_KEY);
    }};

    MultiFactorRegistrationOptionsNode node;

    @Mock
    private MultiFactorRegistrationOptionsNode.Config config;
    @Mock
    private LocaleSelector localeSelector;

    @DataProvider(name = "confirmations")
    public Object[][] confirmations() {
        return new Object[][]{
                {1, GET_APP_OUTCOME},
                {2, SKIP_OUTCOME},
                {3, OPT_OUT_OUTCOME}
        };
    }

    @DataProvider(name = "confirmationsWithoutGetApp")
    public Object[][] confirmationsWithoutGetApp() {
        return new Object[][]{
                {1, SKIP_OUTCOME},
                {2, OPT_OUT_OUTCOME}
        };
    }

    @BeforeMethod
    public void setup() {
        initMocks(this);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processThrowExceptionIfUserNameNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object());

        whenNodeConfigHasDefaultValues();

        // When
        node.process(getContext(sharedState));

        // Then
        // throw exception
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processThrowExceptionIfMFAMethodNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "rod")));

        whenNodeConfigHasDefaultValues();

        // When
        node.process(getContext(sharedState));

        // Then
        // throw exception
    }

    @Test
    public void processShouldReturnCorrectCallbacksDuringFirstPass() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    public void shouldDisplayAllRegistrationOptions() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );

        String[] deviceOptions = {DEFAULT_REGISTER_DEVICE_OPTION_KEY, DEFAULT_GET_APP_OPTION_KEY,
                DEFAULT_SKIP_STEP_OPTION_KEY, DEFAULT_OPT_OUT_OPTION_KEY};

        whenNodeConfigHasDefaultValues(false, true);

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        ConfirmationCallback callback = ((ConfirmationCallback) result.callbacks.get(1));
        assertThat(callback.getOptions()).isEqualTo(deviceOptions);
    }

    @Test
    public void shouldNotDisplaySkipAndOptOutOptions() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );
        String[] deviceOptions = {DEFAULT_REGISTER_DEVICE_OPTION_KEY, DEFAULT_GET_APP_OPTION_KEY};

        whenNodeConfigHasDefaultValues(true, true);

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        ConfirmationCallback callback = ((ConfirmationCallback) result.callbacks.get(1));
        assertThat(callback.getOptions()).isEqualTo(deviceOptions);
    }

    @Test
    public void shouldNotDisplayGetAppOption() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );
        String[] deviceOptions = {DEFAULT_REGISTER_DEVICE_OPTION_KEY,
                DEFAULT_SKIP_STEP_OPTION_KEY, DEFAULT_OPT_OUT_OPTION_KEY};

        whenNodeConfigHasDefaultValues(false, false);

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        ConfirmationCallback callback = ((ConfirmationCallback) result.callbacks.get(1));
        assertThat(callback.getOptions()).isEqualTo(deviceOptions);
    }

    @Test
    public void shouldOnlyDisplayRegisterOption() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );
        String[] deviceOptions = {DEFAULT_REGISTER_DEVICE_OPTION_KEY};

        whenNodeConfigHasDefaultValues(true, false);

        // When
        Action result = node.process(getContext(sharedState));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        ConfirmationCallback callback = ((ConfirmationCallback) result.callbacks.get(1));
        assertThat(callback.getOptions()).isEqualTo(deviceOptions);
    }

    @Test(dataProvider = "confirmations")
    public void shouldGetCorrectOutcomeForChoiceIndex(int index, String response) throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );

        TextOutputCallback textOutputCallback = new TextOutputCallback(
                TextOutputCallback.INFORMATION,
                MAP_DEFAULT_MESSAGE.getOrDefault(Locale.CANADA, "")
        );

        ConfirmationCallback confirmationCallback = new ConfirmationCallback(
                ConfirmationCallback.INFORMATION,
                new String[]{
                        MAP_DEFAULT_REGISTER_DEVICE_OPTION.getOrDefault(Locale.CANADA, ""),
                        MAP_DEFAULT_GET_APP_OPTION.getOrDefault(Locale.CANADA, ""),
                        MAP_DEFAULT_SKIP_STEP_OPTION.getOrDefault(Locale.CANADA, ""),
                        MAP_DEFAULT_OPT_OUT_OPTION.getOrDefault(Locale.CANADA, "")
                },
                DEFAULT_CHOICE_INDEX
        );
        confirmationCallback.setSelectedIndex(index);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        whenNodeConfigHasDefaultValues();

        // When
        Action action = node.process(getContext(arrayList, sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(response);
    }

    @Test(dataProvider = "confirmationsWithoutGetApp")
    public void shouldGetCorrectOutcomeForChoiceIndexWithoutGetApp(int index, String response) throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );

        TextOutputCallback textOutputCallback = new TextOutputCallback(
                TextOutputCallback.INFORMATION,
                MAP_DEFAULT_MESSAGE.getOrDefault(Locale.CANADA, "")
        );

        ConfirmationCallback confirmationCallback = new ConfirmationCallback(
                ConfirmationCallback.INFORMATION,
                new String[]{
                        MAP_DEFAULT_REGISTER_DEVICE_OPTION.getOrDefault(Locale.CANADA, ""),
                        MAP_DEFAULT_SKIP_STEP_OPTION.getOrDefault(Locale.CANADA, ""),
                        MAP_DEFAULT_OPT_OUT_OPTION.getOrDefault(Locale.CANADA, "")
                },
                DEFAULT_CHOICE_INDEX
        );
        confirmationCallback.setSelectedIndex(index);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        whenNodeConfigHasDefaultValues(false, false);

        // When
        Action action = node.process(getContext(arrayList, sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(response);
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState, new ExternalRequestContext.Builder().build(),
                emptyList(), Optional.empty());
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(
                sharedState, new ExternalRequestContext.Builder().build(), callbacks, Optional.empty()
        );
    }

    private void whenNodeConfigHasDefaultValues(boolean mandatory, boolean getApp) {
        whenNodeConfigHasDefaultValues(
                MAP_DEFAULT_MESSAGE,
                mandatory,
                getApp,
                MAP_DEFAULT_REGISTER_DEVICE_OPTION,
                MAP_DEFAULT_GET_APP_OPTION,
                MAP_DEFAULT_SKIP_STEP_OPTION,
                MAP_DEFAULT_OPT_OUT_OPTION
        );
    }

    private void whenNodeConfigHasDefaultValues() {
        whenNodeConfigHasDefaultValues(
                MAP_DEFAULT_MESSAGE,
                DEFAULT_MANDATORY,
                DEFAULT_GET_APP,
                MAP_DEFAULT_REGISTER_DEVICE_OPTION,
                MAP_DEFAULT_GET_APP_OPTION,
                MAP_DEFAULT_SKIP_STEP_OPTION,
                MAP_DEFAULT_OPT_OUT_OPTION
        );
    }

    private void whenNodeConfigHasDefaultValues(Map<Locale, String> message,
            boolean mandatory,
            boolean getApp,
            Map<Locale, String> registerDeviceLabel,
            Map<Locale, String> getAppLabel,
            Map<Locale, String> skipStepLabel,
            Map<Locale, String> optOutLabel) {
        config = mock(MultiFactorRegistrationOptionsNode.Config.class);
        given(config.message()).willReturn(message);
        given(config.mandatory()).willReturn(mandatory);
        given(config.getApp()).willReturn(getApp);
        given(config.registerDeviceLabel()).willReturn(registerDeviceLabel);
        given(config.getAppLabel()).willReturn(getAppLabel);
        given(config.skipStepLabel()).willReturn(skipStepLabel);
        given(config.optOutLabel()).willReturn(optOutLabel);

        node = spy(new MultiFactorRegistrationOptionsNode(config, localeSelector));
    }
}