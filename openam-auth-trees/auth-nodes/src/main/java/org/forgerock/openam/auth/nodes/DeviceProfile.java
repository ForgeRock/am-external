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
 * Copyright 2020-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;


import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Interface to provide default common methods and constants for Device related Nodes.
 */
interface DeviceProfile {

    /**
     * TreeContext Variable name for Device Profile.
     */
    String DEVICE_PROFILE_CONTEXT_NAME = "forgeRock.device.profile";

    /**
     * Json attribute name for Device Metadata.
     */
    String METADATA_ATTRIBUTE_NAME = "metadata";

    /**
     * Json attribute name for Device Identifier.
     */
    String IDENTIFIER_ATTRIBUTE_NAME = "identifier";

    /**
     * Json attribute name for Device Location.
     */
    String LOCATION_ATTRIBUTE_NAME = "location";

    /**
     * Json attribute name for Last Selected Date.
     */
    String LAST_SELECTED_DATE = "lastSelectedDate";

    /**
     * Json attribute name for Device Alias.
     */
    String ALIAS = "alias";

    /**
     * Json attribute name for location latitude.
     */
    String LATITUDE = "latitude";
    /**
     * Json attribute name for location longitude.
     */
    String LONGITUDE = "longitude";

    /**
     * Retrieve the device attribute from the TreeContext.
     *
     * @param context       The TreeContext
     * @param attributeName The Device Attribute Name
     * @return The JSON value which represent the device attribute
     * @throws NodeProcessException When device attribute not found from the context.
     */

    default JsonValue getAttribute(TreeContext context, String attributeName) throws NodeProcessException {
        JsonValue profile = context.sharedState.get(DEVICE_PROFILE_CONTEXT_NAME);
        if (profile != null) {
            if (profile.isDefined(attributeName)) {
                return profile.get(attributeName);
            }
        }
        throw new NodeProcessException(
                "Device Profile Collector Node to collect device attribute is required: " + attributeName);
    }

    /**
     * Retrieve the user identity.
     *
     * @param universalId the un
     * @param nodeState The NodeState
     * @param coreWrapper An instace of the CoreWrapper
     * @param identityUtils An instance of the IdentityUtils,
     * @return The identity represent the user.
     * @throws NodeProcessException When username not found from context or datasource or inactive
     */
    default AMIdentity getUserIdentity(Optional<String> universalId, NodeState nodeState, CoreWrapper coreWrapper,
            IdentityUtils identityUtils) throws NodeProcessException, IdRepoException, SSOException {
        Optional<AMIdentity> userIdentity = getAMIdentity(universalId, nodeState, identityUtils, coreWrapper);
        if (userIdentity.isEmpty() || !userIdentity.get().isExists() || !userIdentity.get().isActive()) {
            throw new NodeProcessException("User does not exist or inactive");
        }
        return userIdentity.get();
    }
}
