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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Supported;

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
 */
@Supported
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
    @Supported
    Action process(TreeContext context) throws NodeProcessException;

    /** Annotation that describes the metadata of the node. */
    @Supported
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Metadata {

        /**
         * The name of the node. If not provided, the simple name of the annotated interface is used.
         * @return the name.
         */
        @Supported
        String name() default "";

        /**
         * The name of the i18n bundle file relative to the root package.
         * <p>If not provided, this will default to the node class name.
         *
         * @return the i18n file name.
         */
        @Supported
        String i18nFile() default "";

        /**
         * A provider for the possible outcomes of the node.
         *
         * @return the outcome provider.
         */
        @Supported
        Class<? extends OutcomeProvider> outcomeProvider();

        /**
         * An interface describing the configuration of the node. The interface should contain methods annotated with
         * {@code Attribute}.
         *
         * @return The config class.
         */
        @Supported
        Class<?> configClass();

        /**
         * A validator for the entire service configuration.
         *
         * @return The config validator class.
         */
        @Supported
        Class<?> configValidator() default Void.class;

        /**
         * A list of tags, which can be categories, keywords or synonyms.
         *
         * @return The list of tags.
         */
        @Supported
        String[] tags() default {};

        /**
         * Prepended to a node's name to differentiate it from other nodes with the same name. This was introduced to
         * allow integration of Marketplace nodes into the core AM product with backwards compatibility.
         *
         * @return The state namespace.
         */
        Namespace namespace() default Namespace.NONE;

        /**
         * A class that will be serialised to JSON that a node can specify to return additional "metadata" to the
         * client for presenting the node.
         * <p>
         * For example "provider" metadata attribute for a set of nodes written by the same external provider.
         * </p>
         * <p>
         * Jackson is used to serialise the class instance to JSON.
         * </p>
         *
         * @return The extensions.
         */
        @Supported
        Class<?> extensions() default Void.class;
    }

    /**
     * Supply the additional detail to be logged with this node's completion event. Subclasses can override this
     * method to add more specific detail.
     *
     * @return The audit entry detail.
     */
    @Supported
    default JsonValue getAuditEntryDetail() {
        return json(object());
    }

    /**
     * Provide a list of shared state data a node consumes.
     *
     * An {@link InputState} consists of a property name and an "isRequired" flag.  The IsRequired flag indicates
     * whether the input is required in order for the node to function.  If the flag is false this indicates that
     * the node will consume this data if it is present but it is not required for the node to function.
     *
     * Example:
     *     public InputState[] getInputs() {
     *         return new InputState[] {
     *             new InputState(IDENTITY),
     *             new InputState("foo", false)
     *         };
     *     }
     *
     * In this example the node declares that it requires state to contain a property named IDENTITY and that it
     * will consume a property named "foo" if it is present.  If "foo" is not present then the node will still
     * function but may be skipping some functionality.
     *
     * This list is used to ensure that state data, both shared and transient, from upstream nodes is left intact
     * for this node to access.  If inputs are not declared there is no guarantee that the data needed by the node
     * will still be present in state when the node executes.
     *
     * This list is also used for tree validation to report errors in tree construction.
     *
     * @return The list of shared state data.
     */
    @Supported
    default InputState[] getInputs() {
        return new InputState[] {};
    }

    /**
     * Provide a list of shared state data a node provides.
     *
     * An {@link OutputState} consists of a property name and a map of node outcomes to a flag indicating whether that
     * outcome is guaranteed to produce that property in state.  Any given output may be provided for all outcomes or
     * any subset of outcomes and perhaps only optionally for some of them.
     *
     * Example:
     *     public OutputState[] getOutputs() {
     *         return new OutputState[] {
     *             new OutputState(PASSWORD),
     *             new OutputState(config.mode(), singletonMap("*", false)
     *         };
     *     }
     *
     * In this example we declare that the node will produce an output named PASSWORD. The lack of an outcome map
     * indicates that this output is provided for all outcomes.  The node also outputs a property named via
     * config.mode() that is optional for all of the node's outcomes, i.e. it may or may not be present for
     * downstream nodes to consume.  This type of output is best consumed by other nodes by declaring an InputState
     * such as new InputState(config.mode(), false).
     *
     * This list is used by tree validation to report errors in tree construction.
     *
     * @return The list of shared state data.
     */
    @Supported
    default OutputState[] getOutputs() {
        return new OutputState[] {};
    }
}
