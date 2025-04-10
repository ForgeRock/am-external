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
 * $Id: AttributeServiceSOAP.java,v 1.5 2009/06/12 22:21:41 mallas Exp $
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
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.AttributeQueryUtil;
import com.sun.identity.saml2.protocol.AttributeQuery;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;

/**
 * This class <code>AttributeServiceSOAP</code> receives and processes 
 * attribute query request using SOAP binding.
 */
public class AttributeServiceSOAP extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AttributeServiceSOAP.class);

    private SOAPCommunicator soapCommunicator;

    public void init() throws ServletException {
        soapCommunicator = InjectorHolder.getInstance(SOAPCommunicator.class);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGetPost(req, resp);
    }
            
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGetPost(req, resp);
    }

    private void doGetPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        // handle DOS attack
        SAMLUtils.checkHTTPContentLength(req);

        AttributeQuery attrQuery = null;

        try {
            SOAPMessage msg = soapCommunicator.getSOAPMessage(req);
            Element elem = soapCommunicator.getSamlpElement(msg,
                    SAML2Constants.ATTRIBUTE_QUERY);
            attrQuery =
                ProtocolFactory.getInstance().createAttributeQuery(elem);
        } catch (Exception ex) {
            logger.error(
                "AttributeServiceSOAP.doGetPost:",  ex);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "failedToCreateAttributeQuery", ex.getMessage());
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("AttributeServiceSOAP.doGetPost: " +
                    "pathInfo is null.");
            }
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "nullPathInfo", SAML2Utils.bundle.getString("nullPathInfo"));
            return;
        }

        String attrQueryProfileAlias = null;
        int index = pathInfo.indexOf(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        if (index > 2) {
            attrQueryProfileAlias = pathInfo.substring(1, index -1);
        }

        String attrAuthorityMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
            req.getRequestURI());


        String attrAuthorityEntityID = null;
        String realm = null;

        try {
            attrAuthorityEntityID =
                SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(
                attrAuthorityMetaAlias);

            realm = SAML2MetaUtils.getRealmByMetaAlias(attrAuthorityMetaAlias);
        } catch (SAML2Exception sme) {
            logger.error("AttributeServiceSOAP.doGetPost", sme);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "invalidMetaAlias", sme.getMessage());
            return;
        }

        SOAPMessage replymsg = null;
        try {
            Response samlResp = AttributeQueryUtil.processAttributeQuery(
                attrQuery, req, resp, attrAuthorityEntityID, realm,
                attrQueryProfileAlias);
            replymsg = soapCommunicator.createSOAPMessage(
                    samlResp.toXMLString(true, true), false);
        } catch (Throwable t) {
            logger.error("AttributeServiceSOAP.doGetPost: " +
                "Unable to create SOAP message:", t);
            replymsg = soapCommunicator.createSOAPFault(SAML2Constants.SERVER_FAULT,
                    "unableToCreateSOAPMessage", null);
        }

        try {
            if (replymsg.saveRequired()) {
                replymsg.saveChanges();
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            SAML2Utils.putHeaders(replymsg.getMimeHeaders(), resp);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            replymsg.writeTo(stream);
            resp.getWriter().println(stream.toString("UTF-8"));
            resp.getWriter().flush();
        } catch (SOAPException soap) {
            logger.error("AttributeServiceSOAP.doGetPost", soap);
            SAMLUtils.sendError(req, resp, 
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "soapError", soap.getMessage());
            return;
        }
    }
}
