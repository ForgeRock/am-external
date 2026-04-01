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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ACCEPT;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ERROR;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.REJECT;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.adapters.PolicySetSelector;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.entitlement.service.PolicySubjectService;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.PolicyEvaluator;
import com.sun.identity.entitlement.PolicyEvaluatorFactory;
import com.sun.identity.entitlement.opensso.SubjectFactory;

/**
 * A node that evaluates policies against a specified resource and policy set.
 */
@Node.Metadata(outcomeProvider = PolicyDecisionNode.OutcomeProvider.class,
        configClass = PolicyDecisionNode.Config.class,
        tags = "authz")
public class PolicyDecisionNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(PolicyDecisionNode.class);
    private static final String BUNDLE = PolicyDecisionNode.class.getName();

    private final Config config;
    private final Realm realm;
    private final Provider<SSOToken> adminTokenProvider;
    private final PolicyEvaluatorFactory evaluatorFactory;
    private final SubjectFactory subjectFactory;
    private final PolicySubjectService policySubjectService;
    private final NodeUserIdentityProvider nodeUserIdentityProvider;

    /**
     * Node Configuration.
     */
    public interface Config {

        /**
         * The Policy Set (Application) to evaluate policies against.
         *
         * @return the policy set name.
         */
        @PolicySetSelector
        @Attribute(order = 100, requiredValue = true)
        String policySet();

        /**
         * The resource name to use for policy evaluation.
         *
         * @return the resource name.
         */
        @Attribute(order = 200, requiredValue = true)
        String resource();

        /**
         * The action to check in the policy evaluation result.
         * @return the action name.
         */
        @Attribute(order = 300, requiredValue = true)
        String action();

        /**
         * Additional environment parameters to include in the policy evaluation. These are static values
         * but can be used with a config provider node to provide dynamic values.
         *
         * @return a map of environment parameters.
         */
        @Attribute(order = 400)
        Map<String, String> environment();
    }

    /**
     * Guice constructor.
     *
     * @param config               The node configuration.
     * @param realm                The realm.
     * @param adminTokenProvider   The admin token provider.
     * @param evaluatorFactory     The policy evaluator factory.
     * @param subjectFactory       The subject factory.
     * @param policySubjectService The policy subject service.
     */
    @Inject
    PolicyDecisionNode(@Assisted Config config,
                                 @Assisted Realm realm,
                                 @Named("AdminToken") Provider<SSOToken> adminTokenProvider,
                                 PolicyEvaluatorFactory evaluatorFactory,
                                 SubjectFactory subjectFactory,
                                 PolicySubjectService policySubjectService,
                                 NodeUserIdentityProvider nodeUserIdentityProvider) {
        this.config = config;
        this.realm = realm;
        this.adminTokenProvider = adminTokenProvider;
        this.evaluatorFactory = evaluatorFactory;
        this.subjectFactory = subjectFactory;
        this.policySubjectService = policySubjectService;
        this.nodeUserIdentityProvider = nodeUserIdentityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        try {
            String policySet = config.policySet();
            String resource = getResource(context);
            final NodeState nodeState = context.getStateFor(this);
            JsonValue subjectJson = getSubjectJson(context, nodeState);
            Map<String, Set<String>> environment = getEnvironmentMap();
            Subject policySubject = policySubjectService.getPolicySubject(
                    json(object(field("subject", subjectJson))),
                    null,
                    environment,
                    realm.asPath());
            if (policySubject != null && policySubject.getPrincipals().isEmpty()) {
                logger.debug("Policy subject was not found or is inactive - setting to null for policy evaluation");
                // if the user is not found an empty subject is returned which can cause issues evaluating policies
                // so that policies that don't care about the subject can be handled the subject is set to null instead
                policySubject = null;
            }

            PolicyEvaluator policyEvaluator = getPolicyEvaluator(policySet);
            List<Entitlement> evaluationResult = policyEvaluator.evaluate(
                    realm.asPath(),
                    policySubject,
                    Collections.singleton(resource),
                    environment);
            if (evaluationResult.isEmpty()) {
                return Action.goTo(REJECT.name()).build();
            } else {
                Entitlement entitlement = evaluationResult.get(0);
                if (entitlement.getActionValues().containsKey(config.action())
                            && entitlement.getActionValues().get(config.action())) {
                    return Action.goTo(ACCEPT.name()).build();
                } else {
                    return Action.goTo(REJECT.name()).build();
                }
            }
        } catch (Exception e) {
            logger.error("Policy evaluation failed", e);
            return Action.goTo(ERROR.name()).build();
        }
    }

    private Map<String, Set<String>> getEnvironmentMap() {
        // Note this is explicitly added to a HashMap to ensure mutability
        // the policy framework may add additional environment parameters to this map during evaluation
        return config.environment().entrySet().stream()
                                                       .collect(Collectors.toMap(Map.Entry::getKey,
                                                               e -> Set.of(e.getValue()),
                                                               (a, b) -> a, HashMap::new));
    }

    private JsonValue getSubjectJson(TreeContext context, NodeState nodeState) throws NodeProcessException {
        String ssoToken = context.request.ssoTokenId;
        if (isNotBlank(ssoToken)) {
            logger.debug("Using existing SSO token for policy evaluation");
            return json(object(field("ssoToken", ssoToken)));
        }

        var identity = nodeUserIdentityProvider.getIdentity(realm,
                context.identifiedIdentity.orElse(null), nodeState);
        return identity.map(id -> id.asUniversalId().getName())
                       .map(sub -> json(object(field("claims", object(field("sub", sub))))))
                       .orElseGet(() -> {
                           logger.debug("No user details found for policy subject");
                           return null;
                       });
    }

    private PolicyEvaluator getPolicyEvaluator(String application) {
        Subject adminSubject = subjectFactory.create(adminTokenProvider.get());
        return evaluatorFactory.create(adminSubject, application);
    }

    /**
     * Get the resource information.
     *
     * @param context the tree context
     * @return the resource name
     * @throws NodeProcessException if there is a problem getting the resource
     */
    protected String getResource(TreeContext context) throws NodeProcessException {
        return config.resource();
    }

    /**
     * Outcome provider for PolicyDecisionNode.
     */
    public static class OutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(ACCEPT.name(), bundle.getString("outcome.accept")),
                    new Outcome(REJECT.name(), bundle.getString("outcome.reject")),
                    new Outcome(ERROR.name(), bundle.getString("outcome.error")));
        }
    }

    /**
     * Possible outcomes.
     */
    public enum Outcome {
        /**
         * Policy evaluation accepted.
         */
        ACCEPT,
        /**
         * Policy evaluation rejected.
         */
        REJECT,
        /**
         * Policy evaluation error.
         */
        ERROR
    }
}
