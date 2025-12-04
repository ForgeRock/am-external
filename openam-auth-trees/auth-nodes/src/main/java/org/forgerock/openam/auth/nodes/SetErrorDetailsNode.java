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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.treehook.ErrorDetailsTreeHook;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that sets exception details on the response when a journey exception is thrown.
 */
@Node.Metadata(outcomeProvider = SetErrorDetailsNode.OutcomeProvider.class,
        configClass = SetErrorDetailsNode.Config.class, tags = {"utilities"})
public class SetErrorDetailsNode extends SingleOutcomeNode {

    private final UUID nodeId;
    private final Config config;
    private final LocalizedMessageProvider localizedMessageProvider;

    @Inject
    SetErrorDetailsNode(@Assisted UUID nodeId, @Assisted Config config, @Assisted Realm realm,
            LocalizedMessageProvider.LocalizedMessageProviderFactory localizedMessageProviderFactory) {
        this.nodeId = nodeId;
        this.config = config;
        this.localizedMessageProvider = localizedMessageProviderFactory.create(realm);
    }

    @Override
    public Action process(TreeContext context) {
        String message = localizedMessageProvider.getLocalizedMessage(context, SetErrorDetailsNode.class,
                config.errorMessage(), "");
        JsonValue data = null;
        if (isNotBlank(message)) {
            data = json(object(field("message", message)));
        }
        return goToNext().addSessionHook(ErrorDetailsTreeHook.class, nodeId,
                SetErrorDetailsNode.class.getSimpleName(), data).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[0];
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[0];
    }

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The localizable exception message to set on tree exception.
         * @return the exception message
         */
        @Attribute(order = 100)
        Map<Locale, String> errorMessage();

        /**
         * The exception details to set on tree exception.
         * @return the exception details
         */
        @Attribute(order = 200)
        Map<String, String> errorDetails();
    }
}
