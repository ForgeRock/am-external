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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest;

import static com.sun.identity.saml2.common.SAML2Utils.getSAML2MetaManagerWithToken;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openam.federation.rest.guice.RestFederationGuiceModule.SAML2_ENTITY_PROVIDER_MAPPER;
import static org.forgerock.openam.forgerockrest.utils.ServerContextUtils.getTokenFromContext;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SAML2_REQUEST_HANDLER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openam.federation.rest.schema.Saml2EntityProvider;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.ObjectEnricher;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * This CREST request handler exposes query functionality for SAML2 entity providers.
 */
@RequestHandler(
        @Handler(
                mvccSupported = false,
                title = SAML2_REQUEST_HANDLER + TITLE,
                description = SAML2_REQUEST_HANDLER + DESCRIPTION,
                resourceSchema = @Schema(fromType = Saml2EntityProvider.class)
        )
)
public class Saml2RequestHandler {

    private final ObjectEnricher objectEnricher;
    private final ObjectMapper objectMapper;
    private static Logger debug = LoggerFactory.getLogger(Saml2RequestHandler.class);


    /**
     * Construct a new instance of the saml2 request handler.
     *
     * @param objectEnricher The annotation based object enricher.
     * @param objectMapper   The object mapper.
     */
    @Inject
    public Saml2RequestHandler(ObjectEnricher objectEnricher,
            @Named(SAML2_ENTITY_PROVIDER_MAPPER) ObjectMapper objectMapper) {
        this.objectEnricher = objectEnricher;
        this.objectMapper = objectMapper;
    }

    /**
     * Queries the configured SAML2 entity providers.
     *
     * @param context The CREST request context.
     * @param request The query request.
     * @param handler The query resource handler.
     * @return The promise of the operation result.
     */
    @Query(operationDescription = @Operation, type = QueryType.FILTER, queryableFields = "*")
    public Promise<QueryResponse, ResourceException> query(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        try {
            String realm = RealmContext.getRealm(context).asPath();

            SAML2MetaManager saml2MetaManager = getSAML2MetaManagerWithToken(getTokenFromContext(context));
            Set<String> entityIds = saml2MetaManager.getAllEntities(realm);

            EnricherContext enricherContext = new HostedEntityContext(ROOT,
                    saml2MetaManager.getAllHostedEntities(realm));

            Set<String> filteredEntityIds;
            try {
                filteredEntityIds = request.getQueryFilter().accept(new EntityQueryFilterVisitor(), entityIds);
            } catch (UnsupportedOperationException e) {
                debug.error("Saml2RequestHandler.query() :: Unsupported query specified={}",
                        request.getQueryFilter(), e);
                return new NotSupportedException("Unsupported query specified. Supported operands: eq, co, sw and !"
                        + ". Supported field: entityId").asPromise();
            }

            List<ResourceResponse> resourceResponses = new ArrayList<>();
            for (String entityId : filteredEntityIds) {
                Saml2EntityProvider provider = new Saml2EntityProvider();
                EntityDescriptorElement entityDescriptor = saml2MetaManager.getEntityDescriptor(realm, entityId);
                objectEnricher.enrich(provider, entityDescriptor.getValue(), enricherContext);
                JsonValue json = json(objectMapper.convertValue(provider, new TypeReference<>() { }));
                resourceResponses.add(Responses.newResourceResponse(provider.getId(),
                        String.valueOf(json.getObject().hashCode()), json));
            }

            return QueryResponsePresentation.perform(handler, request, resourceResponses);
        } catch (SAML2MetaException e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    /**
     * Context that can be used to determine whether a given entity is a hosted entity.
     *
     * @since 7.0.0
     */
    public static final class HostedEntityContext extends EnricherContext {

        private final List<String> hostedEntityIds;

        HostedEntityContext(EnricherContext parent, List<String> hostedEntityIds) {
            super(parent);
            this.hostedEntityIds = hostedEntityIds;
        }

        /**
         * Whether the given entity is a hosted entity.
         *
         * @param entityId an entity ID.
         * @return whether the entity is a hosted entity
         */
        public boolean isHostedEntity(String entityId) {
            return hostedEntityIds.contains(entityId);
        }

    }

}
