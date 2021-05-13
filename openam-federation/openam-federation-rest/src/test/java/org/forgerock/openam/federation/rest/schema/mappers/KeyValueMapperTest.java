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
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.hosted.service.KeyValue;
import org.testng.annotations.Test;

public class KeyValueMapperTest {

    private KeyValueMapper mapper = new KeyValueMapper();

    @Test
    public void shouldIgnoreInvalidInput() {
        List<KeyValue> keyValues = mapper.map(asList(
                "invalid=value=provided",
                "bogus",
                ""), ROOT);
        assertThat(keyValues).isEmpty();
    }

    @Test
    public void shouldMapValidInputsCorrectly() {
        List<KeyValue> keyValues = mapper.map(asList(
                "valid=value",
                "badger=weasel"), ROOT);
        assertThat(keyValues).hasSize(2)
                .containsOnly(new KeyValue("valid", "value"), new KeyValue("badger", "weasel"));
    }

    @Test
    public void shouldSerialiseCorrectly() {
        List<String> strings = mapper.inverse(asList(
                new KeyValue("badger", "weasel"),
                new KeyValue("foo", "bar")),
                null);

        assertThat(strings).hasSize(2)
                .containsOnly("badger=weasel", "foo=bar");
    }
}