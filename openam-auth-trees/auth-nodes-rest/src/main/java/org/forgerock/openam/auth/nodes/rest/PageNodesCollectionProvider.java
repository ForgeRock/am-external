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

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.am.trees.api.NodeUpgrader;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.Schema;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.auth.nodes.framework.script.ConfigProviderScriptGenerator;
import org.forgerock.openam.auth.trees.engine.AuthNodeFactory;
import org.forgerock.openam.core.rest.sms.OutcomeJsonConverter;
import org.forgerock.openam.core.rest.sms.SmsResourceProviderContext;
import org.forgerock.openam.core.sms.SmsJsonConverter;
import org.forgerock.openam.rest.DescriptorUtils;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceSchema;

/**
 * A collection provider for trees and their sub-configurations. Provides single-request CRUD operations for a tree and
 * its sub-configurations.
 */
@CollectionProvider(details = @Handler(mvccSupported = false))
public class PageNodesCollectionProvider extends AuthTreeNodeCollectionProvider {

    private static final String NODES_KEY = "nodes";
    private static final String SCHEMA = "PageNodesCollectionProvider.schema.json";
    private static final Schema JSON_SCHEMA = DescriptorUtils.fromResource(SCHEMA, PageNodesCollectionProvider.class);
    private final PageNodeCollectionProviderUtils pageNodeCollectionProviderUtils;

    @Inject
    PageNodesCollectionProvider(
            SmsResourceProviderContext dependencies, @Assisted("schema") ServiceSchema schema,
            @Assisted("rootSchema") ServiceSchema rootServiceSchema, AuthNodeFactory nodeFactory,
            OutcomeJsonConverter outcomeJsonConverter, ConfigProviderScriptGenerator scriptGenerator,
            PageNodeCollectionProviderUtils pageNodeCollectionProviderUtils, @Assisted Stability stability,
            NodeUpgrader nodeUpgrader, NodeRegistry nodeRegistry) {
        super(dependencies, schema, SchemaType.ORGANIZATION, rootServiceSchema, List.of(schema), "PageNode", true,
                nodeFactory, outcomeJsonConverter, scriptGenerator, stability, nodeUpgrader, nodeRegistry);
        this.pageNodeCollectionProviderUtils = pageNodeCollectionProviderUtils;
    }

    @Override
    protected JsonValue getJsonValue(String resourceId, String realm, ServiceConfig config, Context context)
            throws InternalServerErrorException {
        JsonValue fromSms = super.getJsonValue(resourceId, realm, config, context);
        return pageNodeCollectionProviderUtils.getJsonValue(fromSms);
    }

    @Override
    @Delete(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId) {
        deleteContainedNodes(context, resourceId);
        return super.deleteInstance(context, resourceId);
    }

    private void deleteContainedNodes(final Context context, final String resourceId) {
        readInstance(context, resourceId)
                .then(response -> pageNodeCollectionProviderUtils.deleteNodes(context, response));
    }

    @Override
    protected ConfigurationAttributes getAttributesFromContent(
            String realm, JsonValue content, ConfigurationAttributes config, SmsJsonConverter converter)
            throws BadRequestException, InternalServerErrorException {
        List<String> nodes = pageNodeCollectionProviderUtils.getAttributesFromContent(content);
        return super.getAttributesFromContent(realm, content.copy().put(NODES_KEY, nodes), config, converter);
    }

    @Override
    protected JsonValue createSchema(Optional<Context> context) {
        return JSON_SCHEMA.getSchema();
    }

    @Override
    protected JsonValue createTemplate() {
        return json(object(
                field(NODES_KEY, array())
        ));
    }
}
