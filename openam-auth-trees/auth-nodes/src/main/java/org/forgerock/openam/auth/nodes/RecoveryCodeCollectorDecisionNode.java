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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode.OutcomeProvider;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node.Metadata;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.Config;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.DeviceProfileManager;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.UserOathDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.assistedinject.Assisted;

/**
 * This authentication node allows one to provide an OATH or Push recovery code to authenticate themselves as a second
 * factor.
 */
@Metadata(outcomeProvider = OutcomeProvider.class, configClass = Config.class)
public class RecoveryCodeCollectorDecisionNode extends AbstractDecisionNode {

    private static final Logger LOGGER = LoggerFactory.getLogger("amAuth");
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/RecoveryCodeCollectorDecisionNode";
    private final Config config;
    private final UserPushDeviceProfileManager pushDeviceProfileManager;
    private final UserOathDeviceProfileManager oathDeviceProfileManager;
    private final UserWebAuthnDeviceProfileManager webauthnDeviceProfileManager;

    /**
     * Configuration for Recovery Code Collector Decision Node.
     */
    public interface Config {

        /**
         * Returns the recovery code type associated with this node.
         *
         * @return The recovery code type.
         */
        @Attribute(order = 100)
        default RecoveryCodeType recoveryCodeType() {
            return RecoveryCodeType.OATH;
        }
    }

    /**
     * Guice constructor.
     *
     * @param config The configuration of this node.
     * @param pushDeviceProfileManager The profile manager for push devices.
     * @param oathDeviceProfileManager The profile manager for OATH devices.
     * @param webauthnDeviceProfileManager The profile manager for WebAuthn devices.
     */
    @Inject
    public RecoveryCodeCollectorDecisionNode(@Assisted Config config,
                                             UserPushDeviceProfileManager pushDeviceProfileManager,
                                             UserOathDeviceProfileManager oathDeviceProfileManager,
                                             UserWebAuthnDeviceProfileManager webauthnDeviceProfileManager) {
        this.config = config;
        this.pushDeviceProfileManager = pushDeviceProfileManager;
        this.oathDeviceProfileManager = oathDeviceProfileManager;
        this.webauthnDeviceProfileManager = webauthnDeviceProfileManager;
    }

    @Override
    public Action process(TreeContext context) {
        LOGGER.debug("RecoveryCodeCollectorDecisionNode started");

        DeviceProfileManager<? extends DeviceSettings> profileManager;
        switch (config.recoveryCodeType()) {
        case OATH:
            profileManager = oathDeviceProfileManager;
            break;
        case PUSH:
            profileManager = pushDeviceProfileManager;
            break;
        case WEB_AUTHN:
        default:
            profileManager = webauthnDeviceProfileManager;
            break;
        }

        return context.getCallback(NameCallback.class)
                .map(NameCallback::getName)
                .filter(code -> !Strings.isNullOrEmpty(code))
                .map(code -> goTo(isRecoveryCodeValid(context, profileManager, code)).build())
                .orElseGet(() -> collectRecoveryCode(context));
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(field("recoveryCodeType", config.recoveryCodeType().name())));
    }

    private <T extends DeviceSettings> boolean isRecoveryCodeValid(TreeContext context,
                                                                   DeviceProfileManager<T> profileManager,
                                                                   String code) {
        String username = context.sharedState.get(USERNAME).asString();
        String realm = context.sharedState.get(REALM).asString();
        try {
            T device = CollectionUtils.getFirstItem(profileManager.getDeviceProfiles(username, realm));
            if (device != null) {
                if (device.useRecoveryCode(code)) {
                    profileManager.saveDeviceProfile(username, realm, device);
                    return true;
                }
            }
        } catch (DevicePersistenceException dpe) {
            LOGGER.error("An error occurred while verifying the recovery code", dpe);
        }

        return false;
    }

    private Action collectRecoveryCode(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        return send(new NameCallback(bundle.getString("callback.recoveryCode"))).build();
    }

    /**
     * The type of the recovery code.
     */
    public enum RecoveryCodeType {

        /**
         * OATH recovery code.
         */
        OATH,
        /**
         * Push recovery code.
         */
        PUSH,
        /**
         * WebAuthn recovery code.
         */
        WEB_AUTHN
    }
}
