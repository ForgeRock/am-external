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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.ResponseArtifactMessageEncoding.Encoding;
import org.junit.jupiter.api.Test;

public class MessageEncodingMapperTest {

    private MessageEncodingMapper mapper = new MessageEncodingMapper();

    @Test
    void shouldMapMissingValueToUri() {
        Encoding mappedValue = mapper.map(emptyList(), ROOT);

        assertThat(mappedValue).isEqualTo(Encoding.URI);
    }

    @Test
    void shouldMapSingleValueToEnum() {
        Encoding mappedValue = mapper.map(singletonList("FORM"), ROOT);

        assertThat(mappedValue).isEqualTo(Encoding.FORM);
    }

    @Test
    void shouldIgnoreAdditionalValues() {
        Encoding mappedValue = mapper.map(asList("URI", "FORM"), ROOT);

        assertThat(mappedValue).isEqualTo(Encoding.URI);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnrecognisedValue() {
        assertThatThrownBy(() -> mapper.map(singletonList("typo"), ROOT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No enum constant org.forgerock.openam.federation."
                        + "rest.schema.shared.ResponseArtifactMessageEncoding.Encoding.TYPO");
    }

    @Test
    void shouldInverseCorrectly() {
        List<String> inverse = mapper.inverse(Encoding.FORM, ROOT);

        assertThat(inverse).containsOnly("FORM");
    }

    @Test
    void shouldInverseNullValueToUri() {
        List<String> inverse = mapper.inverse(null, ROOT);

        assertThat(inverse).containsOnly("URI");
    }
}
