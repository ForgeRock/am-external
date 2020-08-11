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
 * Copyright 2017-2019 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.openam.annotations.SupportedAll;

/**
 * A TreeHook encapsulates some functionality that should be executed at the end of a tree, after authentication.
 * They can be added by nodes.
 *
 */
@SupportedAll
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
