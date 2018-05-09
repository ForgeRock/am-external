/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package com.sun.identity.wsfederation.servlet;

import static com.sun.identity.wsfederation.common.WSFederationConstants.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.forgerock.openam.utils.StringUtils;
import org.owasp.esapi.ESAPI;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;

/**
 * A {@link WSFederationAction} implementation that handles incoming MEX requests related to WS-Fed Active Requestor
 * Profile.
 */
public class MexRequest extends WSFederationAction {

    private static final Debug DEBUG = Debug.getInstance("libWSFederation");

    public MexRequest(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * Currently two kind of requests are supported:
     * <ul>
     *  <li>GET requests: The plain old WSDL is returned to the remote clients.</li>
     *  <li>POST requests with SOAP body: In this case a WS-Transport GetResponse is returned that embeds the WSDL in a
     *   MetadataSection element.</li>
     * </ul>
     * @throws ServletException If there was a problem whilst rendering the response.
     * @throws IOException If there was an IO error whilst working with the request or response.
     * @throws WSFederationException If there was an unrecoverable error while processing the request.
     */
    @Override
    public void process() throws ServletException, IOException, WSFederationException {
        final String metaAlias = WSFederationMetaUtils.getMetaAliasByUri(request.getRequestURI());
        if (StringUtils.isEmpty(metaAlias)) {
            DEBUG.error("Unable to get IDP meta alias from request.");
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME, "IDPMetaAliasNotFound", null);
        }

        WSFederationMetaManager metaManager = WSFederationUtils.getMetaManager();
        final String realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
        final String idpEntityId = metaManager.getEntityByMetaAlias(metaAlias);

        if (StringUtils.isEmpty(idpEntityId)) {
            DEBUG.error("Unable to get IDP Entity ID from metaAlias");
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME, "nullIDPEntityID", null);
        }

        final IDPSSOConfigElement idpConfig = metaManager.getIDPSSOConfig(realm, idpEntityId);
        if (idpConfig == null) {
            DEBUG.error("Cannot find configuration for IdP " + idpEntityId);
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME, "unableToFindIDPConfiguration", null);
        }

        final boolean activeRequestorEnabled = Boolean.parseBoolean(WSFederationMetaUtils.getAttribute(idpConfig,
                WSFederationConstants.ACTIVE_REQUESTOR_PROFILE_ENABLED));

        if (!activeRequestorEnabled) {
            DEBUG.warning("Active Requestor Profile is not enabled for the hosted IdP {}", idpEntityId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final String endpointBaseUrl = WSFederationMetaUtils.getEndpointBaseUrl(idpConfig, request);

        request.setAttribute("baseUrl", ESAPI.encoder().encodeForXML(endpointBaseUrl));
        request.setAttribute("metaAlias", ESAPI.encoder().encodeForXML(metaAlias));

        final RequestDispatcher requestDispatcher;

        // If the MEX endpoint was accessed using POST, then this is a SOAP request and we should respond with a SOAP
        // message. If the request was made using GET, then we should just return the WSDL.
        if ("POST".equals(request.getMethod())) {
            try (InputStream is = request.getInputStream()) {
                MimeHeaders headers = SOAPCommunicator.getInstance().getHeaders(request);
                SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
                        .createMessage(headers, is);
                if (DEBUG.messageEnabled()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    soapMessage.writeTo(baos);
                    DEBUG.message("SOAP message received: " + new String(baos.toByteArray(), Charset.forName("UTF-8")));
                }
                final SOAPHeader soapHeader = soapMessage.getSOAPHeader();
                final NodeList nodeList = soapHeader.getElementsByTagNameNS(WSA_NAMESPACE, "MessageID");
                if (nodeList.getLength() == 1) {
                    request.setAttribute("inResponseTo", ESAPI.encoder().encodeForXML(
                            nodeList.item(0).getTextContent()));
                }
            } catch (SOAPException se) {
                DEBUG.error("An error occurred while processing the SOAP request.", se);
            }
            requestDispatcher = request.getRequestDispatcher("/wsfederation/jsp/mex.jsp");
        } else {
            requestDispatcher = request.getRequestDispatcher("/wsfederation/jsp/wsdl.jsp");
        }

        requestDispatcher.forward(request, response);
    }
}
