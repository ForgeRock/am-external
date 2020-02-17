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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.idrepo.ldap;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.messages.Request;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.openam.ldap.LDAPConstants;
import org.forgerock.openam.utils.MapHelper;
import org.forgerock.opendj.ldap.messages.SearchRequest;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.idm.IdType;

/**
 * Contains tests around group memberships in AD for recursive groups.
 */
@PrepareForTest(value = {MemoryBackend.class})
public class ADRecursiveGroupsRepoTest extends IdRepoTestBase {

    private static final String AD_BASIC_SETTINGS = "/config/adbasicsettings.properties";
    private static final String AD_BASIC_LDIF = "/ldif/adbasic.ldif";

    @BeforeClass
    public void setUpSuite() throws Exception {
        super.setUpSuite();
        memoryBackend = Mockito.spy(memoryBackend);
    }

    @Override
    protected String getLDIFPath() {
        return AD_BASIC_LDIF;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRecursiveSearchEnabledDoesExtensibleSearch() throws Exception {
        // Given configuration with recursive search enabled and uniqueMemberAttr defined
        Map<String, Set<String>> basicSettings = MapHelper.readMap(AD_BASIC_SETTINGS);
        basicSettings.put(LDAPConstants.AD_LDAP_RECURSIVE_GROUP_MEMBERSHIP_ENABLED, singleton(Boolean.TRUE.toString()));
        basicSettings.put(LDAPConstants.LDAP_UNIQUE_MEMBER, singleton("member"));
        idrepo.initialize(basicSettings);

        // When memberships are requested
        idrepo.getMemberships(null, IdType.USER, "Demo", IdType.GROUP);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.memoryBackend, atLeastOnce()).handleRequest(captor.capture());
        List<Request> requests = captor.getAllValues();
        // A list of 3 requests is returned from above, 2 SearchRequests and an UnbindRequestImpl
        List<SearchRequest> searchRequests = removeNonSearchRequests(requests);

        // The search filter contains the active directory recursive search extensible filter
        assertThat(requestsContainRecursiveSearchInFilter(searchRequests)).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRecursiveSearchDisabledDoesNoExtensibleSearch() throws Exception {
        // Given configuration with recursive search disabled and memberOfAttr not defined
        Map<String, Set<String>> basicSettings = MapHelper.readMap(AD_BASIC_SETTINGS);
        basicSettings.put(LDAPConstants.AD_LDAP_RECURSIVE_GROUP_MEMBERSHIP_ENABLED, singleton(Boolean.FALSE.toString()));
        basicSettings.remove(LDAPConstants.LDAP_MEMBER_OF);
        idrepo.initialize(basicSettings);

        // When memberships are requested
        idrepo.getMemberships(null, IdType.USER, "Demo", IdType.GROUP);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.memoryBackend, atLeastOnce()).handleRequest(captor.capture());
        List<Request> requests = captor.getAllValues();
        // A list of 3 requests is returned from above, 2 SearchRequests and an UnbindRequestImpl
        List<SearchRequest>searchRequests = removeNonSearchRequests(requests);

        // The search filter does not contain the active directory recursive search extensible filter
        assertThat(requestsContainRecursiveSearchInFilter(searchRequests)).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRecursiveSearchDefaultsToDisabled() throws Exception {
        // Given configuration with recursive search not defined and memberOfAttr not defined
        Map<String, Set<String>> basicSettings = MapHelper.readMap(AD_BASIC_SETTINGS);
        basicSettings.remove(LDAPConstants.AD_LDAP_RECURSIVE_GROUP_MEMBERSHIP_ENABLED);
        basicSettings.remove(LDAPConstants.LDAP_MEMBER_OF);
        idrepo.initialize(basicSettings);

        // When memberships are requested
        idrepo.getMemberships(null, IdType.USER, "Demo", IdType.GROUP);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.memoryBackend, atLeastOnce()).handleRequest(captor.capture());
        List<Request> requests = captor.getAllValues();
        // A list of 3 requests is returned from above, 2 SearchRequests and an UnbindRequestImpl
        List<SearchRequest>searchRequests = removeNonSearchRequests(requests);

        // The search filter does not contain the active directory recursive search extensible filter
        assertThat(requestsContainRecursiveSearchInFilter(searchRequests)).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRecursiveSearchDisabledAndMemberOfDoesSingleEntrySearch() throws Exception {
        // Given configuration with recursive search not defined and memberOfAttr not defined
        Map<String, Set<String>> basicSettings = MapHelper.readMap(AD_BASIC_SETTINGS);
        basicSettings.put(LDAPConstants.AD_LDAP_RECURSIVE_GROUP_MEMBERSHIP_ENABLED, singleton(Boolean.FALSE.toString()));
        final String memberOf = "memberOf";
        basicSettings.put(LDAPConstants.LDAP_MEMBER_OF, singleton(memberOf));
        idrepo.initialize(basicSettings);

        // When memberships are requested
        idrepo.getMemberships(null, IdType.USER, "Demo", IdType.GROUP);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.memoryBackend, atLeastOnce()).handleRequest(captor.capture());
        List<Request> requests = captor.getAllValues();
        // A list of 3 requests is returned from above, 2 SearchRequests and an UnbindRequestImpl
        List<SearchRequest>searchRequests = removeNonSearchRequests(requests);

        // The search filter does not contain the active directory recursive search extensible filter
        assertThat(requestsContainRecursiveSearchInFilter(searchRequests)).isFalse();

        // Verify the search contained the memberOf filter.
        assertThat(requestsContainStringInAttributes(searchRequests, memberOf)).isTrue();
    }

    private List<SearchRequest> removeNonSearchRequests(List<Request> requests) {
        return requests.stream()
            .filter(sr -> sr instanceof SearchRequest)
            .map(sr -> (SearchRequest) sr)
            .collect(Collectors.toList());
    }

    private boolean requestsContainRecursiveSearchInFilter(List<SearchRequest> searchRequests) {
        return searchRequests.stream().map(SearchRequest::getFilter).map(Object::toString)
            .anyMatch(filter -> filter.contains(LDAPConstants.AD_LDAP_MATCHING_RULE_IN_CHAIN_OID));
    }

    private boolean requestsContainStringInAttributes(List<SearchRequest> searchRequests, String searchString) {
        return searchRequests.stream().map(SearchRequest::getAttributes)
            .anyMatch(attribute -> attribute.contains(searchString));
    }
}
