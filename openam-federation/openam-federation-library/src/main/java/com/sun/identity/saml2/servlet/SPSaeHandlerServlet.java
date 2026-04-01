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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.forgerock.openam.utils.ClientUtils;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.sae.api.SecureAttrs;
import com.sun.identity.sae.api.Utils;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Singleton
public class SPSaeHandlerServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SPSaeHandlerServlet.class);

    void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
//  Setup http GET/POST to sp app URL
            String action = "POST";
            // Resolve FM-SP's metaAlias
            String spMetaAlias =
                    SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());

            String servletPath = request.getServletPath();
            String gotoUrl = request.getRequestURL().toString();
            String appBase = gotoUrl.substring(0, gotoUrl.lastIndexOf(servletPath) + 1);
            String errorUrl = appBase + "saml2/jsp/saeerror.jsp";
            String ipaddr = ClientUtils.getClientIPAddress(request);
            String userid = null;

            // This FM-SP's entity ID
            String realm = null;
            String spEntityId = null;


            Object token = null;
            SessionProvider provider = null;

            if (spMetaAlias == null) {
                String errStr = errorUrl + "?errorcode=14&errorstring=SP_NullMetaAlias";
                logger.error(errStr);
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }

            // Check if a user is already authenticated
            boolean loggedIn = false;
            String loggedinPrincipal = null;
            String loggedinAuthLevel = null;
            String idpEntityId = null;
            try {
                provider = SessionManager.getProvider();
                token = provider.getSession(request);
                if ((token != null) && (provider.isValid(token))) {
                    loggedIn = true;
                    loggedinPrincipal = provider.getPrincipalName(token);
                    String[] levelStr =
                            provider.getProperty(token, SessionProvider.AUTH_LEVEL);
                    if ((levelStr != null) && (levelStr.length > 0)) {
                        loggedinAuthLevel = levelStr[0];
                    }
                    String[] infoStrs = provider.getProperty(
                            token, AccountUtils.getNameIDInfoKeyAttribute());
                    if (infoStrs != null && infoStrs.length != 0) {
                        String infoStr = infoStrs[0];
                        int index = infoStr.lastIndexOf(SAML2Constants.SECOND_DELIM);
                        if (index != -1) {
                            infoStr = infoStr.substring(index, infoStr.length());
                        }
                        NameIDInfoKey infoKey = NameIDInfoKey.parse(infoStr);
                        idpEntityId = infoKey.getRemoteEntityID();
                    }
                } else {
                    token = null; // for logging
                }
            } catch (SessionException e) {
                logger.debug("SA_SP:sessionvalidation exc:ignored", e);
                token = null; // for logging
                // Assumed not logged in
            } catch (Exception se) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SA_SP:error parsing NameInfoKey:", se);
                }
            }

            if (loggedIn == false) {
                String errStr = errorUrl + "?errorcode=5&errorstring=SP_No_SSOToken";
                logger.error(errStr);
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }
            String spApp = (String) request.getParameter(SecureAttrs.SAE_PARAM_SPAPPURL);
            if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + spApp, spApp, "URL", 2000, true)) {
                spApp = null;
            }
            if (spApp == null) {
                String errStr = errorUrl + "?errorcode=6&errorstring=SP_NOSPAppURL";
                logger.error(errStr);
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }

            if (idpEntityId == null) {
                String errStr = errorUrl + "?errorcode=8&errorstring=SP_NOIDPEntityID";
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }

            HashMap map = new HashMap();
            map.put(SecureAttrs.SAE_PARAM_AUTHLEVEL, loggedinAuthLevel);
            map.put(SecureAttrs.SAE_PARAM_IDPENTITYID, idpEntityId);
            try {
                realm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
                SAML2MetaManager mm = SAML2Utils.getSAML2MetaManager();
                spEntityId = mm.getEntityByMetaAlias(spMetaAlias);
                // get attr list from configuration
                SPSSOConfigElement spConfig = mm.getSPSSOConfig(realm, spEntityId);
                if (spConfig != null) {
                    Map attrs = SAML2MetaUtils.getAttributes(spConfig);
                    if (attrs != null) {
                        List value = (List) attrs.get(SAML2Constants.ATTRIBUTE_MAP);
                        if (value != null && !value.isEmpty()) {
                            Iterator valueIter = value.iterator();
                            while (valueIter.hasNext()) {
                                String entry = (String) valueIter.next();
                                StringTokenizer st = new StringTokenizer(entry, "=");
                                if (st.countTokens() != 2) {
                                    continue;
                                }
                                st.nextToken();
                                String localAttr = st.nextToken();
                                String[] vals = provider.getProperty(token, localAttr);
                                if ((vals != null) && (vals.length > 0)) {
                                    map.put(localAttr, vals[0]);
                                }
                            }
                        }
                    }
                }
            } catch (SAML2MetaException se) {
            }
            map.put(SecureAttrs.SAE_PARAM_SPENTITYID, spEntityId);

            // Get SAE attributes relating to FM-SP and SPApp
            Map hp = SAML2Utils.getSAEAttrs(
                    realm, spEntityId, SAML2Constants.SP_ROLE, spApp);

            if (hp == null) {
                String errStr = errorUrl
                        + "?errorcode=13&errorstring=Error_invalid_SAEAttrs:"
                        + spApp;
                logger.error(errStr);
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }

            String secret = null;
            String encSecret = null;
            String cryptoType = (String) hp.get(SecureAttrs.SAE_CRYPTO_TYPE);
            if (SecureAttrs.SAE_CRYPTO_TYPE_SYM.equals(cryptoType)) {
                secret = (String) hp.get(SecureAttrs.SAE_CONFIG_SHARED_SECRET);
                encSecret = secret;
            } else if (SecureAttrs.SAE_CRYPTO_TYPE_ASYM.equals(cryptoType)) {
                secret = (String) hp.get(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS);
                encSecret = (String) hp.get(SecureAttrs.SAE_CONFIG_PUBLIC_KEY_ALIAS);
            }
            if (secret == null || secret.length() == 0) {
                String errStr = errorUrl
                        + "?errorcode=13&errorstring=Error_invalid_sharedSecret:"
                        + spApp;
                logger.error(errStr);
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }

            String encAlg =
                    (String) hp.get(SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG);
            String encStrength =
                    (String) hp.get(SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH);
            String saInstanceName = cryptoType + "_" + encAlg + "_" + encStrength;
            SecureAttrs sa = SecureAttrs.getInstance(saInstanceName);
            if (sa == null) {
                Properties prop = new Properties();
                prop.setProperty(SecureAttrs.SAE_CONFIG_CERT_CLASS,
                        "com.sun.identity.sae.api.FMCerts");
                if (encAlg != null) {
                    prop.setProperty(SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG, encAlg);
                }
                if (encStrength != null) {
                    prop.setProperty(SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH,
                            encStrength);
                }
                SecureAttrs.init(saInstanceName, cryptoType, prop);
                sa = SecureAttrs.getInstance(saInstanceName);
            }
            if (encAlg == null) {
                encSecret = null;
            }
            String encodedString = sa.getEncodedString(map, secret, encSecret);
            if (encodedString == null) {
                String errStr = errorUrl
                        + "?errorcode=7&errorstring=Couldnt_secure_attrs:"
                        + map;
                logger.error(errStr);
                String data[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }
            HashMap sParams = new HashMap();
            sParams.put(SecureAttrs.SAE_PARAM_DATA, encodedString);

            String data[] = {map.toString()};
            SAML2Utils.logAccess(Level.INFO, LogUtil.SAE_SP_SUCCESS,
                    data, token, ipaddr, userid, realm, "SAE", null);
            // Comment this redirect and uncomment the below href for debugging.
            // The href at the bottom will take effect
            try {
                if (spApp != null && SAML2Utils.isRelayStateURLValid(request, spApp, SAML2Constants.SP_ROLE)) {
                    Utils.redirect(response, new PrintWriter(response.getWriter(), true), spApp, sParams, action);
                } else {
                    throw new Exception("Redirect URL was not valid " + spApp);
                }
            } catch (Exception ex) {
                String errStr = errorUrl
                        + "?errorcode=7&errorstring=Couldnt_redirect:" + ex
                        + " Map=" + map;
                logger.error(errStr);
                String data1[] = {errStr};
                SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR,
                        data1, token, ipaddr, userid, realm, "SAE", null);
                response.sendRedirect(errStr);
                return;
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

}
