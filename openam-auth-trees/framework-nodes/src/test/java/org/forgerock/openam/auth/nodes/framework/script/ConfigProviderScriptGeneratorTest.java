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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;
import static org.forgerock.openam.scripting.domain.ScriptingLanguage.GROOVY;
import static org.forgerock.openam.scripting.domain.ScriptingLanguage.JAVASCRIPT;
import static org.mockito.BDDMockito.given;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

@ExtendWith(MockitoExtension.class)
class ConfigProviderScriptGeneratorTest {

    @Mock
    private FrameworkNodesGlobalScriptsProvider globalScriptsProvider;
    @Mock
    private Script script;

    private ConfigProviderScriptGenerator configProviderScriptGenerator;


    @BeforeEach
    void setup() {
        configProviderScriptGenerator = new ConfigProviderScriptGenerator(globalScriptsProvider);
    }

    static Stream<Arguments> scriptTestSource() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("message", Map.of());
        properties.put("language", "en-GB");
        properties.put("platform", null);
        properties.put("description", "This is a description \"with quotes\"");

        return Stream.of(
                Arguments.of(JAVASCRIPT,
                        properties,
                        """
                        config = {
                          "message": { },
                          "language": "en-GB",
                          "platform": null,
                          "description": "This is a description \\"with quotes\\""
                        };
                        """),
                Arguments.of(GROOVY,
                        properties,
                        """
                        config = [
                          "message": [:],
                          "language": "en-GB",
                          "platform": null,
                          "description": "This is a description \\"with quotes\\""
                        ];
                        """),
                Arguments.of(JAVASCRIPT, Map.of(), "config = {};"),
                Arguments.of(GROOVY, Map.of(), "config = [:];")
        );
    }

    @ParameterizedTest
    @MethodSource("scriptTestSource")
    void shouldCreateScriptWhenCallingScriptAction(ScriptingLanguage language,
            Map<String, Object> properties, String expectedScript) throws JsonProcessingException, ScriptException {
        // given
        given(globalScriptsProvider.get()).willReturn(List.of(script));
        given(script.getContext()).willReturn(CONFIG_PROVIDER_NODE_SCRIPT.getContext());
        String scriptHeader = "/**\n * This is a script header\n */";
        given(script.getScript()).willReturn(scriptHeader);

        // when
        String generatedScript = configProviderScriptGenerator.generateScript(properties, language);

        // then
        assertThat(generatedScript)
                .containsIgnoringWhitespaces(expectedScript)
                .contains(scriptHeader);
    }

    @Test
    void shouldCreateScriptWithNoHeaderIfDefaultScriptCannotBeFound() throws ScriptException, JsonProcessingException {
        given(globalScriptsProvider.get()).willReturn(List.of());

        // when
        String generatedScript = configProviderScriptGenerator.generateScript(Map.of(), JAVASCRIPT);

        // then
        assertThat(generatedScript)
                .containsIgnoringWhitespaces("config = {};")
                .doesNotContain("/**")
                .doesNotContain("*/");
    }

    @Test
    void shouldThrowNullPointerWhenPropertiesAreNull() {
        // when / then
        assertThatThrownBy(() -> configProviderScriptGenerator.generateScript(null, JAVASCRIPT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties");
    }

    @Test
    void shouldThrowNullPointerWhenLanguageIsNull() {
        // when / then
        Map<String, Object> properties = new LinkedHashMap<>();
        assertThatThrownBy(() -> configProviderScriptGenerator.generateScript(properties, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("language");
    }

}
