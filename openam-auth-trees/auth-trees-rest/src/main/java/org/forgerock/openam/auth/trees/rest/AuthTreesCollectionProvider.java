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
package org.forgerock.openam.auth.trees.rest;

import static com.sun.identity.authentication.util.ISAuthConstants.EMPTY_STRING;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.forgerock.am.trees.api.NodeRegistry.DEFAULT_VERSION;
import static org.forgerock.am.trees.api.TreeBuilder.VERSION;
import static org.forgerock.api.enums.CreateMode.ID_FROM_CLIENT;
import static org.forgerock.api.enums.ParameterSource.PATH;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.Items.items;
import static org.forgerock.api.models.Parameter.parameter;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newDeleteRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Requests.newUpdateRequest;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.auth.node.api.TreeContext.IDM_IDENTITY_RESOURCE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.ARRAY_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.BOOLEAN_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.DESCRIPTION;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.ITEMS;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.NUMBER_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.OBJECT_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.PATTERN_PROPERTIES;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.PROPERTIES;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.READONLY;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.STRING_TYPE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TITLE;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.TYPE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SMS_AUTH_TREES_PROVIDER;
import static org.forgerock.openam.rest.RestConstants.FOR_UI;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.util.LambdaExceptionUtils.rethrowFunction;
import static org.forgerock.util.promise.Promises.newPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.TreeBuilder;
import org.forgerock.am.trees.api.TreeUsage;
import org.forgerock.am.trees.api.TreeUsageException;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider.Outcome;
import org.forgerock.openam.auth.nodes.framework.validators.NodeValueValidator;
import org.forgerock.openam.auth.nodes.framework.validators.StaticNodeValueValidator;
import org.forgerock.openam.auth.trees.engine.AuthNodeFactory;
import org.forgerock.openam.auth.trees.engine.AuthenticationTreesServiceConfig;
import org.forgerock.openam.auth.trees.engine.NodeConfig;
import org.forgerock.openam.auth.trees.validation.AuthTreeValidationEngine;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;
import org.forgerock.openam.core.CoreServicesWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.sms.OutcomeJsonConverter;
import org.forgerock.openam.core.rest.sms.SchemaGeneratorFactory;
import org.forgerock.openam.core.rest.sms.SmsResourceProvider;
import org.forgerock.openam.core.sms.DefaultConfigService;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.core.sms.SmsJsonConverterFactory;
import org.forgerock.openam.rest.DescriptorUtils;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.services.datastore.DataStoreLookup;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.sm.ServiceSchemaManagerFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.Pair;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.config.AMAuthenticationManagerFactory;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;

/**
 * A collection provider for trees and their sub-configurations. Provides single-request CRUD operations for a tree and
 * its sub-configurations.
 */
@CollectionProvider(
        pathParam = @Parameter(name = "treeName", type = "string"),
        details = @Handler(mvccSupported = false))
public class AuthTreesCollectionProvider extends SmsResourceProvider {

    private static final String NODES_KEY = "nodes";
    private static final String DESCRIPTION_KEY = "description";
    private static final String ENABLED_KEY = "enabled";
    private static final String INNER_KEY = "innerTreeOnly";
    private static final String MUST_RUN_KEY = "mustRun";
    private static final String MAXIMUM_SESSION_TIME_KEY = "maximumSessionTime";
    private static final String MAXIMUM_IDLE_TIME_KEY = "maximumIdleTime";
    private static final String TREE_TIMEOUT_KEY = "treeTimeout";
    private static final String GREATER_THAN_ZERO_VALIDATOR_KEY = "GreaterThanZeroValidator";
    private static final String NO_SESSION_KEY = "noSession";
    private static final String TRANSACTIONAL_ONLY = "transactionalOnly";
    private static final String STATIC_NODES_KEY = "staticNodes";
    private static final String CONNECTIONS_KEY = "connections";
    private static final String OUTCOMES_KEY = "_outcomes";
    private static final String OUTCOME_ID_KEY = "id";
    private static final String OUTCOME_DISPLAY_NAME_KEY = "displayName";
    private static final String NODE_TYPE_KEY = "nodeType";
    private static final String VALIDATE_SCHEMA = "AuthTreesCollectionProvider.validate.schema.json";
    private static final String VALIDATE_TREE_SCHEMA = "AuthTreesCollectionProvider.validateTree.schema.json";
    private static final String GET_IDS_SCHEMA = "AuthTreesCollectionProvider.getIds.schema.json";
    private static final String ENTRY_NODE_ID = "entryNodeId";
    private static final String UI_CONFIG_KEY = "uiConfig";
    private static final String CONFIG = "config";
    private static final ResourcePath NODES_ENDPOINT = resourcePath(
            "realm-config/authentication/authenticationtrees/nodes");
    private static final Version RESOURCE_V3 = Version.version(3);
    static final String PAGE_NODE_VERSION = "nodeVersion";

    private final SmsJsonConverter treeConverter;
    private final ServiceSchema treeSchema;
    private final AuthTreeValidationEngine validationEngine;
    private final Logger debug = LoggerFactory.getLogger(AuthTreesCollectionProvider.class);
    private final AuthNodeFactory nodeFactory;
    private final ServiceConfigManagerFactory serviceConfigManagerFactory;
    private final OutcomeJsonConverter outcomeConverter;
    private final JsonValue jsonSchema;
    private final ApiDescription description;
    private final ConnectionFactory connectionFactory;
    private final AnnotatedServiceRegistry annotatedServiceRegistry;
    private final Set<TreeUsage> treeUsages;
    private final CoreServicesWrapper coreServicesWrapper;

