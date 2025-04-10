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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.helpers;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.idm.AMIdentity;

/**
 * Helper methods to deal the fetching of AMIdentity.
 */
@Singleton
public final class AuthNodeUserIdentityHelper implements NodeUserIdentityProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuthNodeUserIdentityHelper.class);
    private final LegacyIdentityService identityService;
    private final CoreWrapper coreWrapper;

    @Inject
    AuthNodeUserIdentityHelper(LegacyIdentityService identityService, CoreWrapper coreWrapper) {
        this.identityService = identityService;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Optional<AMIdentity> getAMIdentity(Optional<String> universalId, NodeState nodeState) {
        universalId = universalId.or(() -> getUniversalId(nodeState));
        Optional<AMIdentity> identity = Optional.empty();
        if (universalId.isPresent()) {
            identity = universalId.map(coreWrapper::getIdentity);
        } else {
            logger.warn("Failed to get the identity based on the information available in the context {}",
                    Arrays.toString(nodeState.keys().toArray()));
        }
        return identity;
    }

    @Override
    public Optional<String> getUniversalId(NodeState nodeState) {
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
        return identityService.getUniversalId(username.asString(), realm.asString(), USER);
    }

    @Override
    public Optional<String> getUniversalId(final String username, final Realm realm) {
        return identityService.getUniversalId(username, realm.asPath(), USER);
    }
}
