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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingJsonUtils;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * If the postponing of device storage has been selected by the Device Binding Node, this node will function
 * to rebuild the device from the provided data, and persist the device into the user's profile.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = DeviceBindingStorageNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class DeviceBindingStorageNode extends AbstractDecisionNode implements DeviceBinding {

    private static final Logger logger = LoggerFactory.getLogger(DeviceBindingStorageNode.class);

    private final DeviceBindingManager deviceBindingManager;
    private final DeviceBindingJsonUtils deviceBindingJsonUtils;
    private final Realm realm;
    private final NodeUserIdentityProvider identityProvider;


    /**
     * Configuration for the node.
     */
    public interface Config {
    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param deviceBindingManager Instance of DeviceBindingManager
     * @param realm The realm
     * @param deviceBindingJsonUtils instance of the utils to help convert device to json
     * @param identityProvider The NodeUserIdentityProvider
     */
    @Inject
    public DeviceBindingStorageNode(DeviceBindingManager deviceBindingManager,
            @Assisted Realm realm,
            DeviceBindingJsonUtils deviceBindingJsonUtils,
            NodeUserIdentityProvider identityProvider) {
        this.deviceBindingManager = deviceBindingManager;
        this.realm = realm;
        this.deviceBindingJsonUtils = deviceBindingJsonUtils;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        try {
            Optional<AMIdentity> user = identityProvider.getAMIdentity(context.universalId,
                    context.getStateFor(this));
            if (user.isEmpty()) {
                throw new DevicePersistenceException("Failed to get the "
                        + "AMIdentity object in the realm " + realm.asPath());
            }
            JsonValue device = context.getStateFor(this).get(DEVICE);
            if (device == null) {
                throw new IllegalStateException("Cannot find Device data.");
            }
            deviceBindingManager.saveDeviceProfile(user.get().getName(), user.get().getRealm(),
                    deviceBindingJsonUtils.toDeviceSettingValue(device));

            return goTo(true).build();
        } catch (DevicePersistenceException e) {
            logger.error("Device Persistence Error.", e);
        } catch (IOException e) {
            logger.error("Failed to convert JsonValue to DeviceBindingSettings object", e);
        } catch (Exception e) {
            logger.error("Failed to store Device Binding data", e);
        }

        return goTo(false).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
            new InputState(REALM),
            new InputState(USERNAME),
            new InputState(DEVICE)
        };
    }
}
