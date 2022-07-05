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

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.forgerock.http.protocol.Cookie;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SetCustomCookieNodeTest {

    @Mock
    JsonValue sharedState;

    @Mock
    SetCustomCookieNode.Config config;

    @Mock
    HttpServletResponse servletResponse;

    @BeforeMethod
    private void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCookiePathFromConfig() throws Exception {
        givenOptionalConfigDefaults();
        given(config.path()).willReturn(Optional.of("test-path"));

        createAndProcessCustomCookieNode();

        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), contains("Path=test-path"));
    }

    @Test
    public void testCookieSameSiteFromConfig() throws Exception {
        givenOptionalConfigDefaults();
        given(config.sameSite()).willReturn(Cookie.SameSite.STRICT);

        createAndProcessCustomCookieNode();

        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), contains("SameSite=strict"));
    }

    @Test
    public void testCookieMaxLifeFromConfigAtEndOfSession() throws Exception {
        givenOptionalConfigDefaults();

        createAndProcessCustomCookieNode();

        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), not(contains("Max-Age=")));
        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), not(contains("Expires=")));
    }

    @Test
    public void testCookieMaxLifeFromConfigInFuture() throws Exception {
        givenOptionalConfigDefaults();
        given(config.maxAge()).willReturn(Optional.of(Duration.ofSeconds(600)));

        createAndProcessCustomCookieNode();

        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), contains("Max-Age="));
        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), contains("Expires="));
    }

    @Test
    public void testCookieUseSecureFlagTrueFromConfig() throws Exception {
        givenOptionalConfigDefaults();
        given(config.useSecureCookie()).willReturn(true);

        createAndProcessCustomCookieNode();

        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), contains("Secure"));
    }

    @Test
    public void testCookieUseSecureFlagFalseFromConfig() throws Exception {
        givenOptionalConfigDefaults();
        given(config.useSecureCookie()).willReturn(false);

        createAndProcessCustomCookieNode();

        Mockito.verify(servletResponse).addHeader(eq("Set-Cookie"), not(contains("Secure")));
    }

    private void givenOptionalConfigDefaults() {
        given(config.name()).willReturn("some cookie name");
        given(config.value()).willReturn("some cookie value");
        given(config.useSecureCookie()).willReturn(false);
        given(config.useHttpOnlyCookie()).willReturn(false);
        given(config.sameSite()).willReturn(Cookie.SameSite.LAX);
    }

    private void createAndProcessCustomCookieNode() throws NodeProcessException {
        SetCustomCookieNode node = new SetCustomCookieNode(config);
        TreeContext context = new TreeContext(sharedState,
                new ExternalRequestContext.Builder().servletResponse(servletResponse).build(),
                emptyList(), Optional.empty());
        node.process(context);
    }

}
