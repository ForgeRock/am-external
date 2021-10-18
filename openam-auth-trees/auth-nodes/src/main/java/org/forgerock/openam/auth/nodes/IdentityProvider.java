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
 * Copyright 2017-2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.CrestQuery;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;

/**
 * Provides an identity from a username.
 */
@Singleton
public class IdentityProvider {

    private CoreWrapper coreWrapper;

    /**
     * Constructor.
     * @param coreWrapper Used to get the AMIdentityRepository.
     */
    @Inject
    public IdentityProvider(CoreWrapper coreWrapper) {
        this.coreWrapper = coreWrapper;
    }

    /**
     * Retrieves an {@link AMIdentity} using the provided username.
     *
     * @param username the username from which to obtain an identity.
     * @param realm the realm of the user.
     * @return An {@link AMIdentity} for the respective user.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public AMIdentity getIdentity(String username, String realm) throws IdRepoException,
            SSOException {
        AMIdentityRepository idrepo = coreWrapper.getAMIdentityRepository(
                coreWrapper.convertRealmPathToRealmDn(realm));
        IdSearchControl idSearchControl = new IdSearchControl();
        idSearchControl.setAllReturnAttributes(true);

        IdSearchResults idSearchResults = idrepo.searchIdentities(IdType.USER,
                new CrestQuery(username), idSearchControl);
        return (AMIdentity) idSearchResults.getSearchResults().iterator().next();
    }
}