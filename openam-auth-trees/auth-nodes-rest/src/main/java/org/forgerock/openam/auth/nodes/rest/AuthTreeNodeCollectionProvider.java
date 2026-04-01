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
 * Copyright 2017-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static com.sun.identity.sm.SMSUtils.VERSION;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.forgerock.api.enums.ParameterSource.ADDITIONAL;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.Query.query;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.GET_UPGRADED_CONFIG;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.LIST_OUTCOMES;
import static org.forgerock.openam.auth.nodes.rest.NodeRestActions.VERSION_INFO;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.DESCRIPTION;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.OBJECT_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.REQUIRED;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TITLE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TYPE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.AUTH_TREE_NODES_RESOURCE;
import static org.forgerock.openam.rest.DescriptorUtils.fromResource;
import static org.forgerock.openam.rest.RestConstants.GET_TYPE;
import static org.forgerock.openam.sm.file.ConfigEntity.DATA_ATTRIBUTE_TYPE;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.NodeUpgrader;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.auth.node.api.ConfigProviderEnabled;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.PageNodeCompatibility;
import org.forgerock.openam.auth.node.api.upgrade.exception.NodeUpgradeException;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderScriptGenerator;
import org.forgerock.openam.auth.trees.engine.AuthNodeFactory;
import org.forgerock.openam.core.rest.sms.OutcomeJsonConverter;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SmsResourceProviderContext;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;

/**
 * An SMS collection provider for Auth Tree Nodes which adds the {@link #listOutcomes} action.
 */
@CollectionProvider(details = @Handler(mvccSupported = false))
public class AuthTreeNodeCollectionProvider extends SmsCollectionProvider {
    /** The outcomes field in the JSON representation of a node. */
    public static final String OUTCOMES = "_outcomes";

    private static final String METADATA_PROPERTY = "metadata";
    private static final String TAGS_PROPERTY = "tags";
    private static final String HELP_PROPERTY = "help";
    private static final String PAGE_NODE_COMPATIBILITY_PROPERTY = "pageNodeCompatibility";
    private final Logger debug = LoggerFactory.getLogger(AuthTreeNodeCollectionProvider.class);
    private final AuthNodeFactory nodeFactory;
    private final OutcomeJsonConverter outcomeConverter;
    private final ConfigProviderScriptGenerator scriptGenerator;
    private final NodeUpgrader nodeUpgrader;

    /**
     * The registry of nodes, used to retrieve node types and their metadata.
     */
    protected final NodeRegistry nodeRegistry;

    @Inject
    AuthTreeNodeCollectionProvider(
            SmsResourceProviderContext dependencies,
            @Assisted("schema") ServiceSchema schema,
            @Assisted SchemaType type, @Assisted("rootSchema") ServiceSchema rootServiceSchema,
            @Assisted List<ServiceSchema> subSchemaPath, @Assisted String uriPath,
            @Assisted boolean serviceHasInstanceName,
            AuthNodeFactory nodeFactory, OutcomeJsonConverter outcomeConverter,
            ConfigProviderScriptGenerator scriptGenerator,
            @Assisted Stability stability,
            NodeUpgrader nodeUpgrader, NodeRegistry nodeRegistry) {

        super(dependencies, schema, type, rootServiceSchema, subSchemaPath, uriPath, serviceHasInstanceName, stability);
        this.nodeFactory = nodeFactory;
        this.outcomeConverter = outcomeConverter;
        this.scriptGenerator = scriptGenerator;
        this.nodeUpgrader = nodeUpgrader;
        this.nodeRegistry = nodeRegistry;
    }

