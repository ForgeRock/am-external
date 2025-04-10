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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import java.util.Optional;

import org.forgerock.openam.core.realms.Realm;

import com.sun.identity.idm.AMIdentity;

/**
 * Provider for getting AMIdentity objects in a node.
 */
public interface NodeUserIdentityProvider {
    /**
     * Gets the AMIdentity object from the information stored in TreeContext.
     *
     * <p>Requires either {@link org.forgerock.openam.auth.node.api.SharedStateConstants#USERNAME} and
     * {@link org.forgerock.openam.auth.node.api.SharedStateConstants#REALM} to be present in the state,
     * or {@link TreeContext#universalId} to be present in the context.
     *
     * @param universalId An optional universal id.
     * @param nodeState   The state data for the node calling this method.
     * @return The AMIdentity object.
     */
    Optional<AMIdentity> getAMIdentity(Optional<String> universalId, NodeState nodeState);

    /**
     * Get the universal id of the identity from the information available in the tree state. Only works for identity
     * type USER.
     *
     * <p>Requires {@link org.forgerock.openam.auth.node.api.SharedStateConstants#USERNAME} and
     * {@link org.forgerock.openam.auth.node.api.SharedStateConstants#REALM} to be present in the state.
     *
     * @param nodeState The state data for the node calling this method.
     * @return An optional of the universalId if it can be found, else an empty optional.
     */
    Optional<String> getUniversalId(NodeState nodeState);

    /**
     * Get the universal id of the identity from the username and realm. Only works for identity type
     * USER.
     * @param username the username of the identity
     * @param realm the realm of the identity
     * @return An optional of the universalId if it can be found, else an empty optional.
     */
    Optional<String> getUniversalId(String username, Realm realm);
}
