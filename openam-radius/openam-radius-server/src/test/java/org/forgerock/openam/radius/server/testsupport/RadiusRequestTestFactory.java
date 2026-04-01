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
package org.forgerock.openam.radius.server.testsupport;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.security.SecureRandom;
import java.util.UUID;

import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.server.RadiusRequest;

/**
 * Factory for the creation of {@link RadiusRequest} objects in known states for testing purposes.
 */
public class RadiusRequestTestFactory {

    /**
     * The secret used to encrypt the user password attribute in test requests.
     */
    public static final String HMAC_MD5_SECRET = "clientSecret";

    /**
     * Creates a basic Access-Request RadiusRequest with the specified userName and userPassword attributes.
     *
     * @param userPassword the user password to include in the request
     * @param userName the username to include in the request
     * @return a {@link RadiusRequest} containing the specified userName and userPassword attributes
     */
    public static RadiusRequest basicAccessRequest(String userPassword, String userName) {
        return RadiusRequestTestFactory.requestBuilder()
                .withUserNameAttribute(userName)
                .withUserPasswordAttribute(userPassword)
                .build();
    }

    /**
     * Creates an Access-Request RadiusRequest with the specified answer and state attributes.
     *
     * @param answer the answer to include in the request
     * @param state the state to include in the request
     * @return a {@link RadiusRequest} containing the specified answer and state attributes
     */
    public static RadiusRequest accessRequestWithAnswer(String answer, String state) {
        return RadiusRequestTestFactory.requestBuilder()
                .withUserPasswordAttribute(answer)
                .withStateAttribute(state)
                .build();
    }

    /**
     * Creates a {@link RadiusRequestBuilder} with default values.
     *
     * @return a new {@link RadiusRequestBuilder} with default test values
     */
    public static RadiusRequestBuilder requestBuilder() {
        return new RadiusRequestBuilder();
    }

    /**
     * Builder class for creating test Access-Request {@link RadiusRequest} instances.
     */
    public static class RadiusRequestBuilder {

        private final Packet requestPacket = new AccessRequest((short) currentTimeMillis(),
                new RequestAuthenticator(new SecureRandom()));

        private RadiusRequestBuilder() {
            // private constructor
        }

        /**
         * Adds a {@link UserNameAttribute} to the request.
         *
         * @param username the username to add
         * @return this {@link RadiusRequestBuilder}
         */
        public RadiusRequestBuilder withUserNameAttribute(String username) {
            UserNameAttribute userNameAttribute = new UserNameAttribute(username);
            requestPacket.addAttribute(userNameAttribute);
            return this;
        }

        /**
         * Adds a {@link UserPasswordAttribute} to the request.
         *
         * @param password the password to add
         * @return this {@link RadiusRequestBuilder}
         */
        public RadiusRequestBuilder withUserPasswordAttribute(String password) {
            UserPasswordAttribute passwordAttribute = new UserPasswordAttribute(requestPacket.getAuthenticator(),
                    HMAC_MD5_SECRET, password);
            requestPacket.addAttribute(passwordAttribute);
            return this;
        }

        /**
         * Adds a {@link StateAttribute} to the request.
         *
         * @param state the state to add
         * @return this {@link RadiusRequestBuilder}
         */
        public RadiusRequestBuilder withStateAttribute(String state) {
            StateAttribute stateAttribute = new StateAttribute(state);
            requestPacket.addAttribute(stateAttribute);
            return this;
        }

        /**
         * Builds the {@link RadiusRequest}.
         *
         * @return the built {@link RadiusRequest}
         */
        public RadiusRequest build() {
            return new RadiusRequest(requestPacket, UUID.randomUUID().toString());
        }
    }
}
