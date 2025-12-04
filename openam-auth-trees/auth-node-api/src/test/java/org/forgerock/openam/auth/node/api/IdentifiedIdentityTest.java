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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.node.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.identity.idm.IdType;

public class IdentifiedIdentityTest {

    @Test
    void testToJson() {
        IdentifiedIdentity identifiedIdentity = new IdentifiedIdentity("test_name", IdType.USER);
        assertThat(identifiedIdentity.toJson().asMap())
                .containsExactlyInAnyOrderEntriesOf(Map.of("name", "test_name", "type", IdType.USER.getName()));
    }

    @Test
    void testFromJson() {
        IdentifiedIdentity identifiedIdentity = IdentifiedIdentity.fromJson(json(object(
                field("name", "test_name"),
                field("type", IdType.USER.getName()))));
        assertThat(identifiedIdentity.getUsername()).isEqualTo("test_name");
        assertThat(identifiedIdentity.getIdentityType()).isEqualTo(IdType.USER);
    }

}
