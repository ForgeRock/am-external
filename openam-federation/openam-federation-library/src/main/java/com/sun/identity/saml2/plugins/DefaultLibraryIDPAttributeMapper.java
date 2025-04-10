/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DefaultLibraryIDPAttributeMapper.java,v 1.3 2009/11/30 21:11:08 exu Exp $
 *
 * Portions Copyrighted 2013-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins;

import static org.forgerock.openam.utils.AttributeUtils.isBinaryAttribute;
import static org.forgerock.openam.utils.AttributeUtils.isStaticAttribute;
import static org.forgerock.openam.utils.AttributeUtils.removeBinaryAttributeFlag;
import static org.forgerock.openam.utils.AttributeUtils.removeStaticAttributeFlag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class <code>DefaultLibraryIDPAttributeMapper</code> implements the
 * <code>IDPAttributeMapper</code> to return the SAML <code>Attribute</code>
 * objects that may be inserted in the SAML Assertion.
 * This IDP attribute mapper reads the attribute map configuration defined
 * in the hosted IDP configuration and construct the SAML
 * <code>Attribute</code> objects. If the mapped values are not present in
 * the data store, this will try to read from the Single sign-on token.
 * <p>
 * Supports attribute mappings defined as:
 *
 * [NameFormatURI|]SAML ATTRIBUTE NAME=["]LOCAL NAME["][;binary]
 *
 * where [] elements are optional.
 *
 * Using "" (double quotes) around the LOCAL NAME will turn it into a static value.
 *
 * Adding ;binary at the end of the LOCAL NAME will indicate that this attribute should be treated as binary and Base64
 * encoded.
 * <p>
 * Examples:
 * <p>
 * <code>
 * email=mail
 * </code>
 * will map the local attribute called mail onto a SAML attribute called email.
 * <p>
 * <code>
 * urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:cn=cn
 * </code>
 * will map the local attribute called cn onto a SAML attribute called
 * urn:mace:dir:attribute-def:cn with a name format of urn:oasis:names:tc:SAML:2.0:attrname-format:uri
 * <p>
 * <code>
 * partnerID="staticPartnerIDValue"
 * </code>
 * will add a static SAML attribute called partnerID with a value of staticPartnerIDValue
 * <p>
 * <code>
 * urn:oasis:names:tc:SAML:2.0:attrname-format:uri|nameID="staticNameIDValue"
 * </code>
 * will add a static SAML attribute called nameID with a value of staticNameIDValue
 * with a name format of urn:oasis:names:tc:SAML:2.0:attrname-format:uri
 *<p>
 *<code>
 * objectGUID=objectGUID;binary
 *</code>
 * will map the local binary attribute called objectGUID onto a SAML attribute called objectGUID Base64 encoded.
 *<p>
 *<code>
 * urn:oasis:names:tc:SAML:2.0:attrname-format:uri|objectGUID=objectGUID;binary
 *</code>
 * will map the local binary attribute called objectGUID onto a SAML attribute called objectGUID Base64 encoded with a
 * name format of urn:oasis:names:tc:SAML:2.0:attrname-format:uri.
 */
