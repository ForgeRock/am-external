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
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.APPLICATION_CONTEXT_ENABLED;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.stream.Stream;

import org.forgerock.guice.core.GuiceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.saml2.common.SAML2Utils;

@ExtendWith(MockitoExtension.class)
public class IDPSSOUtilTest {
    private MockedStatic<SAML2Utils> saml2UtilsMock;
    private MockedStatic<MonitorManager> monitorManagerMock;

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder().build();


    @BeforeEach
    void setUp() {
        monitorManagerMock = Mockito.mockStatic(MonitorManager.class);
        FedMonAgent dummyAgent = Mockito.mock(FedMonAgent.class);
        FedMonSAML2Svc dummySvc = Mockito.mock(FedMonSAML2Svc.class);
        monitorManagerMock.when(MonitorManager::getAgent).thenReturn(dummyAgent);
        monitorManagerMock.when(MonitorManager::getSAML2Svc).thenReturn(dummySvc);
        saml2UtilsMock = Mockito.mockStatic(SAML2Utils.class);
    }

    @AfterEach
    void tearDown() {
        monitorManagerMock.close();
        saml2UtilsMock.close();
    }

    private static Stream<Arguments> scriptedBindingObjectParams() {
        return Stream.of(
                of("true", "DEFAULT", null, true),
                of("true", "DISABLED", null, false),
                of("true", null, null, true),
                of("false", "ENABLED", null, true),
                of("false", "DISABLED", "service", true),
                of("false", "DISABLED", null, false)
        );
    }

    @ParameterizedTest(name = "{index}: idpAttr={0}, spAttr={1}, configuredService={2} => expected={3}")
    @MethodSource("scriptedBindingObjectParams")
    void isScriptedBindingObjectRequiredScenarios(String idpAttr, String spAttr, String configuredService, boolean expected) {
        setAttributeMock(IDP_ROLE, idpAttr);
        setAttributeMock(SP_ROLE, spAttr);
        setConfiguredServiceMock(configuredService);
        assertEquals(expected, IDPSSOUtil.isScriptedBindingObjectRequired("realm", "sp", "idp"));
    }

    private void setAttributeMock(String role, String returnVal) {
        saml2UtilsMock.when(() -> SAML2Utils.getAttributeValueFromSSOConfig(
                        any(), any(), eq(role), eq(APPLICATION_CONTEXT_ENABLED))
                )
                .thenReturn(returnVal);
    }

    private void setConfiguredServiceMock(String configuredServiceForSP) {
        saml2UtilsMock.when(() -> SAML2Utils.getConfiguredServiceForSP(any(), any()))
                .thenReturn(configuredServiceForSP);
    }
}
