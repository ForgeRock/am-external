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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.script;

import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.toMapString;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generates a configuration provider script from service details for every available scripting language.
 */
@Singleton
public class ConfigProviderScriptGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final FrameworkNodesGlobalScriptsProvider globalScriptsProvider;

    @Inject
    ConfigProviderScriptGenerator(FrameworkNodesGlobalScriptsProvider globalScriptsProvider) {
        this.globalScriptsProvider = globalScriptsProvider;
    }

    /**
     * Generates a configuration provider script from the given details.
     *
     * @param properties the service configuration mapped to defaults, which can be null if there is no default
     * @param language   the scripting language to generate the script in.
     * @return the generated script.
     * @throws JsonProcessingException if there is a problem converting the service details to JavaScript.
     * @throws ScriptException         if there is a problem generating the JavaDoc for the script.
     */
    public String generateScript(Map<String, Object> properties, ScriptingLanguage language)
            throws JsonProcessingException, ScriptException {
        Reject.ifNull(properties, "properties must not be null");
        Reject.ifNull(language, "scripting language must be provided");

        StringBuilder script = new StringBuilder(getScriptJavaDoc());
        script.append("config = ");
        return switch (language) {
        case GROOVY -> script.append(toMapString(properties.entrySet().stream().collect(LinkedHashMap::new,
                        (map, entry) -> map.put(wrapStringInQuotes(entry.getKey()),
                                wrapStringInQuotes(entry.getValue())),
                        Map::putAll))).append(";").toString();
        case JAVASCRIPT -> script.append(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(properties))
                                   .append(";").toString();
        };
    }

    private Object wrapStringInQuotes(Object value) {
        if (value instanceof String str) {
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        return value;
    }

    private String getScriptJavaDoc() throws ScriptException {
        return globalScriptsProvider.get().stream()
                .filter(script -> script.getContext().equals(CONFIG_PROVIDER_NODE_SCRIPT.getContext()))
                .findAny()
                       .map(Script::getScript)
                       .filter(script -> script.contains("/**"))
                       .map(script -> "/**" + substringBetween(script, "/**", "*/") + "*/\n\n")
                       .orElse("");
    }
}
