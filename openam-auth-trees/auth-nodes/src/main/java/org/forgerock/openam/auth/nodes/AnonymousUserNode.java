/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
