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
 * Copyright 2017 ForgeRock AS.
 */
/**
 * jon.knight@forgerock.com
 *
 * A node that displays a Google reCAPTCHA widget. 
 */



package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import java.util.Optional;

import static org.forgerock.openam.auth.node.api.Action.send;


/**
 * A node that increases or decreases the current auth level by a fixed, configurable amount.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
    configClass = RecaptchaNode.Config.class)
public class RecaptchaNode extends SingleOutcomeNode {

    private final static String DEBUG_FILE = "RecaptchaNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/RecaptchaNode";

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The amount to increment/decrement the auth level.
         * @return the amount.
         */
        @Attribute(order = 100)
        String siteKey();
    }

    private final Config config;

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public RecaptchaNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        String script =
            "var recaptchaScript = document.createElement(\"script\"); recaptchaScript.type = \"text/javascript\"; recaptchaScript.src = \"https://www.google.com/recaptcha/api.js\"; document.body.appendChild(recaptchaScript); \n" +
            "var callbackScript = document.createElement(\"script\"); callbackScript.type = \"text/javascript\"; callbackScript.text = \"function completed(response) { $(\'input[type=submit]\').trigger(\'click\'); }\"; document.body.appendChild(callbackScript); \n" +
            "spinner.hideSpinner(); \n" +
            "submitted = true; \n" +
            "$(document).ready(function(){ \n" +
            "    fs = $(document.forms[0]).find(\"fieldset\"); \n" +
            "    $('#loginButton_0').hide(); \n" +
            "    strUI1='<div align=\"center\" class=\"g-recaptcha\" data-sitekey=\"" + config.siteKey() + "\" data-callback=\"completed\"></div>'; \n" +
            "    $(fs).prepend(strUI1); \n" +
            "}); \n";


        debug.error("[" + DEBUG_FILE + "]: " + "Starting");

        Optional<HiddenValueCallback> result = context.getCallback(HiddenValueCallback.class);
        if (result.isPresent()) {
            return goToNext().build();
        } else {

            String clientSideScriptExecutorFunction = createClientSideScriptExecutorFunction(script, "clientScriptOutputData",
                    true);
            ScriptTextOutputCallback scriptAndSelfSubmitCallback =
                    new ScriptTextOutputCallback(clientSideScriptExecutorFunction);

            HiddenValueCallback hiddenValueCallback = new HiddenValueCallback("clientScriptOutputData");

            ImmutableList<Callback> callbacks = ImmutableList.of(scriptAndSelfSubmitCallback, hiddenValueCallback);

            return send(callbacks).build();
        }
    }

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
