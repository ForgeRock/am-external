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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.sm.DNMapper;

/**
 * If the postponing of device storage has been selected by the registration node, this node will function
 * to rebuild the device from the provided data, insert recovery codes if necessary and persist the device
 * into the user's profile.
 */
@Node.Metadata(outcomeProvider = WebAuthnDeviceStorageNode.OutcomeProvider.class,
        configClass = WebAuthnDeviceStorageNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class WebAuthnDeviceStorageNode extends AbstractWebAuthnNode {

    private static final String BUNDLE = WebAuthnDeviceStorageNode.class.getName();

    final Logger logger = LoggerFactory.getLogger(AbstractWebAuthnNode.class);

    private final WebAuthnDeviceStorageNode.Config config;
    private final Realm realm;
    private final WebAuthnDeviceJsonUtils webauthnDeviceJsonUtils;
    private final IdentityUtils identityUtils;
    private final CoreWrapper coreWrapper;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Specifies whether to generate recovery codes and store them in the shared state, and
         * the device profile.
         *
         * @return true if the codes are to be generated
         */
        @Attribute(order = 10)
        default boolean generateRecoveryCodes() {
            return true;
        }
    }

    /**
     * The constructor.
     *
     * @param config node config.
     * @param realm the realm.
     * @param clientScriptUtilities utilities for handling the client side scripts.
     * @param webAuthnProfileManager managers user's device profiles.
     * @param secureRandom instance of the secure random generator
     * @param recoveryCodeGenerator instance of the recovery code generator
     * @param webauthnDeviceJsonUtils instance of the utils to help convert device to json
     * @param identityUtils A {@code IdentityUtils} instance.
     * @param coreWrapper A core wrapper instance.
     */
    @Inject
    public WebAuthnDeviceStorageNode(@Assisted WebAuthnDeviceStorageNode.Config config, @Assisted Realm realm,
                                     ClientScriptUtilities clientScriptUtilities,
                                     UserWebAuthnDeviceProfileManager webAuthnProfileManager,
                                     SecureRandom secureRandom, RecoveryCodeGenerator recoveryCodeGenerator,
                                     WebAuthnDeviceJsonUtils webauthnDeviceJsonUtils,
                                     IdentityUtils identityUtils, CoreWrapper coreWrapper) {
        super(clientScriptUtilities, webAuthnProfileManager, secureRandom, recoveryCodeGenerator);
        this.config = config;
        this.realm = realm;
        this.webauthnDeviceJsonUtils = webauthnDeviceJsonUtils;
        this.identityUtils = identityUtils;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        JsonValue deviceJson = context.getState(AbstractWebAuthnNode.WEB_AUTHN_DEVICE_DATA);
        Action.ActionBuilder outcomeBuilder = Action.goTo(SUCCESS_OUTCOME_ID);
        JsonValue transientState = context.transientState.copy();

        logger.debug("storing device data in profile");

        try {
            Optional<AMIdentity> user = getAMIdentity(context, identityUtils, coreWrapper);
            if (user.isEmpty()) {
                throw new DevicePersistenceException("Failed to get the "
                        + "AMIdentity object in the realm " + realm.asPath());
            }
            WebAuthnDeviceSettings device = webauthnDeviceJsonUtils.toDeviceSettingValue(deviceJson);
            setRecoveryCodesOnDevice(config.generateRecoveryCodes(), device, transientState);
            webAuthnProfileManager.saveDeviceProfile(user.get().getName(),
                    DNMapper.orgNameToRealmName(user.get().getRealm()), device);

            logger.debug("outcome success");
            outcomeBuilder.replaceTransientState(transientState);
            return outcomeBuilder.build();
        } catch (CodeException | DevicePersistenceException | IOException e) {
            logger.debug("outcome failure", e);
            return Action.goTo(FAILURE_OUTCOME_ID).build();
        }
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    WebAuthnDeviceStorageNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)));
        }
    }

}