    @Override
    protected Resource.Builder getResourceDescriptor(ServiceSchema schema) {
        Items.Builder items = getItemsDescriptor();
        return Resource.resource()
                .title(getI18NName())
                .description(getSchemaDescription(schema.getI18NKey()))
                .parameters(getParameters())
                .mvccSupported(false)
                .items(items.build())
                .resourceSchema(Schema.schema().schema(createSchema(Optional.empty())).build())
                .query(query().stability(getStability()).type(QueryType.FILTER)
                        .description(QUERY_DESCRIPTION).queryableFields().build())
                .action(action().name("schema").stability(getStability()).description(SCHEMA_DESCRIPTION).build())
                .action(action().name("template").stability(getStability()).description(TEMPLATE_DESCRIPTION).build())
                .action(action().name(LIST_OUTCOMES).stability(getStability())
                        .description(localString("listOutcomes.description"))
                        .request(Schema.schema().schema(json(object(
                                field(TYPE, OBJECT_TYPE),
                                field(TITLE, localString("listOutcomes.request.title")),
                                field(DESCRIPTION, localString("listOutcomes.request.description"))
                        ))).build())
                        .response(fromResource("AuthTreeNodeCollectionProvider.listOutcomes.schema.json", getClass()))
                        .build())
                .action(action().stability(getStability()).name(VERSION_INFO)
                        .description(localString("versionInfo.description"))
                        .response(fromResource("AuthTreeNodeCollectionProvider.versionInfo.versioned.schema.json",
                                getClass()))
                        .build())
                .action(action().stability(getStability()).name(GET_TYPE)
                        .description(localString("getType.description"))
                        .response(fromResource("AuthTreeNodeCollectionProvider.getType.versioned.schema.json",
                                getClass()))
                        .build())
                .action(action().stability(getStability()).name(GET_UPGRADED_CONFIG)
                                .description(localString("getUpgradedConfig.description"))
                                .parameter(Parameter.parameter().type("string")
                                                   .source(ADDITIONAL)
                                                   .required(true).name("targetVersion")
                                                   .description(localString(
                                                           "getUpgradedConfig.targetVersion.description"))
                                                   .build())
                                .request(Schema.schema().schema(json(object(
                                                field(TYPE, OBJECT_TYPE),
                                                field(TITLE, localString("getUpgradedConfig.request.title")),
                                                field(TITLE, localString("getUpgradedConfig.request.description"))
                                        ))).build())
                                .response(Schema.schema().schema(json(object(
                                        field(TYPE, OBJECT_TYPE),
                                        field(TITLE, localString("getUpgradedConfig.response.title")),
                                        field(TITLE, localString("getUpgradedConfig.response.description"))
                                ))).build())
                                .build());
    }

    /**
     * Lists the outcomes for a node.
     * @param context The request context.
     * @param request The action request.
     * @return A promise containing the action response with the outcomes or an error.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> listOutcomes(Context context, ActionRequest request) {
        try {
            return newActionResponse(json(getOutcomes(context, request.getPreferredLocales(), request.getContent())))
                    .asPromise();
        } catch (InternalServerErrorException e) {
            return e.asPromise();
        }
    }

    /**
     * Returns the type of the node, including its tags and metadata.
     * @param context The request context.
     * @return A promise containing the action response with the node type information.
     */
    @Override
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> getType(Context context) {
        return super.getType(context).thenOnResult(result -> {
            List<String> tags = nodeFactory.getTags(this.serviceName, nodeVersion());
            result.getJsonContent().put(TAGS_PROPERTY, tags);
            Map<String, Object> metadata = nodeFactory.getMetadataExtensions(this.serviceName, nodeVersion());
            metadata.put(TAGS_PROPERTY, tags);

            PageNodeCompatibility pageNodeCompatibility =
                    nodeFactory.getPageNodeCompatibility(this.serviceName, nodeVersion());
            if (pageNodeCompatibility != PageNodeCompatibility.UNKNOWN) {
                metadata.put(PAGE_NODE_COMPATIBILITY_PROPERTY, pageNodeCompatibility.name().toLowerCase());
            }
            result.getJsonContent().put(METADATA_PROPERTY, metadata);

            try {
                result.getJsonContent().put(HELP_PROPERTY,
                        resourceBundleCache.getResBundle(schema.getI18NFileName(),
                                context.asContext(LocaleContext.class).getLocale())
                            .getString("nodeHelp"));
            } catch (MissingResourceException e) {
                debug.trace("Unable to find node help message for node {}", serviceName);
            }
        });
    }

