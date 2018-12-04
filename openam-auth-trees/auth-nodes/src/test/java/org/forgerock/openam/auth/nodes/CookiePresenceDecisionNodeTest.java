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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test the Cookie presence decision node.
 */
public class CookiePresenceDecisionNodeTest {

    private static final String DEFAULT_COOKIE_NAME = "My-Cookie";
    private static final String CUSTOM_COOKIE_NAME = "Custom-Cookie";
    private final static String TRUE_OUTCOME_ID = "true";
    private final static String FALSE_OUTCOME_ID = "false";
    private final static String COOKIE_VALUE = "cookie value";

    @Mock
    private CookiePresenceDecisionNode.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void processSetResultInSharedStateWhenNoError() throws Exception {
        //GIVEN
        whenNodeConfigHasValue(DEFAULT_COOKIE_NAME);
        CookiePresenceDecisionNode cookiePresenceDecisionNode = new CookiePresenceDecisionNode(config);

        //WHEN
        Action action = cookiePresenceDecisionNode.process(getContext());

        //THEN
        Assert.assertTrue(config.cookieName().length() > 0);
        assertThat(action.callbacks).isEmpty();
        assertThat(action.sharedState).isNullOrEmpty();
        assertThat(action.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }

    @Test
    public void shouldGetCorrectOutcomeForExistingDefaultCookie() throws Exception {
        //GIVEN
        whenNodeConfigHasValue(DEFAULT_COOKIE_NAME);
        CookiePresenceDecisionNode cookiePresenceDecisionNode = new CookiePresenceDecisionNode(config);

        //WHEN
        Map<String, String> cookie = ImmutableMap.of(DEFAULT_COOKIE_NAME, COOKIE_VALUE);
        Action action = cookiePresenceDecisionNode.process(getContext(cookie));

        //THEN
        assertThat(action.outcome).isEqualTo(TRUE_OUTCOME_ID);
    }

    @Test
    public void shouldGetCorrectOutcomeForNonExistingDefaultCookie() throws Exception {
        //GIVEN
        whenNodeConfigHasValue(DEFAULT_COOKIE_NAME);
        CookiePresenceDecisionNode cookiePresenceDecisionNode = new CookiePresenceDecisionNode(config);

        //WHEN
        Action action = cookiePresenceDecisionNode.process(getContext());

        //THEN
        assertThat(action.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }

    @Test
    public void shouldGetCorrectOutcomeForExistingCustomCookie() throws Exception {
        //GIVEN
        whenNodeConfigHasValue(CUSTOM_COOKIE_NAME);
        CookiePresenceDecisionNode cookiePresenceDecisionNode = new CookiePresenceDecisionNode(config);

        //WHEN
        Map<String, String> cookie = ImmutableMap.of(CUSTOM_COOKIE_NAME, COOKIE_VALUE);
        Action action = cookiePresenceDecisionNode.process(getContext(cookie));

        //THEN
        assertThat(action.outcome).isEqualTo(TRUE_OUTCOME_ID);
    }

    @Test
    public void shouldGetCorrectOutcomeForNotExistingCustomCookie() throws Exception {
        //GIVEN
        whenNodeConfigHasValue(CUSTOM_COOKIE_NAME);
        CookiePresenceDecisionNode cookiePresenceDecisionNode = new CookiePresenceDecisionNode(config);

        //WHEN
        Map<String, String> cookie = ImmutableMap.of(DEFAULT_COOKIE_NAME, COOKIE_VALUE);
        Action action = cookiePresenceDecisionNode.process(getContext(cookie));

        //THEN
        assertThat(action.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }

    @Test
    public void processNoExceptionWhenEmptyCookieName() throws Exception {
        whenNodeConfigHasDefaultValue();

        //GIVEN
        CookiePresenceDecisionNode cookiePresenceDecisionNode = new CookiePresenceDecisionNode(config);

        //WHEN
        Action action = cookiePresenceDecisionNode.process(getContext());

        //THEN
        assertThat(config.cookieName()).isNullOrEmpty();
    }

    private TreeContext getContext() {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().build(), emptyList());
    }

    private TreeContext getContext(Map<String, String> cookies) {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().cookies(cookies).build(), emptyList());
    }

    private void whenNodeConfigHasDefaultValue() {
        config = mock(CookiePresenceDecisionNode.Config.class);
    }

    private void whenNodeConfigHasValue(String cookieName) {
        config = mock(CookiePresenceDecisionNode.Config.class);
        given(config.cookieName()).willReturn(cookieName);
    }
}