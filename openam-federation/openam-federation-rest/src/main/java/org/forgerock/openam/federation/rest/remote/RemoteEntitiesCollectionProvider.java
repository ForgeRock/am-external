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
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.federation.rest.remote;

import static com.sun.identity.saml2.common.SAML2Utils.getSAML2MetaManagerWithToken;
import static com.sun.identity.saml2.meta.SAML2MetaUtils.importSAML2Document;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.forgerockrest.utils.ServerContextUtils.getTokenFromContext;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_403_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SAML2_ENTITIES_COLLECTION_PROVIDER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.utils.JsonValueBuilder.toJsonValue;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.federation.rest.JaxbEntity;
import org.forgerock.openam.federation.rest.Saml2EntitiesCollectionProvider;
import org.forgerock.openam.federation.rest.schema.remote.RemoteSaml2EntityProvider;
import org.forgerock.openam.federation.rest.schema.remote.identity.IdentityProvider;
import org.forgerock.openam.federation.rest.schema.remote.service.ServiceProvider;
import org.forgerock.openam.objectenricher.ObjectEnricher;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.services.context.Context;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * Exposes entity provider level operations for remote SAML2 entity providers.
 */
@CollectionProvider(
        pathParam = @Parameter(
                name = "entityid",
                type = "string",
                description = SAML2_ENTITIES_COLLECTION_PROVIDER + PATH_PARAM + DESCRIPTION
        ),
        details = @Handler(
                mvccSupported = false,
                title = SAML2_ENTITIES_COLLECTION_PROVIDER + "remote." + TITLE,
                description = SAML2_ENTITIES_COLLECTION_PROVIDER + "remote." + DESCRIPTION,
                resourceSchema = @Schema(schemaResource = "/remoteSamlEntityProvider.schema.json")
        )
)
public final class RemoteEntitiesCollectionProvider extends Saml2EntitiesCollectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(RemoteEntitiesCollectionProvider.class);
    private static final String SCHEMA = "schema.";
    private static final String IMPORT_ENTITY = "importEntity.";

    /**
     * Instantiates a new Remote entities collection provider.
     *
     * @param objectEnricher the object enricher
     * @param objectMapper the object mapper
     */
    @Inject
    public RemoteEntitiesCollectionProvider(ObjectEnricher objectEnricher, ObjectMapper objectMapper) {
        super(objectEnricher, objectMapper);
    }

    /**
     * Imports a SAML entity provider using its standard metadata.
     *
     * @param actionRequest The import entity request.
     * @param context The request context.
     * @return The list of imported SAML2 entity providers as JSON.
     */
    @Action(operationDescription = @Operation(
            description = SAML2_ENTITIES_COLLECTION_PROVIDER + IMPORT_ENTITY + ACTION_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = 403,
                            description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_403_DESCRIPTION
                    )
            }),
            request = @Schema(schemaResource = "RemoteEntitiesCollectionProvider.importEntity.request.schema.json"),
            response = @Schema(schemaResource = "RemoteEntitiesCollectionProvider.importEntity.response.schema.json")
    )
    public Promise<ActionResponse, ResourceException> importEntity(ActionRequest actionRequest, Context context) {
        Optional<Document> metadata = Optional.ofNullable(actionRequest.getContent().get("standardMetadata").asString())
                .map(Base64url::decodeToString)
                .map(XMLUtils::toDOMDocument);
        if (metadata.isEmpty()) {
            return new BadRequestException("Invalid standard metadata value in request").asPromise();
        }
        try {
            List<String> entityIds = importSAML2Document(getSAML2MetaManagerWithToken(getTokenFromContext(context)),
                    context.asContext(RealmContext.class).getRealm().asPath(), metadata.get());
            return newResultPromise(newActionResponse(json(object(field("importedEntities", entityIds)))));
        } catch (SAML2MetaException ex) {
            logger.error("An error occurred while importing a remote entity provider", ex);
            return new InternalServerErrorException("Unable to import SAML2 entity provider").asPromise();
        } catch (JAXBException ex) {
            logger.warn("Failed to parse standard metadata for import", ex);
            return new BadRequestException("Unable to parse standard metadata").asPromise();
        }
    }

    /**
     * Returns the JSON schema associated with remote entity providers.
     *
     * @return The remote entity provider's JSON schema.
     */
    @Action(operationDescription = @Operation(
            description = SAML2_ENTITIES_COLLECTION_PROVIDER + SCHEMA + ACTION_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = 403,
                            description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_403_DESCRIPTION)
            })
    )
    public Promise<ActionResponse, ResourceException> schema() {
        return newResultPromise(newActionResponse(toJsonValue(
                getClass().getResourceAsStream("/remoteSamlEntityProvider.schema.json"))));
    }

    @Override
    protected void assertValidEntityLocation(JaxbEntity jaxbEntity) throws BadRequestException {
        if (jaxbEntity.isHosted()) {
            throw new BadRequestException("Hosted entity requested from remote endpoint");
        }
    }

    @Override
    protected void replenishJaxbEntity(JaxbEntity jaxbEntity, JsonValue jsonContent, Context context)
            throws JsonProcessingException, BadRequestException {
        RemoteSaml2EntityProvider entityProvider = jsonAsEntityProvider(jsonContent, RemoteSaml2EntityProvider.class);
        boolean updateIdp = canEnrichRole(jaxbEntity, entityProvider.getIdentityProvider(), IDP);
        boolean updateSp = canEnrichRole(jaxbEntity, entityProvider.getServiceProvider(), SP);
        if (updateIdp) {
            replenishJaxbRole(jaxbEntity, entityProvider.getIdentityProvider(), IDP, ROOT);
        }
        if (updateSp) {
            replenishJaxbRole(jaxbEntity, entityProvider.getServiceProvider(), SP, ROOT);
        }
    }

    private boolean canEnrichRole(JaxbEntity jaxbEntity, Object jsonRole, Saml2EntityRole role)
            throws BadRequestException {
        if (jaxbEntity.hasRole(role)) {
            if (jsonRole == null) {
                throw new BadRequestException(String.format("Cannot remove role (%s) from remote entity", role));
            }
            return true;
        }
        if (jsonRole != null) {
            throw new BadRequestException(String.format("Cannot add role (%s) to remote entity", role));
        }
        return false;
    }

    @Override
    protected Object convertToJsonEntity(JaxbEntity jaxbEntity) {
        RemoteSaml2EntityProvider jsonEntity = new RemoteSaml2EntityProvider();
        jsonEntity.setEntityId(jaxbEntity.getStandardMetadata().getValue().getEntityID());
        enrichRole(jaxbEntity, Saml2EntityRole.IDP, IdentityProvider::new, jsonEntity::setIdentityProvider);
        enrichRole(jaxbEntity, Saml2EntityRole.SP, ServiceProvider::new, jsonEntity::setServiceProvider);
        return jsonEntity;
    }
}
