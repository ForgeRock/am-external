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
package org.forgerock.openam.federation.rest;

import static com.sun.identity.saml2.common.SAML2Constants.PROTOCOL_NAMESPACE;
import static com.sun.identity.saml2.common.SAML2Utils.getSAML2MetaManagerWithToken;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.openam.forgerockrest.utils.ServerContextUtils.getTokenFromContext;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_401_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_403_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ERROR_404_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SAML2_ENTITIES_COLLECTION_PROVIDER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SAML2_ENTITY_ROLES_REQUEST_HANDLER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;
import static org.forgerock.openam.rest.RestUtils.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Update;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.ObjectEnricher;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.meta.Saml2AuthorizationMetaException;
import org.forgerock.services.context.Context;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * This generic resource implementation allows management of SAML2 entity providers.
 */
public abstract class Saml2EntitiesCollectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(Saml2EntitiesCollectionProvider.class);
    private final ObjectEnricher objectEnricher;
    private final ObjectMapper objectMapper;
    private static final JsonPointer SECRET_ID_PTR =
            new JsonPointer("assertionContent/signingAndEncryption/secretIdAndAlgorithms/secretIdIdentifier");
    private static final Pattern SECRET_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]*$");

    /**
     * Instantiates a new Saml 2 entities collection provider.
     *
     * @param objectEnricher the object enricher
     * @param objectMapper the object mapper
     */
    protected Saml2EntitiesCollectionProvider(ObjectEnricher objectEnricher, ObjectMapper objectMapper) {
        this.objectEnricher = objectEnricher;
        this.objectMapper = objectMapper;
    }

    /**
     * Deletes the entity provider from the configuration (including all of its roles).
     *
     * @param context The CREST request context.
     * @param id The resource ID.
     * @return The promise of the operation result.
     */
    @Delete(
            operationDescription = @Operation(
                    description = SAML2_ENTITIES_COLLECTION_PROVIDER + DELETE_DESCRIPTION,
                    errors = {
                            @ApiError(
                                    code = 401,
                                    description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_401_DESCRIPTION
                            ),
                            @ApiError(
                                    code = NOT_FOUND,
                                    description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_404_DESCRIPTION
                            )
                    }
            )
    )
    public final Promise<ResourceResponse, ResourceException> delete(Context context, String id) {
        String realm = RealmContext.getRealm(context).asPath();
        try {
            JaxbEntity jaxbEntity = getJaxbEntity(context, id);
            assertValidEntityLocation(jaxbEntity);
            Promise<ResourceResponse, ResourceException> readResponse = entityAsPromise(jaxbEntity);
            String decodedEntityId = new String(Base64url.decode(id));
            getSAML2MetaManagerWithToken(
                    getTokenFromContext(context)).deleteEntityDescriptor(realm, decodedEntityId);
            return readResponse;
        } catch (Saml2AuthorizationMetaException e) {
            logger.error("The user is not authorised to delete SAML2 entity providers", e);
            return new ForbiddenException(e).asPromise();
        } catch (SAML2MetaException e) {
            logger.error("An error occurred while deleting an entity provider", e);
            return new InternalServerErrorException(e).asPromise();
        } catch (NotFoundException | BadRequestException e) {
            return e.asPromise();
        }
    }

    /**
     * Returns the entity provider role's JSON representation.
     *
     * @param context The CREST context.
     * @param request The read request.
     * @param entityId the entity id
     * @return The promise of the operation result.
     */
    @Read(operationDescription = @Operation(
            description = SAML2_ENTITIES_COLLECTION_PROVIDER + READ_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = 403,
                            description = SAML2_ENTITIES_COLLECTION_PROVIDER + ERROR_403_DESCRIPTION)})
    )
    public final Promise<ResourceResponse, ResourceException> read(Context context, ReadRequest request,
            String entityId) {
        try {
            JaxbEntity jaxbEntity = getJaxbEntity(context, entityId);
            assertValidEntityLocation(jaxbEntity);
            return entityAsPromise(jaxbEntity);
        } catch (SAML2MetaException e) {
            logger.error("An error occurred while getting the entity provider entity with id {}", entityId, e);
            return new InternalServerErrorException(e).asPromise();
        } catch (NotFoundException | BadRequestException nfe) {
            return nfe.asPromise();
        }
    }

    /**
     * Updates an existing SAML2 entity provider role with the provided configuration.
     *
     * @param context The CREST request context.
     * @param request The update request
     * @param entityId the entity id
     * @return The promise of the operation result.
     */
    @Update(operationDescription = @Operation(
            description = SAML2_ENTITY_ROLES_REQUEST_HANDLER + UPDATE_DESCRIPTION,
            errors = {
                    @ApiError(
                            code = 403,
                            description = SAML2_ENTITY_ROLES_REQUEST_HANDLER + ERROR_403_DESCRIPTION)})
    )
    public final Promise<ResourceResponse, ResourceException> update(
            Context context, UpdateRequest request, String entityId) {
        try {
            JaxbEntity jaxbEntity = getJaxbEntity(context, entityId);
            assertValidEntityLocation(jaxbEntity);
            validateSecretIdIdentifier(request.getContent());

            replenishJaxbEntity(jaxbEntity, request.getContent(), context);
            persistJaxbEntity(context, jaxbEntity);

            JsonValue result = entityProviderAsJson(convertToJsonEntity(getJaxbEntity(context, entityId)));
            return Responses.newResourceResponse(entityId,
                    String.valueOf(result.getObject().hashCode()), result).asPromise();
        } catch (ResourceException ex) {
            logger.error(ex.getMessage(), ex);
            return ex.asPromise();
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage(), ex);
            return new BadRequestException("Invalid input at " + ex.getLocation()).asPromise();
        } catch (Saml2AuthorizationMetaException ex) {
            logger.error("The user is not authorised to update SAML2 entity providers", ex);
            return new ForbiddenException(ex).asPromise();
        } catch (SAML2Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new InternalServerErrorException(ex).asPromise();
        }
    }

    /**
     * Ensures that the crud operation is performed for the correct entity location(i.e. hosted, remote).
     *
     * @param jaxbEntity The entity.
     * @throws BadRequestException when the request is for the wrong location.
     */
    protected abstract void assertValidEntityLocation(JaxbEntity jaxbEntity) throws BadRequestException;

    /**
     * Converts the SAML2 entity provider (standard and extended metadata) into a single POJO (hosted or remote) to
     * represent the configuration in JSON.
     *
     * @param jaxbEntity The SAML2 entity provider's configuration.
     * @return The enriched POJO containing the settings.
     */
    protected abstract Object convertToJsonEntity(JaxbEntity jaxbEntity);

    /**
     * Update a SAML2 entity provider.
     *
     * @param jaxbEntity The SAML2 entity provider.
     * @param content The CREST request content.
     * @param context The CREST request context.
     * @throws SAML2Exception on failing to process SAML2 request.
     * @throws JsonProcessingException on failing to process JSON.
     * @throws ResourceException on failing to handle resource.
     */
    protected abstract void replenishJaxbEntity(JaxbEntity jaxbEntity, JsonValue content, Context context)
            throws SAML2Exception, JsonProcessingException, ResourceException;

    /**
     * Returns the entity provider's JSON resource representation as a promise.
     *
     * @param jaxbEntity The JAXB entity to return as resource response.
     * @return The promise of the JSON representation of the JAXB entity.
     */
    protected final Promise<ResourceResponse, ResourceException> entityAsPromise(JaxbEntity jaxbEntity) {
        Object entityProvider = convertToJsonEntity(jaxbEntity);
        JsonValue result = entityProviderAsJson(entityProvider);
        return newResultPromise(newResourceResponse(Base64url.encode(
                jaxbEntity.getStandardMetadata().getValue().getEntityID().getBytes(UTF_8)), result));
    }

    /**
     * Validates the secret id identifier values in the request json payload.
     *
     * @param payload The request json payload.
     * @throws BadRequestException When secret id identifier has non permitted characters.
     */
    protected void validateSecretIdIdentifier(JsonValue payload) throws BadRequestException {
        List<String> secretIdIdentifiers = getSecretIdIdentifiers(payload);
        if (!secretIdIdentifiers.isEmpty() && secretIdIdentifiers.stream()
                .noneMatch(i -> SECRET_ID_PATTERN.matcher(i).matches())) {
            throw new BadRequestException("Invalid character present in Secret ID Identifier");
        }
    }

    private List<String> getSecretIdIdentifiers(JsonValue payload) {
        List<String> secretIdIdentifiers = new ArrayList<>();
        for (JsonValue schemaEntry : payload) {
            if (schemaEntry.isMap() && schemaEntry.get(SECRET_ID_PTR) != null) {
                secretIdIdentifiers.add(schemaEntry.get(SECRET_ID_PTR).asString());
            }
        }
        return secretIdIdentifiers;
    }

    private void persistJaxbEntity(Context context, JaxbEntity jaxbEntity) throws SAML2MetaException {
        String realm = RealmContext.getRealm(context).asPath();
        SAML2MetaManager saml2MetaManager = getSAML2MetaManagerWithToken(getTokenFromContext(context));
        saml2MetaManager.setEntityDescriptor(realm, jaxbEntity.getStandardMetadata());
        saml2MetaManager.setEntityConfig(realm, jaxbEntity.getExtendedMetadata());
    }

    /**
     * Convert an entity provider to a JsonValue object.
     *
     * @param entityProvider the entity provider object.
     * @return the converted entity provider as a JsonValue object.
     */
    protected JsonValue entityProviderAsJson(Object entityProvider) {
        return json(objectMapper.convertValue(entityProvider, new TypeReference<>() { }));
    }

    /**
     * Convert a JsonValue object to a provided entity provider object.
     *
     * @param jsonValue The JsonValue object.
     * @param clazz The entity provider class to be converted to.
     * @param <T> The type of the entity provider POJO.
     * @return the convertedJsonValue as a JsonValue object.
     * @throws JsonProcessingException on failing to read the JsonValue as the provided entity provider class.
     */
    protected final <T> T jsonAsEntityProvider(JsonValue jsonValue, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(jsonValue.toString(), clazz);
    }

    private JAXBElement<BaseConfigType> getExtendedJaxbRole(
            EntityConfigType extendedJaxbEntity, Saml2EntityRole role) {
        return extendedJaxbEntity.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig()
                .stream()
                .filter(role.matchesExtendedJaxbRole())
                .findFirst()
                .orElseGet(() -> getBaseConfigTypeJAXBElement(extendedJaxbEntity, role));
    }

    private JAXBElement<BaseConfigType> getBaseConfigTypeJAXBElement(EntityConfigType extendedJaxbEntity,
            Saml2EntityRole role) {
        JAXBElement<BaseConfigType> extendedJaxbRole = role.createNewExtendedMetadataRole();
        extendedJaxbEntity.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig().add(extendedJaxbRole);
        return extendedJaxbRole;
    }

    /**
     * Get a SAML2 entity from the CREST request context.
     *
     * @param context The CREST request context.
     * @param resourceId The resource ID (Base64url encoded entity ID).
     * @return The {@link JaxbEntity} representing the entity provider's standard and extended settings.
     * @throws SAML2MetaException If there was an error while retrieving the entity provider settings.
     * @throws NotFoundException If the entity provider with the provided ID could not be found.
     */
    protected final JaxbEntity getJaxbEntity(Context context, String resourceId)
            throws SAML2MetaException, NotFoundException {
        String decodedEntityId = new String(Base64url.decode(resourceId));
        String realm = RealmContext.getRealm(context).asPath();

        SAML2MetaManager saml2MetaManager = getSAML2MetaManagerWithToken(getTokenFromContext(context));

        EntityDescriptorElement entityDescriptor = saml2MetaManager.getEntityDescriptor(realm, decodedEntityId);
        if (entityDescriptor == null) {
            throw new NotFoundException("Cannot find SAML2 entity");
        }

        EntityConfigElement entityConfigElement = saml2MetaManager.getEntityConfig(realm, decodedEntityId);
        if (entityConfigElement == null) {
            ObjectFactory objectFactory = new ObjectFactory();
            EntityConfigType entityConfig = objectFactory.createEntityConfigType();
            entityConfig.setHosted(false);
            entityConfig.setEntityID(decodedEntityId);
            entityConfigElement = objectFactory.createEntityConfigElement(entityConfig);
        }

        return new JaxbEntity(entityDescriptor, entityConfigElement);
    }

    private void replenishStandardData(JaxbEntity jaxbEntity, Object jsonRole,
            Saml2EntityRole role, EnricherContext enricherContext) {
        RoleDescriptorType jaxbRole = role.getStandardMetadata(jaxbEntity.getStandardMetadata());
        if (!jaxbRole.getProtocolSupportEnumeration().contains(PROTOCOL_NAMESPACE)) {
            jaxbRole.getProtocolSupportEnumeration().add(PROTOCOL_NAMESPACE);
        }
        objectEnricher.replenish(jaxbRole, jsonRole, enricherContext);
    }

    private <T extends JAXBElement<BaseConfigType>> void replenishExtendedData(JaxbEntity jaxbEntity, Object jsonRole,
            Saml2EntityRole role, EnricherContext enricherContext) {
        EntityConfigElement extendedConfig = jaxbEntity.getExtendedMetadata();
        Map<String, List<String>> extendedData = role.getExtendedMetadata(extendedConfig);
        objectEnricher.replenish(extendedData, jsonRole, enricherContext);
        JAXBElement<BaseConfigType> extendedJaxbRole = getExtendedJaxbRole(extendedConfig.getValue(), role);
        repopulateExtendedJaxbRole(extendedJaxbRole, extendedData);
    }

    private void repopulateExtendedJaxbRole(JAXBElement<BaseConfigType> extendedJaxbRole,
            Map<String, List<String>> extendedData) {
        extendedJaxbRole.getValue().getAttribute().clear();
        List<AttributeType> types = extendedData.entrySet().stream()
                .map(entry -> createAttributeType(entry.getKey(), entry.getValue()))
                .collect(toList());
        extendedJaxbRole.getValue().getAttribute().addAll(types);
        final String metaAlias = "metaAlias";
        if (extendedData.get(metaAlias) != null) {
            extendedJaxbRole.getValue().setMetaAlias(extendedData.get(metaAlias).get(0));
        }
    }

    /**
     * Replenish standard and extended JAXB metadata.
     *
     * @param jaxbEntity the JAXB entity
     * @param jsonRole the JSON-defined role
     * @param role the SAML role
     * @param enricherContext the enricher context
     */
    protected final void replenishJaxbRole(JaxbEntity jaxbEntity, Object jsonRole, Saml2EntityRole role,
            EnricherContext enricherContext) {
        replenishStandardData(jaxbEntity, jsonRole, role, enricherContext);
        replenishExtendedData(jaxbEntity, jsonRole, role, enricherContext);
    }

    /**
     * Enriches a single entity role in the entity provider POJO based on the provided details.
     *
     * @param jaxbEntity The SAML2 entity provider's configuration.
     * @param role The entity role that needs to be enriched.
     * @param jsonRoleSupplier The supplier for the entity role's POJO representation.
     * @param jsonEntityConsumer The setter of the entity provider's POJO so that the role can be added to the
     * provider.
     * @param <T> The type of the entity provider role's POJO.
     */
    protected final <T> void enrichRole(JaxbEntity jaxbEntity,
            Saml2EntityRole role, Supplier<T> jsonRoleSupplier, Consumer<T> jsonEntityConsumer) {

        jaxbEntity.findRole(role).ifPresent(jaxbRole -> {
            T jsonRole = jsonRoleSupplier.get();

            // Enrich object with standard role data
            objectEnricher.enrich(jsonRole, jaxbRole, ROOT);
            // Enrich object with extended data
            objectEnricher.enrich(jsonRole, role.getExtendedMetadata(jaxbEntity.getExtendedMetadata()), ROOT);
            jsonEntityConsumer.accept(jsonRole);
        });
    }

    private AttributeType createAttributeType(String name, List<String> value) {
        AttributeType type = new AttributeType();
        type.setName(name);
        type.getValue().addAll(value);
        return type;
    }
}
