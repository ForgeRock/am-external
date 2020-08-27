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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.hosted.service.AuthenticationContext.AuthenticationComparisonType;
import org.testng.annotations.Test;

public class AuthComparisonTypeMapperTest {

    private AuthComparisonTypeMapper mapper = new AuthComparisonTypeMapper();

    @Test
    public void shouldMapMissingValueToNull() {
        AuthenticationComparisonType mappedValue = mapper.map(emptyList(), ROOT);

        assertThat(mappedValue).isNull();
    }

    @Test
    public void shouldMapSingleValueToEnum() {
        AuthenticationComparisonType mappedValue = mapper.map(singletonList("better"), ROOT);

        assertThat(mappedValue).isEqualTo(AuthenticationComparisonType.BETTER);
    }

    @Test
    public void shouldIgnoreAdditionalValues() {
        AuthenticationComparisonType mappedValue = mapper.map(asList("maximum", "better", "minimum"), ROOT);

        assertThat(mappedValue).isEqualTo(AuthenticationComparisonType.MAXIMUM);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionForUnrecognisedValue() {
        mapper.map(singletonList("typo"), ROOT);
    }

    @Test
    public void shouldInverseCorrectly() {
        List<String> inverse = mapper.inverse(AuthenticationComparisonType.BETTER, ROOT);

        assertThat(inverse).containsOnly("better");
    }

    @Test
    public void shouldInverseNullValueToEmptySet() {
        List<String> inverse = mapper.inverse(null, ROOT);

        assertThat(inverse).isEmpty();
    }
}