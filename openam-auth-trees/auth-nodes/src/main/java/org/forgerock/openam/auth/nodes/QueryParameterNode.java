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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node to instruct the tree to add query parameters to sharedState.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = QueryParameterNode.Config.class,
        configValidator = QueryParameterNode.QueryParameterNodeValidator.class,
        tags = {"utilities"})
public class QueryParameterNode extends SingleOutcomeNode {

    /**
     * Validates the query parameter node, ensuring all provided values to be delimited and delimiters are valid.
     */
    public static class QueryParameterNodeValidator implements ServiceConfigValidator {

        private static final Logger logger =
                LoggerFactory.getLogger(QueryParameterNode.QueryParameterNodeValidator.class);

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            Set<String> allowedParameters = attributes.get("allowedQueryParameters");
            if (CollectionUtils.isEmpty(allowedParameters)) {
                logger.error("No parameters configured - this node is redundant");
                throw new ServiceConfigException("No parameters configured - this node is redundant");
            }

            Set<String> allowedParameterNames = allowedParameters.stream()
                    .map(allowedParameterName ->
                            StringUtils.substringBetween(allowedParameterName, "[", "]"))
                    .collect(Collectors.toSet());

            Set<String> parametersToBeDelimited = attributes.get("queryParametersToBeDelimited");
            validateParametersToBeDelimited(allowedParameterNames, parametersToBeDelimited);
        }

        private void validateParametersToBeDelimited(Set<String> allowedParameterNames,
                Set<String> parametersToBeDelimited) throws ServiceConfigException {

            if (!allowedParameterNames.containsAll(parametersToBeDelimited)) {
                logger.error(
                        "Cannot delimit parameter if it is not configured as a parameter to be stored in shared state");
                throw new ServiceConfigException("Invalid parameter to be delimited configured");
            }
        }
    }

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The parameters to put into sharedState with the value based on the key to be found in query parameters.
         *
         * @return the parameters to put into sharedState with value based on the key to be found in query parameters.
         */
        @Attribute(order = 100)
        Map<String, String> allowedQueryParameters();

        /**
         * The parameters that need to be delimited by a comma before being placed into sharedState as a list.
         *
         * @return the parameters that need to be delimited by a comma, before being placed into sharedState as a list.
         */
        @Attribute(order = 200)
        default Set<String> queryParametersToBeDelimited() {
            return Collections.emptySet();
        }
    }

    private final Config config;

    private static final String DELIMITER = ",";

    /**
     * The QueryParameterNode constructor.
     *
     * @param config The service config.
     */
    @Inject
    public QueryParameterNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);
        config.allowedQueryParameters().forEach((queryParamName, sharedStateKeyName) -> {
            List<String> queryParamValue = context.request.parameters.get(queryParamName);

            if (CollectionUtils.isEmpty(queryParamValue)) {
                nodeState.putShared(sharedStateKeyName, List.of());
                return;
            }

            queryParamValue = queryParamValue.stream()
                    .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                    .collect(Collectors.toList());

            if (queryParamValue.size() == 1 && config.queryParametersToBeDelimited().contains(queryParamName)) {
                nodeState.putShared(sharedStateKeyName, List.of(queryParamValue.get(0).split(DELIMITER)));
            } else {
                nodeState.putShared(sharedStateKeyName, queryParamValue);
            }
        });
        return goToNext().build();
    }

}
