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
 * Copyright 2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

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
 * A node to instruct the tree to add header values to sharedState.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = RequestHeaderNode.Config.class,
        configValidator = RequestHeaderNode.RequestHeaderNodeValidator.class,
        tags = {"utilities"})
public class RequestHeaderNode extends SingleOutcomeNode {

    /**
     * Validates the header node, ensuring all provided headers to be delimited are valid.
     */
    public static class RequestHeaderNodeValidator implements ServiceConfigValidator {

        private static final Logger logger =
                LoggerFactory.getLogger(RequestHeaderNodeValidator.class);

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            Set<String> allowedHeaders = attributes.get("allowedHeaders");
            if (CollectionUtils.isEmpty(allowedHeaders)) {
                logger.error("No headers configured - this node is redundant");
                throw new ServiceConfigException("No headers configured - this node is redundant");
            }

            Set<String> allowedHeaderNames = allowedHeaders.stream()
                    .map(allowedHeaderName ->
                            StringUtils.substringBetween(allowedHeaderName, "[", "]"))
                    .collect(Collectors.toSet());

            Set<String> headersToBeDelimited = attributes.get("headersToBeDelimited");
            validateParametersToBeDelimited(allowedHeaderNames, headersToBeDelimited);
        }

        private void validateParametersToBeDelimited(Set<String> allowedHeaderNames,
                Set<String> headersToBeDelimited) throws ServiceConfigException {

            if (!allowedHeaderNames.containsAll(headersToBeDelimited)) {
                logger.error(
                        "Cannot delimit header if it is not configured as a header to be stored in shared state");
                throw new ServiceConfigException("Invalid header to be delimited configured");
            }
        }
    }

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The headers to put into sharedState with the value based on the key to be found in request headers.
         *
         * @return the headers to put into sharedState with value based on the key to be found in request headers.
         */
        @Attribute(order = 100)
        Map<String, String> allowedHeaders();

        /**
         * The headers that need to be delimited by a comma before being placed into sharedState as a list.
         *
         * @return the headers that need to be delimited by a comma, before being placed into sharedState as a list.
         */
        @Attribute(order = 200)
        default Set<String> headersToBeDelimited() {
            return Collections.emptySet();
        }
    }

    private final Config config;

    private static final String DELIMITER = ",";

    /**
     * The RequestHeaderNode constructor.
     *
     * @param config The service config.
     */
    @Inject
    public RequestHeaderNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);
        config.allowedHeaders().forEach((headerName, sharedStateKeyName) -> {
            List<String> headerValue = context.request.headers.get(headerName);

            if (CollectionUtils.isEmpty(headerValue)) {
                nodeState.putShared(sharedStateKeyName, List.of());
                return;
            }

            if (headerValue.size() == 1 && config.headersToBeDelimited().contains(headerName)) {
                nodeState.putShared(sharedStateKeyName, List.of(headerValue.get(0).split(DELIMITER)));
            } else {
                nodeState.putShared(sharedStateKeyName, headerValue);
            }
        });
        return goToNext().build();
    }

}
