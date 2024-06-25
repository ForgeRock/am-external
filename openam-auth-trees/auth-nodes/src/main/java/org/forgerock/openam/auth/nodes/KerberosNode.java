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
 * Copyright 2017-2024 ForgeRock AS.
 * Portions Copyrighted 2020 IC Consult.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.util.Pair;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.util.DerValue;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.FileExistenceValidator;

/**
 * Kerberos Node.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = KerberosNode.Config.class,
        tags = {"basic authn", "basic authentication", "kerberos", "windows"},
        namespace = Namespace.PRODUCT)
public class KerberosNode extends AbstractDecisionNode {

    private static final String REALM_SEPARATOR = "@";
    private static final String NEGOTIATE = "Negotiate";
    private static final String AUTHORIZATION = "Authorization";
    private static final byte[] SPNEGO_OID = {
        (byte) 0x06, (byte) 0x06, (byte) 0x2b, (byte) 0x06, (byte) 0x01,
        (byte) 0x05, (byte) 0x05, (byte) 0x02};
    private static final byte[] KERBEROS_V5_OID = {
        (byte) 0x06, (byte) 0x09, (byte) 0x2a, (byte) 0x86, (byte) 0x48,
        (byte) 0x86, (byte) 0xf7, (byte) 0x12, (byte) 0x01, (byte) 0x02,
        (byte) 0x02};
    private static final String JSON_PARAMETER_NAME = "jsonContent";
    private static final String FAILURE_ATTRIBUTE = "failure";
    private static final String REASON_ATTRIBUTE = "reason";
    private static final Logger logger = LoggerFactory.getLogger(KerberosNode.class);
    private final Config config;
    private final Realm realm;
    private final IdentityUtils identityUtils;
    private final CoreWrapper coreWrapper;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The name of the Kerberos principal used during authentication.
         * @return The Kerberos principal.
         */
        @Attribute(order = 100, requiredValue = true)
        String principalName();

        /**
         * The absolute pathname of the AD keytab file.
         * @return The absolute path to the keytab file.
         */
        @Attribute(order = 200, requiredValue = true, validators = {FileExistenceValidator.class})
        String keytabFileName();

        /**
         * The name of the Kerberos (Active Directory) realm used for authentication.
         * @return The Kerberos realm.
         */
        @Attribute(order = 300, requiredValue = true)
        String kerberosRealm();

        /**
         * The hostname/IP address of the Kerberos (Active Directory) server.
         * @return The Kerberos server hostname or IP address.
         */
        @Attribute(order = 400, requiredValue = true)
        String kerberosServerName();

        /**
         * Set containing the Trusted Kerberos Realms for User Kerberos tickets.
         * @return A set containing the trusted Kerberos realms.
         */
        @Attribute(order = 500, requiredValue = true)
        Set<String> trustedKerberosRealms();

        /**
         * Returns the fully qualified name of the authenticated user rather than just the username.
         * @return True if the fully qualified name of the authenticated user; false if just the username.
         */
        @Attribute(order = 600)
        default boolean returnPrincipalWithDomainName() {
            return false;
        }

        /**
         * Validate that the user has a matched user profile configured in the data store.
         * @return True if the user should be validated against the data store
         */
        @Attribute(order = 700)
        default boolean lookupUserInRealm() {
            return false;
        }

        /**
         * True, if initiator. False, if acceptor only.
         * @return True if the service is the initiator.
         */
        @Attribute(order = 800)
        default boolean kerberosServiceIsInitiator() {
            return true;
        }
    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm  The realm the node is in.
     * @param identityUtils A {@code IdentityUtils} instance.
     * @param coreWrapper The {@code CoreWrapper} instance.
     */
    @Inject
    public KerberosNode(@Assisted Config config, @Assisted Realm realm,
            IdentityUtils identityUtils, CoreWrapper coreWrapper) {
        this.config = config;
        this.realm = realm;
        this.identityUtils = identityUtils;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) {

        JsonValue newSharedState = context.sharedState.copy();

        HttpServletRequest request = context.request.servletRequest;
        if (request != null && hasWDSSOFailed(request)) {
            logger.warn("Http Auth Failed");
            return goTo(false).build();
        }

        if (!context.getCallback(HttpCallback.class).isPresent()) {
            return Action.send(new HttpCallback(AUTHORIZATION, "WWW-Authenticate", NEGOTIATE, 401)).build();
        }

        Subject serviceSubject = serviceLogin();

        byte[] spnegoToken = getSPNEGOTokenFromHTTPRequest(Objects.requireNonNull(request));
        if (spnegoToken == null) {
            spnegoToken = getSPNEGOTokenFromCallback(context.getCallbacks(HttpCallback.class));
        }

        if (spnegoToken == null) {
            return logAndReturnFalse(newSharedState, "SPNEGO token is not valid.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("SPNEGO token: \n{}", DerValue.printByteArray(spnegoToken, 0, spnegoToken.length));
        }
        final byte[] kerberosToken = parseToken(spnegoToken);

        if (kerberosToken == null) {
            return logAndReturnFalse(newSharedState, "Kerberos token is not valid.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Kerberos token retrieved from SPNEGO token: \n{}",
                    DerValue.printByteArray(kerberosToken, 0, kerberosToken.length));
        }

        Pair<String, Optional<String>> usernameAndMaybeUniversalId;
        try {
            usernameAndMaybeUniversalId = authenticateToken(serviceSubject, kerberosToken,
                    config.trustedKerberosRealms());
            if (isNotEmpty(usernameAndMaybeUniversalId.getFirst())) {
                logger.debug("Authentication succeeded with new cred.");
                return setUserNameAndReturnTrue(usernameAndMaybeUniversalId.getFirst(),
                        usernameAndMaybeUniversalId.getSecond(), newSharedState);
            }
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            logger.error("Exception thrown trying to authenticate the user\n" + ExceptionUtils.getStackTrace(e));
            if (e instanceof GSSException) {
                int major = ((GSSException) e).getMajor();
                if (major == GSSException.CREDENTIALS_EXPIRED) {
                    logger.debug("Credential expired. Re-establish credential...");
                    serviceSubject = serviceLogin();
                    try {
                        usernameAndMaybeUniversalId = authenticateToken(serviceSubject, kerberosToken,
                                config.trustedKerberosRealms());
                        if (isNotEmpty(usernameAndMaybeUniversalId.getFirst())) {
                            logger.debug("Authentication succeeded with new cred.");
                            return setUserNameAndReturnTrue(usernameAndMaybeUniversalId.getFirst(),
                                    usernameAndMaybeUniversalId.getSecond(), newSharedState);
                        }
                    } catch (PrivilegedActionException ex) {
                        logger.warn("Kerberos authentication failed: {}", ex.getMessage(), ex);
                        return goTo(false).build();
                    }
                }
            } else {
                logger.warn("Kerberos authentication failed with PrivilegedActionException wrapped GSSException: {}",
                        e.getMessage(), e);
            }
        }
        return goTo(false).replaceSharedState(newSharedState).build();
    }

    private Action logAndReturnFalse(JsonValue newSharedState, String text, Object... objects) {
        logger.warn(text, objects);
        return goTo(false).replaceSharedState(newSharedState).build();
    }

    private Action setUserNameAndReturnTrue(String username, Optional<String> universalId, JsonValue newSharedState) {
        var action = goTo(true)
                .withUniversalId(universalId);
        if (username != null) {
            newSharedState.put(SharedStateConstants.USERNAME, username);
            action.withIdentifiedIdentity(username, IdType.USER.getName());
        }
        return action.replaceSharedState(newSharedState)
                       .build();
    }

    private Pair<String, Optional<String>> authenticateToken(final Subject serviceSubject, final byte[] kerberosToken,
            final Set<String> trustedRealms) throws PrivilegedActionException {

        if (serviceSubject == null) {
            logger.error("Service subject is null");
            return Pair.empty();
        }

        return Subject.doAs(serviceSubject, (PrivilegedExceptionAction<Pair<String, Optional<String>>>) () -> {
            GSSContext context = GSSManager.getInstance().createContext((GSSCredential) null);
            logger.debug("Context created.");
            byte[] outToken = context.acceptSecContext(kerberosToken, 0, kerberosToken.length);

            if (outToken != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Token returned from acceptSecContext: \n"
                            + DerValue.printByteArray(outToken, 0, outToken.length));
                }
            }

            if (!context.isEstablished()) {
                logger.error("#authenticateToken: Cannot establish context !");
                return Pair.empty();
            } else {
                logger.debug("Context established !");
                GSSName user = context.getSrcName();
                final String userPrincipalName = user.toString();

                // If the whitelist is empty, do not enforce it. This prevents issues with upgrading, and is the
                // expected default behaviour.
                if (!trustedRealms.isEmpty()) {
                    boolean foundTrustedRealm = false;
                    for (final String trustedRealm : trustedRealms) {
                        if (isTokenTrusted(userPrincipalName, trustedRealm)) {
                            foundTrustedRealm = true;
                            break;
                        }
                    }
                    if (!foundTrustedRealm) {
                        logger.error("Kerberos token for {} is not trusted", userPrincipalName);
                        return Pair.empty();
                    }
                }
                // Check if the user account from the Kerberos ticket exists in the realm.
                String userValue = getUserName(userPrincipalName);
                Optional<String> universalId = Optional.empty();
                if (config.lookupUserInRealm()) {
                    universalId = identityUtils.getUniversalId(userValue, realm.asPath(), USER);
                    AMIdentity identity = universalId.map(coreWrapper::getIdentity).orElse(null);
                    if (identity == null || !identity.isExists() || !identity.isActive()) {
                        throw new NodeProcessException(
                                "KerberosNode.authenticateToken: " + ": Unable to find the user "
                                        + userValue + " in org" + realm.toString());
                    }
                }
                logger.debug("KerberosNode.authenticateToken:" + "User authenticated: " + user.toString());
                context.dispose();
                return Pair.of(userValue, universalId);
            }
        });
    }

    /**
     * Checks the request for an attribute "http-auth-failed".
     *
     * @param request THe HttpServletRequest.
     * @return If the attribute is present and set to true, true is returned, otherwise false is returned.
     */
    private boolean hasWDSSOFailed(HttpServletRequest request) {
        try {
            JsonValue jsonBody = JsonValue.json(Json.readJson(request.getParameter(JSON_PARAMETER_NAME)));
            return jsonBody.isDefined(FAILURE_ATTRIBUTE) && jsonBody.isDefined(REASON_ATTRIBUTE)
                    && jsonBody.get(FAILURE_ATTRIBUTE).asBoolean().equals(true)
                    && jsonBody.get(REASON_ATTRIBUTE).asString().equals("http-auth-failed");
        } catch (IOException e) {
            logger.warn(
                    "IOException occurred while trying to extract JSON Attributes {} and {} from request parameter {}",
                    FAILURE_ATTRIBUTE, REASON_ATTRIBUTE, JSON_PARAMETER_NAME);
            logger.debug(
                    "IOException occurred while trying to extract JSON Attributes {} and {} from request parameter {}",
                    FAILURE_ATTRIBUTE, REASON_ATTRIBUTE, JSON_PARAMETER_NAME, e);
            return true;
        }
    }

    private byte[] getSPNEGOTokenFromHTTPRequest(HttpServletRequest req) {
        byte[] spnegoToken = null;
        String header = req.getHeader(AUTHORIZATION);
        if ((header != null) && header.startsWith(NEGOTIATE)) {
            header = header.substring(NEGOTIATE.length()).trim();
            spnegoToken = Base64.decode(header);
        }
        return spnegoToken;
    }

    private byte[] getSPNEGOTokenFromCallback(List<HttpCallback> callbacks) {
        byte[] spnegoToken = null;
        if (callbacks != null && callbacks.size() != 0) {
            String spnegoTokenStr = callbacks.get(0).getAuthorization();
            spnegoToken = Base64.decode(spnegoTokenStr);
        }

        return spnegoToken;
    }

    private byte[] parseToken(byte[] rawToken) {
        byte[] token = rawToken;
        DerValue tmpToken = new DerValue(rawToken);
        if (logger.isDebugEnabled()) {
            logger.debug("token tag: {}", DerValue.printByte(tmpToken.getTag()));
        }
        if (tmpToken.getTag() != (byte) 0x60) {
            return null;
        }

        ByteArrayInputStream tmpInput = new ByteArrayInputStream(tmpToken.getData());

        // check for SPNEGO OID
        byte[] oidArray = new byte[SPNEGO_OID.length];
        tmpInput.read(oidArray, 0, oidArray.length);
        if (Arrays.equals(oidArray, SPNEGO_OID)) {
            logger.debug("SPNEGO OID found in the Auth Token");
            tmpToken = new DerValue(tmpInput);

            // 0xa0 indicates an init token(NegTokenInit); 0xa1 indicates an
            // response arg token(NegTokenTarg). no arg token is needed for us.

            if (tmpToken.getTag() == (byte) 0xa0) {
                logger.debug("DerValue: found init token");
                tmpToken = new DerValue(tmpToken.getData());
                if (tmpToken.getTag() == (byte) 0x30) {
                    logger.debug("DerValue: 0x30 constructed token found");
                    tmpInput = new ByteArrayInputStream(tmpToken.getData());
                    tmpToken = new DerValue(tmpInput);

                    // In an init token, it can contain 4 optional arguments:
                    // a0: mechTypes
                    // a1: contextFlags
                    // a2: octect string(with leading char 0x04) for the token
                    // a3: message integrity value

                    while (tmpToken.getTag() != (byte) -1 && tmpToken.getTag() != (byte) 0xa2) {
                        // look for next mech token DER
                        tmpToken = new DerValue(tmpInput);
                    }
                    if (tmpToken.getTag() != (byte) -1) {
                        // retrieve octet string
                        tmpToken = new DerValue(tmpToken.getData());
                        token = tmpToken.getData();
                    }
                }
            }
        } else {
            logger.debug("SPNEGO OID not found in the Auth Token");
            byte[] krb5Oid = new byte[KERBEROS_V5_OID.length];
            int i = 0;
            for (; i < oidArray.length; i++) {
                krb5Oid[i] = oidArray[i];
            }
            tmpInput.read(krb5Oid, i, krb5Oid.length - i);
            if (!Arrays.equals(krb5Oid, KERBEROS_V5_OID)) {
                logger.debug("Kerberos V5 OID not found in the Auth Token");
                token = null;
            } else {
                logger.debug("Kerberos V5 OID found in the Auth Token");
            }
        }
        return token;
    }

    private String getUserName(String user) {
        String userName = user;
        if (!config.returnPrincipalWithDomainName()) {
            int index = user.indexOf(REALM_SEPARATOR);
            if (index != -1) {
                userName = user.substring(0, index);
            }
        }
        return userName;
    }

    private Subject serviceLogin() {
        logger.debug("New Service Login ...");
        System.setProperty("java.security.krb5.realm", config.kerberosRealm());
        System.setProperty("java.security.krb5.kdc", config.kerberosServerName());

        Configuration configuration = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                String kerberosModuleName = SystemProperties.get(Constants.KRB5_AUTH_NODE_LOGINMODULE,
                        Constants.DEFAULT_KRB5_LOGINMODULE);
                String credType = SystemProperties.get(Constants.KRB5_NODE_CREDENTIAL_TYPE, "acceptor");

                HashMap<String, String> hashmap = new HashMap<>();
                hashmap.put("principal", config.principalName());
                if (kerberosModuleName.equalsIgnoreCase("com.ibm.security.auth.module.Krb5LoginModule")) {
                    hashmap.put("useKeytab", config.keytabFileName());
                    hashmap.put("refreshKrb5Config", "false");
                    if (config.kerberosServiceIsInitiator() && "acceptor".equalsIgnoreCase(credType)) {
                        credType = "both";
                    }
                    hashmap.put("credsType", credType);
                } else {
                    hashmap.put("isInitiator", Boolean.toString(config.kerberosServiceIsInitiator()));
                    hashmap.put("storeKey", "true");
                    hashmap.put("useKeyTab", "true");
                    hashmap.put("keyTab", config.keytabFileName());
                    hashmap.put("doNotPrompt", "true");
                    hashmap.put("refreshKrb5Config", "false");
                }
                logger.info(KerberosNode.class.getName() + " configuration...");
                hashmap.entrySet().forEach(entry -> {
                    logger.info(entry.getKey() + " = " + entry.getValue());
                });
                return new AppConfigurationEntry[]{ new AppConfigurationEntry(
                        kerberosModuleName,
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        hashmap)
                };
            }
        };

        LoginContext lc;
        // perform service authentication using JDK Kerberos module
        try {
            lc = new LoginContext(this.getClass().getName(),
                    null, null, configuration);
            lc.login();
        } catch (LoginException e) {
            logger.error("Error while performing service login", e);
            return null;
        }
        Subject serviceSubject = lc.getSubject();
        logger.debug("Service login succeeded.");
        return serviceSubject;
    }


    /**
     * Iterate until we extract the real exception
     * from PrivilegedActionException(s).
     */
    private Exception extractException(Exception e) {
        while (e instanceof PrivilegedActionException) {
            e = ((PrivilegedActionException) e).getException();
        }
        return e;
    }

    private boolean isTokenTrusted(final String upn, final String realm) {
        boolean trusted = false;
        if (upn != null) {
            final int index = upn.indexOf(REALM_SEPARATOR);
            if (index != -1) {
                final String realmPart = upn.substring(index + 1);
                if (realmPart.equalsIgnoreCase(realm)) {
                    trusted = true;
                }
            }
        }
        return trusted;
    }

}
