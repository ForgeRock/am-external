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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.security.PrivilegedAction;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;

/**
 * A node that decides if the agent id and password exists in the Agents data store.
 *
 * <p>Expects the 'username' field to be present in the shared state and the 'password' field to
 * be present in the transient state.</p>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = AgentDataStoreDecisionNode.Config.class)
public class AgentDataStoreDecisionNode extends DataStoreDecisionNode {

    /**
     * Configuration for the agent data store decision node.
     */
    public interface Config {
    }

    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Guice constructor.
     * @param coreWrapper A core wrapper instance.
     * @param identityUtils A {@code IdentityUtils} instance.
     * @param adminTokenActionProvider A provider for an {@code SSOToken}.
     */
    @Inject
    public AgentDataStoreDecisionNode(CoreWrapper coreWrapper, IdentityUtils identityUtils,
            Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider) {
        super(coreWrapper, identityUtils, adminTokenActionProvider);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AgentDataStoreDecisionNode started");
        return super.process(context);
    }

    @Override
    IdType getIdentityType() {
        return IdType.AGENT;
    }
}
