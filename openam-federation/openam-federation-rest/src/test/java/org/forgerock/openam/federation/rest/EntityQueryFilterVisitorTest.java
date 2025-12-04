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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.federation.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonPointer.ptr;
import static org.mockito.BDDMockito.given;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.JsonPointer;
import org.forgerock.util.query.QueryFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EntityQueryFilterVisitorTest {

    private EntityQueryFilterVisitor entityQueryFilterVisitor;
    private Set<String> resultItems;
    private static final JsonPointer SUPPORTED_FIELD = ptr("/entityId");
    private static final JsonPointer UNSUPPORTED_FIELD = ptr("/notSupportedField");
    @Mock
    private QueryFilter<JsonPointer> queryFilter;

    @BeforeEach
    void setup() {
        entityQueryFilterVisitor = new EntityQueryFilterVisitor();
        resultItems = new HashSet<>();
        resultItems.add("id1");
        resultItems.add("id2");
        resultItems.add("entity");
    }

    @Test
    void visitContainsFilterShouldThrowUnsupportedOperationExceptionForUnsupportedField() {
        assertThatThrownBy(() -> entityQueryFilterVisitor.visitContainsFilter(resultItems, UNSUPPORTED_FIELD, "id"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Only entityId is a supported filter field");
    }

    @Test
    void shouldReturnResultsContainingProvidedValue() {
        Set<String> actual = entityQueryFilterVisitor.visitContainsFilter(resultItems, SUPPORTED_FIELD, "id");
        assertThat(actual).hasSize(2);
    }

    @Test
    void visitEqualsFilterShouldThrowUnsupportedOperationExceptionForUnsupportedField() {
        assertThatThrownBy(() -> entityQueryFilterVisitor.visitEqualsFilter(resultItems, UNSUPPORTED_FIELD, "entity"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Only entityId is a supported filter field");
    }

    @Test
    void shouldReturnResultsEqualToProvidedValue() {
        Set<String> actual = entityQueryFilterVisitor.visitEqualsFilter(resultItems, SUPPORTED_FIELD, "entity");
        assertThat(actual.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnProvidedResultsWhenValueIsTrue() {
        assertThat(entityQueryFilterVisitor.visitBooleanLiteralFilter(resultItems, true)).isEqualTo(resultItems);
    }

    @Test
    void shouldReturnANewHashSetWhenValueIsFalse() {
        Set<String> actual = entityQueryFilterVisitor.visitBooleanLiteralFilter(resultItems, false);
        assertThat(actual).isInstanceOf(HashSet.class).isEmpty();
    }

    @Test
    void visitStartsWithFilterShouldThrowUnsupportedOperationExceptionForUnsupportedField() {
        assertThatThrownBy(() -> entityQueryFilterVisitor.visitStartsWithFilter(resultItems, UNSUPPORTED_FIELD, "id"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Only entityId is a supported filter field");
    }

    @Test
    void shouldReturnResultsStartingWithValue() {
        Set<String> actual = entityQueryFilterVisitor.visitStartsWithFilter(resultItems, SUPPORTED_FIELD, "id");
        assertThat(actual.size()).isEqualTo(2);
    }

    @Test
    void shouldReturnResultsThatDoNotMatchTheQuery() {
        HashSet<String> queryMatches = new HashSet<>();
        queryMatches.add("entity");
        given(queryFilter.accept(entityQueryFilterVisitor, resultItems)).willReturn(queryMatches);

        Set<String> actual = entityQueryFilterVisitor.visitNotFilter(resultItems, queryFilter);
        assertThat(actual.size()).isEqualTo(2);
    }
}
