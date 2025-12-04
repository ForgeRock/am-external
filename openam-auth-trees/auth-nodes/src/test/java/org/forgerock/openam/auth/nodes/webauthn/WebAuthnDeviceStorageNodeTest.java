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
package org.forgerock.openam.auth.nodes.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.EXCEED_DEVICE_LIMIT_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.FAILURE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.webauthn.AbstractWebAuthnNode.SUCCESS_OUTCOME_ID;

import java.util.List;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Test;

class WebAuthnDeviceStorageNodeTest {

    @Test
    void shouldReturnAllOutcomesWhenGetAllOutcomes() {
        // given
        WebAuthnDeviceStorageNode.OutcomeProvider outcomeProvider = new WebAuthnDeviceStorageNode.OutcomeProvider();

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getAllOutcomes(new PreferredLocales());

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                EXCEED_DEVICE_LIMIT_OUTCOME_ID);
    }

    @Test
    void shouldReturnAllOutcomesWhenMaxSavedDevicesIsGreaterThanZero() throws NodeProcessException {
        // given
        WebAuthnDeviceStorageNode.OutcomeProvider outcomeProvider = new WebAuthnDeviceStorageNode.OutcomeProvider();
        var attributes = json(object(field("maxSavedDevices", 1)));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID,
                EXCEED_DEVICE_LIMIT_OUTCOME_ID);
    }

    @Test
    void shouldNotReturnExceedDeviceLimitOutcomeWhenMaxSavedDevicesIsZero() throws NodeProcessException {
        // given
        WebAuthnDeviceStorageNode.OutcomeProvider outcomeProvider = new WebAuthnDeviceStorageNode.OutcomeProvider();
        var attributes = json(object(field("maxSavedDevices", 0)));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID);
    }

    @Test
    void shouldNotReturnExceedDeviceLimitOutcomeWhenAttributesIsNull() throws NodeProcessException {
        // given
        WebAuthnDeviceStorageNode.OutcomeProvider outcomeProvider = new WebAuthnDeviceStorageNode.OutcomeProvider();
        var attributes = json(null);

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                SUCCESS_OUTCOME_ID,
                FAILURE_OUTCOME_ID);
    }

}
