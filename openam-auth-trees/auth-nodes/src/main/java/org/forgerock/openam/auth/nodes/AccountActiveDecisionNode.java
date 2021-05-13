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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;

import java.util.Optional;

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

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Checks if the current user is active.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = AccountActiveDecisionNode.Config.class,
        tags = {"risk"})
public class AccountActiveDecisionNode extends AbstractDecisionNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
    }

    private final Logger logger = LoggerFactory.getLogger(AccountActiveDecisionNode.class);
    private final CoreWrapper coreWrapper;
    private final IdentityUtils identityUtils;

    /**
     * Create the node.
     *
     * @param coreWrapper A core wrapper instance.
     * @param identityUtils An instance of the IdentityUtils.
     */
    @Inject
    public AccountActiveDecisionNode(CoreWrapper coreWrapper, IdentityUtils identityUtils) {
        this.coreWrapper = coreWrapper;
        this.identityUtils = identityUtils;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AccountActiveDecisionNode started");
        Optional<AMIdentity> userIdentity = getAMIdentity(context, identityUtils, coreWrapper);
        if (userIdentity.isEmpty()) {
            throw new NodeProcessException("Failed to get the identity object");
        }

        try {
            logger.debug("Checking user account status");
            return goTo(userIdentity.get().isActive()).build();
        } catch (IdRepoException | SSOException e) {
            logger.warn("Exception checking user status", e);
            throw new NodeProcessException(e);
        }
    }
}
