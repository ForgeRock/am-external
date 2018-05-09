/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common.mapping;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import java.util.Map;
import java.util.Set;

/**
 * Implementations of this interface provide the means to search for and create users given a map of attributes.
 * @see org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider
 *
 * @supported.all.api
 */
public interface AccountProvider {

    /**
     * Search for a user given a map of attributes.
     * @param idrepo The identity repository.
     * @param attr The set of attributes, which should be treated as 'or' statements.
     * @return The first matching user found.
     */
    AMIdentity searchUser(AMIdentityRepository idrepo, Map<String, Set<String>> attr);

    /**
     * Provisions a user with the specified attributes.
     * @param idrepo The identity repository in which the user will be created.
     * @param attributes The user attributes.
     * @return The created user identity.
     * @throws AuthLoginException Thrown if user creation fails.
     */
    AMIdentity provisionUser(AMIdentityRepository idrepo, Map<String, Set<String>> attributes) throws AuthLoginException;
    
}
