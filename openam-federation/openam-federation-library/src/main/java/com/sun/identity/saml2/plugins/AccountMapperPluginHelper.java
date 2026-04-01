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

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2InvalidUserException;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * A helper class with common functions used in account mapping.
 */
public class AccountMapperPluginHelper {

    private static final Logger logger = LoggerFactory.getLogger(AccountMapperPluginHelper.class);

    private DataStoreProvider dsProvider = null;
    private SAML2MetaManager metaManager = null;

    protected DataStoreProvider getDsProvider() throws SAML2Exception {
        if (dsProvider == null) {
            dsProvider = SAML2Utils.getDataStoreProvider();
        }
        return dsProvider;
    }

    private SAML2MetaManager getMetaManager() throws SAML2MetaException {
        if (metaManager == null) {
            metaManager = new SAML2MetaManager();
        }
        return metaManager;
    }

    public boolean useNameIDAsSPUserID(String realm, String entityID, String role) {
        return Boolean.parseBoolean(getAttribute(realm, entityID, SAML2Constants.USE_NAMEID_AS_SP_USERID, role));
    }

    public boolean isAutoFedEnabled(String realm, String entityID, String role) {
        return Boolean.parseBoolean(getAttribute(realm, entityID, SAML2Constants.AUTO_FED_ENABLED, role));
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     *
     * @param realm Realm to check the dynamical profile creation attributes.
     * @return <code>true</code> if dynamical profile creation or ignore profile is enabled, <code>false</code>
     * otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return SAML2PluginsUtils.isDynamicalOrIgnoredProfile(realm);
    }

