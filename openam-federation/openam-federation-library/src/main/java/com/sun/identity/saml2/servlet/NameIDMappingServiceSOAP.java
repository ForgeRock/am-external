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
 * $Id: NameIDMappingServiceSOAP.java,v 1.6 2009/10/14 23:59:44 exu Exp $
 *
 * Portions Copyrighted 2015-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.NameIDMapping;
import com.sun.identity.saml2.protocol.NameIDMappingRequest;
import com.sun.identity.saml2.protocol.NameIDMappingResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;


/**
 * This class <code>NameIDMappingServiceSOAP</code> receives and processes 
 * Name ID mapping request using SOAP binding on IDP side.
 */
public class NameIDMappingServiceSOAP extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(NameIDMappingServiceSOAP.class);

    private SOAPCommunicator soapCommunicator;

    public void init() throws ServletException {
        soapCommunicator = InjectorHolder.getInstance(SOAPCommunicator.class);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        try {
            // handle DOS attack
            SAMLUtils.checkHTTPContentLength(req);
            // Get IDP entity ID
            String idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                req.getRequestURI());
            String idpEntityID = SAML2Utils.getSAML2MetaManager().
                getEntityByMetaAlias(idpMetaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
            if (!SAML2Utils.isIDPProfileBindingSupported(
                realm, idpEntityID, SAML2Constants.NAMEID_MAPPING_SERVICE,
                SAML2Constants.SOAP))
            {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "unsuppoprtedBinding"));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("NameIDMappingServiceSOAP.doPost : " +
                    "uri = " + req.getRequestURI() + ", idpMetaAlias = " +
                    idpMetaAlias + ", idpEntityID = " + idpEntityID);
            }

            SOAPMessage msg = soapCommunicator.getSOAPMessage(req);
            Element reqElem = soapCommunicator.getSamlpElement(msg,
                    SAML2Constants.NAME_ID_MAPPING_REQUEST);

            NameIDMappingRequest nimRequest = ProtocolFactory.getInstance()
                .createNameIDMappingRequest(reqElem);

            NameIDMappingResponse nimResponse =
                NameIDMapping.processNameIDMappingRequest(nimRequest, realm,
               idpEntityID);

            SOAPMessage reply = soapCommunicator.createSOAPMessage(
                    nimResponse.toXMLString(true, true), false);

            if (reply != null) {    
                //  Need to call saveChanges because we're
                // going to use the MimeHeaders to set HTTP
                // response information. These MimeHeaders
                // are generated as part of the save.

                if (reply.saveRequired()) {
                    reply.saveChanges();
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(reply.getMimeHeaders(), resp);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                reply.writeTo(stream);
                resp.getWriter().println(stream.toString("UTF-8"));
                resp.getWriter().flush();
            }
        } catch (SAML2Exception ex) {
            logger.error("NameIDMappingServiceSOAP", ex);
            SAMLUtils.sendError(req, resp, 
                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                 "nameIDMappingFailed", ex.getMessage());
            return;
        } catch (SOAPException soap) {
            logger.error("NameIDMappingServiceSOAP", soap);
            SAMLUtils.sendError(req, resp, 
                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                 "nameIDMappingFailed", soap.getMessage());
            return;
        }
    }
}
