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

package org.forgerock.openam.auth.nodes.helpers;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.idm.AMIdentity;

/**
 * Helper methods to deal the fetching of AMIdentity.
 */
public final class AuthNodeUserIdentityHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthNodeUserIdentityHelper.class);

    private AuthNodeUserIdentityHelper() {
        //private constructor
    }

    /**
     * Gets the AMIdentity object from the information stored in TreeContext.
     *
     *<p>Requires either {@link org.forgerock.openam.auth.node.api.SharedStateConstants#USERNAME} and
     *  {@link org.forgerock.openam.auth.node.api.SharedStateConstants#REALM} to be present in the state,
     *  or {@link TreeContext#universalId} to be present in the context.
     *
     * @param universalId An optional universal id.
     * @param nodeState The state data for the node calling this method.
     * @param identityUtils The IdentityUtils instance.
     * @param coreWrapper The CoreWrapper instance.
     * @return The AMIdentity object.
     */
    public static Optional<AMIdentity> getAMIdentity(Optional<String> universalId, NodeState nodeState,
            IdentityUtils identityUtils, CoreWrapper coreWrapper) {
        universalId = universalId.or(() -> getUniversalId(nodeState, identityUtils));
        Optional<AMIdentity> identity = Optional.empty();
        if (universalId.isPresent()) {
            identity = universalId.map(coreWrapper::getIdentity);
        } else {
            logger.warn("Failed to get the identity based on the information available in the context {}",
                    Arrays.toString(nodeState.keys().toArray()));
        }
        return identity;
    }

    /**
     * Get the universal id of the identity from the information available in the tree state.
     *
     *<p>Requires {@link org.forgerock.openam.auth.node.api.SharedStateConstants#USERNAME} and
     *  {@link org.forgerock.openam.auth.node.api.SharedStateConstants#REALM} to be present in the state.
     *
     * @param nodeState The state data for the node calling this method.
     * @param identityUtils An instance of the IdentityUtils.
     * @return An optional of the universalId if it can be found, else an empty optional.
     */
    public static Optional<String> getUniversalId(NodeState nodeState, IdentityUtils identityUtils) {
        JsonValue username = nodeState.get(USERNAME);
        if (username == null || username.isNull() || isEmpty(username.asString())) {
            logger.warn("Could not find the username in the tree context");
            return Optional.empty();
        }
        JsonValue realm = nodeState.get(REALM);
        if (realm == null || realm.isNull() || isEmpty(realm.asString())) {
            logger.warn("Could not find the realm in the tree context");
            return Optional.empty();
        }
        return identityUtils.getUniversalId(username.asString(), realm.asString(), USER);
    }
}
