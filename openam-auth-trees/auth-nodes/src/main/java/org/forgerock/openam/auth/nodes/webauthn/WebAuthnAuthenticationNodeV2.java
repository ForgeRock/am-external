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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.am.identity.application.IdentityStoreFactory;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.PageNodeCompatibility;
import org.forgerock.openam.auth.nodes.webauthn.flows.AuthenticationFlow;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AuthDataDecoder;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.utils.RecoveryCodeGenerator;

import com.google.inject.assistedinject.Assisted;

/**
 * A web authentication, authentication node. Uses client side javascript to interact with the browser and negotiate
 * authentication according to the webauthn spec. If the credentials, public key encrypted challenge response and
 * other details are valid, the outcome of the node is 'true'.<p>
 * This removes the ability to configure the challenge to be returned as a script, and will in future add the ability
 * to use conditional mediation and a manual button.
 */
@SuppressWarnings("ALL")
@Node.Metadata(outcomeProvider = WebAuthnAuthenticationNode.OutcomeProvider.class,
        configClass = WebAuthnAuthenticationNodeV2.Config.class,
        tags = {"mfa", "multi-factor authentication"},
        pageNodeCompatibility = PageNodeCompatibility.STANDALONE)
@Node.VersionMetadata(name = "WebAuthnAuthenticationNode", version = 2,
        upgrader = AbstractWebAuthnNode.WebAuthnV2NodeUpgrader.class)
public class WebAuthnAuthenticationNodeV2 extends WebAuthnAuthenticationNode {

    /**
     * Configuration for the node.
     */
    public interface Config extends WebAuthnAuthenticationNode.Config {

        @Attribute(order = 10)
        @Override
        default Optional<String> relyingPartyDomain() {
            return Optional.empty();
        }

        @Attribute(order = 15)
        @Override
        default Set<String> origins() {
            return Collections.emptySet();
        }

        @Override
        @Attribute(order = 20)
        default UserVerificationRequirement userVerificationRequirement() {
            return UserVerificationRequirement.PREFERRED;
        }

        @Attribute(order = 30)
        @Override
        default boolean isRecoveryCodeAllowed() {
            return false;
        }

        @Attribute(order = 40)
        @Override
        default int timeout() {
            return 60;
        }

        @Attribute(order = 50)
        @Override
        default boolean requiresResidentKey() {
            return false;
        }

        // this is hard coded to false for the v2 node
        @Override
        default boolean asScript() {
            return false;
        }

        @Attribute(order = 70)
        @Override
        default boolean detectSignCountMismatch() {
            return false;
        }

        @Attribute(order = 80)
        @Override
        default Mediation mediation() {
            return Mediation.DEFAULT;
        }

        @Attribute(order = 90)
        @Override
        default boolean conditionalManualButton() {
            return false;
        }

    }

    /**
     * The constructor.
     *
     * @param config                 the node's config.
     * @param realm                  the realm.
     * @param authenticationFlow     the authentication flow as executable steps.
     * @param clientScriptUtilities  utilities for handling the client side script.
     * @param webAuthnProfileManager management of user's devices
     * @param secureRandom           instance of the secure random generator
     * @param authDataDecoder        instance of the auth data decoder
     * @param recoveryCodeGenerator  instance of the recovery code generator.
     * @param identityStoreFactory   an {@link IdentityStoreFactory} instance
     * @param identityProvider       the identity provider
     * @param deserializer           instance of the assertion object deserializer
     */
    @Inject
    public WebAuthnAuthenticationNodeV2(@Assisted Config config, @Assisted Realm realm,
            AuthenticationFlow authenticationFlow,
            ClientScriptUtilities clientScriptUtilities,
            UserWebAuthnDeviceProfileManager webAuthnProfileManager,
            SecureRandom secureRandom, AuthDataDecoder authDataDecoder,
            RecoveryCodeGenerator recoveryCodeGenerator, IdentityStoreFactory identityStoreFactory,
            NodeUserIdentityProvider identityProvider, WebAuthnOutcomeDeserializer deserializer) {
        super(config, realm, authenticationFlow, clientScriptUtilities, webAuthnProfileManager, secureRandom,
                authDataDecoder, recoveryCodeGenerator, identityStoreFactory, identityProvider, deserializer);
    }

}