    @Inject
    AuthTreesCollectionProvider(@Assisted("tree") ServiceSchema treeSchema,
            SmsJsonConverterFactory converterFactory,
            AuthNodeFactory nodeFactory,
            ServiceConfigManagerFactory serviceConfigManagerFactory,
            OutcomeJsonConverter outcomeConverter, DataStoreLookup dataStoreLookup,
            ConnectionFactory connectionFactory, AMAuthenticationManagerFactory amAuthenticationManagerFactory,
            AnnotatedServiceRegistry annotatedServiceRegistry, SchemaGeneratorFactory schemaGeneratorFactory,
            ServiceSchemaManagerFactory serviceSchemaManagerFactory, Set<TreeUsage> treeUsages,
            DefaultConfigService defaultConfigService,
            CoreServicesWrapper coreServicesWrapper, AuthTreeValidationEngine validationEngine) {
        super(treeSchema, SchemaType.ORGANIZATION, emptyList(), null, true,
                converterFactory.create(treeSchema, false),
                treeSchema, dataStoreLookup, amAuthenticationManagerFactory, schemaGeneratorFactory,
                serviceConfigManagerFactory, serviceSchemaManagerFactory, defaultConfigService);

        this.nodeFactory = nodeFactory;
        this.treeSchema = treeSchema;
        this.validationEngine = validationEngine;
        this.treeConverter = converterFactory.create(treeSchema, false);
        this.serviceConfigManagerFactory = serviceConfigManagerFactory;
        this.outcomeConverter = outcomeConverter;
        this.connectionFactory = connectionFactory;
        this.annotatedServiceRegistry = annotatedServiceRegistry;

        this.treeUsages = treeUsages;
        this.coreServicesWrapper = coreServicesWrapper;

        Map.Entry<String, Object> xfield = field(TreeBuilder.X_POSITION_ID, object(
                field(TYPE, STRING_TYPE),
                field(TITLE, "tree.node.x"),
                field(DESCRIPTION, "tree.node.x.description")
        ));
        Map.Entry<String, Object> yfield = field(TreeBuilder.Y_POSITION_ID, object(
                field(TYPE, STRING_TYPE),
                field(TITLE, "tree.node.y"),
                field(DESCRIPTION, "tree.node.y.description")
        ));

        Map<String, Object> staticNodeProperties = object(xfield, yfield);
        Map<String, Object> nodeProperties = object(
                field(CONNECTIONS_KEY, object(
                        field(TYPE, OBJECT_TYPE),
                        field(TITLE, localString("tree.connections.title")),
                        field(DESCRIPTION, localString("tree.connections.description")),
                        field(PATTERN_PROPERTIES, object(
                                field(".*", object(
                                        field(TYPE, STRING_TYPE),
                                        field(TITLE, localString("tree.connection.title")),
                                        field(DESCRIPTION, localString("tree.connection.description"))
                                ))
                        ))
                )),
                xfield,
                yfield,
                field(OUTCOMES_KEY, object(
                        field(TYPE, ARRAY_TYPE),
                        field(TITLE, localString("tree.outcomes.title")),
                        field(DESCRIPTION, localString("tree.outcomes.description")),
                        field(READONLY, true),
                        field(ITEMS, object(
                                field(TYPE, OBJECT_TYPE),
                                field(TITLE, localString("tree.outcome.title")),
                                field(DESCRIPTION, localString("tree.outcome.description")),
                                field(READONLY, true),
                                field(PROPERTIES, object(
                                        field(OUTCOME_ID_KEY, object(
                                                field(TYPE, STRING_TYPE),
                                                field(TITLE, localString("tree.outcome.id.title")),
                                                field(DESCRIPTION, localString("tree.outcome.id.description")),
                                                field(READONLY, true)
                                        )),
                                        field(OUTCOME_DISPLAY_NAME_KEY, object(
                                                field(TYPE, STRING_TYPE),
                                                field(TITLE, localString("tree.outcome.displayName.title")),
                                                field(DESCRIPTION, localString("tree.outcome.displayName.description")),
                                                field(READONLY, true)
                                        ))
                                ))
                        ))
                ))
        );
        Map<String, Object> treeProperties = object(
                field(DESCRIPTION_KEY, object(
                        field(TYPE, STRING_TYPE),
                        field(TITLE, localString("tree.description.title")),
                        field(DESCRIPTION, localString("tree.description.description"))
                )),
                field(ENABLED_KEY, object(
                        field(TYPE, BOOLEAN_TYPE),
                        field(TITLE, localString("tree.enabled.title")),
                        field(DESCRIPTION, localString("tree.enabled.description"))
                )),
                field(INNER_KEY, object(
                        field(TYPE, BOOLEAN_TYPE),
                        field(TITLE, localString("tree.inner.title")),
                        field(DESCRIPTION, localString("tree.inner.description"))
                )),
                field(MUST_RUN_KEY, object(
                        field(TYPE, BOOLEAN_TYPE),
                        field(TITLE, localString("tree.mustRun.title")),
                        field(DESCRIPTION, localString("tree.mustRun.description"))
                )),
                field(MAXIMUM_SESSION_TIME_KEY, object(
                        field(TYPE, NUMBER_TYPE),
                        field(TITLE, localString("tree.maximumSessionTime.title")),
                        field(DESCRIPTION, localString("tree.maximumSessionTime.description"))
                )),
                field(MAXIMUM_IDLE_TIME_KEY, object(
                        field(TYPE, NUMBER_TYPE),
                        field(TITLE, localString("tree.maximumIdleTime.title")),
                        field(DESCRIPTION, localString("tree.maximumIdleTime.description"))
                )),
                field(TREE_TIMEOUT_KEY, object(
                        field(TYPE, NUMBER_TYPE),
                        field(TITLE, localString("tree.treeTimeout.title")),
                        field(DESCRIPTION, localString("tree.treeTimeout.description"))
                )),
                field(TRANSACTIONAL_ONLY, object(
                        field(TYPE, BOOLEAN_TYPE),
                        field(TITLE, localString("tree.transactionalOnly.title")),
                        field(DESCRIPTION, localString("tree.transactionalOnly.description"))
                )),
                field(NO_SESSION_KEY, object(
                        field(TYPE, BOOLEAN_TYPE),
                        field(TITLE, localString("tree.noSession.title")),
                        field(DESCRIPTION, localString("tree.noSession.description"))
                )),
                field(NODES_KEY, object(
                        field(TYPE, OBJECT_TYPE),
                        field(TITLE, localString("tree.nodes.title")),
                        field(DESCRIPTION, localString("tree.nodes.description")),
                        field(PATTERN_PROPERTIES, object(
                                field(".*", object(
                                        field(TYPE, OBJECT_TYPE),
                                        field(TITLE, localString("tree.node.title")),
                                        field(DESCRIPTION, localString("tree.node.description")),
                                        field(PROPERTIES, nodeProperties)
                                ))
                        ))
                )),
                field(STATIC_NODES_KEY, object(
                        field(TYPE, OBJECT_TYPE),
                        field(TITLE, localString("tree.staticNodes.title")),
                        field(DESCRIPTION, localString("tree.staticNodes.description")),
                        field(PATTERN_PROPERTIES, object(
                                field(".*", object(
                                        field(TYPE, OBJECT_TYPE),
                                        field(TITLE, localString("tree.node.title")),
                                        field(DESCRIPTION, localString("tree.node.description")),
                                        field(PROPERTIES, staticNodeProperties)
                                ))
                        ))
                )),
                field(UI_CONFIG_KEY, object(
                        field(TYPE, OBJECT_TYPE),
                        field(TITLE, localString("tree.uiconfig.title")),
                        field(DESCRIPTION, localString("tree.uiconfig.description")),
                        field(PATTERN_PROPERTIES, object(
                                field(".*", object(
                                        field(TYPE, STRING_TYPE)
                                ))
                        ))
                )),
                field(IDM_IDENTITY_RESOURCE, object(
                        field(TYPE, STRING_TYPE),
                        field(TITLE, localString("tree.identityResource.title")),
                        field(DESCRIPTION, localString("tree.identityResource.description"))
                ))
        );
        jsonSchema = json(object(
                field(TYPE, OBJECT_TYPE),
                field(TITLE, localString("tree.title")),
                field(DESCRIPTION, localString("tree.description")),
                field(PROPERTIES, treeProperties)
        ));
        JsonValue cloneRequestSchema = json(object(
                field(TYPE, OBJECT_TYPE),
                field(PROPERTIES, object(
                        field("newId", object(
                                field(TYPE, STRING_TYPE),
                                field(TITLE, localString("clone.newId")),
                                field(DESCRIPTION, localString("clone.newId.description"))
                        ))
                ))
        ));

        description = ApiDescription.apiDescription().id("fake").version("v")
                .paths(Paths.paths().put("", VersionedPath.versionedPath()
                        .put(VersionedPath.UNVERSIONED, Resource.resource()
                                .title(localString("title"))
                                .description(localString(DESCRIPTION_KEY))
                                .mvccSupported(false)
                                .items(items()
                                        .pathParameter(parameter().name("id").type("string").source(PATH).build())
                                        .read(org.forgerock.api.models.Read.read().build())
                                        .update(org.forgerock.api.models.Update.update().build())
                                        .delete(org.forgerock.api.models.Delete.delete().build())
                                        .create(org.forgerock.api.models.Create.create().mode(ID_FROM_CLIENT).build())
                                        .action(action().name("clone")
                                                .description(localString("clone.description"))
                                                .request(Schema.schema().schema(cloneRequestSchema).build())
                                                .response(Schema.schema().schema(jsonSchema).build())
                                                .build())
                                        .build())
                                .resourceSchema(Schema.schema().schema(jsonSchema).build())
                                .query(org.forgerock.api.models.Query.query().type(QueryType.FILTER)
                                        .description(localString("query.description"))
                                                .queryableFields().build())
                                .action(action().name("schema").description(SCHEMA_DESCRIPTION).build())
                                .action(action().name("template").description(TEMPLATE_DESCRIPTION).build())
                                .action(action().name("getIds")
                                        .description(localString("getIds.description"))
                                        .response(DescriptorUtils.fromResource(GET_IDS_SCHEMA, getClass()))
                                        .build())
                                .action(action().name("validate")
                                        .description(localString("validate.description"))
                                        .request(Schema.schema().schema(jsonSchema).build())
                                        .response(DescriptorUtils.fromResource(VALIDATE_SCHEMA, getClass()))
                                        .stability(Stability.DEPRECATED)
                                        .build())
                                .action(action().name("validateTree")
                                        .description(localString("validateTree.description"))
                                        .request(Schema.schema().schema(jsonSchema).build())
                                        .response(DescriptorUtils.fromResource(VALIDATE_TREE_SCHEMA, getClass()))
                                        .stability(Stability.STABLE)
                                        .build())
                                .build()).build()
                ).build()).build();
    }

