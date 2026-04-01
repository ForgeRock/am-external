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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static com.sun.identity.sm.SchemaType.ORGANIZATION;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.DEFAULT_VALUE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.PROPERTIES;
import static org.forgerock.openam.sm.DefaultAnnotatedServiceRegistry.INSTANCE_SUB_SCHEMA_NAME;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.NodeUpgrader;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.enums.Stability;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderScriptGenerator;
import org.forgerock.openam.auth.trees.engine.AuthNodeFactory;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.OutcomeJsonConverter;
import org.forgerock.openam.core.rest.sms.SchemaGeneratorFactory;
import org.forgerock.openam.core.rest.sms.SmsResourceProviderContext;
import org.forgerock.openam.core.rest.sms.SmsSchemaGenerator;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * A collection provider for trees and their sub-configurations. Provides single-request CRUD operations for a tree and
 * its sub-configurations.
 */
@CollectionProvider(details = @Handler(mvccSupported = false))
public class ConfigProviderNodeCollectionProvider extends AuthTreeNodeCollectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConfigProviderNodeCollectionProvider.class);

    private static final String NODE_TYPE = "nodeType";
    private static final String NODE_VERSION = "nodeVersion";
    private static final String REQUIRED = "required";
    private static final String STATIC_CONFIG_NAME = "staticConfig";

    private final SchemaGeneratorFactory schemaGeneratorFactory;
    private final CoreWrapper coreWrapper;
    private final Set<String> configProviderAttributeNames;

    @Inject
    ConfigProviderNodeCollectionProvider(
            SmsResourceProviderContext dependencies,
            @Assisted("schema") ServiceSchema schema,
            @Assisted("rootSchema") ServiceSchema rootServiceSchema,
            @Assisted String uriPath,
            OutcomeJsonConverter outcomeJsonConverter,
            AuthNodeFactory nodeFactory,
            SchemaGeneratorFactory schemaGeneratorFactory,
            ConfigProviderScriptGenerator scriptGenerator,
            NodeRegistry nodeRegistry, @Assisted Stability stability,
            NodeUpgrader nodeUpgrader, CoreWrapper coreWrapper) {
        super(dependencies, schema, ORGANIZATION, rootServiceSchema, List.of(schema), uriPath, true, nodeFactory,
                outcomeJsonConverter, scriptGenerator, stability, nodeUpgrader, nodeRegistry);
        this.schemaGeneratorFactory = schemaGeneratorFactory;
        this.coreWrapper = coreWrapper;
        var configProviderAttributeNames = new HashSet<>(schema.getAttributeSchemaNames());
        configProviderAttributeNames.add("_id");
        this.configProviderAttributeNames = Collections.unmodifiableSet(configProviderAttributeNames);
    }

    @Override
    public Promise<ActionResponse, ResourceException> schema(Context context) {
        throw new NotImplementedException("This overload must never be called");
    }

    /**
     * Returns the schema for the configuration of a node.
     * @param context The context of the request.
     * @param actionRequest The request containing the node type.
     * @return A promise containing the schema for the configuration of the node.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> schema(Context context, ActionRequest actionRequest) {
        var content = actionRequest.getContent();
        if (content.isNull() || !content.isDefined(NODE_TYPE) || isBlank(content.get(NODE_TYPE).asString())) {
            return super.schema(context).then(response -> {
                response.getJsonContent().get(PROPERTIES).remove(NODE_VERSION);
                response.getJsonContent().get(REQUIRED).asList().remove(NODE_VERSION);
                return response;
            });
        } else {
            JsonValue schema = createSchema(Optional.of(context));
            String nodeType = content.get(NODE_TYPE).asString();
            var versions = nodeRegistry.getVersions(nodeType);
            var versionNames = versions.stream().map("%d.0"::formatted).toList();
            var nodeVersionSchema = schema.get(PROPERTIES).get(NODE_VERSION);
            nodeVersionSchema.put("enum", versions);
            nodeVersionSchema.put("options", Map.of("enum_titles", versionNames));
            nodeVersionSchema.put("enumNames", versionNames);
            var staticConfig = getStaticConfig(nodeType, getNodeVersion(content));
            if (!staticConfig.isEmpty()) {
                try {
                    JsonValue innerSchema = getInnerSchema(context, nodeType);
                    int order = 10_000;
                    for (String attr : staticConfig) {
                        JsonValue property = innerSchema.get("properties").get(attr);
                        if (property.isNull()) {
                            continue;
                        }
                        property.put("propertyOrder", order++);
                        schema.get("properties").put(attr, property.getObject());
                        if (innerSchema.get("required").asList().contains(attr)) {
                            schema.get("required").add(attr);
                        }
                    }
                } catch (SMSException | SSOException e) {
                    logger.error("Error fetching dynamic schema", e);
                    return new InternalServerErrorException("Error fetching dynamic schema").asPromise();
                }
            }
            try {
                createTemplate(content).forEach(
                        defaultProperty -> Optional.ofNullable(schema.get(PROPERTIES)
                                                                       .get(defaultProperty.getPointer()))
                                                   .ifPresent(schemaProperty -> schemaProperty.put(DEFAULT_VALUE,
                                                           defaultProperty.getObject())));
            } catch (InternalServerErrorException e) {
                return e.asPromise();
            }

            return newActionResponse(schema).asPromise();
        }
    }

    private JsonValue getInnerSchema(Context context, String nodeType) throws SSOException, SMSException {
        ServiceSchemaManager ssm = coreWrapper.getServiceSchemaManager(nodeType, coreWrapper.getAdminToken());
        ServiceSchema providedSchema = ssm.getOrganizationSchema().getSubSchema(INSTANCE_SUB_SCHEMA_NAME);


        SmsSchemaGenerator nodeSchema = schemaGeneratorFactory.create(providedSchema, ORGANIZATION);
        var innerSchemaJson = nodeSchema.createSchema(Optional.of(context));
        extractRequiredProperties(innerSchemaJson);
        return innerSchemaJson;
    }

    @Override
    public Promise<ActionResponse, ResourceException> template() {
        throw new NotImplementedException("This overload must never be called");
    }

    /**
     * Creates a template for the configuration of a node.
     * @param actionRequest The request containing the node type.
     * @return A promise containing the template for the configuration of the node.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> template(ActionRequest actionRequest) {
        try {
            return newActionResponse(createTemplate(actionRequest.getContent())).asPromise();
        } catch (InternalServerErrorException e) {
            return e.asPromise();
        }
    }

    private JsonValue createTemplate(JsonValue content) throws InternalServerErrorException {
        JsonValue template = createTemplate();
        if (content.isNull() || !content.isDefined(NODE_TYPE) || isBlank(content.get(NODE_TYPE).asString())) {
            template.remove(NODE_VERSION);
            return template;
        }
        var nodeType = content.get(NODE_TYPE).asString();
        var versions = nodeRegistry.getVersions(nodeType);
        var lastVersion = versions.get(versions.size() - 1);
        template.put(NODE_VERSION, lastVersion);
        var staticConfig = getStaticConfig(nodeType, getNodeVersion(content));
        if (!staticConfig.isEmpty()) {
            try {
                JsonValue innerTemplate = getInnerTemplate(nodeType);
                staticConfig.forEach(attr -> template.putIfNotNull(attr, innerTemplate.get(attr).getObject()));
            } catch (SMSException | SSOException e) {
                logger.error("Error fetching dynamic template", e);
                throw new InternalServerErrorException("Error fetching dynamic template");
            }
        }
        return template;
    }

    @Override
    protected ConfigurationAttributes getAttributesFromContent(
            String realm, JsonValue content, ConfigurationAttributes config, SmsJsonConverter converter)
            throws BadRequestException, InternalServerErrorException {
        JsonValue staticConfigJson = json(object());
        var nodeType = content.get(NODE_TYPE).asString();
        if (nodeType != null) {
            var staticConfig = getStaticConfig(nodeType, getNodeVersion(content));
            if (!staticConfig.isEmpty()) {
                try {
                    JsonValue innerTemplate = getInnerTemplate(nodeType);
                    staticConfig.forEach(value -> {
                        if (content.isDefined(value)) {
                            staticConfigJson.put(value, content.get(value));
                            content.remove(value);
                        } else {
                            // if not provided save the default
                            staticConfigJson.put(value, innerTemplate.get(value).getObject());
                        }
                    });
                } catch (SMSException | SSOException e) {
                    logger.error("Error reading attributes", e);
                    throw new InternalServerErrorException("Error reading attributes");
                }
            }
        }
        content.put(STATIC_CONFIG_NAME, staticConfigJson);
        Set<String> toRemove = new HashSet<>();
        for (String key : content.keys()) {
            if (!configProviderAttributeNames.contains(key)) {
                toRemove.add(key);
            }
        }
        content.keys().removeAll(toRemove);
        return super.getAttributesFromContent(realm, content, config, converter);
    }

    private Integer getNodeVersion(JsonValue content) {
        if (content.isDefined(NODE_VERSION) && content.get(NODE_VERSION).isNotNull()) {
            return content.get(NODE_VERSION).asInteger();
        }
        return 1;
    }

    @Override
    protected JsonValue getJsonValue(String resourceId, String realm, ServiceConfig config, Context context)
            throws InternalServerErrorException {
        JsonValue value = super.getJsonValue(resourceId, realm, config, context);
        JsonValue staticConfig = value.get(STATIC_CONFIG_NAME);

        if (staticConfig.isNotNull()) {
            staticConfig.asMap().forEach(value::put);
            value.remove(STATIC_CONFIG_NAME);
        }
        return value;
    }

    private JsonValue getInnerTemplate(String nodeType) throws SMSException, SSOException {
        ServiceSchemaManager ssm = coreWrapper.getServiceSchemaManager(nodeType, coreWrapper.getAdminToken());
        ServiceSchema providedSchema = ssm.getOrganizationSchema().getSubSchema(INSTANCE_SUB_SCHEMA_NAME);
        ConfigurationAttributes defaults = defaultConfigService.getDefaultsForService(providedSchema);
        return converterFactory.create(providedSchema, false).toJson(defaults, false);
    }

    @Override
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> listOutcomes(Context context, ActionRequest request) {
        var content =  request.getContent();
        var nodeType = content.get(NODE_TYPE).asString();
        if (nodeType != null) {
            JsonValue staticConfig = json(object());
            getStaticConfig(nodeType, getNodeVersion(content)).forEach(value -> {
                if (content.isDefined(value)) {
                    staticConfig.put(value, content.get(value));
                    content.remove(value);
                }
            });
            if (!staticConfig.keys().isEmpty()) {
                content.put(STATIC_CONFIG_NAME, staticConfig);
            }
        }

        return super.listOutcomes(context, request);
    }
}
