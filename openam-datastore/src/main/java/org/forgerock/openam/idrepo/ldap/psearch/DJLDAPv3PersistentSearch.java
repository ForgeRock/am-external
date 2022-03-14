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
 * Copyright 2013-2021 ForgeRock AS.
 */

package org.forgerock.openam.idrepo.ldap.psearch;

import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PERSISTENT_SEARCH_BASE_DN;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PERSISTENT_SEARCH_FILTER;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_PERSISTENT_SEARCH_SCOPE;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_RETRY_INTERVAL;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_NAMING_ATTR;
import static org.forgerock.openam.ldap.LDAPConstants.LDAP_USER_SEARCH_ATTR;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.idrepo.ldap.IdentityMovedOrRenamedListener;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.opendj.ldap.Dn;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.messages.SearchResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.services.ldap.event.LDAPv3PersistentSearch;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * This class will execute persistent search request against the configured datastore. When a result is received, the
 * internal caches will be notified about the changes, so the caches can be dirtied.
 */
public class DJLDAPv3PersistentSearch extends LDAPv3PersistentSearch<IdRepoListener, Set<IdType>> {

    private static final Logger DEBUG = LoggerFactory.getLogger(DJLDAPv3PersistentSearch.class);
    private final SearchResultEntryHandler resultEntryHandler = new PSearchResultEntryHandler();
    private final Set<IdentityMovedOrRenamedListener> movedOrRenamedListenerSet = new HashSet<>(1);
    private final String usersSearchAttributeName;
    private final String usersNamingAttributeName;
    private final ConnectionFactory factory;

    /**
     * Creates a new DJLDAPv3PersistentSearch using the provided configuration data and connection factory.
     *
     * @param configMap Non null. Map containing the configuration data necessary to perform a persistent search,
     *                  including the retry interval, search DN, the filter to apply and scope of the query and
     *                  any user attributes that will be returned through the connection.
     * @param factory Used to produce connections down to the LDAP datastore.
     */
    public DJLDAPv3PersistentSearch(Map<String, Set<String>> configMap, ConnectionFactory factory) {
        super(CollectionHelper.getIntMapAttr(configMap, LDAP_RETRY_INTERVAL, 3000, DEBUG),
                Dn.valueOf(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN)), LDAPUtils
                        .parseFilter(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_FILTER),
                                Filter.objectClassPresent()), LDAPUtils
                        .getSearchScope(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_SCOPE),
                                SearchScope.WHOLE_SUBTREE), factory,
                CollectionHelper.getMapAttr(configMap, LDAP_USER_SEARCH_ATTR),
                CollectionHelper.getMapAttr(configMap, LDAP_USER_NAMING_ATTR));
        usersSearchAttributeName = CollectionHelper.getMapAttr(configMap, LDAP_USER_SEARCH_ATTR);
        usersNamingAttributeName = CollectionHelper.getMapAttr(configMap, LDAP_USER_NAMING_ATTR);
        this.factory = factory;
    }

    /**
     * Adds an {@link IdentityMovedOrRenamedListener} object, which needs to be notified about persistent search results
     * where the identity has been renamed or moved.
     * The caller must ensure that calls to addMovedOrRenamedListener/removeMovedOrRenamedListener invocations are
     * synchronized correctly.
     *
     * @param movedOrRenamedListener The {@link IdentityMovedOrRenamedListener} instance that needs to be notified about
     *                               changes.
     */
    public void addMovedOrRenamedListener(IdentityMovedOrRenamedListener movedOrRenamedListener) {
        movedOrRenamedListenerSet.add(movedOrRenamedListener);
    }

    /**
     * Removes an {@link IdentityMovedOrRenamedListener} if it was registered to get persistent search notifications.
     * The caller must ensure that calls to addMovedOrRenamedListener/removeMovedOrRenamedListener invocations are
     * synchronized correctly.
     *
     * @param movedOrRenamedListener The {@link IdentityMovedOrRenamedListener} instance to remove from the listeners
     */
    public void removeMovedOrRenamedListener(IdentityMovedOrRenamedListener movedOrRenamedListener) {
        movedOrRenamedListenerSet.remove(movedOrRenamedListener);
    }

    @Override
    protected void clearCaches() {
        for (IdRepoListener idRepoListener : getListeners().keySet()) {
            idRepoListener.allObjectsChanged();
        }
    }

    @Override
    protected void connectionLost() {
        //this section intentionally left blank
        DEBUG.debug("DJLDAPv3PersistentSearch.connectionLost(): connection lost against {}", factory.toString());
    }

    @Override
    protected void connectionReestablished() {
        //this section intentionally left blank
        DEBUG.debug("DJLDAPv3PersistentSearch.connectionReestablished(): connection re-established against {}",
                factory.toString());
    }

    @Override
    protected SearchResultEntryHandler getSearchResultEntryHandler() {
        return resultEntryHandler;
    }

    private final class PSearchResultEntryHandler implements LDAPv3PersistentSearch.SearchResultEntryHandler {

        @Override
        public boolean handle(SearchResultEntry entry, String dn, Dn previousDn, PersistentSearchChangeType type) {
            if (type != null) {
                String identitySearchAttrValue = entry.parseAttribute(usersSearchAttributeName).asString();
                String identityNamingAttrValue = entry.parseAttribute(usersNamingAttributeName).asString();
                if (previousDn != null) {
                    for (IdentityMovedOrRenamedListener listener : movedOrRenamedListenerSet) {
                        listener.identityMovedOrRenamed(previousDn, identitySearchAttrValue, identityNamingAttrValue);
                    }
                }

                if (PersistentSearchChangeType.DELETE.equals(type)) {
                    for (IdentityMovedOrRenamedListener listener : movedOrRenamedListenerSet) {
                        listener.identityMovedOrRenamed(entry.getName(), identitySearchAttrValue,
                            identityNamingAttrValue);
                    }
                }

                for (Map.Entry<IdRepoListener, Set<IdType>> listenerEntry : getListeners().entrySet()) {
                    IdRepoListener listener = listenerEntry.getKey();

                    for (IdType idType : listenerEntry.getValue()) {
                        listener.objectChanged(dn, idType, type.intValue(), listener.getConfigMap());
                        if (idType.equals(IdType.USER)) {
                            listener.objectChanged(entry.parseAttribute(usersSearchAttributeName).asString(), idType,
                                    type.intValue(), listener.getConfigMap());
                        }
                    }
                }
            }
            return true;
        }
    }
}