    private LocalizableString localString(String value) {
        return new LocalizableString(SMS_AUTH_TREES_PROVIDER + value, getClass());
    }

    /**
     * Performs a query on the authentication trees collection.
     *
     * @param request The query request containing the filter and other parameters.
     * @param handler The handler to process the query results.
     * @param context The request context.
     * @return The realm associated with the context.
     */
    @Query(operationDescription = @Operation, type = QueryType.FILTER)
    public Promise<QueryResponse, ResourceException> query(QueryRequest request, QueryResourceHandler handler,
            Context context) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return new BadRequestException("Query only supports 'true' filter").asPromise();
        } else {
            return getCollectionData(request, handler, context);
        }
    }

    private Promise<QueryResponse, ResourceException> getCollectionData(QueryRequest request,
                                                                        QueryResourceHandler handler, Context context) {
        try {
            Realm realm = getRealm(context);
            ServiceConfig treesContainer = getTreeContainerConfig(realm);
            for (String treeId : treesContainer.getSubConfigNames()) {
                try {
                    read(treeId, context, request).then(handler::handleResource);
                } catch (RuntimeException e) {
                    debug.warn("Could not load tree due to invalid structure: " + treeId);
                }
            }

            return Responses.newQueryResponse().asPromise();
        } catch (SMSException | SSOException e) {
            debug.warn("The tree service config cannot be loaded", e);
            return new InternalServerErrorException("Unable to read SMS config: " + e.getMessage()).asPromise();
        }
    }

    /**
     * Reads a tree configuration by its ID.
     *
     * @param id The ID of the tree to read.
     * @param context The request context.
     * @param request The request containing additional parameters.
     * @return A promise containing the resource response or an exception if an error occurs.
     */
    @Read(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> read(String id, Context context, Request request) {
        try {
            Realm realm = getRealm(context);
            ServiceConfig treesContainer = getTreeContainerConfig(realm);
            ServiceConfig treeConfig = treesContainer.getSubConfig(id);
            if (treeConfig == null) {
                return new NotFoundException().asPromise();
            }
            JsonValue treeJson = treeConverter.toJson(realm.asPath(), treeConfig.getAttributes(), false);
            Map<String, Object> nodes = treeJson.get(TreeBuilder.NODES).asMap();
            for (String nodeId : nodes.keySet()) {
                JsonValue nodeJson = treeJson.get(TreeBuilder.NODES).get(nodeId);
                if (Boolean.parseBoolean(request.getAdditionalParameter(FOR_UI))) {
                    String nodeType = ((Map) nodes.get(nodeId)).get(TreeBuilder.NODE_TYPE).toString();
                    Integer version = (Integer) ((Map) nodes.get(nodeId)).getOrDefault(VERSION, DEFAULT_VERSION);
                    List<Outcome> nodeOutcomes = nodeFactory.getNodeOutcomes(request.getPreferredLocales(), realm,
                            nodeId, nodeType, version);
                    nodeJson.add(OUTCOMES_KEY, nodeOutcomes.stream().map(outcomeConverter::toJson).collect(toList()));
                }
                if (!nodeJson.keys().contains(VERSION)) {
                    nodeJson.put(VERSION, "1.0");
                } else {
                    // UI representation expects version to be a string with a single decimal point
                    nodeJson.put(VERSION, "%d.0".formatted(nodeJson.get(VERSION).asInteger()));
                }
            }
            treeJson.put(FIELD_CONTENT_ID, id);
            return newResultPromise(
                    newResourceResponse(id, String.valueOf(treeJson.getObject().hashCode()), treeJson));
        } catch (SMSException | SSOException | NodeProcessException e) {
            debug.warn("The tree service config cannot be loaded", e);
            return new InternalServerErrorException("Unable to read SMS config: " + e.getMessage()).asPromise();
        }
    }

    /**
     * Creates a new tree configuration.
     *
     * @param request The create request containing the new tree configuration.
     * @param context The request context.
     * @return A promise containing the resource response or an exception if an error occurs.
     */
    @Create(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> create(CreateRequest request, Context context) {
        try {
            Realm realm = getRealm(context);

            ServiceConfig treeContainerConfig = getTreeContainerConfig(realm);
            if (treeContainerConfig.getSubConfigNames().contains(request.getNewResourceId())) {
                return new PreconditionFailedException().asPromise();
            }
            JsonValue fullTreeJson = request.getContent().copy();
            var nodes = fullTreeJson.get(NODES_KEY);
            addConfigCompatibleNodeVersion(nodes);
            if (nodes.stream().anyMatch(node -> node.isDefined(CONFIG))) {
                var nodesCopy = nodes.copy();
                nodes.forEach(node -> node.remove(CONFIG));
                validateTree(fullTreeJson, realm);
                List<String> invalidNodes = upsertNodes(context, nodesCopy);
                if (!invalidNodes.isEmpty()) {
                    return new BadRequestException("Invalid node config for: "
                            + String.join(", ", invalidNodes)).asPromise();
                }
            } else {
                validateTree(fullTreeJson, realm);
            }
            ConfigurationAttributes treeAttributes = treeConverter.fromJson(realm.asPath(), fullTreeJson);
            treeContainerConfig.addSubConfig(request.getNewResourceId(), "tree", 0, treeAttributes);

            return read(request.getNewResourceId(), context, request);
        } catch (InternalServerErrorException | SMSException | SSOException e) {
            debug.warn("The tree service config cannot be saved", e);
            return new InternalServerErrorException("Unable to save SMS config: " + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Updates an existing tree configuration.
     *
     * @param request The update request containing the updated tree configuration.
     * @param id The ID of the tree to update.
     * @param context The request context.
     * @return A promise containing the resource response or an exception if an error occurs.
     */
    @Update(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> update(UpdateRequest request, String id, Context context) {
        try {
            Realm realm = getRealm(context);

            ServiceConfig treeContainerConfig = getTreeContainerConfig(realm);
            ServiceConfig treeConfig = treeContainerConfig.getSubConfig(id);
            if (treeConfig == null) {
                return new NotFoundException().asPromise();
            }

            JsonValue fullTreeJson = request.getContent().copy();
            var nodes = fullTreeJson.get(NODES_KEY);
            addConfigCompatibleNodeVersion(nodes);
            if (nodes.stream().anyMatch(node -> node.isDefined(CONFIG))) {
                var nodesCopy = nodes.copy();
                nodes.forEach(node -> node.remove(CONFIG));
                validateTree(fullTreeJson, realm);
                validateTreeNotRealmDefaultAndMustRun(fullTreeJson, id, realm);
                List<String> invalidNodes = upsertNodes(context, nodesCopy);
                if (!invalidNodes.isEmpty()) {
                    return new BadRequestException("Invalid node config for: "
                            + String.join(", ", invalidNodes)).asPromise();
                }
            } else {
                validateTree(fullTreeJson, realm);
                validateTreeNotRealmDefaultAndMustRun(fullTreeJson, id, realm);
            }
            Map<String, Set<String>> treeAttributes = treeConverter.fromJson(realm.asPath(), fullTreeJson);
            treeConfig.setAttributes(treeAttributes);

            return read(id, context, request);
        } catch (InternalServerErrorException | SMSException | SSOException e) {
            debug.warn("The tree service config cannot be saved", e);
            return new InternalServerErrorException("Unable to save SMS config: " + e.getMessage()).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private static void addConfigCompatibleNodeVersion(JsonValue nodes) {
        // add default version for nodes
        for (String nodeId : nodes.keys()) {
            var node = nodes.get(nodeId);
            if (!node.keys().contains(VERSION)) {
                node.put(VERSION, DEFAULT_VERSION);
            } else if (node.get(VERSION).isString()) {
                // if the version is a string, convert it to an integer for storage in the configuration
                node.put(VERSION, Integer.valueOf(substringBefore(node.get(VERSION).asString(), ".")));
            }
        }
    }

    /**
     * Deletes a tree configuration by its ID.
     *
     * @param id The ID of the tree to delete.
     * @param context The request context.
     * @param request The delete request.
     * @return A promise containing the resource response or an exception if an error occurs.
     */
    @Delete(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> delete(String id, Context context, DeleteRequest request) {
        AtomicReference<Map<String, NodeConfig>> nodesToDelete = new AtomicReference<>();
        return read(id, context, request).then(resource -> {
            Realm realm = getRealm(context);
            try {
                AuthenticationTreesServiceConfig.Realm realmConfig = annotatedServiceRegistry
                        .getRealmSingleton(AuthenticationTreesServiceConfig.Realm.class, realm)
                        .orElseThrow(NotFoundException::new);
                AuthenticationTreesServiceConfig.Tree treeToDelete = realmConfig.trees().get(id);
                if (treeToDelete == null) {
                    throw new NotFoundException();
                }
                TreeUsage.Response response = treeUsageResponse(getRealm(context), id);
                if (response.inUse()) {
                    throw new InternalServerErrorException(response.message());
                }
                nodesToDelete.set(treeToDelete.nodes());
                realmConfig.trees().delete(id);
            } catch (SMSException | SSOException e) {
                debug.warn("The tree service config cannot be deleted", e);
                throw new InternalServerErrorException("Unable to delete SMS config: " + e.getMessage());
            } catch (TreeUsageException e) {
                throw new InternalServerErrorException("Unable to delete tree: " + e.getMessage());
            }
            return resource;
        }).then(resource -> {
            if (nodesToDelete.get() != null) {
                deleteNodes(context, nodesToDelete.get());
            }
            return resource;
        });
    }

    /**
     * Gets the IDs of all available trees.
     *
     * @param request The action request.
     * @param context The request context.
     * @return A promise containing the action response with the IDs of the trees.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> getIds(ActionRequest request, Context context) {
        Realm realm = getRealm(context);
        List<Object> array = array();
        ServiceConfig treesContainer;
        try {
            treesContainer = getTreeContainerConfig(realm);
            for (String name : treesContainer.getSubConfigNames()) {
                array.add(json(object(field("_id", name))));
            }
        } catch (SMSException | SSOException e) {
            debug.warn("The tree service config cannot be loaded", e);
            return new InternalServerErrorException("Unable to read SMS config: " + e.getMessage()).asPromise();
        }
        return newActionResponse(json(object(field("result", array)))).asPromise();
    }

    /**
     * Validates a tree configuration.
     *
     * @param request The action request containing the tree configuration to validate.
     * @param context The request context.
     * @return A promise containing the action response with validation results.
     */
    @Action(operationDescription = @Operation(stability = Stability.DEPRECATED))
    public Promise<ActionResponse, ResourceException> validate(ActionRequest request, Context context) {
        Realm realm = getRealm(context);
        try {
            validateTree(request.getContent(), realm);
        } catch (ResourceException e) {
            return newActionResponse(json(object(
                    field("valid", "false"),
                    field("warnings", array()),
                    field("errors", array(e.getMessage()))
            ))).asPromise();
        }
        return newActionResponse(json(object(
                field("valid", "true"),
                field("warnings", array())
        ))).asPromise();
    }

    /**
     * Validates a tree configuration and returns the validation results.
     *
     * @param request The action request containing the tree configuration to validate.
     * @param context The request context.
     * @return A promise containing the action response with validation results.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> validateTree(ActionRequest request, Context context) {
        List<ValidationFailure> validationResults = validationEngine.validate(request.getContent(),
                context.asContext(RealmContext.class).getRealm());
        return Responses.newActionResponse(convertValidationResultsToJsonValue(validationResults)).asPromise();
    }

    private JsonValue convertValidationResultsToJsonValue(List<ValidationFailure> validationFailures) {
        Map<ValidationFailure.Severity, List<String>> messagesBySeverity =
                validationFailures.stream().collect(Collectors.groupingBy(
                        ValidationFailure::severity,
                        Collectors.mapping(ValidationFailure::message, Collectors.toList())
                ));

        Map<String, Object> result = new HashMap<>();
        boolean hasNoErrors = !messagesBySeverity.containsKey(ValidationFailure.Severity.ERROR);
        result.put("valid", hasNoErrors);
        Arrays.stream(ValidationFailure.Severity.values()).forEach(severity -> {
            result.put(severity.displayName(), messagesBySeverity.computeIfAbsent(severity, k -> new ArrayList<>()));
        });
        return json(result);
    }

    /**
     * Clones a tree configuration.
     *
     * @param request The action request containing the new tree ID.
     * @param id The ID of the tree to clone.
     * @param context The request context.
     * @return A promise containing the action response with the cloned tree configuration.
     */
    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> clone(ActionRequest request, String id, Context context) {
        final String newTreeId = request.getContent().get("newId").asString();
        if (newTreeId == null) {
            return new BadRequestException("Parameter 'newId' is required").asPromise();
        }

        final PreferredLocales preferredLocales = request.getPreferredLocales();

        return read(id, context, newReadRequest(EMPTY_STRING).setPreferredLocales(preferredLocales))
                .thenAsync(tree -> cloneTreeConfig(context, preferredLocales, newTreeId, tree.getContent()))
                .thenAsync(newTreeConfig -> create(
                        newCreateRequest(EMPTY_STRING, newTreeId, newTreeConfig)
                                .setAdditionalParameter(FOR_UI, request.getAdditionalParameter(FOR_UI))
                                .setPreferredLocales(preferredLocales), context)
                        .thenOnException(e -> {
                            debug.error("Failed to create cloned tree", e);
                            deleteNodes(context, newTreeConfig);
                        }))
                .thenAsync(createResponse -> newActionResponse(createResponse.getContent()).asPromise());
    }

    /**
     * Clones the tree configuration json and returns the newly created config.
     *
     * @param context request context
     * @param preferredLocales preferred request locales
     * @param newTreeId ID to use for the new tree
     * @param existingTreeConfig existing tree config
     * @return new tree config
     */
    private Promise<JsonValue, ResourceException> cloneTreeConfig(Context context, PreferredLocales preferredLocales,
            String newTreeId, JsonValue existingTreeConfig) {
        final Map<String, String> idMap = new ConcurrentHashMap<>();
        idMap.put(TREE_NODE_SUCCESS_ID.toString(), TREE_NODE_SUCCESS_ID.toString());
        idMap.put(TREE_NODE_FAILURE_ID.toString(), TREE_NODE_FAILURE_ID.toString());

        return when(existingTreeConfig.get(NODES_KEY).stream()
                .map(node -> cloneNodeEntry(context, preferredLocales, idMap, node))
                .collect(Collectors.toList()))
                .thenAsync(nodeEntries -> newPromise(() -> existingTreeConfig.copy()
                        .put(FIELD_CONTENT_ID, newTreeId)
                        .put(ENTRY_NODE_ID, idMap.get(existingTreeConfig.get(ENTRY_NODE_ID).asString()))
                        .put(NODES_KEY, nodeEntries.stream()
                                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))))
                .thenOnException(e -> {
                    debug.error("Failed to clone tree", e);
                    deleteNodes(context, json(object(field(NODES_KEY, existingTreeConfig.get(NODES_KEY).stream()
                            .filter(nodeEntry -> idMap.containsKey(nodeEntry.getPointer().leaf()))
                            .collect(Collectors.toMap(nodeEntry -> idMap.get(nodeEntry.getPointer().leaf()),
                                    JsonValue::asMap))))));
                });
    }

    /**
     * Clones a node entry within a larger tree config and returns the new entry paired with the newly generated
     * node ID.
     *
     * @param context request context
     * @param preferredLocales preferred request locales
     * @param idMap map of old node IDs to new node IDs
     * @param nodeEntry json representation of the node entry within a tree
     * @return pair of new node ID and new node entry
     */
    private Promise<Pair<String, JsonValue>, ResourceException> cloneNodeEntry(Context context,
            PreferredLocales preferredLocales, Map<String, String> idMap, JsonValue nodeEntry) {
        final String existingNodeId = nodeEntry.getPointer().leaf();
        final String newNodeId = idMap.computeIfAbsent(existingNodeId, k -> UUID.randomUUID().toString());

        final JsonValue newNodeEntry = nodeEntry.copy();
        newNodeEntry.get(CONNECTIONS_KEY).asMap().entrySet()
                .forEach(connection -> connection.setValue(idMap
                        .computeIfAbsent((String) connection.getValue(), k -> UUID.randomUUID().toString())));

        return cloneNode(context, preferredLocales, existingNodeId, newNodeId, nodeEntry.get(NODE_TYPE_KEY).asString(),
                        getNodeVersion(nodeEntry))
                .thenAsync(ignored -> newPromise(() -> Pair.of(newNodeId, newNodeEntry)));
    }

    /**
     * Clones a node by reading the existing node config and creating a new node with the same configuration values.
     *
     * @param context request context
     * @param preferredLocales preferred request locales
     * @param existingNodeId ID of the node to clone
     * @param newNodeId ID of the new node
     * @param nodeType node collection provider
     * @param nodeVersion node version
     * @return the resource response from the creation request
     */
    private Promise<ResourceResponse, ResourceException> cloneNode(Context context, PreferredLocales preferredLocales,
            String existingNodeId, String newNodeId, String nodeType, Integer nodeVersion) {
        ResourcePath path = createResourcePath(nodeType, nodeVersion);
        return connectionFactory.getConnectionAsync()
                .thenAsync(connection -> connection.readAsync(context,
                            applyVersioning(newReadRequest(path.child(existingNodeId)))
                                    .setPreferredLocales(preferredLocales)))
                .thenAsync(nodeRead -> {
                    if (!nodeType.equals("PageNode")) {
                        return newPromise(nodeRead::getContent);
                    }

                    final JsonValue pageNodeConfig = nodeRead.getContent().copy();
                    return when(pageNodeConfig.get(NODES_KEY).stream()
                            .map(node -> cloneNode(context, preferredLocales, node.get(FIELD_CONTENT_ID).asString(),
                                    UUID.randomUUID().toString(), node.get(NODE_TYPE_KEY).asString(),
                                    getNodeVersion(node))
                                    .thenOnResult(clonedNode -> node.put(FIELD_CONTENT_ID, clonedNode.getId())))
                            .collect(Collectors.toList()))
                            .thenAsync(ignored -> newPromise(() -> pageNodeConfig));
                })
                .thenAsync(nodeConfig -> connectionFactory.getConnectionAsync()
                        .thenAsync(connection -> connection.createAsync(context,
                                applyVersioning(newCreateRequest(path, newNodeId,
                                        nodeConfig.put(FIELD_CONTENT_ID, newNodeId))
                                        .setPreferredLocales(preferredLocales)))));
    }

    /**
     * Deletes all nodes of a given tree.
     *
     * @param context request context
     * @param nodes the map of nodes to delete
     * @return the resource responses of each delete request
     */
    private Promise<List<ResourceResponse>, ResourceException> deleteNodes(Context context,
                                                                           Map<String, NodeConfig> nodes) {
        return when(nodes.entrySet().stream()
                .map(entry -> connectionFactory.getConnectionAsync()
                        .thenAsync(connection -> {
                            DeleteRequest request = applyVersioning(newDeleteRequest(
                                    createResourcePath(
                                            entry.getValue().nodeType(),
                                            entry.getValue().version()
                                    ).child(entry.getKey())));
                            return connection.deleteAsync(context, request);
                        }))
                .collect(Collectors.toList()));
    }

    /**
     * Deletes all nodes of a given tree.
     *
     * @param context request context
     * @param treeConfig the tree configuration containing the nodes to delete
     * @return the resource responses of each delete request
     */
    private Promise<List<ResourceResponse>, ResourceException> deleteNodes(Context context, JsonValue treeConfig) {
        return when(treeConfig.get(NODES_KEY).stream()
                .map(node -> connectionFactory.getConnectionAsync()
                        .thenAsync(connection -> {
                            DeleteRequest request = applyVersioning(newDeleteRequest(
                                    createResourcePath(
                                            node.get(NODE_TYPE_KEY).asString(),
                                            getNodeVersion(node)
                                    ).child(node.getPointer().leaf())));
                            return connection.deleteAsync(context, request);
                        }))
                .collect(Collectors.toList()));
    }

    private Integer getNodeVersion(JsonValue node) {
        JsonValue nodeVersion = node.get(VERSION);
        if (nodeVersion.isNull()) {
            nodeVersion = node.get(PAGE_NODE_VERSION);
        }
        if (nodeVersion.isString()) {
            return Integer.valueOf(substringBefore(nodeVersion.asString(), "."));
        } else if (nodeVersion.isNumber()) {
            return nodeVersion.asInteger();
        } else {
            return DEFAULT_VERSION;
        }
    }

    @Override
    protected JsonValue createSchema(Optional<Context> context) {
        return jsonSchema;
    }

    @Override
    protected JsonValue createTemplate() {
        return json(object(
            field(ENTRY_NODE_ID, ISAuthConstants.TREE_NODE_FAILURE_ID),
            field(NODES_KEY, object()),
            field(STATIC_NODES_KEY, object())
        ));
    }

    private void validateTree(JsonValue tree, Realm realm)
            throws BadRequestException, InternalServerErrorException {
        try {
            if (isBlank(tree.get(ENTRY_NODE_ID).asString())) {
                throw new BadRequestException("Node with ID " + ENTRY_NODE_ID + " must exist in the tree.");
            }
            addConfigCompatibleNodeVersion(tree.get(NODES_KEY));
            Map<String, Set<String>> treeAttributes = treeConverter.fromJson(realm.asPath(), tree);
            validateMissingAttributes(treeAttributes, treeSchema);
        } catch (JsonException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    /**
     * Validates that if the tree being updated is the realm default tree, it is not being updated to be mustRun.
     *
     * @param tree the JsonValue representation of the tree being updated.
     * @param id the name of the tree being updated.
     * @param realm the realm the tree is being updated in.
     * @throws BadRequestException if the tree is the realm default tree and is being updated to be mustRun.
     * @throws InternalServerErrorException if an error occurs while validating the tree.
     */
    private void validateTreeNotRealmDefaultAndMustRun(JsonValue tree, String id, Realm realm)
            throws BadRequestException, InternalServerErrorException {
        try {
            Map<String, Set<String>> treeAttributes = treeConverter.fromJson(realm.asPath(), tree);
            Set<String> mustRun = treeAttributes.get(MUST_RUN_KEY);
            if (mustRun != null && mustRun.contains("true")
                    && (coreServicesWrapper.getOrgConfiguredAuthenticationChain(realm.asPath()).equals(id)
                    || coreServicesWrapper.getOrgConfiguredAdminAuthModule(realm.asPath()).equals(id))) {
                throw new BadRequestException("The default authentication service cannot have mustRun enabled");
            }
        } catch (JsonException | SMSException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private void validateMissingAttributes(Map<String, Set<String>> attributes, ServiceSchema schema)
            throws BadRequestException {
        TreeSet<String> missingAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        missingAttributes.addAll(schema.getAttributeSchemaNames());
        missingAttributes.removeAll(attributes.keySet());
        missingAttributes.remove(RequiredValueValidator.class.getSimpleName());
        missingAttributes.remove(NodeValueValidator.class.getSimpleName());
        missingAttributes.remove(DESCRIPTION_KEY);
        missingAttributes.remove(UI_CONFIG_KEY);
        missingAttributes.remove(STATIC_NODES_KEY);
        missingAttributes.remove(IDM_IDENTITY_RESOURCE);
        missingAttributes.remove(ENABLED_KEY);
        missingAttributes.remove(INNER_KEY);
        missingAttributes.remove(MUST_RUN_KEY);
        missingAttributes.remove(MAXIMUM_SESSION_TIME_KEY);
        missingAttributes.remove(MAXIMUM_IDLE_TIME_KEY);
        missingAttributes.remove(TREE_TIMEOUT_KEY);
        missingAttributes.remove(NO_SESSION_KEY);
        missingAttributes.remove(TRANSACTIONAL_ONLY);
        missingAttributes.remove(GREATER_THAN_ZERO_VALIDATOR_KEY);
        missingAttributes.remove(StaticNodeValueValidator.class.getSimpleName());
        if (!missingAttributes.isEmpty()) {
            throw new BadRequestException("Missing required attribute '" + missingAttributes.iterator().next() + "'");
        }
    }

    private Realm getRealm(Context context) {
        return context.asContext(RealmContext.class).getRealm();
    }

    private List<String> upsertNodes(Context context, JsonValue nodes) {
        List<String> invalidNodes = new ArrayList<>();
        for (JsonValue node : nodes) {
            JsonValue nodeConfig = node.get(CONFIG).defaultTo(json(object()));
            String nodeType = node.get(NODE_TYPE_KEY).asString();
            Integer nodeVersion = node.get(VERSION).defaultTo(DEFAULT_VERSION).asInteger();
            UUID nodeId = UUID.fromString(node.isDefined("_id")
                    ? node.get("_id").asString()
                    : node.getPointer().leaf()
            );
            if (nodeType.equals("PageNode")) {
                addConfigCompatibleNodeVersion(nodeConfig.get(NODES_KEY));
                invalidNodes.addAll(upsertNodes(context, nodeConfig.get(NODES_KEY)));
            }
            try {
                upsertNode(context, nodeType, nodeId, nodeConfig, nodeVersion);
            } catch (ResourceException e) {
                invalidNodes.add(nodeType);
            }

        }
        return invalidNodes;
    }

    private void upsertNode(Context context, String nodeType, UUID nodeId, JsonValue nodeConfig, Integer nodeVersion)
            throws ResourceException {
        ResourcePath path = createResourcePath(nodeType, nodeVersion);
        try {
            // Try to create the node.
            createNode(path, nodeId, nodeConfig, context);
        } catch (PreconditionFailedException | ConflictException e) {
            // The node already exists. Update the node instead.
            try {
                updateNode(path, nodeId, nodeConfig, context);
            } catch (ResourceException ex) {
                debug.error("Failed to update a node with type '{}' from tree config", nodeType, ex);
                throw ex;
            }
        } catch (ResourceException e) {
            debug.error("Failed to create a node with type '{}' from tree config", nodeType, e);
            throw e;
        }
    }

    private void createNode(ResourcePath path, UUID nodeId, JsonValue nodeConfig, Context context)
            throws ResourceException {
        CreateRequest createRequest = applyVersioning(newCreateRequest(path, nodeId.toString(), nodeConfig));
        connectionFactory.getConnection().create(context, createRequest);
    }

    private void updateNode(ResourcePath path, UUID nodeId, JsonValue nodeConfig, Context context)
            throws ResourceException {
        UpdateRequest updateRequest = applyVersioning(newUpdateRequest(path, nodeId.toString(), nodeConfig));
        connectionFactory.getConnection().update(context, updateRequest);
    }

    private <T extends Request> T applyVersioning(T request) {
        return (T) request.setResourceVersion(RESOURCE_V3);
    }

    private ResourcePath createResourcePath(String nodeType, Integer nodeVersion) {
        ResourcePath path = resourcePath("realm-config").child("authentication")
                .child("authenticationtrees").child(NODES_KEY).child(nodeType);

        return path.child(nodeVersion + ".0");
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return description;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return description;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        // Nothing to do as description is static.
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        // Nothing to do as description is static.
    }

    private ServiceConfig getTreeContainerConfig(Realm realm) throws SMSException, SSOException {
        ServiceConfigManager scm = serviceConfigManagerFactory.create(ISAuthConstants.AUTH_TREES_SERVICE_NAME,
                ISAuthConstants.AUTH_TREES_SERVICE_VERSION);
        return scm.getOrganizationConfig(realm.asDN(), null);
    }

    private TreeUsage.Response treeUsageResponse(Realm realm, String treeId) throws TreeUsageException {
        List<TreeUsage.Response> responses = treeUsages.stream()
                .map(rethrowFunction(usage -> usage.reportUsages(realm, treeId)))
                .sorted(Comparator.comparingInt((TreeUsage.Response usage) -> usage.appNames().size()).reversed())
                .toList();
        boolean isTreeInUse = responses.stream().anyMatch(TreeUsage.Response::inUse);
        if (isTreeInUse) {
            String appResponses = responses.stream()
                    .filter(TreeUsage.Response::inUse)
                    .map(response -> String.format("%s, including %s", response.message(),
                            response.appNames()))
                    .collect(Collectors.joining(", and "));

            String message = format("""
                            The authentication tree is in use and cannot be deleted.
                            Tree '%s' is in use by %s in the realm '%s'.
                            Please check the logs for details of the tree usage.""",
                    treeId,
                    appResponses,
                    realm.asPath());
            return new TreeUsage.Response(true, message);
        } else {
            return new TreeUsage.Response(false, "Tree is not in use.");
        }
    }
}
