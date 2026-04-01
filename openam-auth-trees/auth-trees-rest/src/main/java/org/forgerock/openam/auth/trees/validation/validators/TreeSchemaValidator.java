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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.sm.ConfigurationAttributes;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceAttributeValidator;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Validator to validate an authentication tree against its SMS schema.
 */
public class TreeSchemaValidator extends AttributeSchemaValidator  {

    private static final String AUTH_TREES_SERVICE_NAME = "authenticationTreesService";
    private static final String CONFIG = "config";
    private final SmsJsonConverter treeConverter;
    private final ServiceSchema treeSchema;

    @Inject
    TreeSchemaValidator(SmsJsonConverterFactory treeConverterFactory, CoreWrapper coreWrapper)
            throws SMSException, SSOException {
        ServiceSchemaManager serviceSchemaManager = coreWrapper.getServiceSchemaManager(AUTH_TREES_SERVICE_NAME,
                coreWrapper.getAdminToken());
        this.treeSchema = serviceSchemaManager.getOrganizationSchema().getSubSchema("tree");
        // isConvertingForConfigurationFiles is true so that we do not run the validators during conversion
        this.treeConverter = treeConverterFactory.create(treeSchema, true);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
        // This validator manipulates the request body, so we work on a copy
        JsonValue tree = requestBody.copy();

        JsonValue nodes = tree.get("nodes");

        // Check if nodes have config provided and remove if so
        if (nodes.stream().anyMatch(node -> node.isDefined(CONFIG))) {
            nodes.forEach(node -> node.remove(CONFIG));
        }

        try {
            ConfigurationAttributes treeAttributes = treeConverter.fromJson(tree);
            return validateAttributes(treeAttributes);
        } catch (JsonException | BadRequestException | InternalServerErrorException e) {
            return List.of(new ValidationFailure(e.getMessage(), ValidationFailure.Severity.ERROR));
        }
    }

    private List<ValidationFailure> validateAttributes(Map<String, Set<String>> attributes)
            throws BadRequestException {
        List<ValidationFailure> failures = new ArrayList<>();
        Map<String, Set<ServiceAttributeValidator>> validators = treeSchema.getAttributesToValidatorsMap();
        failures.addAll(checkForMissingAttributes(attributes, validators));
        failures.addAll(validateExistingAttributes(attributes, validators));
        return failures;
    }

}
