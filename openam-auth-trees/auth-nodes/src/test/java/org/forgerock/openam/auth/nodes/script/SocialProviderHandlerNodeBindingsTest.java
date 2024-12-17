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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import javax.script.Bindings;

import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.testng.annotations.Test;

public class SocialProviderHandlerNodeBindingsTest {

    @Test
    public void testThatParentBindingsAreAccessibleAndExistingSessionNotPresentWhenNull() {

        ScriptBindings scriptBindings = SocialProviderHandlerNodeBindings.builder()
                .withSelectedIDP(null)
                .withInputData(null, true)
                .withCallbacks(null)
                .withQueryParameters(Collections.emptyMap())
                .withNodeState(null)
                .withHeaders(Collections.emptyMap())
                .withRealm("realm")
                .withExistingSession(null)
                .withSharedState(null)
                .withTransientState(null)
                .withLoggerReference("abcd")
                .build();

        Bindings convert = scriptBindings.convert(EvaluatorVersion.V1_0);

        assertThat(convert).doesNotContainKey("existingSession");
        assertThat(convert).containsKey("requestHeaders");
    }

    @Test
    public void testThatExistingSessionPresentWhenNotNull() {

        ScriptBindings scriptBindings = SocialProviderHandlerNodeBindings.builder()
                .withSelectedIDP(null)
                .withInputData(null, true)
                .withCallbacks(null)
                .withQueryParameters(Collections.emptyMap())
                .withNodeState(null)
                .withHeaders(Collections.emptyMap())
                .withRealm("realm")
                .withExistingSession(Collections.emptyMap())
                .withSharedState(null)
                .withTransientState(null)
                .withLoggerReference("abcd")
                .build();

        Bindings convert = scriptBindings.convert(EvaluatorVersion.V1_0);

        assertThat(convert).containsKey("existingSession");
        assertThat(convert).containsKey("requestHeaders");
    }

}
