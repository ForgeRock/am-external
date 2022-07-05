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
 * Copyright 2021 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

public class ConditionsImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:Conditions xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "NotBefore=\"1970-01-12T13:46:40Z\" NotOnOrAfter=\"1970-01-24T03:33:20Z\">" +
                        "<saml:Condition/>" +
                        "<saml:AudienceRestriction>" +
                        "<saml:Audience>a</saml:Audience><saml:Audience>b</saml:Audience>" +
                        "</saml:AudienceRestriction>" +
                        "<saml:OneTimeUse/>" +
                        "<saml:ProxyRestriction Count=\"2\">" +
                        "<saml:Audience>a</saml:Audience>" +
                        "<saml:Audience>b</saml:Audience>" +
                        "<saml:Audience>c</saml:Audience>" +
                        "</saml:ProxyRestriction></saml:Conditions>" },
                { true, false, "<saml:Conditions " +
                        "NotBefore=\"1970-01-12T13:46:40Z\" NotOnOrAfter=\"1970-01-24T03:33:20Z\">" +
                        "<saml:Condition/>" +
                        "<saml:AudienceRestriction>" +
                        "<saml:Audience>a</saml:Audience><saml:Audience>b</saml:Audience>" +
                        "</saml:AudienceRestriction>" +
                        "<saml:OneTimeUse/>" +
                        "<saml:ProxyRestriction Count=\"2\">" +
                        "<saml:Audience>a</saml:Audience>" +
                        "<saml:Audience>b</saml:Audience>" +
                        "<saml:Audience>c</saml:Audience>" +
                        "</saml:ProxyRestriction></saml:Conditions>" },
                { false, false, "<Conditions " +
                        "NotBefore=\"1970-01-12T13:46:40Z\" NotOnOrAfter=\"1970-01-24T03:33:20Z\">" +
                        "<Condition/>" +
                        "<AudienceRestriction>" +
                        "<Audience>a</Audience><Audience>b</Audience>" +
                        "</AudienceRestriction>" +
                        "<OneTimeUse/>" +
                        "<ProxyRestriction Count=\"2\">" +
                        "<Audience>a</Audience>" +
                        "<Audience>b</Audience>" +
                        "<Audience>c</Audience>" +
                        "</ProxyRestriction></Conditions>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ConditionsImpl conditions = new ConditionsImpl();
        conditions.setConditions(List.of(new ConditionImpl()));
        conditions.setOneTimeUses(List.of(new OneTimeUseImpl())); // Why a list??!
        AudienceRestrictionImpl audienceRestriction = new AudienceRestrictionImpl();
        audienceRestriction.setAudience(List.of("a", "b"));
        conditions.setAudienceRestrictions(List.of(audienceRestriction));
        ProxyRestrictionImpl proxyRestriction = new ProxyRestrictionImpl();
        proxyRestriction.setCount(2);
        proxyRestriction.setAudience(List.of("a", "b", "c"));
        conditions.setProxyRestrictions(List.of(proxyRestriction));
        conditions.setNotBefore(new Date(1000000000L));
        conditions.setNotOnOrAfter(new Date(2000000000L));

        // When
        String xml = conditions.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}