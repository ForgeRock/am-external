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
 * Copyright 2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oath;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.core.rest.devices.services.oath.AuthenticatorOathService;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_DEVICE_PROFILE_KEY;
import static org.forgerock.openam.auth.nodes.oath.OathNodeConstants.OATH_ENABLE_RECOVERY_CODE_KEY;

/**
 * If the postponing of device storage has been selected by the registration node, this node will persist the device
 * into the user's profile.
 */
@Node.Metadata(outcomeProvider = OathDeviceStorageNode.OutcomeProvider.class,
        configClass = OathDeviceStorageNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class OathDeviceStorageNode extends AbstractMultiFactorNode {

    private static final String BUNDLE = OathDeviceStorageNode.class.getName();

    final Logger logger = LoggerFactory.getLogger(OathDeviceStorageNode.class);

    private final Realm realm;
    private final OathDeviceProfileHelper deviceProfileHelper;

    /**
     * Configuration for the node.
     */
    public interface Config { }

    /**
     * The constructor.
     *
     * @param realm the realm.
     * @param deviceProfileHelper stores device profiles.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param identityService an instance of the IdentityService.
     * @param coreWrapper A core wrapper instance.
     */
    @Inject
    public OathDeviceStorageNode(@Assisted Realm realm,
                                 OathDeviceProfileHelper deviceProfileHelper,
                                 MultiFactorNodeDelegate<AuthenticatorOathService> multiFactorNodeDelegate,
                                 LegacyIdentityService identityService,
                                 CoreWrapper coreWrapper) {
        super(realm, coreWrapper, multiFactorNodeDelegate, identityService);
        this.realm = realm;
        this.deviceProfileHelper = deviceProfileHelper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        OathDeviceSettings oathDeviceSettings = deviceProfileHelper
                .getDeviceProfileFromSharedState(context, OATH_DEVICE_PROFILE_KEY);
        if (oathDeviceSettings == null) {
            logger.error("No device profile found on shared state");
            return buildAction(FAILURE_OUTCOME_ID, context);
        }

        boolean generateRecoveryCodes = context.getStateFor(this).isDefined(OATH_ENABLE_RECOVERY_CODE_KEY)
                ? context.getStateFor(this).get(OATH_ENABLE_RECOVERY_CODE_KEY).asBoolean()
                : false;

        AMIdentity userIdentity = getIdentity(context);

        logger.debug("Saving oath device profile.");
        List<String> recoveryCodes = deviceProfileHelper.saveDeviceSettings(oathDeviceSettings,
                json(object()), userIdentity, generateRecoveryCodes);
        setUserToNotSkippable(userIdentity, realm.toString());

        if (CollectionUtils.isNotEmpty(recoveryCodes)) {
            logger.debug("Completed OATH registration. Sending recovery codes.");
            return buildActionWithRecoveryCodes(context, oathDeviceSettings.getDeviceName(), recoveryCodes);
        } else {
            logger.debug("Completed OATH registration.");
            return buildAction(SUCCESS_OUTCOME_ID, context);
        }
    }

    @Override
    protected Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        return builder;
    }

    /**
     * Set the current user should not skip MFA.
     *
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     */
    @VisibleForTesting
    void setUserToNotSkippable(AMIdentity amIdentity, String realm) {
        setUserSkip(amIdentity, realm, SkipSetting.NOT_SKIPPABLE);
    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    OathDeviceStorageNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(SUCCESS_OUTCOME_ID, bundle.getString(SUCCESS_OUTCOME_ID)),
                    new Outcome(FAILURE_OUTCOME_ID, bundle.getString(FAILURE_OUTCOME_ID)));
        }
    }

}
