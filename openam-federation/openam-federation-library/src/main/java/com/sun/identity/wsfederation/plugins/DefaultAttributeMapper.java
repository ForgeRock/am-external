/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultAttributeMapper.java,v 1.4 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2017 ForgeRock AS.
 */
package com.sun.identity.wsfederation.plugins;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.datastore.DataStoreProvider;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Collections;

import org.forgerock.openam.utils.CollectionUtils;

/**
 * This class <code>DefaultAttribute</code> is the base class for
 * <code>DefaultSPAttributeMapper</code> and
 * <code>DefaultIDPAttributeMapper</code> for sharing the common
 * functionality.
 */
public class DefaultAttributeMapper {

    protected static Debug debug = WSFederationUtils.debug;
    protected static ResourceBundle bundle = WSFederationUtils.bundle;
    protected static DataStoreProvider dsProvider = WSFederationUtils.dsProvider;
    protected static final String IDP = "IDP";
    protected static final String SP = "SP";

    /**
     * Returns the attribute map by parsing the configured map in hosted
     * provider configuration
     *
     * @param realm realm name.
     * @param entityId <code>EntityID</code> of the provider to lookup.
     * @param role either SP or IDP.
     * @return a map of local attributes configuration map.
     * This map will have a key as the SAML attribute name and the value
     * is the local attribute.
     * @throws WSFederationException for any failures.
     */
    public Map<String, String> getConfigAttributeMap(String realm, String entityId, String role)
            throws WSFederationException {

        if (realm == null) {
            throw new WSFederationException(bundle.getString("nullRealm"));
        }

        if (entityId == null) {
            throw new WSFederationException(bundle.getString("nullHostEntityID"));
        }

        final String debugClass = "DefaultAttributeMapper.getConfigAttributeMap:";

        try {
            BaseConfigType config;
            if (SP.equals(role)) {
                config = WSFederationUtils.getMetaManager().getSPSSOConfig(realm, entityId);
            } else {
                config = WSFederationUtils.getMetaManager().getIDPSSOConfig(realm, entityId);
            }

            if (config == null) {
                debug.warning("{} configuration is not defined for entityId {} in realm.", debugClass, entityId, realm);
                return Collections.emptyMap();
            }

            Map<String, List<String>> attribConfig = WSFederationMetaUtils.getAttributes(config);
            List<String> mappedAttributes = attribConfig.get(SAML2Constants.ATTRIBUTE_MAP);

            if (CollectionUtils.isEmpty(mappedAttributes)) {
                debug.message("{} Attribute map is not defined for entity: {}", debugClass, entityId);
                return Collections.emptyMap();
            }
            Map<String, String> map = new HashMap<>();
            for (String mappedAttribute : mappedAttributes) {
                if (!mappedAttribute.contains("=")) {
                    debug.message("{} Invalid entry. {}", debugClass, mappedAttribute);
                    continue;
                }

                StringTokenizer st = new StringTokenizer(mappedAttribute, "=");
                map.put(st.nextToken(), st.nextToken());
            }
            return map;

        } catch (WSFederationMetaException sme) {
            debug.error("{} Meta Exception", debugClass, sme);
            throw new WSFederationException(sme);
        }
    }

}
