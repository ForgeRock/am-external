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
 * Copyright 2023 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins.scripted.wrappers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptHelper;

/**
 * This class exposes methods that are only intended to be used by IDP Attribute Mapper script types.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class IdpAttributeMapperHelperScriptWrapper {

    private final IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper;

    /**
     * Construct a new instance of {@link IdpAttributeMapperHelperScriptWrapper}.
     */
    public IdpAttributeMapperHelperScriptWrapper(IdpAttributeMapperScriptHelper idpAttributeMapperScriptHelper) {
        this.idpAttributeMapperScriptHelper = idpAttributeMapperScriptHelper;
    }

    /**
     * Check if a session is valid.
     * This is useful for toolkit clean-up thread.
     *
     * @param session Session object.
     * @return true if the session is valid.
     * @throws SessionException the session exception
     */
    public boolean isSessionValid(Object session) throws SessionException {
        return idpAttributeMapperScriptHelper.isSessionValid(session);
    }

    /**
     * Return the attribute map by parsing the configured map in hosted identity provider configuration.
     *
     * @param realm          realm name.
     * @param hostedEntityId the hosted entity identity.
     * @return a map of local attributes configuration map.
     * This map will have a key as the SAML attribute name and the value is the local attribute.
     * @throws SAML2Exception if any failure.
     */
    public Map<String, String> getHostedIDPConfigAttributeMap(String realm, String hostedEntityId)
            throws SAML2Exception {
        return idpAttributeMapperScriptHelper.getHostedIDPConfigAttributeMap(realm, hostedEntityId);
    }

    /**
     * Return the attribute map by parsing the configured map in remote service provider configuration
     *
     * @param realm          realm name.
     * @param remoteEntityId the remote entity identity.
     * @return a map of local attributes configuration map.
     * This map will have a key as the SAML attribute name and the value is the local attribute.
     * @throws SAML2Exception if any failure.
     */
    public Map<String, String> getRemoteSPConfigAttributeMap(String realm, String remoteEntityId)
            throws SAML2Exception {
        return idpAttributeMapperScriptHelper.getRemoteSPConfigAttributeMap(realm, remoteEntityId);
    }

    /**
     * Return true if ignore profile is enabled for this realm.
     *
     * @param session SSOToken to check the profile creation attributes.
     * @param realm   realm to check the profile creation attributes.
     * @return true if ignore profile is enabled, false otherwise.
     */
    public boolean isIgnoredProfile(Object session, String realm) {
        return idpAttributeMapperScriptHelper.isIgnoredProfile(session, realm);
    }

    /**
     * Return the property value of a session object.
     *
     * @param session  the session
     * @param property the property name
     * @return the property set
     * @throws SessionException on failing to get the datastore provider or reading the value from the datastore.
     */
    public List<String> getPropertySet(Object session, String property) throws SessionException {
        return List.copyOf(idpAttributeMapperScriptHelper.getPropertySet(session, property));
    }

    /**
     * Get attributes from a session.
     *
     * @param session   the session
     * @param attrNames the attr names
     * @return the attributes from session
     * @throws SAML2Exception             on failing to get the datastore provider and on failing to read attributes
     *                                    from the datastore provider.
     * @throws SessionException           on failing to get the session provider.
     * @throws DataStoreProviderException on failing to read the binary attributes from the datastore provider.
     */
    public Map<String, List<String>> getAttributes(Object session, List<String> attrNames)
            throws SAML2Exception, DataStoreProviderException, SessionException {
        return convertToMapOfLists(idpAttributeMapperScriptHelper.getAttributes(session, Set.copyOf(attrNames)));
    }

    /**
     * Get binary attributes from a session.
     *
     * @param session   the session
     * @param attrNames the attr names
     * @return the binary attributes
     * @throws SAML2Exception             on failing to get the datastore provider and on failing to read attributes
     *                                    from the datastore provider.
     * @throws SessionException           on failing to get the session provider.
     * @throws DataStoreProviderException on failing to read the binary attributes from the datastore provider.
     */
    public Map<String, byte[][]> getBinaryAttributes(Object session, List<String> attrNames) throws SAML2Exception,
            DataStoreProviderException, SessionException {
        return idpAttributeMapperScriptHelper.getBinaryAttributes(session, Set.copyOf(attrNames));
    }

    /**
     * For the given attributeName, return true if it is flagged as an attribute.
     *
     * @param attributeName The attributeName to check for the  flag
     * @return true if the attributeName is flagged as an attribute
     */
    public boolean isStaticAttribute(String attributeName) {
        return idpAttributeMapperScriptHelper.isStaticAttribute(attributeName);
    }

    /**
     * Return the attributeName without the  flag if it is included.
     *
     * @param attributeName The attribute name with the  flag included
     * @return The attributeName with the  flag removed
     */
    public String removeStaticAttributeFlag(String attributeName) {
        return idpAttributeMapperScriptHelper.removeStaticAttributeFlag(attributeName);
    }

    /**
     * For the given attributeName, return true if it is flagged as a binary attribute.
     *
     * @param attributeName The attributeName to check for the binary flag
     * @return true if the attributeName is flagged as a binary attribute
     */
    public boolean isBinaryAttribute(String attributeName) {
        return idpAttributeMapperScriptHelper.isBinaryAttribute(attributeName);
    }

    /**
     * Return the attributeName without the binary flag if it is included.
     *
     * @param attributeName The attribute name with the binary flag included
     * @return The attributeName with the binary flag removed
     */
    public String removeBinaryAttributeFlag(String attributeName) {
        return idpAttributeMapperScriptHelper.removeBinaryAttributeFlag(attributeName);
    }

    /**
     * Create a SAML {@link Attribute} object.
     *
     * @param name       attribute name
     * @param nameFormat Name format of the attribute
     * @param values     attribute values
     * @return SAML Attribute element
     */
    public Attribute createSAMLAttribute(String name, String nameFormat, List<String> values) throws SAML2Exception {
        return idpAttributeMapperScriptHelper.createSAMLAttribute(name, nameFormat, Set.copyOf(values));
    }

    /**
     * Return a List of Base64 encoded String values that represent the binary attribute values.
     *
     * @param samlAttribute  the SAML attribute that will be assigned these values
     * @param localAttribute the attribute to find in the map
     * @param binaryValueMap the map of binary values for the all binary attributes
     * @return List of Base64 encoded String values for the given binary attribute values
     */
    public List<String> getBinaryAttributeValues(String samlAttribute, String localAttribute,
            Map<String, byte[][]> binaryValueMap) {
        return List.copyOf(
                idpAttributeMapperScriptHelper.getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap));
    }

    private Map<String, List<String>> convertToMapOfLists(Map<String, Set<String>> attributes) {
        return attributes
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }
}
