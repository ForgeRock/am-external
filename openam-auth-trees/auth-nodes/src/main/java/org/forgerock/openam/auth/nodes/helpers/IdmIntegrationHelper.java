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
 * Copyright 2019-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.helpers;

import static org.forgerock.http.protocol.Status.UNAUTHORIZED;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.PROPERTIES;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.normalizeAttributeName;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.KbaConfig;
import org.forgerock.openam.integration.idm.TermsAndConditionsConfig;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.query.QueryFilter;

/**
 * Helper methods for conversion of ResourceExceptions to NodeProcessExceptions.
 */
public final class IdmIntegrationHelper {

    private IdmIntegrationHelper() {
        // prevent instantiation
    }

    /**
     * Fetch an object from IDM.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param objectId The ID of the object to be retrieved
     * @return The fetched object
     * @throws NodeProcessException on failure to retrieve the object from IDM
     */
    public static Optional<JsonValue> getObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, Optional<String> objectId)
            throws NodeProcessException {
        if (!objectId.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(idmIntegrationService.getObject(realm, preferredLocales, identityResource,
                    objectId.get()));
        } catch (ResourceException e) {
            if (e.getCode() == NotFoundException.NOT_FOUND) {
                return Optional.empty();
            }
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetch an object from IDM.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm the realm for the current auth tree
     * @param preferredLocales the preferred locales
     * @param identityResource the IDM identity resource to be retrieved
     * @param identityAttribute the attribute used to identify the object being retrieved
     * @param objectId the identity of the object
     * @return the retrieved object
     * @throws NodeProcessException on failure to retrieve the object
     */
    public static Optional<JsonValue> getObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String identityAttribute,
            Optional<String> objectId) throws NodeProcessException {
        if (!objectId.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(idmIntegrationService.getObject(realm, preferredLocales, identityResource,
                    identityAttribute, objectId.get()));
        } catch (ResourceException e) {
            if (e.getCode() == NotFoundException.NOT_FOUND) {
                return Optional.empty();
            }
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetch an object from IDM using a query with specified fields.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param identityAttribute The attribute to query for the objectId
     * @param objectId The ID of the object to be retrieved
     * @param fields The object fields to retrieve
     * @return The fetched object
     * @throws NodeProcessException on failure to retrieve the object from IDM
     */
    public static Optional<JsonValue> getObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String identityAttribute,
            Optional<String> objectId, String... fields) throws NodeProcessException {
        if (!objectId.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(idmIntegrationService.getObject(realm, preferredLocales, identityResource,
                    identityAttribute, objectId.get(), fields));
        } catch (ResourceException e) {
            if (e.getCode() == NotFoundException.NOT_FOUND) {
                return Optional.empty();
            }
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetch an object from IDM using a query with specified fields.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param filter the query filter to be applied
     * @param fields The object fields to retrieve
     * @return The fetched object
     * @throws NodeProcessException on failure to retrieve the object from IDM
     */
    public static Optional<JsonValue> getObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, QueryFilter filter, String... fields)
            throws NodeProcessException {

        try {
            return Optional.of(idmIntegrationService.getObject(realm, preferredLocales, identityResource, filter,
                    fields));
        } catch (ResourceException e) {
            if (e.getCode() == NotFoundException.NOT_FOUND) {
                return Optional.empty();
            }
            throw new NodeProcessException(e);
        }
    }

    /**
     * Creates an object in IDM.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be created
     * @param newObject The object being created
     * @return the object that was created
     * @throws NodeProcessException on failure to create the object in IDM
     */
    public static JsonValue createObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, JsonValue newObject)
            throws NodeProcessException {
        try {
            return idmIntegrationService.createObject(realm, preferredLocales, identityResource, newObject);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Patches an object in IDM as the client.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be patched
     * @param objectId The ID of the object to patch
     * @param itemsToPatch map of fields and values to patch
     * @throws NodeProcessException on failure to patch the object in IDM
     */
    public static void patchObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String objectId,
            Map<String, Object> itemsToPatch) throws NodeProcessException {
        patchObject(idmIntegrationService, realm, preferredLocales, identityResource, objectId, itemsToPatch, null);
    }

    /**
     * Patches an object in IDM as the specified subject.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be patched
     * @param objectId The ID of the object to patch
     * @param itemsToPatch map of fields and values to patch
     * @param subject the subject to perform patch as
     * @throws NodeProcessException on failure to patch the object in IDM
     */
    public static void patchObject(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String objectId,
            Map<String, Object> itemsToPatch, String subject) throws NodeProcessException {
        try {
            idmIntegrationService.patchObject(realm, preferredLocales, identityResource, objectId, itemsToPatch,
                    subject);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Retrieve the IDM object schema for a given identity resource.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm the realm for the current auth tree
     * @param preferredLocales the preferred locales
     * @param identityResource the identity resource for which schema is to be retrieved
     * @return the retrieved schema
     * @throws NodeProcessException on failure to retrieve the schema
     */
    public static JsonValue getSchema(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource) throws NodeProcessException {
        try {
            return idmIntegrationService.getSchema(realm, preferredLocales, identityResource);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetch the policy validation requirements for a managed object.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for which policies are requested
     * @return The curated list of policy data
     * @throws NodeProcessException on failure to interact with the IDM policy service
     */
    public static JsonValue getValidationRequirements(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource) throws NodeProcessException {
        try {
            return idmIntegrationService.getValidationRequirements(realm, preferredLocales, identityResource);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Validate a set of inputs using IDM's policy engine according to the object schema in IDM.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for which validation is requested
     * @param inputFields The object containing the inputs to be validated
     * @return The result of the validation request
     * @throws NodeProcessException on failure to interact with the IDM policy service
     */
    public static JsonValue validateInput(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, JsonValue inputFields)
            throws NodeProcessException {
        try {
            return idmIntegrationService.validateInput(realm, preferredLocales, identityResource, inputFields);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Validate a set of inputs using IDM's policy engine according to the object schema in IDM.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for which validation is requested
     * @param inputFields The object containing the inputs to be validated
     * @param properties A list of the properties within inputFields that should be validated
     * @return The result of the validation request
     * @throws NodeProcessException on failure to interact with the IDM policy service
     */
    public static JsonValue validateInput(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, JsonValue inputFields, Set<String> properties)
            throws NodeProcessException {
        try {
            return idmIntegrationService.validateInput(realm, preferredLocales, identityResource, inputFields,
                    properties);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Increments the login count of the specified object.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be incremented
     * @param objectId The ID of the object to increment
     * @throws NodeProcessException on failure to increment login count
     */
    public static void incrementLoginCount(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String objectId)
            throws NodeProcessException {
        try {
            idmIntegrationService.incrementLoginCount(realm, preferredLocales, identityResource, objectId);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Retrieves the login count of the specified object.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param objectId The ID of the object to retrieve
     * @return the login count
     * @throws NodeProcessException on failure to retrieve login count
     */
    public static JsonValue retrieveLoginCount(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String objectId)
            throws NodeProcessException {
        try {
            return idmIntegrationService.retrieveLoginCount(realm, preferredLocales, identityResource, objectId);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Retrieves the create date of the specified object.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param objectId The ID of the object to retrieve
     * @return the create date
     * @throws NodeProcessException on failure to retrieve create date
     */
    public static JsonValue retrieveCreateDate(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String objectId)
            throws NodeProcessException {
        try {
            return idmIntegrationService.retrieveCreateDate(realm, preferredLocales, identityResource, objectId);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Update the terms of the specified object.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be updated
     * @param objectId The ID of the object to updated
     * @param terms the new accepted terms information
     * @throws NodeProcessException on failure to update the terms
     */
    public static void updateTermsAccepted(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String objectId, JsonValue terms)
            throws NodeProcessException {
        try {
            idmIntegrationService.updateTermsAccepted(realm, preferredLocales, identityResource, objectId, terms);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetch a set of IDM reconciliation mappings that require consent for a given identity resource.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for which mappings are requested
     * @return The list of zero or more IDM mappings that require consent for a given identity resource
     * @throws NodeProcessException on failure to retrieve the mappings from IDM
     */
    public static JsonValue getConsentMappings(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource) throws NodeProcessException {
        try {
            return idmIntegrationService.getConsentMappings(realm, preferredLocales, identityResource);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetch the configuration for KBA questions and answers.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @return The configuration for KBA questions and answers
     * @throws NodeProcessException on failure to retrieve the KBA configuration from IDM
     */
    public static KbaConfig getKbaConfig(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales) throws NodeProcessException {
        try {
            return idmIntegrationService.getKbaConfig(realm, preferredLocales);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetches the percentage of user-editable fields that are not empty.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param identityAttribute The attribute to query for the objectId
     * @param objectId The ID of the object to be retrieved
     * @return The object's completeness percentage
     * @throws NodeProcessException on failure to retrieve the completeness percentage from IDM
     */
    public static float getProfileCompleteness(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String identityAttribute, String objectId)
            throws NodeProcessException {
        try {
            return idmIntegrationService.getProfileCompleteness(realm, preferredLocales, identityResource,
                    identityAttribute, objectId);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetches the active terms and conditions.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm the realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @return the active configuration
     * @throws NodeProcessException on failure to retrieve the active terms
     */
    public static Optional<TermsAndConditionsConfig> getActiveTerms(IdmIntegrationService idmIntegrationService,
            Realm realm, PreferredLocales preferredLocales) throws NodeProcessException {
        try {
            return Optional.of(idmIntegrationService.getActiveTerms(realm, preferredLocales));
        } catch (ResourceException e) {
            if (e.getCode() == NotFoundException.NOT_FOUND) {
                return Optional.empty();
            }
            throw new NodeProcessException(e);
        }
    }

    /**
     * Fetches the an object's accepted Terms and Conditions.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param identityAttribute The attribute to query for the objectId
     * @param objectId The ID of the object to be retrieved
     * @return The object's accepted terms
     * @throws NodeProcessException on failure to retrieve the accepted terms from IDM
     */
    public static JsonValue getAcceptedTerms(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String identityResource, String identityAttribute, String objectId)
            throws NodeProcessException {
        try {
            return idmIntegrationService.getAcceptedTerms(realm, preferredLocales, identityResource, identityAttribute,
                    objectId);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Retrieve an attribute from the context.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param context the context potentially containing the attribute
     * @param attribute the attribute name
     * @return the retrieved attribute
     */
    public static Optional<JsonValue> getAttributeFromContext(IdmIntegrationService idmIntegrationService,
            TreeContext context, String attribute) {
        return idmIntegrationService.getAttributeFromContext(context, attribute);
    }

    /**
     * Retrieve the username value from the context.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param context the context potentially containing the username
     * @return the retrieved value
     */
    public static Optional<JsonValue> getUsernameFromContext(IdmIntegrationService idmIntegrationService,
            TreeContext context) {
        return idmIntegrationService.getUsernameFromContext(context);
    }

    /**
     * Convert an optional JsonValue to an optional String.
     *
     * @param json the JsonValue to convert
     * @return optional string
     */
    public static Optional<String> stringAttribute(Optional<JsonValue> json) {
        return json.isPresent() && json.get().isString() ? Optional.of(json.get().asString()) : Optional.empty();
    }

    /**
     * Returns a normalized list of properties within a given schema.
     *
     * @param schema object definition
     * @return normalized list of properties
     */
    public static List<String> listSchemaProperties(JsonValue schema) {
        return schema.get(PROPERTIES).stream()
                .flatMap(property -> property.isDefined(PROPERTIES)
                        ? listSchemaProperties(property).stream()
                        : Stream.of(normalizeAttributeName(property.getPointer())))
                .collect(Collectors.toList());
    }

    /**
     * Returns the best localized message.
     *
     * @param context the current context
     * @param localeSelector the locale selector
     * @param node the node for which the message is being translated
     * @param localisations the map of localized messages
     * @param defaultMessageKey the default message key to use if no translation found
     * @param <C> the type of node
     * @return the localized message
     */
    public static <C extends Node> String getLocalisedMessage(TreeContext context, LocaleSelector localeSelector,
            Class<C> node, Map<Locale, String> localisations, String defaultMessageKey) {
        PreferredLocales preferredLocales = context.request.locales;
        Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localisations.keySet());

        String message = null;
        if (bestLocale != null) {
            message = localisations.get(bestLocale);
        } else if (localisations.size() > 0) {
            message = localisations.get(localisations.keySet().iterator().next());
        }

        if (message == null) {
            ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(node.getName(), node.getClassLoader());
            return bundle.getString(defaultMessageKey);
        }
        return message;
    }

    /**
     * Returns a list of identity providers, including local authentication, associated with a managed object.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param locales the preferred locales
     * @param identityResource The identity resource for the object to be retrieved
     * @param identityAttribute The attribute to query for the objectId
     * @param identity The ID of the object to be retrieved
     * @param passwordAttribute The object attribute that stores the password for local authn
     * @return A list of authn providers associated with the object
     * @throws NodeProcessException on failure to identify or retrieve the object
     */
    public static List<String> getObjectIdentityProviders(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales locales, String identityResource, String identityAttribute, String identity,
            String passwordAttribute) throws NodeProcessException {
        try {
            return idmIntegrationService.getObjectIdentityProviders(realm, locales, identityResource, identityAttribute,
                    identity, passwordAttribute);
        } catch (ResourceException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Patches an object in IDM as the specified subject.
     *
     * @param idmIntegrationService the IdmIntegrationService
     * @param realm The realm of the requesting tree
     * @param preferredLocales the preferred locales
     * @param connectorName the name of the connector in IDM
     * @param objectType the objectType for the target object in the connector
     * @param username The username for passthrough authentication
     * @param password The password for passthrough authentication
     * @return True iff passthrough authentication was successful
     * @throws NodeProcessException on failure to test authentication
     */
    public static boolean passthroughAuth(IdmIntegrationService idmIntegrationService, Realm realm,
            PreferredLocales preferredLocales, String connectorName, String objectType, String username,
            String password) throws NodeProcessException {
        try {
            return idmIntegrationService.passthroughAuth(realm, preferredLocales, connectorName, objectType,
                    username, password);
        } catch (ResourceException e) {
            // when invalid credentials are provided, a 401 PermanentException is thrown
            if (e.getCode() == UNAUTHORIZED.getCode()) {
                return false;
            }
            throw new NodeProcessException(e);
        }
    }
}
