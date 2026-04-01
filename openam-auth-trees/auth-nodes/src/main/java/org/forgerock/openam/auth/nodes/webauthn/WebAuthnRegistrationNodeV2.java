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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.flows.RegisterFlowFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceJsonUtils;
import org.forgerock.openam.sm.validation.SecretIdValidator;
import org.forgerock.openam.utils.RecoveryCodeGenerator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.validation.PositiveIntegerValidator;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A web authentication registration node. Uses client side javascript to interact with the browser and negotiate
 * Registration according to the webauthn spec. Results in credentials, public key and other data which can be used
 * to authenticate.<p>
 * This removes the ability to configure the challenge to be returned as a script.
 */
@Node.Metadata(outcomeProvider = WebAuthnRegistrationNode.OutcomeProvider.class,
        configClass = WebAuthnRegistrationNodeV2.Config.class,
        tags = {"mfa", "multi-factor authentication"})
@Node.VersionMetadata(name = "WebAuthnRegistrationNode", version = 2,
        upgrader = AbstractWebAuthnNode.WebAuthnV2NodeUpgrader.class)
public class WebAuthnRegistrationNodeV2 extends WebAuthnRegistrationNode {

    /**
     * Configuration for the node.
     */
    public interface Config extends WebAuthnRegistrationNode.Config {

        @Attribute(order = 10, validators = RequiredValueValidator.class)
        @Override
        default String relyingPartyName() {
            return "ForgeRock";
        }

        @Attribute(order = 20)
        @Override
        Optional<String> relyingPartyDomain();

        @Attribute(order = 25)
        @Override
        default Set<String> origins() {
            return Collections.emptySet();
        }

        @Attribute(order = 30)
        @Override
        default UserVerificationRequirement userVerificationRequirement() {
            return UserVerificationRequirement.PREFERRED;
        }

        @Attribute(order = 40)
        @Override
        default AttestationPreference attestationPreference() {
            return AttestationPreference.NONE;
        }

        @Attribute(order = 50)
        @Override
        default Set<CoseAlgorithm> acceptedSigningAlgorithms() {
            Set<CoseAlgorithm> algorithms = new HashSet<>();
            algorithms.add(CoseAlgorithm.ES256);
            algorithms.add(CoseAlgorithm.RS256);
            return algorithms;
        }

        @Attribute(order = 60)
        @Override
        default AuthenticatorAttachment authenticatorAttachment() {
            return AuthenticatorAttachment.UNSPECIFIED;
        }

        @Attribute(order = 65, validators = SecretIdValidator.class)
        @Override
        default Optional<String> trustStoreAlias() {
            return Optional.of("trustalias");
        }

        @Attribute(order = 68)
        @Override
        default boolean enforceRevocationCheck() {
            return false;
        }

        @Attribute(order = 70)
        @Override
        default int timeout() {
            return 60;
        }

        @Attribute(order = 80)
        @Override
        default boolean excludeCredentials() {
            return false;
        }

        @Attribute(order = 90)
        @Override
        default boolean generateRecoveryCodes() {
            return true;
        }

        @Attribute(order = 100)
        @Override
        default boolean storeAttestationDataInTransientState() {
            return false;
        }

        @Attribute(order = 110)
        @Override
        default boolean postponeDeviceProfileStorage() {
            return false;
        }

        @Attribute(order = 120)
        @Override
        default boolean requiresResidentKey() {
            return false;
        }

        @Attribute(order = 130)
        @Override
        Optional<String> displayNameSharedState();

        // this is hard coded to false for the v2 node
        @Override
        default boolean asScript() {
            return false;
        }

        @Attribute(order = 150, requiredValue = true, validators = {PositiveIntegerValidator.class})
        @Override
        default int maxSavedDevices() {
            return 0;
        }

        @Attribute(order = 170)
        @Override
        default boolean validateFidoU2fAaguid() {
            return true;
        }

        @Attribute(order = 180)
        @Override
        default FidoCertificationLevel fidoCertificationLevel() {
            return FidoCertificationLevel.OFF;
        }
    }

    /**
     * The constructor.
     *
     * @param config                  node config.
     * @param realm                   the realm.
     * @param registerFlowFactory     a factory for the registration ceremony flow.
     * @param clientScriptUtilities   utilities for handling the client side scripts.
     * @param webAuthnProfileManager  managers user's device profiles.
     * @param secureRandom            instance of the secure random generator
     * @param recoveryCodeGenerator   instance of the recovery code generator
     * @param webAuthnDeviceJsonUtils Helper to convert device to json
     * @param identityProvider        the identity provider
     * @param deserializer            a deserializer for the webauthn outcome
     */
    @Inject
    public WebAuthnRegistrationNodeV2(@Assisted Config config, @Assisted Realm realm,
            RegisterFlowFactory registerFlowFactory,
            ClientScriptUtilities clientScriptUtilities,
            UserWebAuthnDeviceProfileManager webAuthnProfileManager,
            SecureRandom secureRandom, RecoveryCodeGenerator recoveryCodeGenerator,
            WebAuthnDeviceJsonUtils webAuthnDeviceJsonUtils,
            NodeUserIdentityProvider identityProvider,
            WebAuthnOutcomeDeserializer deserializer) {
        super(config, realm, registerFlowFactory, clientScriptUtilities, webAuthnProfileManager, secureRandom,
                recoveryCodeGenerator, webAuthnDeviceJsonUtils, identityProvider, deserializer);

    }
}
