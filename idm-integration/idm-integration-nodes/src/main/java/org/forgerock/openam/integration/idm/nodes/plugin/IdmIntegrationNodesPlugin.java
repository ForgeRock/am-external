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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.integration.idm.nodes.plugin;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.nodes.NodesPlugin.NODES_PLUGIN_VERSION;

import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode;
import org.forgerock.openam.auth.nodes.AttributeCollectorNode;
import org.forgerock.openam.auth.nodes.AttributePresentDecisionNode;
import org.forgerock.openam.auth.nodes.AttributeValueDecisionNode;
import org.forgerock.openam.auth.nodes.ConsentNode;
import org.forgerock.openam.auth.nodes.CreateObjectNode;
import org.forgerock.openam.auth.nodes.CreatePasswordNode;
import org.forgerock.openam.auth.nodes.DisplayUserNameNode;
import org.forgerock.openam.auth.nodes.EmailSuspendNode;
import org.forgerock.openam.auth.nodes.EmailTemplateNode;
import org.forgerock.openam.auth.nodes.IdentifyExistingUserNode;
import org.forgerock.openam.auth.nodes.IncrementLoginCountNode;
import org.forgerock.openam.auth.nodes.KbaCreateNode;
import org.forgerock.openam.auth.nodes.KbaDecisionNode;
import org.forgerock.openam.auth.nodes.KbaVerifyNode;
import org.forgerock.openam.auth.nodes.LoginCountDecisionNode;
import org.forgerock.openam.auth.nodes.NodesPlugin;
import org.forgerock.openam.auth.nodes.PassthroughAuthenticationNode;
import org.forgerock.openam.auth.nodes.PatchObjectNode;
import org.forgerock.openam.auth.nodes.ProfileCompletenessDecisionNode;
import org.forgerock.openam.auth.nodes.ProvisionDynamicAccountNode;
import org.forgerock.openam.auth.nodes.ProvisionIdmAccountNode;
import org.forgerock.openam.auth.nodes.QueryFilterDecisionNode;
import org.forgerock.openam.auth.nodes.RequiredAttributesDecisionNode;
import org.forgerock.openam.auth.nodes.SelectIdPNode;
import org.forgerock.openam.auth.nodes.SessionDataNode;
import org.forgerock.openam.auth.nodes.SocialProviderHandlerNode;
import org.forgerock.openam.auth.nodes.SocialProviderHandlerNodeV2;
import org.forgerock.openam.auth.nodes.TermsAndConditionsDecisionNode;
import org.forgerock.openam.auth.nodes.TimeSinceDecisionNode;
import org.forgerock.openam.auth.nodes.ValidatedPasswordNode;
import org.forgerock.openam.auth.nodes.ValidatedUsernameNode;
import org.forgerock.openam.plugins.AmPlugin;

import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.plugins.PluginException;

/**
 * Node installation plugin for the IDM integration nodes.
 */
public class IdmIntegrationNodesPlugin extends AbstractNodeAmPlugin {

    /**
     * The current version of the IDM integration nodes plugin.
     */
    public static final String IDM_NODES_PLUGIN_VERSION = "3.0.0";

    @Override
    public String getPluginVersion() {
        return IDM_NODES_PLUGIN_VERSION;
    }

    @Override
    public Map<Class<? extends AmPlugin>, String> getDependencies() {
        return singletonMap(NodesPlugin.class, "[1.0.0," + NODES_PLUGIN_VERSION + "]");
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
        if (inRange("1.0.0", fromVersion, "2.0.0")) {
            pluginTools.upgradeAuthNode(SocialProviderHandlerNode.class);
            pluginTools.upgradeAuthNode(SocialProviderHandlerNodeV2.class);
        }
        if (inRange("1.0.0", fromVersion, "3.0.0")) {
            pluginTools.upgradeAuthNode(EmailSuspendNode.class);
        }
        super.upgrade(fromVersion);
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return new ImmutableMap.Builder<String, Iterable<? extends Class<? extends Node>>>()
                       .put("1.0.0", asList(
                               ValidatedUsernameNode.class,
                               ValidatedPasswordNode.class,
                               TermsAndConditionsDecisionNode.class,
                               PatchObjectNode.class,
                               ProfileCompletenessDecisionNode.class,
                               QueryFilterDecisionNode.class,
                               RequiredAttributesDecisionNode.class,
                               SelectIdPNode.class,
                               TimeSinceDecisionNode.class,
                               PassthroughAuthenticationNode.class,
                               CreatePasswordNode.class,
                               AcceptTermsAndConditionsNode.class,
                               AttributePresentDecisionNode.class,
                               AttributeCollectorNode.class,
                               AttributeValueDecisionNode.class,
                               ConsentNode.class,
                               CreateObjectNode.class,
                               DisplayUserNameNode.class,
                               IdentifyExistingUserNode.class,
                               KbaCreateNode.class,
                               KbaDecisionNode.class,
                               KbaVerifyNode.class,
                               ProvisionDynamicAccountNode.class,
                               ProvisionIdmAccountNode.class,
                               SessionDataNode.class,
                               EmailSuspendNode.class,
                               EmailTemplateNode.class,
                               IncrementLoginCountNode.class,
                               LoginCountDecisionNode.class,
                               SocialProviderHandlerNode.class,
                               SocialProviderHandlerNodeV2.class)).build();
    }
}
