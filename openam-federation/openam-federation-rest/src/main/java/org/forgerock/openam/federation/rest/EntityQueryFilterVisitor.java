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

package org.forgerock.openam.federation.rest;

import static org.forgerock.json.JsonPointer.ptr;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import com.google.common.collect.ImmutableSet;

/**
 * QueryFilterVisitor that implements SAML2 entity filtering functionality.
 */
public final class EntityQueryFilterVisitor
        extends BaseQueryFilterVisitor<Set<String>, Set<String>, JsonPointer> {

    private static final Set<JsonPointer> SUPPORTED_FILTER_FIELDS = ImmutableSet.of(ptr("/entityId"));

    @Override
    public Set<String> visitContainsFilter(Set<String> resultItems, JsonPointer field, Object valueAssertion) {
        if (!SUPPORTED_FILTER_FIELDS.contains(field)) {
            throw new UnsupportedOperationException("Only entityId is a supported filter field");
        }
        return resultItems.stream()
                .filter(v -> v.toLowerCase().contains(((String) valueAssertion).toLowerCase()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> visitEqualsFilter(Set<String> resultItems, JsonPointer field, Object valueAssertion) {
        if (!SUPPORTED_FILTER_FIELDS.contains(field)) {
            throw new UnsupportedOperationException("Only entityId is a supported filter field");
        }
        return resultItems.stream()
                .filter(v -> v.equalsIgnoreCase((String) valueAssertion))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> visitBooleanLiteralFilter(Set<String> resultItems, boolean value) {
        return value ? resultItems : new HashSet<>();
    }

    @Override
    public Set<String> visitStartsWithFilter(Set<String> resultItems, JsonPointer field, Object valueAssertion) {
        if (!SUPPORTED_FILTER_FIELDS.contains(field)) {
            throw new UnsupportedOperationException("Only entityId is a supported filter field");
        }
        return resultItems.stream()
                .filter(v -> v.toLowerCase().startsWith(((String) valueAssertion).toLowerCase()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> visitNotFilter(Set<String> resultItems, QueryFilter<JsonPointer> queryFilter) {
        Set<String> queryMatches = queryFilter.accept(this, resultItems);
        Set<String> filteredSet = new HashSet<>(resultItems);
        filteredSet.removeAll(queryMatches);
        return filteredSet;
    }
}
