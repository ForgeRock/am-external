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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ACCEPT;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ERROR;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.REJECT;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.function.Failable;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.entitlement.service.PolicySubjectService;
import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.PolicyEvaluatorFactory;
import com.sun.identity.entitlement.opensso.SubjectFactory;

/**
 * Opinionated implementation of the {@link PolicyDecisionNode} for use with App policies and App authentication.
 */
@Node.Metadata(outcomeProvider = AppPolicyDecisionNode.OutcomeProvider.class,
        configClass = AppPolicyDecisionNode.Config.class,
        tags = "authz")
public class AppPolicyDecisionNode extends PolicyDecisionNode {

    private final AuthScriptUtilities authScriptUtils;

    /**
     * Guice constructor.
     *
     * @param config                   The node configuration.
     * @param realm                    The realm.
     * @param adminTokenProvider       The admin token provider.
     * @param evaluatorFactory         The policy evaluator factory.
     * @param subjectFactory           The subject factory.
     * @param policySubjectService     The policy subject service.
     * @param nodeUserIdentityProvider The identity provider.
     * @param authScriptUtils      The auth script utilities.
     */
    @Inject
    AppPolicyDecisionNode(@Assisted Config config, @Assisted Realm realm,
            @Named("AdminToken") Provider<SSOToken> adminTokenProvider,
            PolicyEvaluatorFactory evaluatorFactory, SubjectFactory subjectFactory,
            PolicySubjectService policySubjectService,
            NodeUserIdentityProvider nodeUserIdentityProvider, AuthScriptUtilities authScriptUtils) {
        super(config.getConfig(), realm, adminTokenProvider, evaluatorFactory, subjectFactory,
                policySubjectService, nodeUserIdentityProvider);
        this.authScriptUtils = authScriptUtils;
    }

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Returns a filled-in config object which extends {@link PolicyDecisionNode.Config}.
         *
         * @return the config
         */
        default PolicyDecisionNode.Config getConfig() {
            return new AppPolicyDecisionConfig();
        }

    }

    private static final class AppPolicyDecisionConfig implements PolicyDecisionNode.Config {

        @Override
        public String policySet() {
            return "customerApplicationPolicySet";
        }

        @Override
        public String resource() {
            // this should never be accessed
            throw new NotImplementedException();
        }

        @Override
        public String action() {
            return "Access";
        }

        @Override
        public Map<String, String> environment() {
            return Map.of();
        }
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        if (getResource(context) == null) {
            return Action.goTo("UNKNOWN_RESOURCE").build();
        }
        return super.process(context);
    }

    @Override
    protected String getResource(TreeContext context) throws NodeProcessException {
        return authScriptUtils.getSAMLData(context).map(SAMLScriptedBindingObject::getApplicationId)
                       .orElseGet(Failable.asSupplier(() -> authScriptUtils.getOauthDecisionNodeApplication(
                               context).map(OAuthScriptedBindingObject::getApplicationId).orElse(null)));
    }

    /**
     * Outcome provider for AppPolicyDecisionNode.
     */
    public static class OutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(
                    AppPolicyDecisionNode.class.getName(), OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(ACCEPT.name(), bundle.getString("outcome.accept")),
                    new Outcome(REJECT.name(), bundle.getString("outcome.reject")),
                    new Outcome(ERROR.name(), bundle.getString("outcome.error")),
                    new Outcome("UNKNOWN_RESOURCE", bundle.getString("outcome.unknownResource")));
        }
    }
}
