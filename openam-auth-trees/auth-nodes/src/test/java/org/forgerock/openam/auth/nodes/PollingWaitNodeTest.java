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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;

import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Test;

class PollingWaitNodeTest {

    @Test
    void shouldReturnAllOutcomesWhenGetAllOutcomes() {
        // given
        PollingWaitNode.PollingWaitOutcomeProvider outcomeProvider = new PollingWaitNode.PollingWaitOutcomeProvider();

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getAllOutcomes(new PreferredLocales());

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                PollingWaitNode.PollingWaitOutcome.DONE.name(),
                PollingWaitNode.PollingWaitOutcome.EXITED.name(),
                PollingWaitNode.PollingWaitOutcome.SPAM.name());
    }

    @Test
    void shouldNotReturnExitedOrSpamOutcomesWhenAttributesIsNull() {
        // given
        PollingWaitNode.PollingWaitOutcomeProvider outcomeProvider = new PollingWaitNode.PollingWaitOutcomeProvider();
        var attributes = json(null);

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                PollingWaitNode.PollingWaitOutcome.DONE.name());
    }

    @Test
    void shouldReturnAllAttributesWhenExitableIsTrueAndSpamDetectionIsEnabled() {
        // given
        PollingWaitNode.PollingWaitOutcomeProvider outcomeProvider = new PollingWaitNode.PollingWaitOutcomeProvider();
        var attributes = json(object(
                field("exitable", true),
                field("spamDetectionEnabled", true)
        ));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                PollingWaitNode.PollingWaitOutcome.DONE.name(),
                PollingWaitNode.PollingWaitOutcome.EXITED.name(),
                PollingWaitNode.PollingWaitOutcome.SPAM.name());
    }

    @Test
    void shouldNotReturnSpamOutcomeWhenSpamDetectionIsDisabled() {
        // given
        PollingWaitNode.PollingWaitOutcomeProvider outcomeProvider = new PollingWaitNode.PollingWaitOutcomeProvider();
        var attributes = json(object(
                field("exitable", true),
                field("spamDetectionEnabled", false)
        ));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                PollingWaitNode.PollingWaitOutcome.DONE.name(),
                PollingWaitNode.PollingWaitOutcome.EXITED.name());
    }

    @Test
    void shouldNotReturnExitOutcomeWhenExitableIsFalse() {
        // given
        PollingWaitNode.PollingWaitOutcomeProvider outcomeProvider = new PollingWaitNode.PollingWaitOutcomeProvider();
        var attributes = json(object(
                field("exitable", false),
                field("spamDetectionEnabled", true)
        ));

        // when
        List<OutcomeProvider.Outcome> outcomes = outcomeProvider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList()).containsExactly(
                PollingWaitNode.PollingWaitOutcome.DONE.name(),
                PollingWaitNode.PollingWaitOutcome.SPAM.name());
    }
}
