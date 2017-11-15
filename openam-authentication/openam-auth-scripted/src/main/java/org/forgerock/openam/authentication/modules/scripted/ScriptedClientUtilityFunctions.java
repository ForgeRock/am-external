/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
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
    public static String createClientSideScriptExecutorFunction(String script, String outputParameterId,
                boolean clientSideScriptEnabled) {
        String collectingDataMessage = "";
        if (clientSideScriptEnabled) {
            collectingDataMessage = "    messenger.messages.addMessage( message );\n";
        }

        String spinningWheelScript = "if (window.require) {\n" +
                "    var messenger = require(\"org/forgerock/commons/ui/common/components/Messages\"),\n" +
                "        spinner =  require(\"org/forgerock/commons/ui/common/main/SpinnerManager\"),\n" +
                "        message =  {message:\"Collecting Data...\", type:\"info\"};\n" +
                "    spinner.showSpinner();\n" +
                collectingDataMessage +
                "}";

        return String.format(
                spinningWheelScript +
                "(function(output) {\n" +
                "    var autoSubmitDelay = 0,\n" +
                "        submitted = false;\n" +
                "    function submit() {\n" +
                "        if (submitted) {\n" +
                "            return;\n" +
                "        }" +
                "        if (!(window.jQuery)) {\n" + // Crude detection to see if XUI is not present.
                "            document.forms[0].submit();\n" +
                "        } else {\n" +
                "            $('input[type=submit]').trigger('click');\n" +
                "        }\n" +
                "        submitted = true;\n" +
                "    }\n" +
                "    %s\n" + // script
                "    setTimeout(submit, autoSubmitDelay);\n" +
                "}) (document.forms[0].elements['%s']);\n", // outputParameterId
                script,
                outputParameterId);
    }

}
