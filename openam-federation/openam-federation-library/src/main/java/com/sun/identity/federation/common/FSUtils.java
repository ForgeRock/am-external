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
 * $Id: FSUtils.java,v 1.10 2009/11/20 23:52:57 ww203982 Exp $
 *
 * Portions Copyrighted 2013-2020 ForgeRock AS.
 */
package com.sun.identity.federation.common;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * This class contain constants used in the SDK.
 */
public class FSUtils {
    private static final Logger logger = LoggerFactory.getLogger(FSUtils.class);

    private static String deploymentURI =
        SystemConfigurationUtil.getProperty(
            "com.iplanet.am.services.deploymentDescriptor");

    private static Logger debug = LoggerFactory.getLogger(FSUtils.class);

    private static String server_protocol =
        SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL);
    private static String server_host =
        SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
    private static String server_port =
        SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
    private static String server_uri = SystemPropertiesManager.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    private static String localURL = server_protocol + "://" + server_host +
            ":" + server_port + server_uri;
    private static int int_server_port = 0;
    static {
        try {
            int_server_port = Integer.parseInt(server_port);
        } catch (NumberFormatException nfe) {
            debug.error("Unable to parse port " + server_port, nfe);
        }
    }

    /**
     * Constructor
     */
    private FSUtils() {
    }

    /**
     * Test if url in argument is in the same web deployment URI as AM.
     *
     * @param request HttpServletRequest
     * @param url The url to check.
     * @return true if request and url are in the same web container else false
     */
    public static boolean isSameContainer(HttpServletRequest request, String url) {
        boolean result = false;
        logger.debug("isSameContainer called");

        try {
            String sourceHost = request.getServerName();
            int sourcePort = request.getServerPort();
            logger.debug("SourceHost={} SourcePort={}", sourceHost, sourcePort);

            URL target = new URL(url);
            String targetHost = target.getHost();
            int targetPort = target.getPort() != -1 ? target.getPort() : target.getDefaultPort();
            logger.debug("targetHost={} targetPort={}", targetHost, targetPort);

            int index = url.indexOf(deploymentURI + "/");
            if (request.getContextPath().isEmpty() || !sourceHost.equals(targetHost) || sourcePort != targetPort
                    || index <= 0) {
                logger.debug("Source and Target may not be on the same container.");
            } else {
                logger.debug("Source and Target are on the same container.");
                result = true;
            }
        } catch (Exception ex) {
            logger.error("Exception occurred", ex);
        }
        return result;
    }
 
    /**
     * Forwards or redirects to a new URL. This method will use
     * {@link RequestDispatcher#forward(ServletRequest, ServletResponse)} if the target url is in the same web
     * deployment URI as AM. Otherwise it will redirect.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param url the target URL to be forwarded to redirected.  
     */
    public static void forwardRequest(HttpServletRequest request, HttpServletResponse response, String url) {
        logger.debug("forwardRequest called");

        try {
            // IBM websphere is not able to handle forwards with long urls.
            boolean isWebSphere = false;
            String container = SystemConfigurationUtil.getProperty(Constants.IDENTITY_WEB_CONTAINER);
            if (container != null && container.contains("IBM")) {
                isWebSphere = true;
            }

            int index = url.indexOf(deploymentURI + "/");
            if (isWebSphere || !isSameContainer(request, url)) {
                logger.debug("Redirecting to target: {}", url);
                response.sendRedirect(url);
            } else {
                String resource = url.substring(index + deploymentURI.length());
                logger.debug("Forwarding to: {}", resource);
                RequestDispatcher dispatcher = request.getRequestDispatcher(resource);
                try {
                    dispatcher.forward(request, response);
                } catch (Exception e) {
                    logger.error("Exception occurred while trying to forward to resource: {}", resource, e);
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred", ex);
        }
    }

    private static boolean requireAddCookie(HttpServletRequest request) {
        return CookieUtils.getCookieFromReq(request, getlbCookieName()) == null;
    }

    /**
     * Detects if a request simply needs loadbalancer cookies adding and to be redirected to be handled elsewhere.
     *
     * @param request The HTTP request in question.
     * @param response The response associated with the request.
     */
    public static void setLbCookieIfNecessary(HttpServletRequest request, HttpServletResponse response) {
        if (!requireAddCookie(request)) {
            return;
        }

        debug.debug("FSUtils.setLbCookieIfNecessary: lbCookie not set.");
        setlbCookie(request, response);
    }

    /**
     * Gets remote service URLs
     * @param request http request
     * @return remote service URLs
     */
    public static List<String> getRemoteServiceURLs(HttpServletRequest request) {
        List<String> remoteServiceURLs = new ArrayList<>();
        try {
            List<String> serverList = SystemConfigurationUtil.getServerList();
            if (CollectionUtils.isNotEmpty(serverList)) {
                List<String> siteList = SystemConfigurationUtil.getSiteList();
                debug.debug("FSUtils.getRemoteServiceURLs: servers={}, siteList={}", serverList, siteList);
                serverList.removeAll(siteList);
                debug.debug("FSUtils.getRemoteServiceURLs: servers after removing sites = {}", serverList);
                String requestURL = request.getScheme() + "://" +
                        request.getServerName() + ":" +
                        request.getServerPort();
                debug.debug("FSUtils.getRemoteServiceURLs: requestURL = {}", requestURL);
                for (String serviceURL : serverList) {
                    if (!serviceURL.equalsIgnoreCase(requestURL) && !serviceURL.equalsIgnoreCase(localURL)) {
                        remoteServiceURLs.add(serviceURL);
                    }
                }
            }
        } catch (SystemConfigurationException e) {
            if (debug.isDebugEnabled()) {
                debug.debug("FSUtils.getRemoteServiceURLs:", e);
            }
        }
        debug.debug("FSUtils.getRemoteServiceURLs: final list of remote service URLs = {}", remoteServiceURLs);
        return remoteServiceURLs;
    }

    /**
     * Sets load balancer cookie.
     * @param response HttpServletResponse object
     */
    public static void setlbCookie(HttpServletRequest request, HttpServletResponse response) {
        String cookieName = getlbCookieName();
        String cookieValue = getlbCookieValue();
        Cookie cookie;
        if (StringUtils.isNotEmpty(cookieName)) {
            Set<String> domains = SystemConfigurationUtil.getCookieDomainsForRequest(request);
            for (String domain : domains) {
                cookie = CookieUtils.newCookie(cookieName, cookieValue, "/", domain);
                CookieUtils.addCookieToResponse(response, cookie);
            }
        }
    }

    private static String getlbCookieName() {
        return SystemPropertiesManager.get(Constants.AM_LB_COOKIE_NAME, "amlbcookie");
    }

    private static String getlbCookieValue() {
        String loadBalanceCookieValue = SystemPropertiesManager.get(Constants.AM_LB_COOKIE_VALUE);
        if (Strings.isNullOrEmpty(loadBalanceCookieValue) && !SPCache.isFedlet) {
            try {
                return SystemConfigurationUtil.getServerID(server_protocol, server_host, int_server_port, server_uri);
            } catch (SystemConfigurationException scex) {
                debug.error("FSUtils.getlbCookieValue:", scex);
                return null;
            }
        }
        return loadBalanceCookieValue;
    }
}
