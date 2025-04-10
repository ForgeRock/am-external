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

import static org.forgerock.openam.authentication.api.AuthenticationConstants.REALM;
import static org.forgerock.openam.authentication.api.AuthenticationConstants.SUCCESS_URL;
import static org.forgerock.openam.authentication.api.AuthenticationConstants.TOKEN_ID;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.treehook.SuccessDetailsTreeHook;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * A node that sets success details on the response from the Success node.
 */
@Node.Metadata(outcomeProvider = SetSuccessDetailsNode.OutcomeProvider.class,
        configClass = SetSuccessDetailsNode.Config.class, tags = {"utilities"})
public class SetSuccessDetailsNode extends SingleOutcomeNode {

    private final UUID nodeId;

    @Inject
    SetSuccessDetailsNode(@Assisted UUID nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public Action process(TreeContext context) {
        return goToNext().addSessionHook(SuccessDetailsTreeHook.class, nodeId,
                        SetSuccessDetailsNode.class.getSimpleName())
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
         * The details to set on tree success.
         * @return the success details
         */
        @Attribute(order = 100, validators = {ResponseBodyPredefinedKeysValidator.class})
        Map<String, String> successDetails();

        /**
         * The session properties to set on tree success.
         * @return the session properties
         */
        @Attribute(order = 200, validators = {ResponseBodyPredefinedKeysValidator.class})
        Map<String, String> sessionProperties();
    }

    /**
     * Validates that the keys in the response body are not predefined success response keys.
     */
    static final class ResponseBodyPredefinedKeysValidator implements ServiceAttributeValidator {

        private static final List<String> PREDEFINED_KEYS = List.of(TOKEN_ID, REALM, SUCCESS_URL);

        @Override
        public boolean validate(Set<String> values) {
            return values.stream()
                    .map(v -> StringUtils.substringBetween(v, "[", "]"))
                    .noneMatch(ResponseBodyPredefinedKeysValidator::isPredefinedKey);
        }

        private static boolean isPredefinedKey(String string) {
            return PREDEFINED_KEYS.contains(string);
        }
    }
}
