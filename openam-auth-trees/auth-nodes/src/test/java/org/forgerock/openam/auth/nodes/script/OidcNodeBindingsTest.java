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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;

import java.util.Collections;

import javax.script.Bindings;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.shared.debug.Debug;

public class OidcNodeBindingsTest {

    @Test
    public void testThatParentBindingsAreAccessibleAndExistingSessionNotPresentWhenNull() {
        JsonValue json = json(field("a", "b"));
        ScriptBindings scriptBindings = OidcNodeBindings.builder()
                .withJwtClaims(json)
                .withNodeState(null)
                .withHeaders(Collections.emptyMap())
                .withRealm("realm")
                .withExistingSession(null)
                .withLoggerReference("abcd")
                .build();

        Bindings convert = scriptBindings.convert(EvaluatorVersion.V1_0);

        assertThat(convert).doesNotContainKey("existingSession");

        assertThat(convert.get("jwtClaims")).isEqualTo(json);
        assertThat(convert.get("logger")).isEqualTo(Debug.getInstance("abcd"));
    }

    @Test
    public void testThatParentBindingsAreAccessibleAndExistingSessionPresentWhenNotNull() {
        JsonValue json = json(field("a", "b"));
        ScriptBindings scriptBindings = OidcNodeBindings.builder()
                .withJwtClaims(json)
                .withNodeState(null)
                .withHeaders(Collections.emptyMap())
                .withRealm("realm")
                .withExistingSession(Collections.emptyMap())
                .withLoggerReference("abcd")
                .build();

        Bindings convert = scriptBindings.convert(EvaluatorVersion.V1_0);

        assertThat(convert).containsKey("existingSession");
    }
}
