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
 * Copyright 2014-2022 ForgeRock AS.
 */
package org.forgerock.openam.scripting.idrepo;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.am.identity.persistence.IdentityStore;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentity;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;

import com.google.inject.assistedinject.Assisted;

/**
 * A repository to retrieve user information within a scripting module's script
 *
 * @deprecated Use {@link ScriptedIdentityRepository} instead.
 */
@Deprecated
public class ScriptIdentityRepository extends ScriptedIdentityRepository {
    /**
     * Constructor for <code>ScriptIdentityRepository</code> object
     *
     * @param identityStore The IdentityStore object used to retrieve/persist user information
     */
    public ScriptIdentityRepository(IdentityStore identityStore) {
        super(identityStore);
    }

    /**
     * Constructor for <code>ScriptIdentityRepository</code> object
     *
     * @param realm The realm for which to generate a new mechanism to retrieve/persist user information to
     */
    @Inject
    public ScriptIdentityRepository(@Assisted Realm realm) {
        super(realm);
    }

    /**
     * Constructor for <code>ScriptIdentityRepository</code> object
     *
     * @param identityStore The IdentityStore object used to retrieve/persist user information
     * @param userSearchAttributes The Alias Search Attribute values
     */
    public ScriptIdentityRepository(IdentityStore identityStore, Set<String> userSearchAttributes) {
        super(identityStore, userSearchAttributes);
    }

    /**
     * Returns a particular attribute for a particular user
     *
     * @param userName      The name of the user
     * @param attributeName The attribute name to be returned
     * @return A set of Strings containing all values of the attribute
     */
    public Set getAttribute(String userName, String attributeName) {
        ScriptedIdentity amIdentity = getIdentity(userName);
        if (amIdentity != null) {
            return amIdentity.getAttribute(attributeName);
        } else {
            return new HashSet<String>();
        }
    }

    /**
     * Sets a particular attribute for a particular user. If the attribute already exists it will be overridden.
     *
     * @param userName       The name of the user
     * @param attributeName  The attribute name to be set
     * @param attributeValues The new value of the attribute
     */
    public void setAttribute(String userName, String attributeName, String[] attributeValues) {
        ScriptedIdentity amIdentity = getIdentity(userName);
        if (amIdentity != null) {
            amIdentity.setAttribute(attributeName, attributeValues);
            amIdentity.store();
        }
    }

    /**
     * Adds an attribute to the list of values already assigned to the attributeName
     * @param userName The name of the user
     * @param attributeName The attribute name to be added to
     * @param attributeValue The value to be added
     */
    public void addAttribute(String userName, String attributeName, String attributeValue) {
        ScriptedIdentity amIdentity = getIdentity(userName);
        if (amIdentity != null) {
            amIdentity.addAttribute(attributeName, attributeValue);
            amIdentity.store();
        }
    }

    /**
     * Helper factory for Guice to generate new ScriptIdentityRepository instances.
     */
    public interface Factory {
        /**
         * Construct a new ScriptIdentityRepository.
         * 
         * @param realm the realm in which this repository accessor will operate.
         * @return the new repository accessor.
         */
        ScriptIdentityRepository create(Realm realm);
    }

}
