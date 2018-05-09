/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;

import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.session.Session;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.DNMapper;

/**
 * The <code>OAuth2PostAuthnPlugin</code> implements
 * AMPostAuthProcessInterface interface for authentication
 * post processing. This class can only be used for the OAuth2 authentication
 * module.
 * 
 * The post processing class can be assigned per ORGANIZATION or SERVICE
 */
public class OAuth2PostAuthnPlugin implements AMPostAuthProcessInterface {

    private static String FB_API_KEY = "api_key";
    private static String FB_SESSION_KEY  ="session_key";
    private static String FB_NEXT = "next";
    
    /** Post processing on successful authentication.
     * @param requestParamsMap - map contains HttpServletRequest parameters
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @param ssoToken  authenticated user's ssoToken
     * @exception Authentication Exception when there is an error
     */
    public void onLoginSuccess(Map requestParamsMap,
            HttpServletRequest request,
            HttpServletResponse response,
            SSOToken ssoToken)
            throws AuthenticationException {

        OAuthUtil.debugMessage("OAuth2PostAuthnPlugin:onLoginSuccess called");

    }

    /** Post processing on failed authentication.
     * @param requestParamsMap - map contains HttpServletRequest parameters
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @exception AuthenticationException when there is an error
     */
    public void onLoginFailure(Map requestParamsMap,
            HttpServletRequest request,
            HttpServletResponse response)
            throws AuthenticationException {
        
        OAuthUtil.debugMessage("OAuth2PostAuthnPlugin:onLoginFailure called");

    }

    /** Post processing on Logout.
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @param ssoToken - user's session
     */
    public void onLogout(HttpServletRequest request,
            HttpServletResponse response,
            SSOToken ssoToken)
            throws AuthenticationException {
        
        OAuthUtil.debugMessage("OAuth2PostAuthnPlugin:onLogout called " + request.getRequestURL());
        String gotoParam = request.getParameter(PARAM_GOTO);
        String serviceURI = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR); 

        try {
            String loginURL = OAuthUtil.findCookie(request, COOKIE_PROXY_URL);
            String accessToken = ssoToken.getProperty(SESSION_OAUTH_TOKEN);

            OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: OAUTH2 Token is: " + accessToken);
            Session is = AuthD.getSession(ssoToken.getTokenID().toString());
            String logoutBehaviour = is.getProperty(SESSION_LOGOUT_BEHAVIOUR);
            logoutBehaviour = StringUtils.isEmpty(logoutBehaviour) ? "prompt" : logoutBehaviour;
            if ("donotlogout".equalsIgnoreCase(logoutBehaviour)) {
                return;
            }
            
            if (accessToken != null && !accessToken.isEmpty()) {
                OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: OAuth2 logout");

                String logoutURL =
                        OAuthUtil.findCookie(request, COOKIE_LOGOUT_URL);

                if (logoutURL.toLowerCase().contains("facebook")) {
                    OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: facebook");
                    String origUrl = URLEncoder.encode(loginURL, "UTF-8");
                    String query = "";
                    if (accessToken.contains("\\|")) { 
                        // Non encrypted token
                        String[] tokenParts = accessToken.split("\\|");
                        String api_key = tokenParts[0];
                        String session_key = tokenParts[1];
                        query = FB_API_KEY +"=" + api_key + "&" + FB_SESSION_KEY + 
                                "=" + session_key + "&" + FB_NEXT + "=" + origUrl;
                    } else {      
                        // Encrypted token
                        query = FB_NEXT + "=" + origUrl + "&" + 
                                PARAM_ACCESS_TOKEN +"=" + accessToken;
                    }
                    logoutURL += "?" + query;
                }

                logoutURL = serviceURI + "/oauth2c/OAuthLogout.jsp?" + PARAM_LOGOUT_URL +
                        "=" + URLEncoder.encode(logoutURL, "UTF-8");;
                
                if (logoutBehaviour.equalsIgnoreCase("logout")) {
                    logoutURL += "&" + PARAM_LOGGEDOUT + "=logmeout";
                }
                
                if (gotoParam != null && !gotoParam.isEmpty()) {
                    logoutURL = logoutURL + "&" + PARAM_GOTO + "=" + 
                            URLEncoder.encode(gotoParam, "UTF-8");
                } 

                String realm = DNMapper.orgNameToRealmName(ssoToken.getProperty("Organization"));
                logoutURL = logoutURL + "&" + PARAM_LOGOUT_REALM + "=" + URLEncoder.encode(realm, "UTF-8");

                OAuthUtil.debugMessage("OAuth2PostAuthnPlugin: redirecting to: "
                        + logoutURL);

                request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL,
                        logoutURL);
            }
        } catch (Exception ex) {
            OAuthUtil.debugError("OAuth2PostAuthnPlugin: onLogout exception "
                    + "while setting the logout property :", ex);
        }

    }
}