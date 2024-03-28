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
 * Copyright 2018-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;

import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.mfa.CombinedMultiFactorRegistrationNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorRegistrationOptionsNode;
import org.forgerock.openam.auth.nodes.mfa.OptOutMultiFactorAuthenticationNode;
import org.forgerock.openam.auth.nodes.oath.OathDeviceStorageNode;
import org.forgerock.openam.auth.nodes.oath.OathRegistrationNode;
import org.forgerock.openam.auth.nodes.oath.OathTokenVerifierNode;
import org.forgerock.openam.auth.nodes.oidc.OidcNode;
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

    /**
     * The current version of the nodes plugin.
     * N.B. If upgrading this version you must ensure that the amPluginService version in the latest.groovy
     * FBC upgrade rules file is kept in sync.
     */
    public static final String NODES_PLUGIN_VERSION = "21.0.0";

    @Override
    public String getPluginVersion() {
        return NODES_PLUGIN_VERSION;
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (fromVersion.equals("1.0.0")) {
            pluginTools.upgradeAuthNode(ZeroPageLoginNode.class);
        } else if (inRangeLess("3.0.0", fromVersion, "6.0.0")) {
            pluginTools.upgradeAuthNode(WebAuthnAuthenticationNode.class);
        }
        if (inRangeLess("1.0.0", fromVersion, "5.0.0")) {
            pluginTools.upgradeAuthNode(ScriptedDecisionNode.class);
        }
        if (inRangeLess("2.0.0", fromVersion, "17.0.0")) {
            pluginTools.upgradeAuthNode(OneTimePasswordSmsSenderNode.class);
            pluginTools.upgradeAuthNode(OneTimePasswordSmtpSenderNode.class);
        }
        if (inRangeLess("2.0.0", fromVersion, "8.0.0")) {
            pluginTools.upgradeAuthNode(RetryLimitDecisionNode.class);
        }
        if (inRange("3.0.0", fromVersion, "8.2.2")) {
            pluginTools.upgradeAuthNode(MessageNode.class);
        }
        if (inRange("1.0.0", fromVersion, "8.4.1")) {
            pluginTools.upgradeAuthNode(ChoiceCollectorNode.class);
        }
        if (inRange("6.0.0", fromVersion, "8.4.1")) {
            pluginTools.upgradeAuthNode(PushRegistrationNode.class);
        }
        if (inRange("7.0.0", fromVersion, "9.0.0")) {
            pluginTools.upgradeAuthNode(OathRegistrationNode.class);
        }
        if (inRange("3.0.0", fromVersion, "8.4.1")) {
            pluginTools.upgradeAuthNode(WebAuthnRegistrationNode.class);
        }
        if (inRange("5.0.0", fromVersion, "8.4.1")) {
            pluginTools.upgradeAuthNode(WebAuthnDeviceStorageNode.class);
        }
        if (inRange("5.0.0", fromVersion, "9.0.0")) {
            pluginTools.upgradeAuthNode(Saml2Node.class);
        }
        if (inRange("2.0.0", fromVersion, "14.0.0")) {
            pluginTools.upgradeAuthNode(PushAuthenticationSenderNode.class);
        }
        if (inRange("10.1.0", fromVersion, "11.0.1")) {
            pluginTools.upgradeAuthNode(CombinedMultiFactorRegistrationNode.class);
        }
        if (inRange("5.0.0", fromVersion, "15.0.0")) {
            pluginTools.upgradeAuthNode(CertificateValidationNode.class);
        }
        if (inRange("2.0.0", fromVersion, "16.0.0")) {
            pluginTools.upgradeAuthNode(LdapDecisionNode.class);
        }
        if (inRange("2.0.0", fromVersion, "19.0.0")) {
            pluginTools.upgradeAuthNode(PersistentCookieDecisionNode.class);
        }
        if (inRange("11.1.0", fromVersion, "20.0.0")) {
            pluginTools.upgradeAuthNode(DeviceSigningVerifierNode.class);
        }
        if (inRange("8.2.0", fromVersion, "21.0.0")) {
            pluginTools.upgradeAuthNode(CaptchaNode.class);
        }
        super.upgrade(fromVersion);
    }

    private boolean inRangeLess(String minVersion, String version, String maxVersion) {
        return VersionComparison.compareVersionStrings(minVersion, version) >= 0
                && VersionComparison.compareVersionStrings(maxVersion, version) < 0;
    }

    private boolean inRange(String minVersion, String version, String maxVersion) {
        return VersionComparison.compareVersionStrings(minVersion, version) >= 0
                && VersionComparison.compareVersionStrings(maxVersion, version) <= 0;
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
                        LdapDecisionNode.class,
                        MeterNode.class,
                        OneTimePasswordCollectorDecisionNode.class,
                        OneTimePasswordGeneratorNode.class,
                        OneTimePasswordSmsSenderNode.class,
                        OneTimePasswordSmtpSenderNode.class,
                        PersistentCookieDecisionNode.class,
                        PollingWaitNode.class,
                        PushAuthenticationSenderNode.class,
                        PushResultVerifierNode.class,
                        RecoveryCodeCollectorDecisionNode.class,
                        RegisterLogoutWebhookNode.class,
                        RetryLimitDecisionNode.class,
                        SocialOAuthIgnoreProfileNode.class,
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
                        AccountActiveDecisionNode.class,
                        KerberosNode.class,
                        ReCaptchaNode.class,
                        Saml2Node.class,
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
                .put("10.0.0", asList(
                        OathDeviceStorageNode.class)
                )
                .put("10.1.0", asList(
                        CombinedMultiFactorRegistrationNode.class)
                )
                .put("11.0.0", asList(
                        OidcNode.class
                ))
                .put("11.1.0", asList(
                        DeviceBindingNode.class,
                        DeviceSigningVerifierNode.class
                ))
                .put("12.0.0", asList(
                        QueryParameterNode.class)
                )
                .put("13.0.0", asList(
                        DeviceBindingStorageNode.class
                ))
                .put("19.0.0", asList(
                        RequestHeaderNode.class
                ))
                .build();
    }
}