    /**
     * Returns the attribute value configured in the given entity
     * SP or IDP configuration.
     *
     * @param realm         realm name.
     * @param entityID      hosted <code>EntityID</code>.
     * @param attributeName name of the attribute.
     */
    public String getAttribute(String realm, String entityID, String attributeName, String role) {
        if (realm == null || entityID == null || attributeName == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AccountMapperPluginHelper.getAttribute: " +
                        "null input parameters.");
            }
            return null;
        }

        try {
            JAXBElement<BaseConfigType> config = getSSOConfig(realm, entityID, role);
            Map<String, List<String>> attributes = SAML2MetaUtils.getAttributes(config);

            if (attributes == null || attributes.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("AccountMapperPluginHelper.getAttribute:" +
                            " attribute configuration is not defined for " +
                            "Entity " + entityID + " realm =" + realm + " role=" + role);
                }
                return null;
            }

            List<String> list = attributes.get(attributeName);
            if (list != null && !list.isEmpty()) {
                return list.iterator().next();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("AccountMapperPluginHelper.getAttribute: " +
                        attributeName + " is not configured.");
            }
            return null;

        } catch (SAML2MetaException sme) {
            if (logger.isWarnEnabled()) {
                logger.warn("AccountMapperPluginHelper.getAttribute:" +
                        "Meta Exception", sme);
            }
        }
        return null;
    }

    protected final JAXBElement<BaseConfigType> getSSOConfig(String realm, String entityID, String role)
            throws SAML2MetaException {
        if (IDP_ROLE.equals(role)) {
            return getMetaManager().getIDPSSOConfig(realm, entityID);
        } else {
            return getMetaManager().getSPSSOConfig(realm, entityID);
        }
    }

    /**
     * Returns user for the auto federate attribute.
     *
     * @param realm          Realm name.
     * @param entityID       Hosted <code>EntityID</code>.
     * @param assertion      <code>Assertion</code> from the identity provider.
     * @param defaultValue   Default value to return if no auto-federated user is found.
     * @param decryptionKeys Set of decryption keys to use for decrypting the NameID if it is encrypted.
     * @param role           The role of the entity, either SP or IDP.
     * @return Auto federation mapped user from the assertion auto federation <code>AttributeStatement</code>. if the
     * statement does not have the auto federation attribute then the NameID value will be used if use NameID as SP user
     * ID is enabled, otherwise null.
     * @throws SAML2Exception If there is an error during the process, such as decryption failure.
     */
    public String getAutoFedUser(String realm, String entityID, Assertion assertion, String defaultValue,
            Set<PrivateKey> decryptionKeys, String role) throws SAML2Exception {
        if (!isAutoFedEnabled(realm, entityID, role)) {
            if (logger.isDebugEnabled()) {
                logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation is disabled.");
            }
            return null;
        }

        String autoFedAttribute = getAttribute(realm, entityID, SAML2Constants.AUTO_FED_ATTRIBUTE, role);
        if (autoFedAttribute == null || autoFedAttribute.isEmpty()) {
            logger.error("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                    "Auto federation is enabled but the auto federation attribute is not configured.");
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation attribute is set to: "
                    + autoFedAttribute);
        }

        Set<String> autoFedAttributeValue = null;
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                        "Assertion does not have any attribute statements.");
            }
        } else {
            for (AttributeStatement statement : attributeStatements) {
                autoFedAttributeValue = getAttribute(statement, autoFedAttribute, decryptionKeys);
                if (autoFedAttributeValue != null && !autoFedAttributeValue.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                                "Found auto federation attribute value in Assertion: " + autoFedAttributeValue);
                    }
                    break;
                }
            }
        }

        if (autoFedAttributeValue == null || autoFedAttributeValue.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: Auto federation attribute is not specified"
                        + " as an attribute.");
            }
            if (!useNameIDAsSPUserID(realm, entityID, role)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: NameID as SP UserID was not enabled "
                            + " and auto federation attribute " + autoFedAttribute + " was not found in the Assertion");
                }
                return null;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: Trying now to autofederate with nameID"
                            + ", nameID =" + defaultValue);
                }
                autoFedAttributeValue = CollectionUtils.asSet(defaultValue);
            }
        }

        String autoFedMapAttribute = null;
        DefaultSPAttributeMapper attributeMapper = new DefaultSPAttributeMapper();
        Map<String, String> attributeMap = attributeMapper.getConfigAttributeMap(realm, entityID, SP_ROLE);
        if (attributeMap == null || attributeMap.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: attribute map is not configured.");
            }
        } else {
            autoFedMapAttribute = attributeMap.get(autoFedAttribute);
        }

        if (autoFedMapAttribute == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: " +
                        "Auto federation attribute map is not specified in config.");
            }
            // assume it is the same as the auto fed attribute name
            autoFedMapAttribute = autoFedAttribute;
        }

        for (String s : autoFedAttributeValue) {
            validateUserId(s);
        }

        try {
            Map<String, Set<String>> map = new HashMap<>(1);
            map.put(autoFedMapAttribute, autoFedAttributeValue);

            if (logger.isDebugEnabled()) {
                logger.debug("DefaultLibrarySPAccountMapper.getAutoFedUser: Search map: " + map);
            }

            String userId = getDsProvider().getUserID(realm, map);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            } else {
                // check dynamic profile creation or ignore profile, if enabled,
                // return auto-federation attribute value as uid
                if (isDynamicalOrIgnoredProfile(realm)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("DefaultLibrarySPAccountMapper: dynamical user creation or ignore profile " +
                                "enabled : uid=" + autoFedAttributeValue);
                    }
                    // return the first value as uid
                    return autoFedAttributeValue.iterator().next();
                }
            }
        } catch (DataStoreProviderException dse) {
            if (logger.isWarnEnabled()) {
                logger.warn("DefaultLibrarySPAccountMapper.getAutoFedUser: Datastore provider exception", dse);
            }
        }

        return null;
    }

    /**
     * Validates the user ID.
     *
     * @param userId User ID to validate.
     * @throws SAML2InvalidUserException if there was an error validating the user id.
     */
    public void validateUserId(String userId) throws SAML2InvalidUserException {
        DataStoreProvider dsProvider;
        try {
            dsProvider = getDsProvider();
        } catch (SAML2Exception e) {
            logger.error("AccountMapperPluginHelper.getDsProvider initialization failed", e);
            throw new SAML2InvalidUserException("Could not initialize DataStoreProvider");
        }
        try {
            if (dsProvider.isUsernameUniversalId(userId)) {
                throw new SAML2InvalidUserException("Invalid user");
            }
        } catch (DataStoreProviderException e) {
            logger.error("Invalid universalId username", e);
            throw new SAML2InvalidUserException("Invalid user");
        }
    }

    private Set<String> getAttribute(AttributeStatement statement, String attributeName,
            Set<PrivateKey> decryptionKeys) {
        if (logger.isDebugEnabled()) {
            logger.debug("DefaultLibrarySPAccountMapper.getAttribute: attribute Name =" + attributeName);
        }

        // check it if the attribute needs to be encrypted?
        List<Attribute> list = statement.getAttribute();
        List<EncryptedAttribute> encList = statement.getEncryptedAttribute();
        if (encList != null && !encList.isEmpty()) {
            // a new list to hold the union of clear and encrypted attributes
            List<Attribute> allList = new ArrayList<>();
            if (list != null) {
                allList.addAll(list);
            }
            list = allList;
            for (EncryptedAttribute encryptedAttribute : encList) {
                try {
                    list.add(encryptedAttribute.decrypt(decryptionKeys));
                } catch (SAML2Exception se) {
                    logger.error("Decryption error:", se);
                    return null;
                }
            }
        }

        for (Attribute attribute : list) {
            if (!attributeName.equalsIgnoreCase(attribute.getName())) {
                continue;
            }

            List<String> values = attribute.getAttributeValueString();
            if (values == null || values.isEmpty()) {
                return null;
            }
            return new HashSet<>(values);
        }
        return null;
    }

    /**
     * Returns whether the NameID-Format should be persisted in the user data store or not.
     * @param realm          Realm name.
     * @param hostEntityID   Hosted <code>EntityID</code>.
     * @return <code>true</code> if the NameID-Format should be persisted in the user data store,
     *      otherwise <code>false</code>.
     */
    public boolean shouldPersistNameIDFormat(String realm, String hostEntityID) {
        return !Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSSOConfig(realm, hostEntityID,
                SP_ROLE, SAML2Constants.SP_DO_NOT_WRITE_FEDERATION_INFO));
    }

    /**
     * Returns the NameID from the given assertion. If the NameID is encrypted, it will be decrypted using the
     * decryption keys provided.
     *
     * @param assertion      The SAML2 Assertion containing the NameID.
     * @param decryptionKeys Set of private keys used for decrypting the NameID if it is encrypted.
     * @return NameID from the assertion.
     * @throws SAML2Exception If there is an error during decryption.
     */
    public NameID getNameID(Assertion assertion, Set<PrivateKey> decryptionKeys) throws SAML2Exception {
        EncryptedID encryptedID = assertion.getSubject().getEncryptedID();
        if (encryptedID != null) {
            return encryptedID.decrypt(decryptionKeys);
        }
        return assertion.getSubject().getNameID();
    }
}
