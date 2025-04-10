/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultIDPAccountMapper.java,v 1.9 2008/11/10 22:57:02 veiming Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static org.forgerock.openam.utils.AttributeUtils.isBinaryAttribute;
import static org.forgerock.openam.utils.AttributeUtils.removeBinaryAttributeFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2InvalidNameIDPolicyException;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.shared.encode.Base64;

/**
 * This class <code>DefaultIDPAccountMapper</code> is the default implementation of the <code>IDPAccountMapper</code>
 * that is used to map the <code>SAML</code> protocol objects to the user accounts at the <code>IdentityProvider</code>
 * side of SAML v2 plugin.
 * Custom implementations may extend from this class to override some of these implementations if they choose to do so.
 */
public class DefaultIDPAccountMapper extends DefaultAccountMapper implements IDPAccountMapper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIDPAccountMapper.class);
    private final IDPAccountMapperUtils idpAccountMapperUtils;

    public DefaultIDPAccountMapper() {
        logger.debug("DefaultIDPAccountMapper.constructor");
        role = IDP_ROLE;
        idpAccountMapperUtils = new IDPAccountMapperUtils();
    }

    @Override
    public NameID getNameID(Object session, String hostEntityID, String remoteEntityID, String realm,
            String nameIDFormat) throws SAML2Exception {
        String userID;
        try {
            SessionProvider sessionProv = SessionManager.getProvider();
            userID = sessionProv.getPrincipalName(session);
        } catch (SessionException se) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("invalidSSOToken"));
        }

        String nameIDValue = null;
        if (nameIDFormat.equals(SAML2Constants.NAMEID_TRANSIENT_FORMAT)) {
            nameIDValue = idpAccountMapperUtils.getNameIdFromSession(session, remoteEntityID);
            if (nameIDValue == null) {
                nameIDValue = getNameIDValueFromUserProfile(realm, hostEntityID, remoteEntityID, userID, nameIDFormat);
                if (nameIDValue == null) {
                    nameIDValue = SAML2Utils.createNameIdentifier();
                }
            }
        } else {
            nameIDValue = getNameIDValueFromUserProfile(realm, hostEntityID, remoteEntityID, userID, nameIDFormat);
            if (nameIDValue == null) {
                if (nameIDFormat.equals(SAML2Constants.PERSISTENT)) {
                    // Double check that NameID persistence is enabled, there is no point in generating a value if not.
                    if (shouldPersistNameIDFormat(realm, hostEntityID, remoteEntityID, nameIDFormat)) {
                        nameIDValue = SAML2Utils.createNameIdentifier();
                    } else {
                        throw new SAML2InvalidNameIDPolicyException(
                                bundle.getString("unableToGenerateNameIDValuePersistenceDisabled"));
                    }
                } else {
                    throw new SAML2Exception(bundle.getString("unableToGenerateNameIDValue"));
                }
            }
        }

        NameID nameID = AssertionFactory.getInstance().createNameID(); 
        nameID.setValue(nameIDValue);
        nameID.setFormat(nameIDFormat);
        nameID.setNameQualifier(hostEntityID);
        nameID.setSPNameQualifier(remoteEntityID);
        nameID.setSPProvidedID(null);
        return nameID;
    }

    @Override
    public String getIdentity(NameID nameID, String hostEntityID, String remoteEntityID, String realm)
            throws SAML2Exception {

        if (nameID == null) {
            return null;
        }

        if (hostEntityID == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID")); 
        }

        if (remoteEntityID == null) {
            throw new SAML2Exception(bundle.getString("nullRemoteEntityID")); 
        }

        if (realm == null) {
            throw new SAML2Exception(bundle.getString("nullRealm")); 
        }

        if (logger.isDebugEnabled()) {
            logger.debug("DefaultIDPAccountMapper.getIdentity: realm = " + realm + ", hostEntityID = " + hostEntityID
                    + ", remoteEntityID = " + remoteEntityID);
        }

        try {
            return dsProvider.getUserID(realm, SAML2Utils.getNameIDKeyMap(nameID, hostEntityID, remoteEntityID, realm,
                    role));
        } catch (DataStoreProviderException dse) {
            logger.error("DefaultIDPAccountMapper.getIdentity(NameIDMappingRequest): ", dse);
            throw new SAML2Exception(dse.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * This implementation first checks whether NameID persistence has been completely disabled at the IdP level
     * (idpDisableNameIDPersistence setting), and if not, it will look at the SP configuration as well
     * (spDoNotWriteFederationInfo setting).
     *
     * @param realm {@inheritDoc}
     * @param hostEntityID {@inheritDoc}
     * @param remoteEntityID {@inheritDoc}
     * @param nameIDFormat {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean shouldPersistNameIDFormat(String realm, String hostEntityID, String remoteEntityID,
            String nameIDFormat) {
        final boolean disableNameIDPersistence = Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSSOConfig(realm,
                hostEntityID, IDP_ROLE, SAML2Constants.IDP_DISABLE_NAMEID_PERSISTENCE));
        if (disableNameIDPersistence) {
            return false;
        }
        return !Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSSOConfig(realm, remoteEntityID,
                SAML2Constants.SP_ROLE, SAML2Constants.SP_DO_NOT_WRITE_FEDERATION_INFO));
    }

    /**
     * Retrieves the appropriate NameID value from the user profile for the hosted and remote entities.
     * This implementation first checks if any NameID value mappings have been defined at the SP level, using those
     * exclusively if so, and if none are defined, will collect NameID value mappings from the IdP configuration.
     *
     * @param realm Name of the realm.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @param userID Universal identifier of the user.
     * @param nameIDFormat <code>NameID</code> format.
     * @return The <code>NameID</code> value from the user's profile.
     */
    protected String getNameIDValueFromUserProfile(String realm, String hostEntityID, String remoteEntityID,
               String userID, String nameIDFormat) {
        String nameIDValue = null;
        Map<String, String> formatAttrMap = getFormatAttributeMap(realm, hostEntityID, remoteEntityID);
        String attrName = formatAttrMap.get(nameIDFormat);
        if (attrName != null) {
            try {
                if (isBinaryAttribute(attrName)) {
                    attrName = removeBinaryAttributeFlag(attrName);
                    byte[][] attributeValues = dsProvider.getBinaryAttribute(userID, attrName);
                    if (attributeValues != null && attributeValues.length > 0) {
                        nameIDValue = Base64.encode(attributeValues[0]);
                    }
                } else {
                    Set<String> attrValues = dsProvider.getAttribute(userID, attrName);
                    if (attrValues != null && !attrValues.isEmpty()) {
                        nameIDValue = attrValues.iterator().next();
                    }
                }
            } catch (DataStoreProviderException dspe) {
                if (logger.isWarnEnabled()) {
                    logger.warn("DefaultIDPAccountMapper.getNameIDValueFromUserProfile:", dspe);
                }
            }
        }

        return nameIDValue;
    }


    protected String getNameIDValueFromUserProfile(String realm, String hostEntityID, String userID,
                   String nameIDFormat) {
        return getNameIDValueFromUserProfile(realm, hostEntityID, null, userID, nameIDFormat);
    }

    private Map<String, String> getFormatAttributeMap(String realm, String hostEntityID, String remoteEntityID) {
        if (remoteEntityID == null) {
            String key = hostEntityID + "|" + realm;
            Map<String, String> formatAttributeMap = IDPCache.formatAttributeHash.get(key);

            if (formatAttributeMap != null) {
                return formatAttributeMap;
            }

            List<String> idpAttributeValues = SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID, role,
                    SAML2Constants.NAME_ID_FORMAT_MAP);
            formatAttributeMap = mapAttributes(idpAttributeValues);

            IDPCache.formatAttributeHash.put(key, formatAttributeMap);
            return formatAttributeMap;

        } else {
            String key = remoteEntityID + "|" + realm;
            Map<String, String> formatAttributeMap = SPCache.formatAttributeHash.get(key);

            if (formatAttributeMap != null) {
                return formatAttributeMap;
            }

            List<String> spAttributeValues = SAML2Utils.getAllAttributeValueFromSSOConfig(realm, remoteEntityID,
                    SAML2Constants.SP_ROLE, SAML2Constants.NAME_ID_FORMAT_MAP);
            formatAttributeMap = mapAttributes(spAttributeValues);

            if (formatAttributeMap.isEmpty()) {
                List<String> idpAttributeValues = SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID,
                        role, SAML2Constants.NAME_ID_FORMAT_MAP);
                formatAttributeMap = mapAttributes(idpAttributeValues);
            }

            SPCache.formatAttributeHash.put(key, formatAttributeMap);
            return formatAttributeMap;
        }
    }

    private Map<String, String> mapAttributes(List<String> values) {
        Map<String, String> attrMap = new HashMap<>();
        if (values != null) {
            for (String value : values) {
                int index = value.indexOf('=');
                if (index != -1) {
                    String format = value.substring(0, index).trim();
                    String attrName = value.substring(index + 1).trim();
                    if (!format.isEmpty() && !attrName.isEmpty()) {
                        attrMap.put(format, attrName);
                    }
                }
            }
        }
        return attrMap;
    }
}
