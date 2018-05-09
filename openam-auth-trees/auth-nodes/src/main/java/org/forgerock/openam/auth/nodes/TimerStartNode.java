/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.Reject;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which records the current time to a property within the authentication shared state.
 *
 * This node should be used with {@link TimerStopNode}.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = TimerStartNode.Config.class)
public class TimerStartNode extends SingleOutcomeNode {

    static final String DEFAULT_START_TIME_PROPERTY = "TimerNodeStartTime";

    private final Debug logger;
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Identifier of property into which start time should be stored.
         *
         * Defaults to {@literal "TimerNodeStartTime"}
         *
         * @return property name.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        default String startTimeProperty() {
            return DEFAULT_START_TIME_PROPERTY;
        }
    }

    /**
     * Constructs a new {@link TimerStartNode} instance.
     *
     * @param config
     *          Node configuration.
     * @param logger
     *          Debug logging instance for authentication.
     */
    @Inject
    public TimerStartNode(
            @Assisted TimerStartNode.Config config,
            @Named("amAuth") Debug logger) {
        Reject.ifTrue(isEmpty(config.startTimeProperty()), "TimerStartNode config must define start time property key");
        this.config = config;
        this.logger = logger;
    }

    @Override
    public Action process(TreeContext context) {
        long startTimeMillis = currentTimeMillis();
        logger.message("{} recording start time {} in {}",
                TimerStartNode.class.getSimpleName(), startTimeMillis, config.startTimeProperty());
        JsonValue newSharedState = context.sharedState.copy();
        newSharedState.put(config.startTimeProperty(), startTimeMillis);
        return goToNext().replaceSharedState(newSharedState).build();
    }

}

