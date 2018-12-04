/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ImportSAML2MetaData.java,v 1.5 2008/07/08 01:12:01 exu Exp $
 *
 * Portions Copyrighted 2011-2018 ForgeRock AS.
 */
package com.sun.identity.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.forgerock.openam.utils.CollectionUtils;
import org.w3c.dom.Document;

import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * Import SAML2 Metadata.
 */
public class ImportSAML2MetaData {

    private static final Debug DEBUG = Debug.getInstance("workflow");

    private ImportSAML2MetaData() {
    }

    /**
     * Imports meta and extended metadata.
     *
     * @param realm Realm of the entity.
     * @param metadata Meta data.
     * @param extended extended data.
     * @return realm and entity ID.
     */
    public static String[] importData(
        String realm,
        String metadata,
        String extended
    ) throws  WorkflowException {
        String entityID = null;

        try {
            EntityConfigElement configElt = null;
            if (extended != null) {
                Object obj = SAML2MetaUtils.convertStringToJAXB(extended);
                configElt = (obj instanceof EntityConfigElement) ? (EntityConfigElement)obj : null;
                if (configElt != null) {
                    List<JAXBElement<BaseConfigType>> config =
                            configElt.getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    if (!config.isEmpty()) {
                        JAXBElement<BaseConfigType> bConfig = config.iterator().next();
                        if (configElt.getValue().isHosted()) {
                            // get the realm from the extended meta and use for import
                            realm = SAML2MetaUtils.getRealmByMetaAlias(bConfig.getValue().getMetaAlias());
                        }
                        List<String> cots = SAML2MetaUtils.getAttributes(bConfig).get(SAML2Constants.COT_LIST);
                        if (CollectionUtils.isNotEmpty(cots)) {
                            CircleOfTrustManager manager = new CircleOfTrustManager();
                            Set<String> allRealmCots = manager.getAllCirclesOfTrust(realm);
                            List<String> invalidCots = new ArrayList<>();
                            for (String cot : cots) {
                                if (!allRealmCots.contains(cot)) {
                                    invalidCots.add(cot);
                                }
                            }
                            if (!invalidCots.isEmpty()) {
                                String combinedCots = String.join(", ", invalidCots);
                                DEBUG.error("Unknown Circle of Trust(s): {} in extended metadata for realm: {}",
                                        combinedCots, realm);
                                throw new WorkflowException("invalid-cot-extended-data", combinedCots, realm);
                            }
                        }
                    }
                }
            }

            SAML2MetaManager metaManager = new SAML2MetaManager();
            // Load the metadata if it has been provided
            if (metadata != null) {
                entityID = importSAML2MetaData(metaManager, realm, metadata);
            }
            // Load the extended metadata if it has been provided
            if (configElt != null) {
                metaManager.createEntityConfig(realm, configElt);
            }
        } catch (SAML2MetaException | JAXBException | COTException e) {
            DEBUG.error("An error occurred while importing the SAML metadata", e);
            throw new WorkflowException(e.getMessage(), e);
        }

        String[] results = {realm, entityID};
        return results;
    }

   private static String importSAML2MetaData(SAML2MetaManager metaManager, String realm,
           String metadata)
            throws SAML2MetaException, JAXBException, WorkflowException {

       String result = null;

       Document doc = XMLUtils.toDOMDocument(metadata, DEBUG);
       if (doc == null) {
           throw new WorkflowException(
                   "import-entity-exception-invalid-descriptor", null);
       } else {
           List<String> entityIds = SAML2MetaUtils.importSAML2Document(metaManager, realm, doc);
           if (entityIds.isEmpty()) {
               throw new WorkflowException(
                       "import-entity-exception-invalid-descriptor", null);
           } else {
               result = entityIds.iterator().next();
           }
       }

       return result;
    }
}

