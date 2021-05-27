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
 * Copyright 2018-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;

import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode;
import org.forgerock.openam.auth.nodes.push.PushResultVerifierNode;
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
        return "5.1.0";
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (fromVersion.equals("1.0.0")) {
            pluginTools.upgradeAuthNode(ZeroPageLoginNode.class);
        } else if (VersionComparison.compareVersionStrings("3.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("5.0.0", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(WebAuthnAuthenticationNode.class);
            pluginTools.upgradeAuthNode(WebAuthnRegistrationNode.class);
            pluginTools.upgradeAuthNode(ScriptedDecisionNode.class);
        }
        if (VersionComparison.compareVersionStrings("2.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("5.1.0", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(RetryLimitDecisionNode.class);
        }
        super.upgrade(fromVersion);
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return ImmutableMap.of(
                "1.0.0", asList(
                        AuthLevelDecisionNode.class,
                        DataStoreDecisionNode.class,
                        PasswordCollectorNode.class,
                        RemoveSessionPropertiesNode.class,
                        ScriptedDecisionNode.class,
                        SetSessionPropertiesNode.class,
                        UsernameCollectorNode.class,
                        ZeroPageLoginNode.class),
                "2.0.0", asList(
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
                        TimerStopNode.class),
                "3.0.0", asList(
                        AgentDataStoreDecisionNode.class,
                        CookiePresenceDecisionNode.class,
                        MessageNode.class,
                        WebAuthnRegistrationNode.class,
                        WebAuthnAuthenticationNode.class,
                        RecoveryCodeDisplayNode.class),
                "4.0.0", asList(
                        MetadataNode.class,
                        SocialOpenIdConnectNode.class),
                "5.0.0", asList(
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
                        CertificateValidationNode.class
                )
        );
    }
}
