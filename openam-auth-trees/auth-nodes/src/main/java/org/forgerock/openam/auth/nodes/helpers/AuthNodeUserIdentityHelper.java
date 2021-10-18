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
 * Copyright 2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.helpers;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Optional;

import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.StringUtils;
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
     * @param context The tree context.
     * @param identityUtils The IdentityUtils instance.
     * @param coreWrapper The CoreWrapper instance.
     * @return The AMIdentity object.
     */
    public static Optional<AMIdentity> getAMIdentity(TreeContext context,
            IdentityUtils identityUtils, CoreWrapper coreWrapper) {
        Optional<String> universalId = getUniversalId(context, identityUtils);
        Optional<AMIdentity> identity = Optional.empty();
        if (universalId.isPresent()) {
            identity = universalId.map(coreWrapper::getIdentity);
        } else {
            logger.warn("Failed to get the identity based on the information available in the context {}",
                    context.sharedState.getObject().toString());
        }
        return identity;
    }

    /**
     * Get the universal id of the identity from the information availab    le in the tree context.
     *
     * @param context The tree context.
     * @param identityUtils An instance of the IdentityUtils.
     * @return The universal Id.
     */
    public static Optional<String> getUniversalId(TreeContext context, IdentityUtils identityUtils) {
        if (context.universalId.isPresent()) {
            return context.universalId;
        }

        String username = context.sharedState.get(USERNAME).asString();
        if (StringUtils.isEmpty(username)) {
            logger.warn("Could not find the username in the tree context");
        }
        String realm = context.sharedState.get(REALM).asString();
        if (StringUtils.isEmpty(realm)) {
            logger.warn("Could not find the realm in the tree context");
        }
        return Optional.of(identityUtils.getUniversalId(username, USER, realm));
    }
}