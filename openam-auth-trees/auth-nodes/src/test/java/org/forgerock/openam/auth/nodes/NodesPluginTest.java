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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.apache.commons.lang3.stream.Streams.failableStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.forgerock.openam.auth.nodes.mfa.CombinedMultiFactorRegistrationNode;
import org.forgerock.openam.auth.nodes.oath.OathRegistrationNode;
import org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode;
import org.forgerock.openam.auth.nodes.push.PushRegistrationNode;
import org.forgerock.openam.auth.nodes.saml2.Saml2Node;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnDeviceStorageNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.x509.CertificateValidationNode;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.PluginTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the {@link NodesPlugin}.
 * <p>Currently only tests for {@code NodesPlugin::upgrade}.
 */
@ExtendWith(MockitoExtension.class)
public class NodesPluginTest {

    @Mock
    private PluginTools pluginTools;

    private NodesPlugin nodesPlugin;

    @BeforeEach
    void setup() {
        nodesPlugin = new NodesPlugin();
        nodesPlugin.setPluginTools(pluginTools);
    }

    private static Object[] upgradeDataProvider() {
        return new Object[][]{
                {"12.0.0", new Class[]{LdapDecisionNode.class,
                        PushAuthenticationSenderNode.class, CertificateValidationNode.class,
                        OneTimePasswordSmsSenderNode.class, OneTimePasswordSmtpSenderNode.class,
                        PersistentCookieDecisionNode.class, DeviceSigningVerifierNode.class, CaptchaNode.class,
                        WebAuthnAuthenticationNode.class, SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"11.0.0", new Class[]{CombinedMultiFactorRegistrationNode.class, LdapDecisionNode.class,
                        PushAuthenticationSenderNode.class, OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class, CertificateValidationNode.class,
                        PersistentCookieDecisionNode.class, CaptchaNode.class, WebAuthnAuthenticationNode.class,
                        SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"10.0.0", new Class[]{LdapDecisionNode.class, PushAuthenticationSenderNode.class,
                        OneTimePasswordSmsSenderNode.class, OneTimePasswordSmtpSenderNode.class,
                        CertificateValidationNode.class, PersistentCookieDecisionNode.class, CaptchaNode.class,
                        WebAuthnAuthenticationNode.class, SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"9.0.0", new Class[]{LdapDecisionNode.class, Saml2Node.class, OathRegistrationNode.class,
                        PushAuthenticationSenderNode.class, OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class, CertificateValidationNode.class,
                        PersistentCookieDecisionNode.class, CaptchaNode.class,
                        WebAuthnAuthenticationNode.class, SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"8.0.0", new Class[]{LdapDecisionNode.class, Saml2Node.class, WebAuthnDeviceStorageNode.class,
                        WebAuthnRegistrationNode.class, PushAuthenticationSenderNode.class, OathRegistrationNode.class,
                        PushRegistrationNode.class, ChoiceCollectorNode.class, MessageNode.class,
                        OneTimePasswordSmsSenderNode.class, OneTimePasswordSmtpSenderNode.class,
                        CertificateValidationNode.class, PersistentCookieDecisionNode.class,
                        WebAuthnAuthenticationNode.class, SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"7.0.0", new Class[]{LdapDecisionNode.class, Saml2Node.class, WebAuthnDeviceStorageNode.class,
                        WebAuthnRegistrationNode.class, PushAuthenticationSenderNode.class, OathRegistrationNode.class,
                        PushRegistrationNode.class, ChoiceCollectorNode.class, MessageNode.class,
                        RetryLimitDecisionNode.class,
                        OneTimePasswordSmsSenderNode.class, OneTimePasswordSmtpSenderNode.class,
                        CertificateValidationNode.class, PersistentCookieDecisionNode.class,
                        WebAuthnAuthenticationNode.class, SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"6.0.0", new Class[]{LdapDecisionNode.class, Saml2Node.class, WebAuthnDeviceStorageNode.class,
                        WebAuthnRegistrationNode.class, PushAuthenticationSenderNode.class,
                        PushRegistrationNode.class, ChoiceCollectorNode.class, MessageNode.class,
                        RetryLimitDecisionNode.class, OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class, CertificateValidationNode.class,
                        PersistentCookieDecisionNode.class, WebAuthnAuthenticationNode.class,
                        SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"5.0.0", new Class[]{LdapDecisionNode.class, Saml2Node.class, WebAuthnDeviceStorageNode.class,
                        WebAuthnRegistrationNode.class, PushAuthenticationSenderNode.class, ChoiceCollectorNode.class,
                        MessageNode.class, RetryLimitDecisionNode.class,
                        OneTimePasswordSmsSenderNode.class, OneTimePasswordSmtpSenderNode.class,
                        WebAuthnAuthenticationNode.class, CertificateValidationNode.class,
                        PersistentCookieDecisionNode.class, SetSessionPropertiesNode.class, ScriptedDecisionNode.class,
                        DeviceMatchNode.class}},
                {"4.0.0", new Class[]{LdapDecisionNode.class, WebAuthnRegistrationNode.class,
                        PushAuthenticationSenderNode.class, ChoiceCollectorNode.class, MessageNode.class,
                        RetryLimitDecisionNode.class, OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class, WebAuthnAuthenticationNode.class,
                        ScriptedDecisionNode.class, PersistentCookieDecisionNode.class,
                        SetSessionPropertiesNode.class}},
                {"3.0.0", new Class[]{LdapDecisionNode.class, WebAuthnRegistrationNode.class,
                        PushAuthenticationSenderNode.class, ChoiceCollectorNode.class, MessageNode.class,
                        RetryLimitDecisionNode.class, OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class, WebAuthnAuthenticationNode.class,
                        ScriptedDecisionNode.class, PersistentCookieDecisionNode.class,
                        SetSessionPropertiesNode.class}},
                {"2.0.0", new Class[]{LdapDecisionNode.class, PushAuthenticationSenderNode.class,
                        ChoiceCollectorNode.class, RetryLimitDecisionNode.class, OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class, ScriptedDecisionNode.class,
                        PersistentCookieDecisionNode.class, SetSessionPropertiesNode.class}},
                {"1.0.0", new Class[]{ChoiceCollectorNode.class, ScriptedDecisionNode.class, ZeroPageLoginNode.class,
                        SetSessionPropertiesNode.class}},
                {"0.0.0", new Class[]{}},
        };
    }

    @ParameterizedTest
    @MethodSource("upgradeDataProvider")
    public void testUpgrade(String version, Class<?>[] classes) throws PluginException {
        nodesPlugin.upgrade(version);
        failableStream(Arrays.stream(classes)).forEach(clas -> {
            verify(pluginTools).upgradeAuthNode(Mockito.eq(clas));
        });
        verify(pluginTools, times(classes.length)).upgradeAuthNode(any());
    }
}
