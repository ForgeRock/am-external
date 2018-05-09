/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A TreeHook encapsulates some functionality that should be executed at the end of a tree, after authentication.
 * They can be added by nodes.
 *
 * @supported.all.api
 */
public interface TreeHook {
    /** Session hook class key. */
    String SESSION_HOOK_CLASS_KEY = "sessionHookClass";
    /** Node ID key. */
    String NODE_ID_KEY = "nodeId";
    /** Node type key. */
    String NODE_TYPE_KEY = "nodeType";

    /**
     * Main method that will contain the logic that needs to be executed when the session hook is called.
     *
     * @throws TreeHookException if an exception occurs.
     */
    void accept() throws TreeHookException;

    /** Annotation that describes the metadata of the node. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Metadata {
        /**
         * An interface describing the configuration of the node. The interface should contain methods annotated with.
         * {@code Attribute}.
         */
        Class<?> configClass();
    }
}
