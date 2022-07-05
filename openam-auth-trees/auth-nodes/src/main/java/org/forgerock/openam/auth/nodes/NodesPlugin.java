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
 * Copyright 2018-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;

import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode;
import org.forgerock.openam.auth.nodes.mfa.OptOutMultiFactorAuthenticationNode;
import org.forgerock.openam.auth.nodes.oath.OathRegistrationNode;
import org.forgerock.openam.auth.nodes.oath.OathTokenVerifierNode;
import org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode;
import org.forgerock.openam.auth.nodes.push.PushRegistrationNode;
import org.forgerock.openam.auth.nodes.push.PushResultVerifierNode;
import org.forgerock.openam.auth.nodes.push.PushWaitNode;
import org.forgerock.openam.auth.nodes.saml2.Saml2Node;
import org.forgerock.openam.auth.nodes.saml2.WriteFederationInformationNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnDeviceStorageNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode;
import org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode;
import org.forgerock.openam.auth.nodes.x509.CertificateValidationNode;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.VersionComparison;

import com.google.common.collect.ImmutableMap;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class NodesPlugin extends AbstractNodeAmPlugin {

    @Override
    public String getPluginVersion() {
        return "9.0.0";
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (fromVersion.equals("1.0.0")) {
            pluginTools.upgradeAuthNode(ZeroPageLoginNode.class);
        } else if (VersionComparison.compareVersionStrings("3.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("6.0.0", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(WebAuthnAuthenticationNode.class);
            pluginTools.upgradeAuthNode(WebAuthnRegistrationNode.class);
        }
        if (VersionComparison.compareVersionStrings("1.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("5.0.0", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(ScriptedDecisionNode.class);
        }
        if (VersionComparison.compareVersionStrings("2.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("7.0.1", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(OneTimePasswordSmsSenderNode.class);
            pluginTools.upgradeAuthNode(OneTimePasswordSmtpSenderNode.class);
        }
        if (VersionComparison.compareVersionStrings("2.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.0.0", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(RetryLimitDecisionNode.class);
        }
        if (VersionComparison.compareVersionStrings("5.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.0.0", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(SocialProviderHandlerNode.class);
        }
        if (VersionComparison.compareVersionStrings("3.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.2.2", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(MessageNode.class);
        }
        if (VersionComparison.compareVersionStrings("1.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.4.1", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(ChoiceCollectorNode.class);
        }
        if (VersionComparison.compareVersionStrings("6.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.4.1", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(PushRegistrationNode.class);
        }
        if (VersionComparison.compareVersionStrings("7.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.4.1", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(OathRegistrationNode.class);
        }
        if (VersionComparison.compareVersionStrings("2.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.5.0", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(PushAuthenticationSenderNode.class);
        }
        if (VersionComparison.compareVersionStrings("3.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.4.1", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(WebAuthnRegistrationNode.class);
        }
        if (VersionComparison.compareVersionStrings("5.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("8.4.1", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(WebAuthnDeviceStorageNode.class);
        }
        if (VersionComparison.compareVersionStrings("5.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("9.0.0", fromVersion) <= 0) {
            pluginTools.upgradeAuthNode(Saml2Node.class);
        }

        super.upgrade(fromVersion);
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return new ImmutableMap.Builder<String, Iterable<? extends Class<? extends Node>>>()
                .put("1.0.0", asList(
                        ChoiceCollectorNode.class,
                        AuthLevelDecisionNode.class,
                        DataStoreDecisionNode.class,
                        PasswordCollectorNode.class,
                        RemoveSessionPropertiesNode.class,
                        ScriptedDecisionNode.class,
                        SetSessionPropertiesNode.class,
                        UsernameCollectorNode.class,
                        ZeroPageLoginNode.class)
                )
                .put("2.0.0", asList(
                        AccountLockoutNode.class,
                        AnonymousUserNode.class,
                        CreatePasswordNode.class,
                        LdapDecisionNode.class,
                        MeterNode.class,
                        OneTimePasswordCollectorDecisionNode.class,
                        OneTimePasswordGeneratorNode.class,
                        OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class,
                        PersistentCookieDecisionNode.class,
                        PollingWaitNode.class,
                        ProvisionDynamicAccountNode.class,
                        ProvisionIdmAccountNode.class,
                        PushAuthenticationSenderNode.class,
                        PushResultVerifierNode.class,
                        RecoveryCodeCollectorDecisionNode.class,
                        RegisterLogoutWebhookNode.class,
                        RetryLimitDecisionNode.class,
                        SocialOAuthIgnoreProfileNode.class,
                        SessionDataNode.class,
                        SetFailureUrlNode.class,
                        SetSuccessUrlNode.class,
                        SocialFacebookNode.class,
                        SocialGoogleNode.class,
                        SocialNode.class,
                        TimerStartNode.class,
                        TimerStopNode.class)
                )
                .put("3.0.0", asList(
                        AgentDataStoreDecisionNode.class,
                        CookiePresenceDecisionNode.class,
                        MessageNode.class,
                        WebAuthnRegistrationNode.class,
                        WebAuthnAuthenticationNode.class,
                        RecoveryCodeDisplayNode.class)
                )
                .put("4.0.0", asList(
                        MetadataNode.class,
                        SocialOpenIdConnectNode.class)
                )
                .put("5.0.0", asList(
                        AcceptTermsAndConditionsNode.class,
                        AccountActiveDecisionNode.class,
                        AttributePresentDecisionNode.class,
                        AttributeCollectorNode.class,
                        AttributeValueDecisionNode.class,
                        ConsentNode.class,
                        CreateObjectNode.class,
                        DisplayUserNameNode.class,
                        EmailSuspendNode.class,
                        EmailTemplateNode.class,
                        IdentifyExistingUserNode.class,
                        IncrementLoginCountNode.class,
                        KbaCreateNode.class,
                        KbaDecisionNode.class,
                        KbaVerifyNode.class,
                        KerberosNode.class,
                        LoginCountDecisionNode.class,
                        PatchObjectNode.class,
                        ProfileCompletenessDecisionNode.class,
                        QueryFilterDecisionNode.class,
                        ReCaptchaNode.class,
                        RequiredAttributesDecisionNode.class,
                        Saml2Node.class,
                        SelectIdPNode.class,
                        SocialProviderHandlerNode.class,
                        TermsAndConditionsDecisionNode.class,
                        TimeSinceDecisionNode.class,
                        ValidatedPasswordNode.class,
                        ValidatedUsernameNode.class,
                        AnonymousSessionUpgradeNode.class,
                        WriteFederationInformationNode.class,
                        WebAuthnDeviceStorageNode.class,
                        DeviceProfileCollectorNode.class,
                        DeviceTamperingVerificationNode.class,
                        DeviceLocationMatchNode.class,
                        DeviceMatchNode.class,
                        DeviceSaveNode.class,
                        DeviceGeoFencingNode.class,
                        CertificateCollectorNode.class,
                        CertificateUserExtractorNode.class,
                        CertificateValidationNode.class)
                )
                .put("6.0.0", asList(
                        PushRegistrationNode.class,
                        OptOutMultiFactorAuthenticationNode.class,
                        GetAuthenticatorAppNode.class,
                        MultiFactorRegistrationOptionsNode.class)
                )
                .put("7.0.0", asList(
                        OathRegistrationNode.class,
                        OathTokenVerifierNode.class)
                )
                .put("8.1.0", asList(
                        PassthroughAuthenticationNode.class)
                )
                .put("8.2.0", asList(
                        CaptchaNode.class)
                )
                .put("8.3.0", asList(
                        DebugNode.class)
                )
                .put("8.4.0", asList(
                        SetCustomCookieNode.class)
                )
                .put("8.5.0", asList(
                        PushWaitNode.class)
                )
                .build();
    }
}
