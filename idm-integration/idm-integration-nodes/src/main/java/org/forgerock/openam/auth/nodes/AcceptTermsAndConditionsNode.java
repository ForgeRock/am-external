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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getActiveTerms;

import java.time.ZoneOffset;
import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.TermsAndConditionsCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.TermsAndConditionsConfig;
import org.forgerock.openam.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that adds the accepted terms &amp; conditions to the shared state.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AcceptTermsAndConditionsNode.Config.class,
        tags = {"identity management"})
public class AcceptTermsAndConditionsNode extends SingleOutcomeNode {
    static final String TERMS_ACCEPTED = "termsAccepted";
    static final String ACCEPT_DATE = "acceptDate";
    static final String TERMS_VERSION = "termsVersion";

    private final Logger logger = LoggerFactory.getLogger(AcceptTermsAndConditionsNode.class);
    private final IdmIntegrationService idmIntegrationService;
    private final Realm realm;

    /**
     * Configuration for the node.
     */
    public interface Config {

    }

    /**
     * Create the node.
     *
     * @param realm The realm context
     * @param idmIntegrationService Service stub for the IDM integration service
     */
    @Inject
    public AcceptTermsAndConditionsNode(@Assisted Realm realm, IdmIntegrationService idmIntegrationService) {
        this.realm = realm;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("AcceptTermsAndConditionsNode started");
        Optional<TermsAndConditionsCallback> callback = context.getCallback(TermsAndConditionsCallback.class);

        // return callback if empty or did not accept terms
        if (!callback.isPresent() || !callback.get().getAccept()) {
            logger.debug("Retrieving active terms configuration");
            TermsAndConditionsConfig activeTerms =
                    getActiveTerms(idmIntegrationService, realm, context.request.locales)
                            .orElseThrow(() -> new NodeProcessException("Terms configuration not found"));

            logger.debug("Collecting acceptance of version {} of terms", activeTerms.getVersion());
            TermsAndConditionsCallback termsCallback =
                    new TermsAndConditionsCallback(activeTerms.getVersion(),
                            activeTerms.getTerms(), activeTerms.getCreateDate());

            context.sharedState.put(TERMS_VERSION, activeTerms.getVersion());
            return send(termsCallback).build();
        }

        JsonValue sharedState = context.sharedState.copy();
        sharedState.put(TERMS_ACCEPTED, object(
                field(ACCEPT_DATE, Time.zonedDateTime(ZoneOffset.UTC).toString()),
                field(TERMS_VERSION, context.sharedState.get(TERMS_VERSION).asString()))
        );

        return goToNext()
                .replaceSharedState(sharedState)
                .replaceTransientState(context.transientState.copy())
                .build();
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(TERMS_ACCEPTED)
        };
    }
}
