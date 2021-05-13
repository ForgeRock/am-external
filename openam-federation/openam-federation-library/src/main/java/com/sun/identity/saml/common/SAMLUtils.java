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
 * $Id: SAMLUtils.java,v 1.16 2010/01/09 19:41:06 qcheng Exp $
 *
 * Portions Copyrighted 2012-2020 ForgeRock AS.
 */
package com.sun.identity.saml.common;

import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.soap.MimeHeaders;

import org.apache.xml.security.c14n.Canonicalizer;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.federation.util.XmlSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class contains some utility methods for processing SAML protocols.
 */
@Supported
public class SAMLUtils extends SAMLUtilsCommon {

    private static final Logger logger = LoggerFactory.getLogger(SAMLUtils.class);

    /**
     * Attribute that specifies maximum content length for SAML request in
     * <code>AMConfig.properties</code> file.
     */
    public static final String HTTP_MAX_CONTENT_LENGTH = "com.sun.identity.saml.request.maxContentLength";

    /**
     * Default maximum content length is set to 16k.
     */
    public static final int defaultMaxLength = 16384;

    /**
     * Default maximum content length in string format.
     */
    public static final String DEFAULT_CONTENT_LENGTH = String.valueOf(defaultMaxLength);

    private static final String ERROR_JSP = "/saml2/jsp/autosubmittingerror.jsp";

    private static int maxContentLength;

    static {
        XmlSecurity.init();
        try {
            maxContentLength = Integer.parseInt(SystemConfigurationUtil.getProperty(SAMLUtils.HTTP_MAX_CONTENT_LENGTH,
                    SAMLUtils.DEFAULT_CONTENT_LENGTH));
        } catch (NumberFormatException ne) {
            logger.error("Wrong format of SAML request max content length. Take default value.");
            maxContentLength = SAMLUtils.defaultMaxLength;
        }
    }

    /**
     * Constructor
     * iPlanet-PRIVATE-DEFAULT-CONSTRUCTOR
     */
    private SAMLUtils() {
    }

