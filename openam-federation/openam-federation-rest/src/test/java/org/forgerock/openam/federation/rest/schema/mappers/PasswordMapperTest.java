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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class PasswordMapperTest {

    private PasswordMapper mapper = new PasswordMapper();

    @Test
    public void shouldReturnNullForNonNullValue() {
        String mapped = mapper.map(ImmutableList.of("hello"), ROOT);

        assertThat(mapped).isNull();
    }

    @Test
    public void shouldEncryptNewValue() {
        List<String> encrypted = mapper.inverse("hello world", ROOT);

        assertThat(encrypted).isNotNull().isNotEqualTo("hello world");
    }
}
