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
 * Copyright 2017-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.saml2;

import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getUniversalId;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;

import java.util.Optional;

import org.forgerock.am.saml2.impl.Saml2SsoResponseUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.am.identity.application.LegacyIdentityService;

import com.google.inject.Inject;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This authentication node persists the NameID to username mapping to the user store.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = WriteFederationInformationNode.Config.class,
        tags = {"social", "federation"},
        namespace = Namespace.PRODUCT)
public class WriteFederationInformationNode extends SingleOutcomeNode {

    private final Saml2SsoResponseUtils ssoResponseUtils;
    private final LegacyIdentityService identityService;

    /**
     * Node configuration.
     */
    public interface Config {
    }

    /**
     * Guice injected constructor.
     *
     * @param ssoResponseUtils SAML2 response utilities.
     * @param identityService An instance of IdentityService.
     */
    @Inject
    public WriteFederationInformationNode(Saml2SsoResponseUtils ssoResponseUtils,
            LegacyIdentityService identityService) {
        this.ssoResponseUtils = ssoResponseUtils;
        this.identityService = identityService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;

        if (!sharedState.isDefined(USER_INFO_SHARED_STATE_KEY)) {
            throw new NodeProcessException("No user information has been found in the shared state. You must call a "
                    + "node that sets this information first");
        }
        JsonValue attributes = context.sharedState.get(USER_INFO_SHARED_STATE_KEY).get("attributes");

        String infoAttribute = AccountUtils.getNameIDInfoAttribute();

        if (!attributes.isDefined(infoAttribute)) {
            throw new NodeProcessException(AccountUtils.getNameIDInfoAttribute() + " is not defined in shared state. "
                    + "You must first call a node that sets this information first");
        }

        String infoAttributeValue = attributes.get(infoAttribute).get(0).asString();
        NodeState nodeState = context.getStateFor(this);
        try {
            Optional<String> universalId = context.universalId.or(() -> getUniversalId(nodeState, identityService));
            ssoResponseUtils.linkAccounts(infoAttributeValue, universalId.orElse(null));
        } catch (SAML2Exception ex) {
            throw new NodeProcessException("Unable to link accounts", ex);
        }

        return goToNext().build();
    }
}
