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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.validation.validators;

import static org.forgerock.am.trees.api.TreeBuilder.NODES;
import static org.forgerock.am.trees.api.TreeBuilder.NODE_TYPE;
import static org.forgerock.am.trees.api.TreeBuilder.VERSION;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceAttributeValidator;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Validator to validate node configuration.
 */
public class NodeConfigValidator extends AttributeSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(NodeConfigValidator.class);
    private static final String PAGE_NODE_TYPE = "PageNode";
    private static final String DEFAULT_NODE_VERSION = "1.0";
    private static final String NODE_SUBSCHEMA_PATH = "instance";
    private static final String CONFIG = "config";

    private final CoreWrapper coreWrapper;
    private final SmsJsonConverterFactory jsonConverterFactory;

    @Inject
    NodeConfigValidator(CoreWrapper coreWrapper, SmsJsonConverterFactory jsonConverterFactory) {
        this.coreWrapper = coreWrapper;
        this.jsonConverterFactory = jsonConverterFactory;
    }

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        List<ValidationFailure> validationFailures = new ArrayList<>();
        JsonValue nodes = requestBody.get(NODES);
        if (nodes.stream().anyMatch(node -> node.isDefined(CONFIG))) {
            for (JsonValue node : nodes) {
                validationFailures.addAll(validateNode(node, realm));
            }
        }
        return validationFailures;
    }

    private List<ValidationFailure> validateNode(JsonValue node, Realm realm) {
        JsonValue nodeConfig = node.get(CONFIG).defaultTo(json(object()));
        String nodeType = node.get(NODE_TYPE).asString();
        String nodeVersion = getNodeVersion(node);
        String nodeId = node.isDefined("_id") ? node.get("_id").asString() : node.getPointer().leaf();
        if (nodeType.equals(PAGE_NODE_TYPE)) {
            JsonValue innerNodes = node.get(CONFIG).get(NODES);
            List<ValidationFailure> validationFailures = new ArrayList<>();
            for (JsonValue innerNode : innerNodes) {
                if (innerNode.isDefined(CONFIG)) {
                    validationFailures.addAll(validateNode(innerNode, realm));
                }
            }
            return validationFailures;
        } else {
            return validateNode(nodeId, nodeType, nodeVersion, nodeConfig, realm);
        }
    }

    private List<ValidationFailure> validateNode(String nodeId, String nodeType, String nodeVersion,
                                                 JsonValue nodeConfig, Realm realm) {
        List<ValidationFailure> failures = new ArrayList<>();
        try {
            ServiceSchemaManager serviceSchemaManager = coreWrapper.getServiceSchemaManager(nodeType, nodeVersion,
                    coreWrapper.getAdminToken());
            ServiceSchema serviceSchema = serviceSchemaManager.getOrganizationSchema()
                                                  .getSubSchema(NODE_SUBSCHEMA_PATH);
            SmsJsonConverter jsonConverter = jsonConverterFactory.create(serviceSchema, true);
            ConfigurationAttributes config = jsonConverter.fromJson(nodeConfig);
            Map<String, Set<ServiceAttributeValidator>> validators = serviceSchema.getAttributesToValidatorsMap();
            ConfigurationAttributes defaults = serviceSchema.getAttributeDefaults();

            failures.addAll(validateExistingAttributes(config, validators));
            failures.addAll(checkForMissingAttributes(config, validators, defaults));

            var serviceConfigValidator = serviceSchema.getServiceConfigValidator();
            if (serviceConfigValidator != null) {
                try {
                    serviceConfigValidator.validate(realm, List.of(NODE_SUBSCHEMA_PATH),
                            config);
                } catch (ServiceConfigException | ServiceErrorException e) {
                    failures.add(new ValidationFailure(e.getMessage(), ERROR));
                }
            }

        } catch (SSOException | SMSException e) {
            failures.add(new ValidationFailure("'nodeType' '" + nodeType + "' with 'version' '" + nodeVersion
                                                       + "' is not recognized.", ERROR));
        } catch (BadRequestException e) {
            failures.add(new ValidationFailure(e.getMessage(), ERROR));
        } catch (InternalServerErrorException e) {
            logger.error("An error occurred while validating node {} configuration for 'nodeType' '{}'"
                    + " with 'version' '{}'", nodeId, nodeType, nodeVersion, e);
            failures.add(new ValidationFailure("An error occurred during validation.", ERROR));
        }
        return failures.stream()
                       .map(error ->
                                    new ValidationFailure("Node '%s' configuration is invalid. %s"
                                                                  .formatted(nodeId, error.message()),
                                            error.severity())).toList();
    }

    private String getNodeVersion(JsonValue node) {
        JsonValue version = node.get(VERSION);
        if (version.isNumber()) {
            return version.asInteger() + ".0";
        } else if (version.isString()) {
            return version.asString();
        } else {
            return DEFAULT_NODE_VERSION;
        }
    }

    @Override
    public boolean isBlocking() {
        return true;
    }
}