public class DefaultLibraryIDPAttributeMapper extends DefaultAttributeMapper implements IDPAttributeMapper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLibraryIDPAttributeMapper.class);

    private final AttributeMapperPluginHelper pluginHelper;

    private final ValidationHelper validationHelper;

    /**
     * Constructor
     */
    public DefaultLibraryIDPAttributeMapper() {
        this.pluginHelper = new AttributeMapperPluginHelper();
        this.validationHelper = new ValidationHelper();
    }

    /**
     * Returns list of SAML <code>Attribute</code> objects for the 
     * IDP framework to insert into the generated <code>Assertion</code>.
     * 
     * @param session Single sign-on session.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @param realm name of the realm.
     * @exception SAML2Exception if any failure.
     */
    public List getAttributes(Object session, String hostEntityID, String remoteEntityID, String realm)
            throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostEntityID);
        validationHelper.validateSession(session);

        String debugMethod = "DefaultLibraryIDPAttributeMapper.getAttributes: ";

        try {
            if (!SessionManager.getProvider().isValid(session)) {
                logger.warn(debugMethod + "Invalid session.");
                return null;
            }

            Map<String, String> configMap = getConfigAttributeMap(realm, remoteEntityID, SP);
            logger.debug(debugMethod + "Remote SP attribute map = {}", configMap);
            if (CollectionUtils.isEmpty(configMap)) {
                configMap = getConfigAttributeMap(realm, hostEntityID, IDP);
                if (CollectionUtils.isEmpty(configMap)) {
                    logger.debug(debugMethod + "Configuration map is not defined.");
                    return null;
                }
                logger.debug(debugMethod + "Hosted IDP attribute map = {}", configMap);
            }

            List<Attribute> attributes = new ArrayList<>();
            Map<String, Set<String>> stringValueMap = null;
            Map<String, byte[][]> binaryValueMap = null;

            // Don't try to read the attributes from the datastore if the ignored profile is enabled in this realm.
            if (!isIgnoredProfile(session, realm)) {
                try {
                    // Resolve attributes to be read from the datastore.
                    Set<String> stringAttributes = new HashSet<>(configMap.size());
                    Set<String> binaryAttributes = new HashSet<>(configMap.size());
                    for (String localAttribute : configMap.values()) {
                        if (isStaticAttribute(localAttribute)) {
                            // skip over, handled directly in next step
                        } else if (isBinaryAttribute(localAttribute)) {
                            // add it to the list of attributes to treat as being binary
                            binaryAttributes.add(removeBinaryAttributeFlag(localAttribute));
                        } else {
                            stringAttributes.add(localAttribute);
                        }
                    }
                    if (!stringAttributes.isEmpty()) {
                        stringValueMap = pluginHelper.getAttributes(session, stringAttributes);
                    }
                    if (!binaryAttributes.isEmpty()) {
                        binaryValueMap = pluginHelper.getBinaryAttributes(session, binaryAttributes);
                    }
                } catch (DataStoreProviderException dse) {
                    logger.warn(debugMethod + "Error accessing the datastore.", dse);
                    //continue to check in ssotoken.
                }
            }

            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String samlAttribute =  entry.getKey();
                String localAttribute = entry.getValue();
                String nameFormat = null;
                // check if samlAttribute has format nameFormat|samlAttribute
                StringTokenizer tokenizer = new StringTokenizer(samlAttribute, "|");
                if (tokenizer.countTokens() > 1) {
                    nameFormat = tokenizer.nextToken();
                    samlAttribute = tokenizer.nextToken();
                }

                Set<String> attributeValues = null;
                if (isStaticAttribute(localAttribute)) {
                    localAttribute = removeStaticAttributeFlag(localAttribute);
                    // Remove the static flag before using it as the static value
                    attributeValues = CollectionUtils.asSet(localAttribute);
                    logger.debug(debugMethod + "Adding static value {} for attribute named {}",
                            localAttribute, samlAttribute);
                } else {
                    if (isBinaryAttribute(localAttribute)) {
                        // Remove the flag as not used for lookup
                        localAttribute = removeBinaryAttributeFlag(localAttribute);
                        attributeValues = pluginHelper.getBinaryAttributeValues(samlAttribute, localAttribute,
                                binaryValueMap);
                    } else {
                        if (stringValueMap != null && !stringValueMap.isEmpty()) {
                            attributeValues = stringValueMap.get(localAttribute);
                        } else {
                            logger.debug(debugMethod + "{} string value map was empty or null.", localAttribute);
                        }
                    }

                    // If all else fails, try to get the value from the users ssoToken
                    if (CollectionUtils.isEmpty(attributeValues)) {
                        logger.debug(debugMethod + "User profile does not have value for {}, checking SSOToken.",
                                localAttribute);
                        attributeValues =
                               CollectionUtils.asSet(SessionManager.getProvider().getProperty(session, localAttribute));
                    }
                }
                if (CollectionUtils.isEmpty(attributeValues)) {
                    logger.debug(debugMethod + "{} not found in user profile or SSOToken.", localAttribute);
                } else {
                    attributes.add(pluginHelper.createSAMLAttribute(samlAttribute, nameFormat, attributeValues));
                }
            }

            return attributes;      

        } catch (SessionException se) {
            logger.error(debugMethod + "Error with the user's session.", se);
            throw new SAML2Exception(se);
        }
    }

    /**
     * Return true if ignore profile is enabled for this realm.
     *
     * @param session SSOToken to check the profile creation attributes.
     * @param realm realm to check the profile creation attributes.
     * @return true in all cases in this implementation.
     */
    protected boolean isIgnoredProfile(Object session, String realm) {
        return true;
    }
}
