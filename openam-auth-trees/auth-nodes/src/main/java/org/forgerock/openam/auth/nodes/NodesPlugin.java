/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Arrays.asList;

import java.util.Map;

import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode;
import org.forgerock.openam.auth.nodes.push.PushResultVerifierNode;
import org.forgerock.openam.plugins.PluginException;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class NodesPlugin extends AbstractNodeAmPlugin {

    @Override
    public String getPluginVersion() {
        return "2.0.0";
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (fromVersion.equals("1.0.0")) {
            pluginTools.upgradeAuthNode(ZeroPageLoginNode.class);
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
                TimerStopNode.class
        ));
    }
}