    /**
     * If node versioning is enabled, this action retrieves version information about a node, e.g.:<br>
     * <code>Version: 1.0<br>.
     * Changelog: Initial version of node</code>
     * @param context {@link Context}
     * @return Promise containing the version information and a status code.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> versionInfo(Context context) {
        return newActionResponse(json(object(
                field("version", getServiceVersion()),
                field("changelog", getChangelog(context))
        ))).asPromise();

    }

    /**
     * Generates a script for the configuration provider.
     * @param context The request context.
     * @param actionRequest The action request containing the language for the script.
     * @return A promise containing the action response with the generated script or an error.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> configProviderScript(
            Context context, ActionRequest actionRequest) {
        ScriptingLanguage language;
        try {
            language = Optional.ofNullable(actionRequest.getContent().get("language").asString())
                    .map(String::toUpperCase)
                    .map(ScriptingLanguage::valueOf)
                    .orElse(ScriptingLanguage.JAVASCRIPT);
        } catch (IllegalArgumentException e) {
            return newResourceException(BAD_REQUEST, "Invalid language").asPromise();
        }
        try {
            var template = super.createTemplate();
            var staticConfig = getStaticConfig(serviceName, nodeVersion());
            Map<String, Object> properties = super.createSchema(Optional.of(context))
                    .get("properties")
                    .defaultTo(Map.of())
                    .asMap()
                    .entrySet().stream()
                    .sorted(comparingInt(entry -> json(entry.getValue()).get("propertyOrder").asInteger()))
                    .map(Map.Entry::getKey)
                    .filter(key -> !staticConfig.contains(key))
                    .collect(LinkedHashMap::new,
                            (map, key) -> map.put(key, template.get(key).getObject()), Map::putAll);

            String script = scriptGenerator.generateScript(properties, language);
            JsonValue responseContent = json(object(
                    field("script", Base64.encode(script.getBytes(StandardCharsets.UTF_8)))
            ));
            return newActionResponse(responseContent).asPromise();
        } catch (JsonProcessingException | ScriptException e) {
            debug.error("Unable to generate default script for node", e);
            return newResourceException(INTERNAL_ERROR, "Failed to generate script").asPromise();
        }
    }

    /**
     * Upgrades the node configuration to a target version.
     * @param context The request context.
     * @param actionRequest The action request containing the target version and content to upgrade.
     * @return A promise containing the action response with the upgraded configuration or an error.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> getUpgradedConfig(Context context, ActionRequest actionRequest) {
        String targetVersion = actionRequest.getAdditionalParameter("targetVersion");
        if (isBlank(targetVersion)) {
            return new BadRequestException("targetVersion must be supplied").asPromise();
        }
        int target;
        try {
            target = (int) Float.parseFloat(targetVersion);
        } catch (NumberFormatException e) {
            return new BadRequestException("targetVersion must be a valid version").asPromise();
        }
        if (target < nodeVersion()) {
            return new BadRequestException("targetVersion must not be less than current version").asPromise();
        }
        try {
            var response = nodeUpgrader.upgrade(getServiceName(), nodeVersion(), target,
                    actionRequest.getContent());
            return newActionResponse(response).asPromise();
        } catch (NodeUpgradeException e) {
            return switch (e.reason()) {
            case UNKNOWN_VERSION -> new BadRequestException("targetVersion not upgradable").asPromise();
            case UPGRADE_FAILED -> {
                debug.warn("Failed to upgrade node config for {}", getServiceName(), e);
                yield new InternalServerErrorException("Failed to upgrade node config").asPromise();
            }
            case NO_UPGRADE_METADATA -> {
                debug.warn("No upgrade metadata found for node {}", getServiceName(), e);
                yield new InternalServerErrorException("No upgrade metadata found").asPromise();
            }
            case INVALID_CONFIGURATION -> new BadRequestException("Invalid configuration").asPromise();
            case UNSUPPORTED_UPGRADE -> new BadRequestException("Upgrade not supported from this node version")
                                                .asPromise();
            };
        }
    }

    /**
     * Get the static configuration for a node type.
     * @param nodeType The node type.
     * @param nodeVersion The node version.
     * @return A list of static configuration names.
     */
    protected List<String> getStaticConfig(String nodeType, Integer nodeVersion) {
        var config = nodeRegistry.getConfigType(nodeType, nodeVersion);
        return Arrays.stream(config.getMethods())
                       .filter(method -> method.isAnnotationPresent(ConfigProviderEnabled.StaticConfig.class)
                                                 && method.isAnnotationPresent(
                               org.forgerock.openam.annotations.sm.Attribute.class))
                       .sorted(Comparator.comparingInt(method -> method.getAnnotation(
                               org.forgerock.openam.annotations.sm.Attribute.class).order()))
                       .map(method -> {
                           var resourceName = method.getAnnotation(
                                   org.forgerock.openam.annotations.sm.Attribute.class).resourceName();
                           return StringUtils.isNotBlank(resourceName) ? resourceName : method.getName();
                       })
                       .toList();
    }