    /**
     * Generates an ID String with length of SAMLConstants.ID_LENGTH.
     * @return string the ID String; or null if it fails.
     */
    public static String generateAssertionID() {
        String encodedID = generateID();
        if (encodedID == null) {
            return null;
        }

        String id = null;
        try {
            id = SystemConfigurationUtil.getServerID(SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_PROTOCOL),
                    SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_HOST),
                    Integer.parseInt(SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_PORT)),
                    SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_URI));
        } catch (Exception ex) {
            logger.debug("SAMLUtil:generateAssertionID: exception obtain serverID:", ex);
        }
        if (id == null) {
            return encodedID;
        } else {
            return (encodedID + id);
        }
    }

    /**
     * Checks content length of a http request to avoid dos attack.
     * In case SAML inter-op with other SAML vendor who may not provide content
     * length in HttpServletRequest. We decide to support no length restriction
     * for Http communication. Here, we use a special value (e.g. 0) to
     * indicate that no enforcement is required.
     * @param request <code>HttpServletRequest</code> instance to be checked.
     * @exception ServletException if context length of the request exceeds
     *   maximum content length allowed.
     */
    public static void checkHTTPContentLength(HttpServletRequest request) throws ServletException {
        if (maxContentLength != 0) {
            int length =  request.getContentLength();
            if (logger.isDebugEnabled()) {
                logger.debug("HttpRequest content length= " +length);
            }
            if (length > maxContentLength) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "content length too large" + length);
                }
                throw new ServletException(
                SAMLUtils.bundle.getString("largeContentLength"));
            }
        }
    }

    /**
     * Returns a <code>MimeHeaders</code> object that contains the headers
     * in the given <code>HttpServletRequest</code> object.
     *
     * @param req the <code>HttpServletRequest</code> object.
     * @return a new <code>MimeHeaders</code> object containing the headers.
     */
    public static MimeHeaders getMimeHeaders(HttpServletRequest req) {
        MimeHeaders headers = new MimeHeaders();
        if (req == null) {
            logger.debug("SAMLUtils.getMimeHeaders: null input");
            return headers;
        }

        Enumeration<String> enumerator = req.getHeaderNames();

        while (enumerator.hasMoreElements()) {
            String headerName = enumerator.nextElement();
            String headerValue = req.getHeader(headerName);

            StringTokenizer values = new StringTokenizer(headerValue, ",");
            while (values.hasMoreTokens()) {
                headers.addHeader(headerName, values.nextToken().trim());
            }
        }

        return headers;
    }

    /**
     * Gets input Node Canonicalized
     *
     * @param node Node
     * @return Canonical element if the operation succeeded. Otherwise, return null.
     */
    public static Element getCanonicalElement(Node node) {
        try {
            Canonicalizer c14n = Canonicalizer.getInstance("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
            byte[] outputBytes = c14n.canonicalizeSubtree(node);
            DocumentBuilder documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            Document doc = documentBuilder.parse(new ByteArrayInputStream(outputBytes));
            return doc.getDocumentElement();
        } catch (Exception e) {
            logger.error("Response:getCanonicalElement: Error while performing canonicalization on the input Node.");
            return null;
        }
    }

     /**
      * Sends to error page URL for SAML protocols. If the error page is
      * hosted in the same web application, forward is used with
      * parameters. Otherwise, redirection or HTTP POST is used with
      * parameters.
      * Three parameters are passed to the error URL:
      *  -- errorcode : Error key, this is the I18n key of the error message.
      *  -- httpstatuscode : Http status code for the error
      *  -- message : detailed I18n'd error message
      * @param request HttpServletRequest object
      * @param response HttpServletResponse object
      * @param httpStatusCode Http Status code
      * @param errorCode Error code
      * @param errorMsg Detailed error message
      */
     public static void sendError(HttpServletRequest request, HttpServletResponse response, int httpStatusCode,
             String errorCode, String errorMsg) {
         String errorUrl = SystemConfigurationUtil.getProperty(SAMLConstants.ERROR_PAGE_URL,
                 SAMLConstants.DEFAULT_ERROR_PAGE_URL);
         logger.debug("SAMLUtils.sendError: error page {}", errorUrl);
         String tmp = errorUrl.toLowerCase();
         if (!tmp.startsWith("http://") && !tmp.startsWith("https://")) {
             String newUrl = getErrorUrl(errorUrl, httpStatusCode, errorCode, errorMsg);

             // use forward
             forwardRequest(newUrl, request, response);
         } else {
             String binding = SystemConfigurationUtil.getProperty(SAMLConstants.ERROR_PAGE_HTTP_BINDING,
                     SAMLConstants.HTTP_POST);
             if (SAMLConstants.HTTP_REDIRECT.equals(binding)) {
                 String newUrl = getErrorUrl(errorUrl, httpStatusCode, errorCode, errorMsg);

                 // use FSUtils, this may be redirection or forward
                 FSUtils.forwardRequest(request, response, newUrl);
             } else {
                 // Populate request attributes to be available for rendering.
                 request.setAttribute("ERROR_URL", errorUrl);
                 request.setAttribute("ERROR_CODE_NAME", SAMLConstants.ERROR_CODE);
                 request.setAttribute("ERROR_CODE", errorCode);
                 request.setAttribute("ERROR_MESSAGE_NAME", SAMLConstants.ERROR_MESSAGE);
                 request.setAttribute("ERROR_MESSAGE", urlEncodeQueryParameterNameOrValue(errorMsg));
                 request.setAttribute("HTTP_STATUS_CODE_NAME", SAMLConstants.HTTP_STATUS_CODE);
                 request.setAttribute("HTTP_STATUS_CODE", httpStatusCode);
                 request.setAttribute("SAML_ERROR_KEY", bundle.getString("samlErrorKey"));
                 // Forward to auto-submitting form.
                 forwardRequest(ERROR_JSP, request, response);
             }
         }
     }

    /**
     * Constructs the SAML error page URL using the provided parameters.
     *
     * @param errorUrl The error page's base URL.
     * @param httpStatusCode The HTTP status code to add as a query parameter.
     * @param errorCode The error code to add as a query parameter.
     * @param errorMsg The error message to add as a query parameter.
     * @return The error URL including all provided parameters.
     */
    public static String getErrorUrl(String errorUrl, int httpStatusCode, String errorCode, String errorMsg) {
        return errorUrl + (errorUrl.contains("?") ? "&" : "?")
                + SAMLConstants.ERROR_CODE + "=" + errorCode + "&"
                + SAMLConstants.HTTP_STATUS_CODE + "=" + httpStatusCode + "&"
                + SAMLConstants.ERROR_MESSAGE + "=" + urlEncodeQueryParameterNameOrValue(errorMsg);
    }

    /**
     * Forwards to the passed URL.
     *
     * @param url
     *         Forward URL
     * @param request
     *         Request object
     * @param response
     *         Response object
     */
    private static void forwardRequest(String url, HttpServletRequest request, HttpServletResponse response) {
        try {
            request.getRequestDispatcher(url).forward(request, response);
        } catch (ServletException | IOException ex) {
            handleForwardError(url, ex, response);
        }
    }

    /**
     * Handle any forward error.
     *
     * @param url
     *         Attempted forward URL
     * @param exception
     *         Caught exception
     * @param response
     *         Response object
     */
    private static void handleForwardError(String url, Exception exception, HttpServletResponse response) {
        logger.error("SAMLUtils.sendError: Exception occurred while trying to forward to resource: " + url, exception);

        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exception.getMessage());
        } catch (IOException ioE) {
            logger.error("Failed to inform the response of caught exception", ioE);
        }
    }

}
