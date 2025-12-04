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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOToken;

/**
 * A TreeHook encapsulates some functionality that should be executed at the end of a tree, after authentication.
 * They can be added by nodes.
 *
 * The following parameters can be injected into the constructor of a class implementing this interface using the
 * {@link Assisted} annotation:
 *  <ul>
 *  <li>{@link SSOToken}</li> - the token containing details of the new session created on successful completion
 *  of an authentication journey
 *  <li>{@link Response}</li> - the response sent to the user on successful completion
 *  of an authentication journey
 *  <li>{@link Request}</li> - the request submitted by the user agent
 *  <li>{@link Realm}</li> - the realm in which the authentication is taking place
 *  <li> {@code config}</li> - the node configuration object; the type of this configuration is defined
 *  by the annotation {@TreeHook.Metadata}
 *  </ul>
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
    /** Node config key. */
    String NODE_CONFIG_KEY = "nodeConfig";
    /** Arguments for the session hook. */
    String HOOK_DATA = "data";

    /**
     * Main method that will contain the logic that needs to be executed when the session hook is called.
     *
     * @throws TreeHookException if an exception occurs.
     */
    void accept() throws TreeHookException;

    /**
     * Main method that will contain the logic that needs to be executed on tree failure.
     * The session cannot be read or modified on failure.
     *
     * @throws TreeHookException if an exception occurs.
     */
    default void acceptFailure() throws TreeHookException {
        // do nothing
    }

    /**
     * Main method that will contain the logic that needs to be executed on tree exception.
     * The session cannot be read or modified on exception.
     *
     * @throws TreeHookException if an exception occurs.
     */
    default void acceptException() throws TreeHookException {
        // do nothing
    }

    /** Annotation that describes the metadata of the node. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Metadata {
        /**
         * An interface describing the configuration of the node. The interface should contain methods annotated with.
         * {@code Attribute}.
         *
         * @return The config class.
         */
        Class<?> configClass();
    }
}
