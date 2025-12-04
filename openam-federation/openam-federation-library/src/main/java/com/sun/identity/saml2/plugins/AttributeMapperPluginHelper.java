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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * A helper class with common functions used in attribute mapping.
 */
public class AttributeMapperPluginHelper {

    private static final Logger logger = LoggerFactory.getLogger(AttributeMapperPluginHelper.class);
    private static ResourceBundle bundle = SAML2Utils.bundle;
    private DataStoreProvider dsProvider = null;

    private DataStoreProvider getDsProvider() throws SAML2Exception {
        if (dsProvider == null) {
            dsProvider = SAML2Utils.getDataStoreProvider();
        }
        return dsProvider;
    }

    /**
     * Create a SAML {@link Attribute} object.
     *
     * @param name       attribute name.
     * @param nameFormat Name format of the attribute
     * @param values     attribute values.
     * @return SAML <code>Attribute</code> element.
     * @throws SAML2Exception if any failure.
     */
    public Attribute createSAMLAttribute(String name, String nameFormat, Set<String> values) throws SAML2Exception {

        if (name == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullInput"));
        }

        AssertionFactory factory = AssertionFactory.getInstance();
        Attribute attribute = factory.createAttribute();

        attribute.setName(name);
        if (nameFormat != null) {
            attribute.setNameFormat(nameFormat);
        }
        if (values != null && !values.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (String value : values) {
                list.add(XMLUtils.escapeSpecialCharacters(value));
            }
            attribute.setAttributeValueString(list);
        }

        return attribute;
    }

    /**
     * Return a Set of Base64 encoded String values that represent the binary attribute values.
     *
     * @param localAttribute the attribute to find in the map.
     * @param samlAttribute  the SAML attribute that will be assigned these values
     * @param binaryValueMap the map of binary values for the all binary attributes.
     * @return Set of Base64 encoded String values for the given binary attribute values.
     */
    public Set<String> getBinaryAttributeValues(String samlAttribute, String localAttribute,
            Map<String, byte[][]> binaryValueMap) {

        Set<String> result = null;

        // Expect to find the value in the binary Map
        if (binaryValueMap == null || binaryValueMap.isEmpty()) {
            logger.debug("IDPAttributeMapper.getBinaryAttributeValues: {} was flagged as a binary but binary" +
                            " value map was empty or null", localAttribute);
        } else {
            byte[][] values = binaryValueMap.get(localAttribute);
            if (values == null || values.length <= 0) {
                logger.debug("IDPAttributeMapper.getBinaryAttributeValues: {} was flagged as a binary but no value" +
                                " was found", localAttribute);
            } else {
                // Base64 encode the binary values before they are added as an attribute value
                result = new HashSet<>(values.length);
                for (byte[] value : values) {
                    result.add(Base64.encode(value));
                }
                logger.debug("IDPAttributeMapper.getBinaryAttributeValues: adding {} as a binary for attribute" +
                                " named {}", localAttribute, samlAttribute);
            }
        }

        return result;
    }

    /**
     * Get attributes from a session.
     *
     * @param session   the session
     * @param attrNames the attribute names
     * @return the attributes from session
     * @throws SAML2Exception             on failing to get the datastore provider and on failing to read attributes
     *                                    from the datastore provider.
     * @throws SessionException           on failing to get the session provider.
     * @throws DataStoreProviderException on failing to read the attributes from the datastore provider.
     */
    public Map<String, Set<String>> getAttributes(Object session, Set<String> attrNames) throws SAML2Exception,
            SessionException, DataStoreProviderException {
        return getDsProvider().getAttributes(SessionManager.getProvider().getPrincipalName(session), attrNames);
    }

    /**
     * Get binary attributes from a session.
     *
     * @param session   the session
     * @param attrNames the attribute names
     * @return the binary attributes
     * @throws SAML2Exception             on failing to get the datastore provider and on failing to read attributes
     *                                    from the datastore provider.
     * @throws SessionException           on failing to get the session provider.
     * @throws DataStoreProviderException on failing to read the binary attributes from the datastore provider.
     */
    public Map<String, byte[][]> getBinaryAttributes(Object session, Set<String> attrNames) throws SAML2Exception,
            SessionException, DataStoreProviderException {
        return getDsProvider().getBinaryAttributes(SessionManager.getProvider().getPrincipalName(session),
                attrNames);
    }

}
