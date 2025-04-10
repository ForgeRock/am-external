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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.service.AMAccountLockout;
import com.sun.identity.idm.AMIdentity;

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
    private final AMAccountLockout.Factory amAccountLockoutFactory;
    private final Realm realm;
    private final NodeUserIdentityProvider identityProvider;

    /**
     * Create the node.
     *
     * @param realm                   the realm context
     * @param amAccountLockoutFactory factory for generating account lockout objects
     * @param identityProvider        the identity provider
     */
    @Inject
    public AccountActiveDecisionNode(@Assisted Realm realm, AMAccountLockout.Factory amAccountLockoutFactory,
            NodeUserIdentityProvider identityProvider) {
        this.amAccountLockoutFactory = amAccountLockoutFactory;
        this.realm = realm;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AccountActiveDecisionNode started");
        Optional<AMIdentity> userIdentity = identityProvider.getAMIdentity(context.universalId,
                context.getStateFor(this));
        if (userIdentity.isEmpty()) {
            throw new NodeProcessException("Failed to get the identity object");
        }

        AMAccountLockout accountLockout = amAccountLockoutFactory.create(this.realm);
        logger.debug("Checking user account status");
        return goTo(!accountLockout.isAccountLocked(userIdentity.get().getName())).build();
    }
}
