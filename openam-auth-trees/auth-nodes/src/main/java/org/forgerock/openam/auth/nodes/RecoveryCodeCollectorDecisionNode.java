/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.NameCallback;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode.OutcomeProvider;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node.Metadata;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.Config;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final UserDeviceSettingsDao<OathDeviceSettings> oathDevicesDao;
    private final UserDeviceSettingsDao<PushDeviceSettings> pushDevicesDao;

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
     * @param oathDevicesDao The DAO to be used to access OATH device settings.
     * @param pushDevicesDao The DAO to be used to access Push device settings.
     */
    @Inject
    public RecoveryCodeCollectorDecisionNode(@Assisted Config config,
            UserDeviceSettingsDao<OathDeviceSettings> oathDevicesDao,
            UserDeviceSettingsDao<PushDeviceSettings> pushDevicesDao) {
        this.config = config;
        this.oathDevicesDao = oathDevicesDao;
        this.pushDevicesDao = pushDevicesDao;
    }

    @Override
    public Action process(TreeContext context) {
        LOGGER.debug("RecoveryCodeCollectorDecisionNode started");
        UserDeviceSettingsDao<? extends DeviceSettings> dao =
                RecoveryCodeType.OATH == config.recoveryCodeType() ? oathDevicesDao : pushDevicesDao;
        return context.getCallback(NameCallback.class)
                .map(NameCallback::getName)
                .filter(code -> !Strings.isNullOrEmpty(code))
                .map(code -> goTo(isRecoveryCodeValid(context, dao, code)).build())
                .orElseGet(() -> collectRecoveryCode(context));
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(field("recoveryCodeType", config.recoveryCodeType().name())));
    }

    private <T extends DeviceSettings> boolean isRecoveryCodeValid(TreeContext context, UserDeviceSettingsDao<T> dao,
            String code) {
        String username = context.sharedState.get(USERNAME).asString();
        String realm = context.sharedState.get(REALM).asString();
        try {
            T device = CollectionUtils.getFirstItem(dao.readDeviceSettings(username, realm));
            if (device != null) {
                List<String> recoveryCodes = new ArrayList<>(device.getRecoveryCodes());
                if (recoveryCodes.remove(code)) {
                    device.setRecoveryCodes(recoveryCodes);
                    dao.saveDeviceSettings(username, realm, singletonList(device));
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
        PUSH
    }
}
