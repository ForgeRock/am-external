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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import javax.inject.Inject;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.SessionUpgradeVerifier;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that connect the user as anonymous.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = AnonymousUserNode.Config.class, tags = {"utilities"})
public class AnonymousUserNode extends SingleOutcomeNode {

    private final LegacyIdentityService identityService;
    private final Config config;

    /**
     * The config for the node.
     */
    public interface Config {

        /**
         * The anonymous user name.
         * @return the anonymous user name
         */
        @Attribute(order = 100, validators = RequiredValueValidator.class)
        default String anonymousUserName() {
            return "anonymous";
        }
    }

    /**
     * Constructor.
     * @param identityService An instance of the IdentityService.
     * @param config the config
     */
    @Inject
    public AnonymousUserNode(LegacyIdentityService identityService,  @Assisted AnonymousUserNode.Config config) {
        this.identityService = identityService;
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        String username = getAnonymousUser();
        String realm = context.sharedState.get(REALM).asString();
        return goToNext()
                .addNodeType(context, SessionUpgradeVerifier.ANONYMOUS_MODULE_TYPE)
                .withUniversalId(identityService.getUniversalId(username, realm, USER))
                .withIdentifiedIdentity(username, USER)
                .replaceSharedState(context.sharedState.put(USERNAME, username))
                .build();
    }

    private String getAnonymousUser() throws NodeProcessException {
        String anonUser = config.anonymousUserName();
        if (anonUser != null && !anonUser.isEmpty()) {
            return anonUser;
        }
        throw new NodeProcessException("Anonymous user name could not be found in the configuration");
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(USERNAME)
        };
    }
}
