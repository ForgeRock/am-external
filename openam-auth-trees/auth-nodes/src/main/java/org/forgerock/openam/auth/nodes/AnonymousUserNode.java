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
 * Copyright 2018-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.SessionUpgradeVerifier;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that connect the user as anonymous.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AnonymousUserNode.Config.class)
public class AnonymousUserNode extends SingleOutcomeNode {

    private final Config config;

    /**
     * The config for the node.
     */
    public interface Config {

        /**
         * The anonymous user name.
         * @return the anonymous user name
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default String anonymousUserName() {
            return "anonymous";
        }
    }

    /**
     * Constructor.
     * @param config the config
     */
    @Inject
    public AnonymousUserNode(@Assisted AnonymousUserNode.Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        return goToNext()
                .addNodeType(context, SessionUpgradeVerifier.ANONYMOUS_MODULE_TYPE)
                .replaceSharedState(context.sharedState.put(SharedStateConstants.USERNAME, getAnonymousUser()))
                .build();
    }

    private String getAnonymousUser() throws NodeProcessException {
        String anonUser = config.anonymousUserName();
        if (anonUser != null && !anonUser.isEmpty()) {
            return anonUser;
        }
        throw new NodeProcessException("Anonymous user name could not be found in the configuration");
    }
}
