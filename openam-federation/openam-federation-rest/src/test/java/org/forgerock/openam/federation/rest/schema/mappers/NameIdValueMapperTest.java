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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.NameIdValueMap;
import org.junit.jupiter.api.Test;

public class NameIdValueMapperTest {

    private NameIdValueMapper mapper = new NameIdValueMapper();

    @Test
    void shouldMapNonBinaryMappings() {
        List<NameIdValueMap> mapped = mapper.map(singletonList("urn:hello:world=badger"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        NameIdValueMap entry = mapped.get(0);
        assertThat(entry.getKey()).isEqualTo("urn:hello:world");
        assertThat(entry.getValue()).isEqualTo("badger");
        assertThat(entry.getBinary()).isFalse();
    }

    @Test
    void shouldMapBinaryMappings() {
        List<NameIdValueMap> mapped = mapper.map(singletonList("urn:hello:world=badger;binary"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        NameIdValueMap entry = mapped.get(0);
        assertThat(entry.getKey()).isEqualTo("urn:hello:world");
        assertThat(entry.getValue()).isEqualTo("badger");
        assertThat(entry.getBinary()).isTrue();
    }

    @Test
    void shouldInvertNonBinaryEntries() {
        List<String> inverted = mapper.inverse(
                singletonList(new NameIdValueMap("urn:hello:world", "badger", false)), ROOT);

        assertThat(inverted)
                .isNotNull()
                .hasSize(1)
                .containsOnly("urn:hello:world=badger");
    }

    @Test
    void shouldInvertBinaryEntries() {
        List<String> inverted = mapper.inverse(
                singletonList(new NameIdValueMap("urn:hello:world", "badger", true)), ROOT);

        assertThat(inverted)
                .isNotNull()
                .hasSize(1)
                .containsOnly("urn:hello:world=badger;binary");
    }
}