    @Override
    protected JsonValue createSchema(Optional<Context> context) {
        JsonValue result = super.createSchema(context);
        extractRequiredProperties(result);
        return result;
    }

    /**
     * Extracts the required properties from the schema and adds them to the result.
     * The required properties are determined by checking if the "required" property is set to true for each property.
     * The "required" property is then removed from the schema.
     *
     * @param result The JSON value representing the schema.
     */
    protected static void extractRequiredProperties(JsonValue result) {
        Set<String> allProperties = result.get("properties").keys();
        List<String> requiredProperties = allProperties.stream().filter(property -> {
            JsonValue required = result.get(new JsonPointer(String.format("properties/%s/required", property)));
            return required != null && required.asBoolean();
        }).collect(toList());

        result.add(REQUIRED, requiredProperties);
        allProperties.forEach(property ->
                result.remove(new JsonPointer(String.format("properties/%s/required", property))));
    }

    @Override
    protected JsonValue getJsonValue(String resourceId, String realm, ServiceConfig config, Context context)
            throws InternalServerErrorException {
        final JsonValue result = super.getJsonValue(resourceId, realm, config, context);
        result.get(DATA_ATTRIBUTE_TYPE).put(VERSION, getServiceVersion());
        result.put(OUTCOMES, getOutcomes(context,
                new PreferredLocales(singletonList(context.asContext(LocaleContext.class).getLocale())), result));
        return result;
    }

    @Override
    protected ConfigurationAttributes getAttributesFromContent(
            String realm, JsonValue content, ConfigurationAttributes config, SmsJsonConverter converter)
            throws BadRequestException, InternalServerErrorException {
        final JsonValue modifiedContent = content.clone();
        modifiedContent.remove(OUTCOMES);
        return super.getAttributesFromContent(realm, modifiedContent, config, converter);
    }

    /**
     * Get the service version of the node.
     * @return the service version of the node.
     */
    protected int nodeVersion() {
        return (int) Float.parseFloat(schema.getVersion());
    }

    private LocalizableString localString(String key) {
        return new LocalizableString(AUTH_TREE_NODES_RESOURCE + key, getClass());
    }

    private List<Object> getOutcomes(Context context, PreferredLocales preferredLocales, JsonValue nodeConfig)
            throws InternalServerErrorException {
        try {
            OutcomeProvider provider = nodeFactory.getOutcomeProvider(context.asContext(RealmContext.class).getRealm(),
                    schema.getServiceName(), nodeVersion());
            return provider.getOutcomes(preferredLocales, nodeConfig)
                    .stream()
                    .map(outcomeConverter::toJson)
                    .collect(toList());
        } catch (NodeProcessException | RuntimeException e) {
            debug.error("The node outcomes cannot be fetched", e);
            throw new InternalServerErrorException(e);
        }
    }

    /**
     * Gets the changelog from the properties file relating to a version of a node. E.g. CustomAuthNodeV2.properties
     * If there is no changelog entry in the properties file, a MissingResourceException is caught, and the changelog
     * will be described as 'No changelog entry found'.
     * @param context {@link Context}
     * @return The changelog as a String
     */
    private String getChangelog(Context context) {
        try {
            return resourceBundleCache
                    .getResBundle(schema.getI18NFileName(), context.asContext(LocaleContext.class).getLocale())
                    .getString("changelog." + getServiceVersion());
        } catch (MissingResourceException e) {
            return "No changelog entry found";
        }
    }
}
