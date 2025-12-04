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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.EXISTING_SESSION;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.HEADERS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;

/**
 * Script bindings for the OidcNode script.
 */
public final class OidcNodeBindings implements LegacyScriptBindings {

    private static final String JWT_CLAIMS = "jwtClaims";

    private final JsonValue jwtClaims;
    private final NodeState nodeState;
    private final Map<String, List<String>> headers;
    private final Object existingSession;

    /**
     * Constructor for OidcNodeBindings.
     *
     * @param builder The builder.
     */
    private OidcNodeBindings(Builder builder) {
        this.jwtClaims = builder.jwtClaims;
        this.nodeState = builder.nodeState;
        this.headers = builder.headers;
        this.existingSession = builder.existingSession;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the Builder.
     */
    public static OidcNodeBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(JWT_CLAIMS, jwtClaims);
        bindings.put(HEADERS_IDENTIFIER, headers);
        bindings.put(STATE_IDENTIFIER, nodeState);
        bindings.putIfDefined(EXISTING_SESSION, existingSession);
        return bindings;
    }

    /**
     * Step 1 of the builder.
     */
    public interface OidcNodeBindingsStep1 {
        /**
         * Sets JWT claims.
         *
         * @param jwtClaims the JWT claims
         * @return the next step of the {@link Builder}
         */
        OidcNodeBindingsStep2 withJwtClaims(JsonValue jwtClaims);
    }

    /**
     * Step 2 of the builder.
     */
    public interface OidcNodeBindingsStep2 {
        /**
         * Sets the {@link NodeState}.
         *
         * @param nodeState the {@link NodeState}
         * @return the next step of the {@link Builder}
         */
        OidcNodeBindingsStep3 withNodeState(NodeState nodeState);
    }

    /**
     * Step 3 of the builder.
     */
    public interface OidcNodeBindingsStep3 {
        /**
         * Sets the headers.
         *
         * @param headers the headers
         * @return the next step of the {@link Builder}
         */
        OidcNodeBindingsStep4 withHeaders(Map<String, List<String>> headers);
    }

    /**
     * Step 4 of the builder.
     */
    public interface OidcNodeBindingsStep4 {
        /**
         * Sets existing session.
         *
         * @param existingSession the existing session
         * @return the next step of the {@link Builder}
         */
        OidcNodeBindingsFinalStep withExistingSession(Map<String, String> existingSession);
    }

    /**
     * Final step of the builder.
     */
    public interface OidcNodeBindingsFinalStep {
        /**
         * Build the {@link OidcNodeBindings}.
         *
         * @return The built {@link OidcNodeBindings}.
         */
        OidcNodeBindings build();
    }


    /**
     * Builder object to construct a {@link OidcNodeBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder implements OidcNodeBindingsStep1,
            OidcNodeBindingsStep2, OidcNodeBindingsStep3, OidcNodeBindingsStep4, OidcNodeBindingsFinalStep {

        private JsonValue jwtClaims;
        private NodeState nodeState;
        private Map<String, List<String>> headers;
        private Map<String, String> existingSession;

        /**
         * Sets the Jwt Claims {@link JsonValue}.
         *
         * @param jwtClaims The JWT Claims.
         * @return The next step of the builder.
         */
        @Override
        public OidcNodeBindingsStep2 withJwtClaims(JsonValue jwtClaims) {
            this.jwtClaims = jwtClaims;
            return this;
        }

        /**
         * Sets the {@link NodeState}s.
         *
         * @param nodeState The {@link NodeState}.
         * @return The next step of the builder.
         */
        @Override
        public OidcNodeBindingsStep3 withNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * Sets the headers.
         *
         * @param headers The headers.
         * @return The next step of the builder.
         */
        @Override
        public OidcNodeBindingsStep4 withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the existing session.
         *
         * @param existingSession The existing session.
         * @return The next step of the builder.
         */
        @Override
        public OidcNodeBindingsFinalStep withExistingSession(Map<String, String> existingSession) {
            this.existingSession = existingSession;
            return this;
        }

        /**
         * Creates the {@link OidcNodeBindings} from the configured attributes.
         *
         * @return an instance of {@link OidcNodeBindings}.
         */
        public OidcNodeBindings build() {
            return new OidcNodeBindings(this);
        }
    }
}
