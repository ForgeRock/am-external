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
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.servlet;

import java.io.IOException;
import java.util.List;

import org.owasp.esapi.ESAPI;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Export the AM SAML metadata.
 */
@Singleton
public class ExportSamlMetadataServlet extends AbstractSAMLHttpServlet {

    void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // This JSP is used to export standard entity metadata,
        // there are three supported query parameters:
        //    * role     -- role of the entity: sp, idp or any
        //    * realm    -- realm of the entity
        //    * entityid -- ID of the entity to be exported
        //    * sign     -- sign the metadata if the value is "true"
        // If none of the query parameter is specified, it will try to export
        // the first hosted SP metadata under root realm. If there is no hosted
        // SP under the root realm, the first hosted IDP under root realm will
        // be exported. If there is no hosted SP or IDP, an error message will
        // be displayed.
        String metaXML = null;
        String errorMsg = null;
        try {
            SAMLUtils.checkHTTPContentLength(request);
            String role = request.getParameter("role");
            if ((role == null) || (role.length() == 0)) {
                // default role is any if not specified
                role = "any";
            }
            String realm = request.getParameter("realm");
            if ((realm == null) || (realm.length() == 0)) {
                // default to root realm
                realm = "/";
            }
            String entityID = request.getParameter("entityid");
            boolean sign = "true".equalsIgnoreCase(request.getParameter("sign"));
            SAML2MetaManager manager = new SAML2MetaManager();
            if ((entityID == null) || (entityID.length() == 0)) {
                // find first available one
                List<String> providers;
                if ("sp".equals(role)) {
                    providers = manager.getAllHostedServiceProviderEntities(realm);
                } else if ("idp".equals(role)) {
                    providers = manager.getAllHostedIdentityProviderEntities(realm);
                } else {
                    // will return any role
                    // try SP first
                    providers = manager.getAllHostedServiceProviderEntities(realm);
                    if ((providers == null) || providers.isEmpty()) {
                        providers = manager.getAllHostedIdentityProviderEntities(realm);
                    }
                }
                if ((providers != null) && !providers.isEmpty()) {
                    entityID = providers.iterator().next();
                }
            }

            if ((entityID == null) || (entityID.length() == 0)) {
                errorMsg = "No matching entity metadata found.";
            } else {
                metaXML = SAML2MetaUtils.exportStandardMeta(realm, entityID,
                        sign);
                if (metaXML == null) {
                    errorMsg = "No metadata for entity \""
                            + ESAPI.encoder().encodeForHTML(entityID)
                            + "\" under realm \""
                            + ESAPI.encoder().encodeForHTML(realm)
                            + "\" found.";
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger("jsp.saml2.exportmetadata").error("An error occurred while retrieving metadata", e);
            errorMsg = e.getMessage();
        }
        if (errorMsg != null) {
            response.getWriter().print("ERROR : " + ESAPI.encoder().encodeForHTML(errorMsg));
        } else {
            response.setContentType("text/xml");
            response.setHeader("Pragma", "no-cache");
            response.setContentType("text/xml; charset=utf-8");
            response.getWriter().print(metaXML);
        }


    }
}
