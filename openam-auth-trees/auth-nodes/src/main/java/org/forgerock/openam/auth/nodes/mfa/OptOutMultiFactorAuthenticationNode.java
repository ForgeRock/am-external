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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.MFA_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.OATH_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.PUSH_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.WEBAUTHN_METHOD;

import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.UserOathDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.core.rest.devices.services.push.AuthenticatorPushService;
import org.forgerock.openam.core.rest.devices.services.webauthn.AuthenticatorWebAuthnService;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.sun.identity.idm.AMIdentity;

/**
 * Node which allow users to skip or opt-out second factor authentication.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = OptOutMultiFactorAuthenticationNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class OptOutMultiFactorAuthenticationNode extends SingleOutcomeNode {

    private static final Logger logger = LoggerFactory.getLogger(OptOutMultiFactorAuthenticationNode.class);

    private final NodeUserIdentityProvider identityProvider;

    private MultiFactorNodeDelegate<?> multiFactorNodeDelegate;

    /**
     * The node constructor.
     *
     * @param identityProvider the identity provider.
     */
    @Inject
    public OptOutMultiFactorAuthenticationNode(NodeUserIdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OptOutMultiFactorAuthenticationNode started.");
        optOutMultiFactorAuthenticationForUserAccount(context);
        return goToNext().build();
    }

    /**
     * Sets the current user in the current realm to opt-out multi-factor authentication.
     * (both values, username and realm taken from the shared state).
     *
     * @param context the context of the tree authentication.
     * @throws NodeProcessException if either of these occur:
     *                              1. There is a problem reading either username or realm from the shared config.
     *                              2. An error happens when AM is setting the user as skippable.
     */
    private void optOutMultiFactorAuthenticationForUserAccount(TreeContext context) throws NodeProcessException {
        String realm = context.sharedState.get(SharedStateConstants.REALM).asString();

        String username = context.sharedState.get(SharedStateConstants.USERNAME).asString();
        if (StringUtils.isBlank(username)) {
            logger.debug("No username found.");
            throw new NodeProcessException("Expected username to be set.");
        }

        String mfaMethod = context.sharedState.get(MFA_METHOD).asString();
        if (StringUtils.isBlank(mfaMethod)) {
            logger.debug("No MFA method found.");
            throw new NodeProcessException("Expected MFA method to be set.");
        }

        multiFactorNodeDelegate = getMultiFactorNodeDelegate(mfaMethod);
        DeviceSettings deviceSettings = getDeviceSettings(mfaMethod, realm, username);
        AMIdentity amIdentity = getIdentityFromIdentifier(context);
        SkipSetting skippable = shouldSkip(amIdentity, realm);

        if (skippable == SkipSetting.SKIPPABLE
                || (skippable == SkipSetting.NOT_SKIPPABLE && deviceSettings == null)) {
            logger.debug("Skip Multi Factor Authentication node is being skipped. User '{}' already "
                    + "opt-out or is not allowed to skip.", username);
        } else {
            logger.debug("User '{}' opt-out second factor registration.", username);
            setUserToSkippable(amIdentity, realm);
        }
    }

    /**
     * Set the current device should be skipped.
     *
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     */
    @VisibleForTesting
    void setUserToSkippable(AMIdentity amIdentity, String realm) {
        try {
            multiFactorNodeDelegate.setUserSkip(amIdentity, realm, SkipSetting.SKIPPABLE);
        } catch (NodeProcessException e) {
            logger.debug("Unable to set user attribute as skippable.", e);
        }
    }

    /**
     * Determines if the current device should be skipped.
     *
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     * @return the user's skippable attribute.
     */
    @VisibleForTesting
    SkipSetting shouldSkip(AMIdentity amIdentity, String realm) throws NodeProcessException {
        return multiFactorNodeDelegate.shouldSkip(amIdentity, realm);
    }

    /**
     * Returns the user identity.
     *
     * @param context the context of the tree authentication.
     * @return the AM user identity object.
     * @throws NodeProcessException if could not retrieve the user identity.
     */
    @VisibleForTesting
    AMIdentity getIdentityFromIdentifier(TreeContext context) throws NodeProcessException {
        Optional<AMIdentity> userIdentity = identityProvider.getAMIdentity(context.universalId,
                context.getStateFor(this));
        if (userIdentity.isEmpty()) {
            throw new NodeProcessException("Failed to get the identity object");
        }
        return userIdentity.get();
    }

    /**
     * Return the second factor utility class.
     *
     * @param mfaMethod the second factor method.
     * @return the second factor utility object.
     * @throws NodeProcessException if unsupported MFA method has been set
     */
    @VisibleForTesting
    MultiFactorNodeDelegate<?> getMultiFactorNodeDelegate(String mfaMethod) throws NodeProcessException {
        MultiFactorNodeDelegate<?> delegate;
        switch (mfaMethod) {
        case PUSH_METHOD:
            delegate = InjectorHolder.getInstance(Key.get(
                    new TypeLiteral<MultiFactorNodeDelegate<AuthenticatorPushService>>() {
                    }));
            break;
        case OATH_METHOD:
            delegate = InjectorHolder.getInstance(Key.get(
                    new TypeLiteral<MultiFactorNodeDelegate<AuthenticatorOathService>>() {
                    }));
            break;
        case WEBAUTHN_METHOD:
            delegate = InjectorHolder.getInstance(Key.get(
                    new TypeLiteral<MultiFactorNodeDelegate<AuthenticatorWebAuthnService>>() {
                    }));
            break;
        default:
            logger.debug("Unsupported MFA method has been set.");
            throw new NodeProcessException("Unsupported MFA method has been set.");
        }
        return delegate;
    }

    /**
     * Retrieves the Push, Oath or WebAuthn device settings of the user's profile.
     *
     * @param mfaMethod the second factor method.
     * @param realm the realm.
     * @param username the username.
     * @return the device settings.
     * @throws NodeProcessException if a unsupported MFA method has been set or unable to retrieve the
     *                              device profile manager.
     */
    @VisibleForTesting
    DeviceSettings getDeviceSettings(String mfaMethod, String realm, String username) throws NodeProcessException {
        DeviceSettings deviceSettings;
        try {
            switch (mfaMethod) {
            case PUSH_METHOD:
                UserPushDeviceProfileManager userPushDeviceProfileManager =
                        InjectorHolder.getInstance(UserPushDeviceProfileManager.class);
                deviceSettings = CollectionUtils.getFirstItem(userPushDeviceProfileManager
                        .getDeviceProfiles(username, realm));
                break;
            case OATH_METHOD:
                UserOathDeviceProfileManager userOathDeviceProfileManager =
                        InjectorHolder.getInstance(UserOathDeviceProfileManager.class);
                deviceSettings = CollectionUtils.getFirstItem(userOathDeviceProfileManager
                        .getDeviceProfiles(username, realm));
                break;
            case WEBAUTHN_METHOD:
                UserWebAuthnDeviceProfileManager userWebAuthnDeviceProfileManager =
                        InjectorHolder.getInstance(UserWebAuthnDeviceProfileManager.class);
                deviceSettings = CollectionUtils.getFirstItem(userWebAuthnDeviceProfileManager
                        .getDeviceProfiles(username, realm));
                break;
            default:
                logger.debug("Unsupported MFA method has been set.");
                throw new NodeProcessException("Unsupported MFA method has been set.");
            }
        } catch (DevicePersistenceException dpe) {
            throw new NodeProcessException(dpe);
        }
        return deviceSettings;
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
            new InputState(REALM),
            new InputState(USERNAME),
            new InputState(MFA_METHOD)
        };
    }

    /**
     * The node configuration.
     */
    public interface Config {
    }
}
