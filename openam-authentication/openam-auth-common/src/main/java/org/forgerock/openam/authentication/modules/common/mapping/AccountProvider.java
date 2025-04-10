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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.common.mapping;

import java.util.Map;
import java.util.Set;

import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;

/**
 * Implementations of this interface provide the means to search for and create users given a map of attributes.
 * @see org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider
 *
 */
@SupportedAll
public interface AccountProvider {

    /**
     * Search for a user given a map of attributes.
     * @param idrepo The identity repository.
     * @param attr The set of attributes, which should be treated as 'or' statements.
     * @return The first matching user found.
     */
    AMIdentity searchUser(IdentityStore idrepo, Map<String, Set<String>> attr);

    /**
     * Provisions a user with the specified attributes.
     * @param idrepo The identity repository in which the user will be created.
     * @param attributes The user attributes.
     * @return The created user identity.
     * @throws AuthLoginException Thrown if user creation fails.
     */
    AMIdentity provisionUser(IdentityStore idrepo, Map<String, Set<String>> attributes) throws AuthLoginException;
    
}
