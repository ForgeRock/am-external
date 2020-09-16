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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode;
import org.forgerock.openam.auth.nodes.push.PushResultVerifierNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnAuthenticationNode;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.VersionComparison;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class NodesPlugin extends AbstractNodeAmPlugin {

    @Override
    public String getPluginVersion() {
        return "4.5.0";
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (fromVersion.equals("1.0.0")) {
            pluginTools.upgradeAuthNode(ZeroPageLoginNode.class);
        } else if (VersionComparison.compareVersionStrings("3.0.0", fromVersion) >= 0
                && VersionComparison.compareVersionStrings("4.5.0", fromVersion) < 0) {
            pluginTools.upgradeAuthNode(WebAuthnAuthenticationNode.class);
            pluginTools.upgradeAuthNode(WebAuthnRegistrationNode.class);
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
                SocialOpenIdConnectNode.class,
                MetadataNode.class,
                AnonymousSessionUpgradeNode.class)
        );
    }
}
