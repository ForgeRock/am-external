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
import org.forgerock.openam.auth.nodes.treehook.FailureDetailsTreeHook;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that sets failure details on the response from the Failure node.
 */
@Node.Metadata(outcomeProvider = SetFailureDetailsNode.OutcomeProvider.class,
        configClass = SetFailureDetailsNode.Config.class, tags = {"utilities"})
public class SetFailureDetailsNode extends SingleOutcomeNode {

    private final UUID nodeId;
    private final Config config;
    private final LocalizedMessageProvider localizedMessageProvider;

    @Inject
    SetFailureDetailsNode(@Assisted UUID nodeId, @Assisted Config config, @Assisted Realm realm,
            LocalizedMessageProvider.LocalizedMessageProviderFactory localizedMessageProviderFactory) {
        this.nodeId = nodeId;
        this.config = config;
        this.localizedMessageProvider = localizedMessageProviderFactory.create(realm);
    }

    @Override
    public Action process(TreeContext context) {
        String message = localizedMessageProvider.getLocalizedMessage(context, SetFailureDetailsNode.class,
                config.failureMessage(), "");
        JsonValue data = null;
        if (isNotBlank(message)) {
            data = json(object(field("message", message)));
        }
        return goToNext().addSessionHook(FailureDetailsTreeHook.class, nodeId,
                SetFailureDetailsNode.class.getSimpleName(), data)
                       .build();
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
         * The localizable failure message to set on tree failure.
         * @return the failure message
         */
        @Attribute(order = 100)
        Map<Locale, String> failureMessage();

        /**
         * The failure details to set on tree failure.
         * @return the failure details
         */
        @Attribute(order = 200)
        Map<String, String> failureDetails();
    }
}
