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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import static com.sun.identity.saml2.common.SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml2.common.SAML2AuthnConfig;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;
import com.sun.identity.saml2.protocol.impl.RequestedAuthnContextImpl;

@ExtendWith(MockitoExtension.class)
public class DefaultIDPAuthnContextMapperTest {

    private MockedStatic<Realms> mockedRealms;

    private static CircleOfTrustManager cotManager;
    private static final String TEST_COT = "TestCOT";

    // custom class refs defined in src/test/resources/saml2/idp-extended.xml
    private static final String CUSTOM_REF_SERVICE_MYTREE = "custom-reference-service-myTree";
    private static final String CUSTOM_REF_SERVICE_NOT_A_SERVICE = "custom-reference-notAService";
    private static final String TREE_NAME = "myTree";
    private static final String REALM = "testRealm";

    private static TreeProvider treeProvider;
    private static SAML2AuthnConfig saml2AuthnConfig;

    @Mock
    Realm realm;
    @Mock
    Tree tree;

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(TreeProvider.class, treeProvider)
            .addInstanceBinding(SAML2AuthnConfig.class, saml2AuthnConfig)
            .build();

    @BeforeAll
    static void init() throws Exception {
        cotManager = new CircleOfTrustManager();
        treeProvider = mock(TreeProvider.class);
        saml2AuthnConfig = mock(SAML2AuthnConfig.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        TestCaseConfigurationInstance.resetConfiguration();
        mockedRealms = Mockito.mockStatic(Realms.class);
    }

    @AfterEach
    void tearDown() {
        mockedRealms.close();
    }

    private static Object[][] comparisonData() {
        return new Object[][]{
                // Exact match - default requested is PasswordProtected (level 5), so expecting this
                {"exact", "testrealmexact", 5, CLASSREF_PASSWORD_PROTECTED_TRANSPORT},
                // Minimum match, not lower than requested (default of PasswordProtected),
                // so expecting PasswordProtected as that matches what was requested
                {"minimum", "testrealmmin", 5, CLASSREF_PASSWORD_PROTECTED_TRANSPORT},
                // Maximum level not more than requested (default of PasswordProtected), so expecting 5
                {"maximum", "testrealmmax", 5, CLASSREF_PASSWORD_PROTECTED_TRANSPORT},
                // Better than match. Default of PasswordProtected requested, so better than is Kerberos, level 10
                {"better", "testrealmbetter", 10, "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos"}
        };
    }

    @ParameterizedTest
    @MethodSource("comparisonData")
    public void testAuthnComparison(String comparison,
            String realmName,
            int expectedAuthnLevel,
            String expectedAuthnClassRef) throws Exception {
        // given
        AuthnRequest authnRequest = createAuthnRequest(comparison, CLASSREF_PASSWORD_PROTECTED_TRANSPORT);
        configureRealm(realmName);
        DefaultIDPAuthnContextMapper mapper = new DefaultIDPAuthnContextMapper();

        // when
        IDPAuthnContextInfo info = mapper.getIDPAuthnContextInfo(authnRequest, "openam-saml2-idp", realmName,
                "openam-saml2-sp");

        // then
        assertThat(info).isNotNull();
        assertThat(info.getAuthnContext()).isNotNull();
        assertThat(info.getAuthnContext().getAuthnContextClassRef()).isEqualTo(expectedAuthnClassRef);
        assertThat(info.getAuthnLevel()).isEqualTo(expectedAuthnLevel);
    }

    @Test
    void idpAuthnContextInfoRequiresRedirectToAuthIfTreeIsMustRun() throws Exception {
        // given
        AuthnRequest authnRequest = createAuthnRequest("exact", CUSTOM_REF_SERVICE_MYTREE);
        configureRealm();
        DefaultIDPAuthnContextMapper mapper = new DefaultIDPAuthnContextMapper();
        given(tree.mustRun()).willReturn(true);
        given(treeProvider.getTree(any(Realm.class), eq(TREE_NAME))).willAnswer(inv -> Optional.of(tree));
        mockedRealms.when(() -> Realms.of(any())).thenReturn(realm);

        // when
        IDPAuthnContextInfo info = mapper.getIDPAuthnContextInfo(authnRequest, "openam-saml2-idp", "testrealm",
                "openam-saml2-sp");

        // then
        assertThat(info.requiresRedirectToAuth).isTrue();
    }

    @Test
    void idpAuthnContextInfoDoesNotRequireRedirectToAuthIfTreeIsNotMustRun() throws Exception {
        // given
        AuthnRequest authnRequest = createAuthnRequest("exact", CUSTOM_REF_SERVICE_MYTREE);
        configureRealm();
        DefaultIDPAuthnContextMapper mapper = new DefaultIDPAuthnContextMapper();
        given(tree.mustRun()).willReturn(false);
        given(treeProvider.getTree(any(Realm.class), eq(TREE_NAME))).willAnswer(inv -> Optional.of(tree));
        mockedRealms.when(() -> Realms.of(any())).thenReturn(realm);

        // when
        IDPAuthnContextInfo info = mapper.getIDPAuthnContextInfo(authnRequest, "openam-saml2-idp", "testrealm",
                "openam-saml2-sp");

        // then
        assertThat(info.requiresRedirectToAuth).isFalse();
    }

    @Test
    void idpAuthnContextInfoDoesNotRequireRedirectIfTreeDoesNotExist() throws Exception {
        // given
        AuthnRequest authnRequest = createAuthnRequest("exact", CUSTOM_REF_SERVICE_MYTREE);
        configureRealm();
        DefaultIDPAuthnContextMapper mapper = new DefaultIDPAuthnContextMapper();
        given(treeProvider.getTree(any(Realm.class), eq(TREE_NAME))).willReturn(Optional.empty());
        mockedRealms.when(() -> Realms.of(any())).thenReturn(realm);

        // when
        IDPAuthnContextInfo info = mapper.getIDPAuthnContextInfo(authnRequest, "openam-saml2-idp", "testrealm",
                "openam-saml2-sp");

        // then
        assertThat(info.requiresRedirectToAuth).isFalse();
    }

    @Test
    void idpAuthnContextInfoDoesNotRequireRedirectIfRedirectIsNotToAService() throws Exception {
        // given
        AuthnRequest authnRequest = createAuthnRequest("exact", CUSTOM_REF_SERVICE_NOT_A_SERVICE);
        configureRealm();
        DefaultIDPAuthnContextMapper mapper = new DefaultIDPAuthnContextMapper();

        // when
        IDPAuthnContextInfo info = mapper.getIDPAuthnContextInfo(authnRequest, "openam-saml2-idp", "testrealm",
                "openam-saml2-sp");

        // then
        assertThat(info.requiresRedirectToAuth).isFalse();
    }

    private void configureRealm() throws Exception {
        configureRealm(REALM);
    }

    private void configureRealm(String realm) throws Exception {
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, COTConstants.ACTIVE));
        // Load IDP metadata
        TestCaseConfigurationInstance.configureSaml2(realm, "/saml2/idp.xml", "/saml2/idp-extended.xml");
    }

    private AuthnRequest createAuthnRequest(String comparison, String requestedContext) throws Exception {
        RequestedAuthnContext authnContext = new RequestedAuthnContextImpl();
        authnContext.setComparison(comparison);
        authnContext.setAuthnContextClassRef(List.of(requestedContext));
        AuthnRequest authnRequest = new AuthnRequestImpl();
        authnRequest.setRequestedAuthnContext(authnContext);
        return authnRequest;
    }
}
