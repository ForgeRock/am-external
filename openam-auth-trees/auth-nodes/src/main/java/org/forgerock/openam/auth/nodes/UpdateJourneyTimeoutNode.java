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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.MAX_TREE_DURATION_IN_MINUTES;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.I18nKey;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.AuthSessionHelper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.sm.annotations.adapters.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * A node to update the timeout of a journey.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = UpdateJourneyTimeoutNode.Config.class,
        tags = {"utilities"}, configValidator = UpdateJourneyTimeoutNode.ValueValidator.class)
public class UpdateJourneyTimeoutNode extends SingleOutcomeNode {


    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The operation to perform on the timeout.
         *
         * @return the operation to perform.
         */
        @Attribute(order = 100, requiredValue = true)
        Operation operation();

        /**
         * The value of the timeout in minutes.
         *
         * @return the value of the timeout in minutes.
         */
        @Attribute(order = 200, requiredValue = true)
        @TimeUnit(MINUTES)
        Duration value();

    }

    private final Config config;
    private final AuthSessionHelper authSessionHelper;

    /**
     * Constructs a new UpdateJourneyTimeoutNode instance.
     *
     * @param config Node configuration.
     * @param authSessionHelper The AuthSessionHelper.
     */
    @Inject
    public UpdateJourneyTimeoutNode(@Assisted Config config, AuthSessionHelper authSessionHelper) {
        this.config = config;
        this.authSessionHelper = authSessionHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        return switch (config.operation()) {
        case SET -> setJourneyTimeout(context);
        case MODIFY -> modifyJourneyTimeout(context);
        };
    }

    private Action setJourneyTimeout(TreeContext context) throws NodeProcessException {
        if (config.value().isNegative()) {
            throw new NodeProcessException("The timeout value must be positive when SET operation is selected");
        }

        NodeState nodeState = context.getStateFor(this);
        nodeState.putTransient(MAX_TREE_DURATION_IN_MINUTES, config.value().toMinutes());

        return goToNext().withMaxTreeDuration(config.value()).build();
    }

    private Action modifyJourneyTimeout(TreeContext context) throws NodeProcessException {
        Duration modifiedMaxDuration;
        NodeState nodeState = context.getStateFor(this);

        if (nodeState.isDefined(MAX_TREE_DURATION_IN_MINUTES)) {
            modifiedMaxDuration = config.value().plus(nodeState.get(MAX_TREE_DURATION_IN_MINUTES).asLong(),
                    ChronoUnit.MINUTES);
        } else {
            if (isEmpty(context.request.authId)) {
                return setJourneyTimeout(context);
            }
            modifiedMaxDuration = config.value().plus(getMaxDurationFromAuthId(context));
        }
        if (modifiedMaxDuration.isNegative()) {
            throw new NodeProcessException("The resulting timeout value must be positive");
        }

        nodeState.putTransient(MAX_TREE_DURATION_IN_MINUTES, modifiedMaxDuration.toMinutes());
        return goToNext().withMaxTreeDuration(modifiedMaxDuration).build();
    }

    private Duration getMaxDurationFromAuthId(TreeContext context) throws NodeProcessException {
        Session session = authSessionHelper.getAuthSession(context.request.authId);
        try {
            return Duration.ofMinutes(session.getMaxSessionTime());
        } catch (SessionException e) {
            throw new NodeProcessException("Error getting the session max duration", e);
        }
    }

    /**
     * The operation to perform on the timeout.
     */
    public enum Operation {

        /**
         * Set the timeout.
         */
        @I18nKey("operation.set")
        SET,

        /**
         * Modify the timeout.
         */
        @I18nKey("operation.modify")
        MODIFY
    }

    /**
     * Validator for the node configuration.
     * <ul>
     *     <li>Both operation and value must not be null</li>
     *     <li>Value must be positive if operation is SET</li>
     * </ul>
     * @see ServiceConfigValidator
     */
    static final class ValueValidator implements ServiceConfigValidator {

        static final String VALUE = "value";
        static final String OPERATION = "operation";

        private static final Logger logger = LoggerFactory.getLogger(UpdateJourneyTimeoutNode.ValueValidator.class);

        private final AnnotatedServiceRegistry serviceRegistry;

        @Inject
        ValueValidator(AnnotatedServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {

            Optional<UpdateJourneyTimeoutNode.Config> possibleExistingConfig;
            String nodeId = configPath.get(configPath.size() - 1);

            try {
                possibleExistingConfig = serviceRegistry.getRealmInstance(UpdateJourneyTimeoutNode.Config.class, realm,
                        nodeId);
            } catch (SSOException | SMSException e) {
                logger.error("An error occurred validating the update journey timeout node configuration", e);
                throw new ServiceErrorException("Error validating the update journey timeout node configuration");
            }

            String value = getConfigValue(VALUE, Config::value, possibleExistingConfig, attributes);
            String operation = getConfigValue(OPERATION, Config::operation, possibleExistingConfig, attributes);

            if (value == null || operation == null) {
                throw new ServiceConfigException("The value and operation attributes are required");
            }

            if (Operation.SET.name().equals(operation) && Long.parseLong(value) < 0) {
                throw new ServiceConfigException("The timeout value must be positive when SET operation is selected");
            }
        }

        private String getConfigValue(String attribute, Function<UpdateJourneyTimeoutNode.Config, ?> attributeMethod,
                Optional<UpdateJourneyTimeoutNode.Config> existingConfig, Map<String, Set<String>> attributes) {
            if (attributes.containsKey(attribute)) {
                return getFirstItem(attributes.get(attribute));
            }
            Object value = existingConfig.map(attributeMethod).orElse(null);
            return value == null ? null : String.valueOf(value);
        }
    }

}
