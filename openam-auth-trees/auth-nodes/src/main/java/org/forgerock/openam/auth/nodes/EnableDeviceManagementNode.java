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

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.SUCCESS_OUTCOME_ID;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Enable Device Management node grants the user the ability to manage their devices in a self-service Journey.
 */
@Node.Metadata(outcomeProvider = EnableDeviceManagementNode.OutcomeProvider.class,
        configClass = EnableDeviceManagementNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class EnableDeviceManagementNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(EnableDeviceManagementNode.class);
    private static final String BUNDLE = EnableDeviceManagementNode.class.getName();

    private final Config config;
    private final NodeUserIdentityProvider identityProvider;
    private static final String ELEVATED_PRIVILEGES = "ElevatedPrivileges";

    /**
     * The Enable Device Management node configuration.
     */
    public interface Config {
        /**
         * Specifies the device enforcement strategy.
         *
         * @return the {@link DeviceEnforcementStrategy} denoting the strategy to be performed on device verification.
         */
        @Attribute(order = 100, requiredValue = true)
        default DeviceEnforcementStrategy deviceEnforcementStrategy() {
            return DeviceEnforcementStrategy.SAME;
        }
    }

    /**
     * Constructs a new EnableDeviceManagementNode instance.
     *
     * @param config           the Node configuration.
     * @param identityProvider the NodeUserIdentityProvider.
     */
    @Inject
    public EnableDeviceManagementNode(@Assisted Config config, NodeUserIdentityProvider identityProvider) {
        this.config = config;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("EnableDeviceManagementNode started");

        NodeState nodeState = context.getStateFor(this);
        Optional<AMIdentity> userIdentity = identityProvider.getAMIdentity(context.universalId, nodeState);
        if (userIdentity.isEmpty()) {
            logger.error("User does not exist or inactive");
            return Action.goTo(FAILURE_OUTCOME_ID).build();
        }

        Action.ActionBuilder actionBuilder = Action.goTo(SUCCESS_OUTCOME_ID);
        switch (config.deviceEnforcementStrategy()) {
        case SAME:
            break;
        case ANY:
            if (nodeState.isDefined(NODE_TYPE)) {
                actionBuilder.addNodeType(context, ELEVATED_PRIVILEGES);
            } else {
                logger.warn("User is not authenticated via MFA");
                return Action.goTo(FAILURE_OUTCOME_ID).build();
            }
            break;
        case NONE:
            actionBuilder.addNodeType(context, ELEVATED_PRIVILEGES);
            break;
        }

        return actionBuilder.build();
    }

    /**
     * Provides the Enable Device Management node's set of outcomes.
     */
    public static final class OutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    EnableDeviceManagementNode.OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID))
            );
        }
    }

    /**
     * Session Device Enforcement Strategy.
     */
    public enum DeviceEnforcementStrategy {
        /**
         * Unrestricted, No device check performed.
         */
        NONE,
        /**
         * Relaxed, but requires at least one MFA device enrolled.
         */
        ANY,
        /**
         * No change in behaviour.
         */
        SAME
    }
}
