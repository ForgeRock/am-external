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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.hosted;

import static com.sun.identity.saml2.common.SAML2Utils.getSAML2MetaManagerWithToken;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.forgerockrest.utils.ServerContextUtils.getTokenFromContext;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_403_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SAML2_ENTITIES_COLLECTION_PROVIDER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.utils.JsonValueBuilder.toJsonValue;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateNotSupportedException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.federation.rest.JaxbEntity;
import org.forgerock.openam.federation.rest.Saml2EntitiesCollectionProvider;
import org.forgerock.openam.federation.rest.schema.hosted.HostedSaml2EntityProvider;
import org.forgerock.openam.federation.rest.schema.hosted.identity.IdentityProvider;
import org.forgerock.openam.federation.rest.schema.hosted.service.ServiceProvider;
import org.forgerock.openam.objectenricher.ObjectEnricher;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigType;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * Exposes entity provider level operations for hosted SAML2 entity providers.
 */
@CollectionProvider(
        pathParam = @Parameter(
                name = "entityid",
                type = "string",
                description = SAML2_ENTITIES_COLLECTION_PROVIDER + PATH_PARAM + DESCRIPTION
        ),
        details = @Handler(
                mvccSupported = false,
                title = SAML2_ENTITIES_COLLECTION_PROVIDER + "hosted." + TITLE,
                description = SAML2_ENTITIES_COLLECTION_PROVIDER + "hosted." + DESCRIPTION,
                resourceSchema = @Schema(schemaResource = "/hostedSamlEntityProvider.schema.json")
        )
)
public final class HostedEntitiesCollectionProvider extends Saml2EntitiesCollectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(HostedEntitiesCollectionProvider.class);
    private static final String ENTITY_ID = "entityId";
    private static final JsonPointer META_ALIAS_PTR = new JsonPointer("services/metaAlias");

    /**
     * Instantiates a new Hosted entities collection provider.
     *
     * @param objectEnricher the object enricher
     * @param objectMapper the object mapper
     */
    @Inject
    public HostedEntitiesCollectionProvider(ObjectEnricher objectEnricher, ObjectMapper objectMapper) {
        super(objectEnricher, objectMapper);
    }

    /**
     * Creates an entity provider.
     *
     * @param context the request context.
     * @param request the {@link CreateRequest}.
     * @return The promise of the operation result.
     */
    @Create(
            modes = CreateMode.ID_FROM_SERVER,
            operationDescription = @Operation(
                    description = SAML2_ENTITIES_COLLECTION_PROVIDER + CREATE_DESCRIPTION,
                    errors = {
                            @ApiError(
                                    code = 403,
                                    description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_403_DESCRIPTION)
                    }
            )
    )
    public Promise<ResourceResponse, ResourceException> create(Context context, CreateRequest request) {
        if (request.getNewResourceId() != null) {
            return new CreateNotSupportedException("Create not supported with client provided ID").asPromise();
        }
        try {
            JsonValue jsonEntity = request.getContent();
            final String entityId = isNotBlank(jsonEntity.get(ENTITY_ID).asString())
                    ? jsonEntity.get(ENTITY_ID).asString().trim()
                    : UUID.randomUUID().toString();
            final SAML2MetaManager saml2MetaManager = getSAML2MetaManagerWithToken(getTokenFromContext(context));
            validateEntityJson(context, request, saml2MetaManager);
            JaxbEntity jaxbEntity = createJaxbEntity(entityId);

            replenishJaxbEntity(jaxbEntity, request.getContent(), context);
            final String realm = RealmContext.getRealm(context).asPath();
            if (saml2MetaManager.getEntityDescriptor(realm, entityId) != null) {
                return new PreconditionFailedException("An entity provider with the provided entity ID already exist")
                        .asPromise();
            }

            saml2MetaManager.createEntity(
                    SAML2MetaUtils.MetadataUpdateType.CREATE,
                    realm,
                    jaxbEntity.getStandardMetadata(),
                    jaxbEntity.getExtendedMetadata()
            );
            return entityAsPromise(jaxbEntity);
        } catch (JsonProcessingException | SAML2Exception e) {
            logger.error("An error occurred while creating an entity provider", e);
            return new InternalServerErrorException(e).asPromise();
        } catch (ResourceException e) {
            logger.debug("An error occurred while creating an entity provider", e);
            return e.asPromise();
        }
    }

    private void validateEntityJson(Context context, CreateRequest request, SAML2MetaManager saml2MetaManager)
            throws ResourceException {
        final Realm realm = RealmContext.getRealm(context);
        try {
            saml2MetaManager.validateMetaAliasForNewEntity(realm.asPath(), getMetaAliases(request));
        } catch (SAML2MetaException e) {
            throw new BadRequestException(e);
        }
        validateEntityJson(request.getContent(), realm);
    }

    private List<String> getMetaAliases(CreateRequest request) {
        List<String> metaAliases = new ArrayList<>();
        for (JsonValue schemaEntry : request.getContent()) {
            if (schemaEntry.isMap()) {
                metaAliases.add(schemaEntry.get(META_ALIAS_PTR).asString());
            }
        }
        return metaAliases;
    }

    /**
     * Returns the JSON schema associated with hosted entity providers.
     *
     * @return The hosted entity provider's JSON schema.
     */
    @Action(operationDescription = @Operation(
            description = SAML2_ENTITIES_COLLECTION_PROVIDER + "schema." + ACTION_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = 403,
                            description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_403_DESCRIPTION)
            })
    )
    public Promise<ActionResponse, ResourceException> schema() {
        return newResultPromise(newActionResponse(toJsonValue(
                getClass().getResourceAsStream("/hostedSamlEntityProvider.schema.json"))));
    }

    @Override
    protected void assertValidEntityLocation(JaxbEntity jaxbEntity) throws BadRequestException {
        if (!jaxbEntity.isHosted()) {
            throw new BadRequestException("Remote entity requested from hosted endpoint");
        }
    }

    @Override
    protected void replenishJaxbEntity(JaxbEntity jaxbEntity, JsonValue jsonContent, Context context)
            throws JsonProcessingException {
        HostedSaml2EntityProvider jsonEntity = jsonAsEntityProvider(jsonContent, HostedSaml2EntityProvider.class);

        // IDP
        IdentityProvider idp = jsonEntity.getIdentityProvider();
        replenishHostedJaxbRole(idp, jaxbEntity, IDP);
        // SP
        ServiceProvider sp = jsonEntity.getServiceProvider();
        replenishHostedJaxbRole(sp, jaxbEntity, SP);
    }

    private void replenishHostedJaxbRole(Object provider, JaxbEntity jaxbEntity, Saml2EntityRole role) {
        if (doesRoleRequireReplenish(jaxbEntity, provider, role)) {
            replenishJaxbRole(jaxbEntity, provider, role, ROOT);
        }
    }

    @Override
    protected Object convertToJsonEntity(JaxbEntity jaxbEntity) {
        HostedSaml2EntityProvider jsonEntity = new HostedSaml2EntityProvider();
        jsonEntity.setEntityId(jaxbEntity.getStandardMetadata().getValue().getEntityID());
        enrichRole(jaxbEntity, Saml2EntityRole.IDP, IdentityProvider::new, jsonEntity::setIdentityProvider);
        enrichRole(jaxbEntity, Saml2EntityRole.SP, ServiceProvider::new, jsonEntity::setServiceProvider);
        return jsonEntity;
    }

    private boolean doesRoleRequireReplenish(JaxbEntity jaxbEntity, Object jsonRole, Saml2EntityRole role) {
        if (jsonRole != null) {
            jaxbEntity.addRole(role);
            return true;
        }
        jaxbEntity.removeRole(role);

        return false;
    }

    private JaxbEntity createJaxbEntity(String entityId) {
        com.sun.identity.saml2.jaxb.metadata.ObjectFactory metadataFactory =
                new com.sun.identity.saml2.jaxb.metadata.ObjectFactory();
        EntityDescriptorType entityDescriptorType = metadataFactory.createEntityDescriptorType();
        EntityDescriptorElement entityDescriptor = metadataFactory.createEntityDescriptorElement(entityDescriptorType);
        entityDescriptor.getValue().setEntityID(entityId);

        com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory configFactory =
                new com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory();
        EntityConfigType entityConfig = configFactory.createEntityConfigType();
        entityConfig.setHosted(true);
        entityConfig.setEntityID(entityId);

        EntityConfigElement entityConfigElement = configFactory.createEntityConfigElement(entityConfig);
        return new JaxbEntity(entityDescriptor, entityConfigElement);
    }
}
