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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oath;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oath.OathRegistrationHelper.NEXT_OPTION;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * The OATH Registration node is a registration node that does not authenticate a user but
 * allows a user already authenticated earlier to register their mobile device.
 */
@Node.Metadata(outcomeProvider = OathRegistrationNode.OutcomeProvider.class,
        configClass = OathRegistrationConfig.class,
        tags = {"mfa", "multi-factor authentication"})
public class OathRegistrationNode extends AbstractMultiFactorNode {

    private static final Logger logger = LoggerFactory.getLogger(OathRegistrationNode.class);
    private static final String BUNDLE = OathRegistrationNode.class.getName();

    private final OathRegistrationConfig config;
    private final OathRegistrationHelper oathRegistrationHelper;

    /**
     * The Oath registration node constructor.
     *
     * @param config                  the node configuration.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param oathRegistrationHelper  the oath registration helper class.
     * @param identityProvider        the identity provider.
     */
    @Inject
    public OathRegistrationNode(
            @Assisted OathRegistrationConfig config,
            MultiFactorNodeDelegate<AuthenticatorOathService> multiFactorNodeDelegate,
            OathRegistrationHelper oathRegistrationHelper, NodeUserIdentityProvider identityProvider) {
        super(multiFactorNodeDelegate, identityProvider);
        this.config = config;
        this.oathRegistrationHelper = oathRegistrationHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OathRegistrationNode started.");

        String username = context.sharedState.get(USERNAME).asString();
        if (StringUtils.isBlank(username)) {
            logger.error("No username found.");
            throw new NodeProcessException("Expected username to be set.");
        }

        if (context.getCallback(ConfirmationCallback.class)
                .filter(callback -> callback.getSelectedIndex() == NEXT_OPTION)
                .isPresent()) {
            if (config.postponeDeviceProfileStorage()) {
                logger.debug("Postpone storing device data.");
                return buildAction(SUCCESS_OUTCOME_ID, context);
            } else {
                OathDeviceSettings oathDeviceSettings = oathRegistrationHelper
                        .getDeviceProfileFromSharedState(context);
                if (oathDeviceSettings == null) {
                    logger.error("No device profile found on shared state");
                    return buildAction(FAILURE_OUTCOME_ID, context);
                }

                AMIdentity userIdentity = getIdentity(context);

                logger.debug("Saving oath device profile.");
                List<String> recoveryCodes = oathRegistrationHelper.saveDeviceSettings(oathDeviceSettings,
                        userIdentity, config.generateRecoveryCodes());

                if (CollectionUtils.isNotEmpty(recoveryCodes)) {
                    logger.debug("Completed OATH registration. Sending recovery codes.");
                    return buildActionWithRecoveryCodes(context, oathDeviceSettings.getDeviceName(), recoveryCodes);
                } else {
                    logger.debug("Completed OATH registration.");
                    return buildAction(SUCCESS_OUTCOME_ID, context);
                }
            }
        }

        return startRegistration(context);
    }

    private Action startRegistration(TreeContext context) throws NodeProcessException {
        OathDeviceSettings oathDeviceSettings = oathRegistrationHelper.createDeviceSettings(config);
        try {
            AMIdentity userIdentity = getIdentity(context);

            List<Callback> callbacks = createScanQRCodeCallbacks(context, oathDeviceSettings, userIdentity);

            oathRegistrationHelper.updateSharedState(context.getStateFor(this),
                    oathDeviceSettings, config.generateRecoveryCodes());

            return send(callbacks)
                    .replaceSharedState(context.sharedState.copy())
                    .build();
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }

    private List<Callback> createScanQRCodeCallbacks(TreeContext context, OathDeviceSettings deviceProfile,
                                             AMIdentity identity) throws NodeProcessException {
        Map<String, String> params = oathRegistrationHelper.buildURIParameters(deviceProfile, identity.getName(),
                config);

        Callback textOutputCallback = oathRegistrationHelper.createLocalizedTextCallback(context, this.getClass(),
                config.scanQRCodeMessage());
        Callback hiddenCallback = oathRegistrationHelper.createHiddenCallback(identity, params, config.algorithm(),
                config.issuer(), config.accountName().toString());
        Callback qrCodeCallback = oathRegistrationHelper.createQRCodeCallback(identity, params, config.algorithm(),
                config.issuer(), config.accountName().toString());
        Callback confirmationCallback = oathRegistrationHelper.createLocalizedConfirmationCallback(context,
                this.getClass());

        return ImmutableList.of(
                textOutputCallback,
                qrCodeCallback,
                hiddenCallback,
                confirmationCallback
        );
    }

    @Override
    protected Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        return oathRegistrationHelper.cleanupSharedState(context, builder, config.postponeDeviceProfileStorage());
    }

    /**
     * Provides the oath registration node's set of outcomes.
     */
    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    OathRegistrationNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID))
            );
        }
    }

}
