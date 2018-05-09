/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.json.JsonValue;

/**
 * A node is the core abstraction within an authentication tree. Trees are made up of nodes, which may modify the
 * shared state and/or request input from the user via callbacks.
 *
 * <p>A node *must* not store any state in memory and should instead use the shared state mechanism for this.</p>
 *
 * <p>Nodes are instantiated using dependency injection and can therefore use an {@code @Inject}-annotated
 * constructor.</p>
 *
 * <p>All concrete implementations of this class should be annotated with {@link Metadata}.</p>
 *
 * @supported.all.api
 * @since AM 5.5.0
 */
public interface Node {

    /**
     * Performs processing on the given shared state, which holds all the data gathered by nodes that have already
     * executed as part of this authentication session in the tree.
     *
     * <p>This method is invoked when the node is reached in the tree.</p>
     *
     * @param context The context of the tree authentication.
     * @return The next action to perform. Must not be null.
     *
     * @throws NodeProcessException If there was a problem processing that could not be resolved to a single outcome.
     */
    Action process(TreeContext context) throws NodeProcessException;

    /** Annotation that describes the metadata of the node. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Metadata {
        /**
         * A provider for the possible outcomes of the node.
         */
        Class<? extends OutcomeProvider> outcomeProvider();

        /**
         * An interface describing the configuration of the node. The interface should contain methods annotated with
         * {@code Attribute}.
         */
        Class<?> configClass();

        /**
         * A validator for the entire service configuration.
         */
        Class<?> configValidator() default Void.class;
    }

    /**
     * Supply the additional detail to be logged with this node's completion event. Subclasses can override this
     * method to add more specific detail.
     *
     * @return The audit entry detail.
     */
    default JsonValue getAuditEntryDetail() {
        return json(object());
    }
}
