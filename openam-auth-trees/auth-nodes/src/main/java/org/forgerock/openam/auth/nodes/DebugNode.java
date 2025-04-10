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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.audit.context.AuditRequestContext.getAuditRequestContext;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.ClientScriptUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

/**
 * A node which displays debug information about the current login tree. It
 * collects information from various sources such as the tree state (shared,
 * transient, secret), the identity objects universalId and the transaction id
 * which can be used as a reference in logs.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
      configClass = DebugNode.Config.class)
public class DebugNode extends SingleOutcomeNode {

    // This is used to grab the value of the transactionId
    private final AuditRequestContext auditRequestContext;
    // Utility which allows us to load a .js file as a string
    private final ClientScriptUtilities scriptUtils = new ClientScriptUtilities();
    // The javascript file we will later load in to a callback
    private static final String SCRIPT_PATH = "org/forgerock/openam/auth/nodes/DebugNode.js";
    // Bundle name that is used later in order to get locale translations
    private static final String BUNDLE_NAME = DebugNode.class.getName().replace(".", "/");
    // Logger instance which will allow us to add debug node logs to AM logs
    private final Logger logger = LoggerFactory.getLogger(DebugNode.class);
    // Stores the node's configuration attributes
    private final Config config;

    /**
     * Configuration for the debug node.
     */
    public interface Config {
        /**
        * Allow debug log popup to be turned on or off.
        *
        * @return whether popup is enabled.
        */
        @Attribute(order = 100)
        default boolean popupEnabled() {
            return true;
        }
    }

    /**
     * Create the node.
     * @param config The node config.
     */
    @Inject
    public DebugNode(@Assisted DebugNode.Config config) {
        this.config = config;
        this.auditRequestContext = getAuditRequestContext();
    }

    /**
     * Generates a json object of all the translations from the resource bundle
     * and returns it as a string, so it can be used by the loaded javascript file.
     *
     * @return stringified json
     */
    private String getTranslationsFromBundle(ResourceBundle bundle) {
        JsonValue translations = json(object());
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            translations.put(key, bundle.getString(key));
        }
        return translations.toString();
    }

    private String generateDebugScriptWithoutPopup() {
        return "window.frDebugNode.moveToNextNode();";
    }

    /**
     * Generates a new string of javascript which will add the latest debug log
     * to the page.
     *
     * @return string of javascript
     */
    private String generateDebugScriptWithPopup(String logs, TreeContext treeContext) {
        StringBuilder script = new StringBuilder();

        // Get the translated text for the node from the node bundle
        ResourceBundle bundle = treeContext.request.locales
                              .getBundleInPreferredLocale(BUNDLE_NAME, getClass().getClassLoader());
        String translations = getTranslationsFromBundle(bundle);

        // Create the popup window if it doesn't already exist
        script.append("window.frDebugNode.createPopup();");

        // Set the translations in JS so they can be used in the popup window
        script.append("window.frDebugNode.setTranslations('")
              .append(translations)
              .append("');");

        // Set the popup window title from translations
        script.append("window.frDebugNode.setPopupTitle('")
              .append(bundle.getString("popup.title"))
              .append("');");

        // Add the Realm name to the debug output
        JsonValue realm = treeContext.getStateFor(this).get(REALM);
        if (realm != null) {
            script.append("window.frDebugNode.setRealm('")
                  .append(realm.toString().replace("\"", ""))
                  .append("');");
        }

        // Add the new logs to the debug output
        script.append("window.frDebugNode.appendLog('")
              .append(logs)
              .append("');");

        // Auto submit the page to move to the next node in the tree
        script.append("window.frDebugNode.moveToNextNode();");

        return script.toString();
    }

    /**
     * Collects logs for the current state of the tree from treeContext.
     *
     * @return debug log
     */
    private String getLogs(TreeContext treeContext) {
        JsonValue log = json(object());

        // If the identity objects universalId exists, add it to the logs
        if (treeContext.universalId.isPresent()) {
            log.put("universalId", treeContext.universalId.get());
        }

        // Add the transactionId
        log.put("transactionId", this.auditRequestContext.getTransactionIdValue());

        // Get the state of the current node
        NodeState nodeState = treeContext.getStateFor(this);
        // Get all the nodes state keys
        Set<String> stateKeys = nodeState.keys();
        // Combine the keys with their values
        for (String key : stateKeys) {
            log.put(key, nodeState.get(key));
        }

        return log.toString();
    }

    @Override
    public Action process(TreeContext treeContext) throws NodeProcessException {
        // The process function is called before and after the node has been
        // shown. If the treeContext has callbacks this function has been
        // invoked at the end of the debug node lifecycle, and the tree should
        // proceed to the next step.
        if (treeContext.hasCallbacks()) {
            return goToNext().build();
        }

        String logs = getLogs(treeContext);
        // Add the latest logs to the debug logger
        logger.debug("DebugNode: {}", logs);

        String localScript;
        // If popup window is enabled, generate a script to update the popup
        // window and submit the page. If it is disabled, generate a script that
        // will submit the page only.
        if (config.popupEnabled()) {
            localScript = generateDebugScriptWithPopup(logs, treeContext);
        } else {
            localScript = generateDebugScriptWithoutPopup();
        }

        // Generate callbacks and send them to the page. When the script
        // callbacks are rendered, they will auto submit the page and move the
        // user to the next node. However, for a very brief period while the
        // scripts load, the login page will render and show a login button. A
        // TextOutputCallback has been used here to indicate to the user what is
        // happening instead of leaving the page blank.
        TextOutputCallback textCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "Debug Step");
        ScriptTextOutputCallback loadedScriptCallback = new ScriptTextOutputCallback(
            scriptUtils.getScriptAsString(SCRIPT_PATH));
        ScriptTextOutputCallback localScriptCallback = new ScriptTextOutputCallback(localScript);
        return send(loadedScriptCallback, localScriptCallback, textCallback).build();
    }
}
