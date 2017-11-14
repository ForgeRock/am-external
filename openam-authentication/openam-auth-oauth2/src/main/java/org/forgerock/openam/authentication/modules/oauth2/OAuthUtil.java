/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2017 ForgeRock AS. All rights reserved.
 * Copyright © 2011 Cybernetica AS.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.authentication.modules.oauth2;

import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;
import org.forgerock.openam.xui.XUIState;

import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;

public class OAuthUtil  {

    private static Debug debug = Debug.getInstance("amAuth");

    public static StringBuilder getOriginalUrl(HttpServletRequest request) {
        StringBuilder originalUrl = new StringBuilder();
        String requestedQuery = request.getQueryString();
        String realm = null;

        String authCookieName = AuthUtils.getAuthCookieName();

        final XUIState xuiState = InjectorHolder.getInstance(XUIState.class);

        if (xuiState.isXUIEnabled()) {
            // When XUI is in use the request URI points to the authenticate REST endpoint, which shouldn't be
            // presented to the end-user, hence we use the contextpath only and rely on index.html and the
            // XUIFilter to direct the user towards the XUI.
            originalUrl.append(request.getContextPath());
            // The REST endpoint always exposes the realm parameter even if it is not actually present on the
            // query string (e.g. DNS alias or URI segment was used), so this logic here is just to make sure if
            // the realm parameter was not present on the querystring, then we add it there.
            if (requestedQuery != null && !requestedQuery.contains("realm=")) {
                realm = request.getParameter("realm");
            }
        } else {
            //In case of legacy UI the request URI will be /openam/UI/Login, which is safe to use.
            originalUrl.append(request.getRequestURI());
        }

        if (StringUtils.isNotEmpty(realm)) {
            originalUrl.append("?realm=").append(urlEncodeQueryParameterNameOrValue(realm));
        }

        if (requestedQuery != null) {
            if (requestedQuery.endsWith(authCookieName + "=")) {
                requestedQuery = requestedQuery.substring(0,
                        requestedQuery.length() - authCookieName.length() - 1);
            }
            originalUrl.append(originalUrl.indexOf("?") == - 1 ? '?' : '&');
            originalUrl.append(requestedQuery);
        }
        return originalUrl;
    }

    public static String findCookie(HttpServletRequest request, String cookieName) {

        String result = "";
        String value = CookieUtils.getCookieValueFromReq(request, cookieName);
        if (value != null) {
            result = value;
            debugMessage("OAuthUtil.findCookie()" + "Cookie "
                        + cookieName
                        + " found. "
                        + "Content is: " + value);
        }

        return result;
    }
    
    public static String getParamValue(String query, String param) {

        String paramValue = "";
        if (query != null && query.length() != 0) {
            String[] paramsArray = query.split("\\&");
            for (String parameter : paramsArray) {
                if (parameter.startsWith(param)) {
                    paramValue = parameter.substring(parameter.indexOf("=") + 1);
                    break;
                }
            }
        }
        return paramValue;
    }
    
