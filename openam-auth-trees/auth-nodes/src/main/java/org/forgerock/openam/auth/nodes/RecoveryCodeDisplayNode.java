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
 * Copyright 2018-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.ClientScriptUtilities;
import org.forgerock.openam.utils.CollectionUtils;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

/**
 * This authentication node is used to display a set of generate recovery codes to a user in a list, with a title.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = RecoveryCodeDisplayNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class RecoveryCodeDisplayNode extends SingleOutcomeNode {

    private static final String BUNDLE = RecoveryCodeDisplayNode.class.getName();
    private static final String SCRIPT = "org/forgerock/openam/auth/nodes/RecoveryCodeDisplayNode.js";

    /**
     * Key under which recovery codes should be inserted into the transient state by the preceding node.
     */
    public static final String RECOVERY_CODE_KEY = "recoveryCodes";

    /**
     * Key under which the device name should be inserted into the transient state by the preceding node.
     */
    public static final String RECOVERY_CODE_DEVICE_NAME = "recoveryCodeDeviceName";

    private static final String RECOVERY_CODE_TITLE = "recoveryCodeTitle";
    private static final String RECOVERY_CODE_DEVICE_INTRO = "recoveryCodeDeviceIntroduction";
    private static final String RECOVERY_CODE_DESCRIPTION = "recoveryCodeDescription";

    private final ClientScriptUtilities scriptUtils;

    /**
     * Empty config.
     */
    public interface Config {
    }

    /**
     * Generates a new RecoveryCodeDisplayNode.
     *
     * @param scriptUtils Used to read the JavaScript to render codes.
     */
    @Inject
    public RecoveryCodeDisplayNode(ClientScriptUtilities scriptUtils) {
        this.scriptUtils = scriptUtils;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        NodeState nodeState = context.getStateFor(this);
        List<String> codes = nodeState.isDefined(RECOVERY_CODE_KEY)
                ? nodeState.get(RECOVERY_CODE_KEY).asList(String.class) : null;

        if (CollectionUtils.isNotEmpty(codes)) {
            String name = nodeState.get(RECOVERY_CODE_DEVICE_NAME).asString();
            ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE,
                    RecoveryCodeDisplayNode.OutcomeProvider.class.getClassLoader());

            List<String> entries = new ArrayList<>();
            entries.add(bundle.getString(RECOVERY_CODE_TITLE));
            entries.add(bundle.getString(RECOVERY_CODE_DEVICE_INTRO));
            entries.add(name);
            entries.add(bundle.getString(RECOVERY_CODE_DESCRIPTION));
            entries.addAll(codes);

            String dehydratedScript = scriptUtils.getScriptAsString(SCRIPT);
            String script = String.format(dehydratedScript, entries.toArray());

            ScriptTextOutputCallback scriptCallback = new ScriptTextOutputCallback(script);

            nodeState.remove(RECOVERY_CODE_KEY);
            nodeState.remove(RECOVERY_CODE_DEVICE_NAME);
            return send(scriptCallback).build();
        } else {
            return goToNext().build();
        }

    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(RECOVERY_CODE_KEY),
            new InputState(RECOVERY_CODE_DEVICE_NAME)
        };
    }
}
