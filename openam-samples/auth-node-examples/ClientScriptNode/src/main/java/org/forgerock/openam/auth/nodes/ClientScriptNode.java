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
 * An authentication node which executes a Javascript on the client browser.
 */


package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.base.Strings;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.scripting.Script;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import javax.security.auth.callback.Callback;
import java.util.Optional;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.scripting.ScriptConstants.ScriptContext.AUTHENTICATION_CLIENT_SIDE;
import javax.inject.Inject;



/**
 * A node that executes a client-side Javascript and stores any resulting output in the shared state.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
    configClass = ClientScriptNode.Config.class)
public class ClientScriptNode extends SingleOutcomeNode {

    private final static String DEBUG_FILE = "ClientScriptNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ClientScriptNode";

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The amount to increment/decrement the auth level.
         * @return the amount.
         */
        @Attribute(order = 100)
        @Script(AUTHENTICATION_CLIENT_SIDE)
        ScriptConfiguration script();

        @Attribute(order = 200)
        String scriptResult();
    }

    private final Config config;

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ClientScriptNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Optional<String> result = context.getCallback(HiddenValueCallback.class).map(HiddenValueCallback::getValue).filter(scriptOutput -> !Strings.isNullOrEmpty(scriptOutput));
        if (result.isPresent()) {
            JsonValue newSharedState = context.sharedState.copy();
            newSharedState.put(config.scriptResult(), result.get());
            return goToNext().replaceSharedState(newSharedState).build();
        } else {
            String clientSideScriptExecutorFunction = createClientSideScriptExecutorFunction(config.script().getScript(), config.scriptResult(),
                    true);
            ScriptTextOutputCallback scriptAndSelfSubmitCallback =
                    new ScriptTextOutputCallback(clientSideScriptExecutorFunction);

            HiddenValueCallback hiddenValueCallback = new HiddenValueCallback(config.scriptResult());

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
