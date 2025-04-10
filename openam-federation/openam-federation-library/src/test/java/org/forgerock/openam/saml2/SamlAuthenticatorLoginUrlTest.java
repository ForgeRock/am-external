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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.util.Set;

import org.forgerock.guice.core.GuiceBind;
import org.forgerock.guice.core.GuiceExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;

@ExtendWith({MockitoExtension.class, GuiceExtension.class})
class SamlAuthenticatorLoginUrlTest {

    @Mock
    @GuiceBind(SystemPropertiesWrapper.class)
    SystemPropertiesWrapper systemProperties;


    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldContainSPEntityIDWhenDataHasSPEntityID(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {

        // given
        var authenticator = new SamlAuthenticatorLoginUrl(baseUrl);

        //when
        authenticator
            .addSpEntityId("testEntityId");

        //then
        var result = authenticator.getUrl();
        assertBaseUrl(result, baseUrl);
        assertThat(result).contains(expectedQueryParamSeparator + "spEntityID=testEntityId");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldNotContainSPEntityIDWhenDataHasNoSPEntityID(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {

        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl).getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).doesNotContain(expectedQueryParamSeparator + "spEntityID=testEntityId");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldHaveAuthServiceWhenTreeIsConfigured(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {
        //given
        var treeName = "testTree";
        var contextInfo = new IDPAuthnContextInfo(null, Set.of("service=" + treeName), null);

        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addParams(contextInfo.getAuthnTypeAndValuesAsMap())
            .getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).contains(expectedQueryParamSeparator + "service=" + treeName);
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldNotHaveAuthServiceWhenTreeIsNotConfigured(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {
        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl).getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).doesNotContain(expectedQueryParamSeparator + "service=");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldHaveCorrectContextInfoWhenMultipleContextInfoConfiguredAndNoTreeIsAssociated(String baseUrl,
        String expectedQueryParamSeparator) throws URISyntaxException {

        //given
        var contextInfo = new IDPAuthnContextInfo(null, Set.of("type1=value1", "type2=value2=abc"), null);

        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addParams(contextInfo.getAuthnTypeAndValuesAsMap())
            .getUrl();

        //then
        assertThat(result).startsWith(baseUrl + expectedQueryParamSeparator);
        assertThat(result).contains("type1=value1");
        assertThat(result).contains("type2=value2%3Dabc");
        assertThat(result).doesNotContain("service=");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldHaveCorrectEncodingInUri(String baseUrl, String expectedQueryParamSeparator) throws URISyntaxException {
        //given
        var contextInfo = new IDPAuthnContextInfo(null, Set.of("type1=value!@#$%^&*()_+{}|:<>?"), null);

        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addParams(contextInfo.getAuthnTypeAndValuesAsMap())
            .getUrl();

        //then
        assertThat(result).startsWith(baseUrl + expectedQueryParamSeparator);
        assertThat(result).contains("type1=value%21%40%23%24%25%5E%26*%28%29_%2B%7B%7D%7C%3A%3C%3E%3F");
        assertThat(result).doesNotContain("service=");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldHaveContextInfoWhenContextInfoConfiguredAndNoTreeIsAssociated(String baseUrl,
        String expectedQueryParamSeparator) throws URISyntaxException {
        //given
        var contextInfo = new IDPAuthnContextInfo(null, Set.of("type=value"), null);

        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addParams(contextInfo.getAuthnTypeAndValuesAsMap())
            .getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).contains(expectedQueryParamSeparator + "type=value");
        assertThat(result).doesNotContain("service=");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldNotHaveContextInfoWhenContextInfoConfiguredAndTreeIsAssociated(String baseUrl,
        String expectedQueryParamSeparator) throws URISyntaxException {
        //given
        var treeName = "testTree";
        var contextInfo = new IDPAuthnContextInfo(null, Set.of("service=" + treeName), null);
        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addParams(contextInfo.getAuthnTypeAndValuesAsMap())
            .getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).contains(expectedQueryParamSeparator + "service=" + treeName);
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldHaveForceAuthWhenSessionUpgradeIsTrue(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {
        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addForceAuth(true)
            .getUrl();
        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).contains(expectedQueryParamSeparator + "ForceAuth=true");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldNotHaveForceAuthWhenSessionUpgradeIsFalse(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {
        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl).getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).doesNotContain(expectedQueryParamSeparator + "ForceAuth=true");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldHaveCorrectQueryParamSeparator(String baseUrl, String expectedQueryParamSeparator)
        throws URISyntaxException {
        //when
        String result = new SamlAuthenticatorLoginUrl(baseUrl)
            .addSpEntityId("foo")
            .getUrl();

        //then
        assertBaseUrl(result, baseUrl);
        assertThat(result).contains(baseUrl + expectedQueryParamSeparator + "spEntityID");
    }

    @ParameterizedTest
    @CsvSource({"https://test.com, ?",
        "https://test.com?realm=%2F, &"})
    void shouldThrowExceptionWhenUriConstructionFails() {
        assertThatThrownBy(() ->
            // This will throw an exception because the URI is invalid (missing a scheme)
            new SamlAuthenticatorLoginUrl("://test.com")
        ).isInstanceOf(URISyntaxException.class);
    }

    private static void assertBaseUrl(String result, String baseUrl) {
        assertThat(result).startsWith(baseUrl);
    }
}
