/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.testng.annotations.Test;

public class UsernameCollectorNodeTest {
    UsernameCollectorNode node = new UsernameCollectorNode();

    @Test
    public void testProcessWithNoCallbacksReturnsASingleCallback() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        PreferredLocales preferredLocales = mock(PreferredLocales.class);
        ResourceBundle resourceBundle = new PasswordCollectorNodeTest.MockResourceBundle("Username");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action result = node.process(getContext(sharedState, preferredLocales, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) result.callbacks.get(0)).getPrompt()).isEqualTo("Username");
        assertThat((Object) result.sharedState).isNull();
        assertThat(sharedState).isObject().containsExactly(entry("initial", "initial"));
    }

    private TreeContext getContext(JsonValue sharedState, PreferredLocales preferredLocales,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, new Builder().locales(preferredLocales).build(), callbacks);
    }

    @Test
    public void testProcessWithCallbacksAddsToState() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        NameCallback callback = new NameCallback("prompt");
        callback.setName("bob");

        // When
        Action result = node.process(getContext(sharedState, new PreferredLocales(), singletonList(callback)));

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState).isObject().contains(USERNAME, "bob").contains("initial", "initial");
        assertThat(sharedState).isObject().containsExactly(entry("initial", "initial"));
    }
}
