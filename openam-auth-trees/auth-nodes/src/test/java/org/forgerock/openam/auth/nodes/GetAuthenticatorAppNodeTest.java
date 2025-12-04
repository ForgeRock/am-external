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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.GetAuthenticatorAppNode.APPLE_APP_LINK;
import static org.forgerock.openam.auth.nodes.GetAuthenticatorAppNode.GOOGLE_APP_LINK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

@ExtendWith(MockitoExtension.class)
public class GetAuthenticatorAppNodeTest {

    static final String MESSAGE = "Get the app from the {{appleLink}} or on {{googleLink}}";
    static final String CONTINUE_LABEL = "Continue";
    static final Map<Locale, String> MAP_DEFAULT_MESSAGE = new HashMap<>() {{
        put(Locale.CANADA, MESSAGE);
    }};
    static final Map<Locale, String> MAP_CONTINUE_LABEL = new HashMap<>() {{
        put(Locale.CANADA, CONTINUE_LABEL);
    }};
    static final Locale DEFAULT_LOCALE = Locale.CANADA;
    @Mock
    GetAuthenticatorAppNode.Config config;
    GetAuthenticatorAppNode node;
    @Mock
    private LocaleSelector localeSelector;

    @Test
    void processNoException() throws Exception {
        // Given
        whenNodeConfigHasDefaultValues();

        GetAuthenticatorAppNode node = new GetAuthenticatorAppNode(config, localeSelector);

        // When
        node.process(getContext());

        // Then
        // no exception
    }

    @Test
    void processShouldReturnCorrectCallbacksDuringFirstPass() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void shouldDisplayCorrectOptions() throws Exception {
        // Given
        String[] options = {CONTINUE_LABEL};

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        ConfirmationCallback callback = ((ConfirmationCallback) result.callbacks.get(1));
        assertThat(callback.getOptions()).isEqualTo(options);
    }

    @Test
    void processShouldContinueWhenSelected() throws Exception {
        // Given
        whenNodeConfigHasDefaultValues();

        ScriptTextOutputCallback textOutputCallback = new ScriptTextOutputCallback("placeholder");

        ConfirmationCallback confirmationCallback = new ConfirmationCallback(
                ConfirmationCallback.INFORMATION,
                new String[]{
                        MAP_CONTINUE_LABEL.getOrDefault(Locale.CANADA, "")
                },
                0
        );
        confirmationCallback.setSelectedIndex(0);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        // When
        Action result = node.process(getContext());

        // Then
        assertThat(result.sharedState).isNullOrEmpty();
        assertThat(result.transientState).isNullOrEmpty();
        assertThat(result.outcome).isNullOrEmpty();
    }

    @Test
    void shouldDisplayCorrectDefaultMessage() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        String expectedMessage = "<center>Get the app from the <a target='_blank' "
                + "href='" + APPLE_APP_LINK + "'>Apple App Store</a> or on "
                + "<a target='_blank' href='" + GOOGLE_APP_LINK + "'>"
                + "Google Play Store</a></center>";

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        String message = ((ScriptTextOutputCallback) result.callbacks.get(0)).getMessage();
        assertThat(message).contains(expectedMessage);
    }

    @Test
    void shouldDisplayCorrectMessageWithLabels() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        String expectedMessage = "<center>Apple: <a target='_blank' "
                + "href='" + APPLE_APP_LINK + "'>Apple App Store</a>"
                + "</center>";

        String message = "Apple: <a target='_blank' href='{{appleLink}}'>{{appleLabel}}</a>";
        Map<Locale, String> customGetAppMessage = new HashMap<>() {{
            put(Locale.CANADA, message);
        }};

        whenNodeConfigHasDefaultValues(customGetAppMessage, MAP_CONTINUE_LABEL,
                APPLE_APP_LINK, GOOGLE_APP_LINK, DEFAULT_LOCALE);


        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        String resultMessage = ((ScriptTextOutputCallback) result.callbacks.get(0)).getMessage();
        assertThat(resultMessage).contains(expectedMessage);
    }

    @Test
    void shouldDisplayCorrectLocalizedMessage() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        String message = "Holen Sie sich die App von {{appleLink}} oder auf {{googleLink}}";
        Map<Locale, String> customGetAppMessage = new HashMap<>() {{
            put(Locale.GERMANY, message);
        }};
        Map<Locale, String> continueLabel = new HashMap<>() {{
            put(Locale.GERMANY, "Fortsetzen");
        }};

        whenNodeConfigHasDefaultValues(customGetAppMessage, continueLabel,
                APPLE_APP_LINK, GOOGLE_APP_LINK, Locale.GERMANY);

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        assertThat(((ScriptTextOutputCallback) result.callbacks.get(0)).getMessage())
                .contains("Holen Sie sich die App von");
    }

    private TreeContext getContext() {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(
                sharedState, transientState, new ExternalRequestContext.Builder().build(), callbacks, Optional.empty()
        );
    }

    private void whenNodeConfigHasDefaultValues() {
        whenNodeConfigHasDefaultValues(MAP_DEFAULT_MESSAGE, MAP_CONTINUE_LABEL,
                APPLE_APP_LINK, GOOGLE_APP_LINK, DEFAULT_LOCALE);
    }

    private void whenNodeConfigHasDefaultValues(
            Map<Locale, String> message,
            Map<Locale, String> continueLabel,
            String appleLink,
            String googleLink,
            Locale locale) {
        config = mock(GetAuthenticatorAppNode.Config.class);
        given(config.message()).willReturn(message);
        given(config.continueLabel()).willReturn(continueLabel);
        given(config.appleLink()).willReturn(appleLink);
        given(config.googleLink()).willReturn(googleLink);

        localeSelector = mock(LocaleSelector.class);
        given(localeSelector.getBestLocale(any(), any())).willReturn(locale);

        node = spy(
                new GetAuthenticatorAppNode(
                        config,
                        localeSelector
                )
        );
    }

}
