/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.scripted;

/**
 * Useful functions to be used with client side scripts, and methods for injection of such scripts, used with scripted
 * auth modules.
 */
public class ScriptedClientUtilityFunctions {

    /**
     * Creates an anonymous function which causes the script to run automatically when the client page containing the
     * function is rendered. This function takes as an argument the id of the form element which will hold the values
     * returned by the client script to the server.
     *
     * @param script The client side script to be ran.
     * @param outputParameterId The id of the form element.
     * @return The anonymous function, supplied with the element with the id.
     */
    public static String createClientSideScriptExecutorFunction(String script, String outputParameterId) {
        return String.format(
                "(function(output) {\n" +
                "    var autoSubmitDelay = 0,\n" +
                "        submitted = false;\n" +
                "    function submit() {\n" +
                "        if (submitted) {\n" +
                "            return;\n" +
                "        }\n" +
                "        document.querySelector(\"input[type=submit]\").click();\n" +
                "        submitted = true;\n" +
                "    }\n" +
                "    %s\n" + // script
                "    setTimeout(submit, autoSubmitDelay);\n" +
                "}) (document.forms[0].elements['%s']);\n", // outputParameterId
                script,
                outputParameterId);
    }

}
