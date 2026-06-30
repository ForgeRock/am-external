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
 * Copyright 2026 Ping Identity Corporation.
 */
package com.sun.identity.cot;

import static com.sun.identity.cot.COTConstants.ACTIVE;
import static com.sun.identity.cot.COTConstants.INACTIVE;
import static com.sun.identity.cot.COTConstants.SAML2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance.configureSaml2;
import static org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance.resetConfiguration;

import java.util.Set;

import org.forgerock.guice.core.GuiceTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CircleOfTrustManager}, specifically verifying that the CoT status (active/inactive)
 * is correctly enforced when checking entity membership.
 */
public class CircleOfTrustManagerTest extends GuiceTestCase {

    private static final String REALM = "/";
    private static final String COT_NAME = "TestCOT";
    private static final String IDP_ENTITY_ID = "openam-saml2-idp";

    private static CircleOfTrustManager cotManager;

    @BeforeAll
    static void init() throws Exception {
        cotManager = new CircleOfTrustManager();
    }

    @BeforeEach
    void setUp() {
        resetConfiguration();
        COTCache.clear();
    }

    @AfterEach
    void tearDown() {
        resetConfiguration();
        COTCache.clear();
    }

    @Test
    void shouldReturnTrueForEntityInActiveCircleOfTrust() throws Exception {
        // Given
        configureSaml2(REALM, "/saml2/idp.xml", "/saml2/idp-extended.xml");
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(COT_NAME, REALM, ACTIVE));
        cotManager.addCircleOfTrustMember(REALM, COT_NAME, SAML2, IDP_ENTITY_ID, false);

        // When
        boolean result = cotManager.isInCircleOfTrust(REALM, COT_NAME, SAML2, IDP_ENTITY_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForEntityInInactiveCircleOfTrust() throws Exception {
        // Given
        configureSaml2(REALM, "/saml2/idp.xml", "/saml2/idp-extended.xml");
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(COT_NAME, REALM, INACTIVE));
        cotManager.addCircleOfTrustMember(REALM, COT_NAME, SAML2, IDP_ENTITY_ID, false);

        // When
        boolean result = cotManager.isInCircleOfTrust(REALM, COT_NAME, SAML2, IDP_ENTITY_ID);

        // Then
        assertThat(result)
                .as("Entity should not be found in an inactive Circle of Trust")
                .isFalse();
    }

    @Test
    void shouldReturnFalseForEntityNotInActiveCircleOfTrust() throws Exception {
        // Given
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(COT_NAME, REALM, ACTIVE));

        // When
        boolean result = cotManager.isInCircleOfTrust(REALM, COT_NAME, SAML2, "https://other.example.com");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldIncludeActiveCotsInAllActiveCirclesOfTrust() throws Exception {
        // Given
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor("ActiveCOT", REALM, ACTIVE));
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor("InactiveCOT", REALM, INACTIVE));

        // When
        Set activeCots = cotManager.getAllActiveCirclesOfTrust(REALM);

        // Then
        assertThat(activeCots).contains("ActiveCOT");
        assertThat(activeCots).doesNotContain("InactiveCOT");
    }

    @Test
    void shouldReturnActiveStatusFromGetCircleOfTrust() throws Exception {
        // Given
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(COT_NAME, REALM, ACTIVE));

        // When
        CircleOfTrustDescriptor descriptor = cotManager.getCircleOfTrust(REALM, COT_NAME, false);

        // Then
        assertThat(descriptor.getCircleOfTrustStatus()).isEqualToIgnoringCase(ACTIVE);
    }

    @Test
    void shouldReturnInactiveStatusFromGetCircleOfTrust() throws Exception {
        // Given
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(COT_NAME, REALM, INACTIVE));

        // When
        CircleOfTrustDescriptor descriptor = cotManager.getCircleOfTrust(REALM, COT_NAME, false);

        // Then
        assertThat(descriptor.getCircleOfTrustStatus()).isEqualToIgnoringCase(INACTIVE);
    }

    @Test
    void shouldFindEntityInActiveCoTWhenInactiveCoTAlsoContainsEntity() throws Exception {
        // Given
        configureSaml2(REALM, "/saml2/idp.xml", "/saml2/idp-extended.xml");
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(COT_NAME, REALM, INACTIVE));
        cotManager.addCircleOfTrustMember(REALM, COT_NAME, SAML2, IDP_ENTITY_ID, false);

        String activeCotName = COT_NAME + "-active";
        cotManager.createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(activeCotName, REALM, ACTIVE));
        cotManager.addCircleOfTrustMember(REALM, activeCotName, SAML2, IDP_ENTITY_ID, false);

        // When
        boolean inInactiveCot = cotManager.isInCircleOfTrust(REALM, COT_NAME, SAML2, IDP_ENTITY_ID);
        boolean inActiveCot = cotManager.isInCircleOfTrust(REALM, activeCotName, SAML2, IDP_ENTITY_ID);

        // Then
        assertThat(inInactiveCot)
                .as("Entity should NOT be found in the inactive Circle of Trust")
                .isFalse();
        assertThat(inActiveCot)
                .as("Entity should be found in the active Circle of Trust")
                .isTrue();
    }
}