    static boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }
    
    public static void sendEmail(String from, String emailAddress, String activCode,
              Map<String, String> SMTPConfig, ResourceBundle bundle, String linkURL)
    throws NoEmailSentException {
        try {
            String gatewayEmailImplClass = SMTPConfig.get(KEY_EMAIL_GWY_IMPL);
            if (from != null || emailAddress != null) {
                // String from = bundle.getString(MESSAGE_FROM);
                String subject = bundle.getString(MESSAGE_SUBJECT);
                String message = bundle.getString(MESSAGE_BODY);
                message = message.replace("#ACTIVATION_CODE#", activCode);
                
                String link = "";
                try {
                     link = linkURL + "?" + PARAM_ACTIVATION + "=" +
                     URLEncoder.encode(activCode, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                   debugError("OAuthUtil.sendEmail(): Error while encoding", ex);
                }

                message = message.replace("#ACTIVATION_LINK#", link.toString());
                EmailGateway gateway =  Class.forName(gatewayEmailImplClass).
                        asSubclass(EmailGateway.class).newInstance();
                gateway.sendEmail(from, emailAddress, subject, message, SMTPConfig);
                debugMessage("OAuthUtil.sendEmail(): sent email to " +
                            emailAddress);
            } else {
                  debugMessage("OAuthUtil.sendEmail(): unable to send email");

            }
        } catch (ClassNotFoundException cnfe) {
            debugError("OAuthUtil.sendEmail(): " + "class not found " +
                        "EmailGateway class", cnfe);
        } catch (InstantiationException ie) {
            debugError("OAuthUtil.sendEmail(): " + "can not instantiate " +
                        "EmailGateway class", ie);
        } catch (IllegalAccessException iae) {
            debugError("OAuthUtil.sendEmail(): " + "can not access " +
                        "EmailGateway class", iae);
        }
    }
    
    public static boolean debugMessageEnabled() {
        return debug.messageEnabled();
    } 
    
    public static void debugMessage(String message) {
        if (debug.messageEnabled()) {
            debug.message(message);
        }
    }

    public static void debugWarning(String message) {
        if (debug.warningEnabled()) {
            debug.warning(message);
        }
    }

    /**
     * Helper method for warning debug statements
     * @see Debug#warning(String, Object...)
     *
     * @param message The debug message format.
     * @param params The parameters to the message, optionally with a {@code Throwable} as
     *               the last parameter.
     */
    public static void debugWarning(String message, Object... params) {
        if (debug.warningEnabled()) {
            debug.warning(message, params);
        }
    }
    
    public static void debugError(String message, Throwable t) {
        if (debug.errorEnabled()) {
            debug.error(message, t);
        }
    }
    
    public static void debugError(String message) {
        if (debug.errorEnabled()) {
            debug.error(message);
        }
    }
    
    public static String oAuthEncode(String toEncode) throws UnsupportedEncodingException {
        if (toEncode != null && !toEncode.isEmpty()) {
            return URLEncoder.encode(toEncode, "UTF-8").
                    replace("+", "%20").
                    replace("*", "%2A").
                    replace("%7E", "~");

        } else {
            return "";
        }
    }

    /**
     * Search for the user in the realm, using the instantiated account mapper.
     *
     * @param realm
     *         The realm in which the user belongs.
     * @param accountProvider
     *         The Account Provider instance using which the user account can be found.
     * @param userNames
     *         The username.
     *
     * @return The user name if exist; <code>null<</code> when not found.
     */
    public static String getUser(String realm, AccountProvider accountProvider,
            Map<String, Set<String>> userNames) {

        String user = null;
        if ((userNames != null) && !userNames.isEmpty()) {
            AMIdentity userIdentity = accountProvider.searchUser(
                    AMLoginModule.getAMIdentityRepository(realm), userNames);
            if (userIdentity != null) {
                user = userIdentity.getName();
            }
        }

        return user;
    }

    /**
     * Returns normalized user lookup attribute either from the profile info or id token.
     *
     * @param svcProfileResponse
     *         The Profile response from the IDP.
     * @param attributeMapperConfig
     *         The configured attribute mapper configuration.
     * @param attributeMapper
     *         The configured attribute mapper.
     * @param jwtClaims
     *         The id token claim.
     *
     * @return Normalized attributes of the user.
     *
     * @throws AuthLoginException
     *         when normalization fails.
     */
    public static Map getAttributes(String svcProfileResponse, Map<String, String> attributeMapperConfig,
            AttributeMapper attributeMapper, JwtClaimsSet jwtClaims) throws AuthLoginException {
        try {
            attributeMapper.getClass().getDeclaredMethod("getAttributes", Map.class, String.class);
            return attributeMapper.getAttributes(attributeMapperConfig, svcProfileResponse);
        } catch (NoSuchMethodException e) {
            return attributeMapper.getAttributes(attributeMapperConfig, jwtClaims);
        }
    }

    /**
     * Returns normalized attributes either from the profile info or id token.
     *
     * @param svcProfileResponse
     *         The Profile response from the IDP.
     * @param attributeMapperConfig
     *         The configured attribute mapper configuration.
     * @param attributeMappers
     *         The configured attribute mappers.
     * @param jwtClaims
     *         The id token claim.
     *
     * @return Normalized attributes of the user.
     *
     * @throws AuthLoginException
     *         when normalization fails.
     */
    public static Map<String, Set<String>> getAttributesMap(Map<String, String> attributeMapperConfig,
            Set<String> attributeMappers, String svcProfileResponse, JwtClaimsSet jwtClaims) {
        Map<String, Set<String>> attributes = new HashMap<>();
        for (String attributeMapperClassname : attributeMappers) {
            try {
                AttributeMapper attributeMapper = getConfiguredType(AttributeMapper.class, attributeMapperClassname);
                attributeMapper.init(OAuthParam.BUNDLE_NAME);
                attributes.putAll(getAttributes(svcProfileResponse, attributeMapperConfig, attributeMapper, jwtClaims));
            } catch (ClassCastException ex) {
                debugError("Attribute Mapper is not actually an implementation of AttributeMapper.", ex);
            } catch (Exception ex) {
                debugError("OAuth.getUser: Problem when trying to get the Attribute Mapper", ex);
            }
        }
        debugMessage("OAuth.getUser: creating new user; attributes = " + attributes);
        return attributes;
    }

    /**
     * Creates an instance of Account Mapper.
     *
     * @param className The name of the class which implements Account Mapper.
     * @return The Account Mapper class name.
     * @throws AuthLoginException when instantiation fails.
     */
    public static AttributeMapper<?> instantiateAccountMapper(String className) throws AuthLoginException {
        return instantiateByClass(AttributeMapper.class, className);
    }

    /**
     * Creates an instance of the Account Provider.
     * @param className The Account Provider class name.
     * @return Account Provider instance.
     * @throws AuthLoginException
     */
    public static AccountProvider instantiateAccountProvider(String className) throws AuthLoginException {
        return instantiateByClass(AccountProvider.class, className);
    }

    private static <T> T instantiateByClass(Class<T> classType, String className) throws AuthLoginException {
        try {
            return getConfiguredType(classType, className);
        } catch (ClassCastException ex) {
            debugError(className + "is not actually an implementation of " + classType, ex);
            throw new AuthLoginException("Problem when trying to instantiate " + className, ex);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException |
                IllegalAccessException | ClassNotFoundException e) {
            throw new AuthLoginException("Problem when trying to instantiate " + className, e);
        }
    }

    private static <T> T getConfiguredType(Class<T> type, String config) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String[] parameters = new String[0];
        int delimiter = config.indexOf('|');
        if (delimiter > -1) {
            parameters = config.substring(delimiter + 1).split("\\|");
            config = config.substring(0, delimiter);
        }

        Class<? extends T> clazz = Class.forName(config).asSubclass(type);
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        Arrays.fill(parameterTypes, String.class);
        return clazz.getConstructor(parameterTypes).newInstance(parameters);
    }

}
