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
 * Copyright 2017-2022 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;

import java.security.AccessController;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jwk.KeyUseConstants;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.dpro.session.ProofOfPossessionTokenRestriction;
import org.forgerock.openam.utils.AMKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.DNMapper;

/**
 * Authenticates a subject via JWT-based Proof of Possession (PoP) challenge-response. The subject provides one or more
 * <em>confirmation keys</em>, either already configured in the subject's identity profile or supplied as part of an
 * earlier authentication step. These confirmation keys are typically public keys for some signature algorithm. The
 * authentication module will produce a challenge JWT that contains a random value and an expiry time. The subject must
 * sign this challenge using its associated private key and return the response within the expiry time.
 * <p>
 * In addition to the signature-based authentication, this module also supports the response being encrypted either
 * using ephemeral ECDH encryption or using a pre-configured key from the user profile. This allows additional metadata
 * to be included in the response to be added to shared state for further authentication modules and post-authentication
 * plugins to process.
 *
 * @since AM 5.5.0
 */
public class JwtProofOfPossession extends AMLoginModule {

    /**
     * Shared state key for storing and retrieving the confirmation key associated with the request. Should be a
     * JsonValue that is the value of the "cnf" claim.
     */
    public static final String SHARED_STATE_CONFIRMATION_KEY = "org.forgerock.openam.authentication.modules.jwtpop.cnf";

    public static final String SHARED_STATE_POP_VERIFIED_CLAIMS =
            "org.forgerock.openam.authentication.modules.jwtpop.verified_claims";

    private static final String TLS_SESSION_ID_ATTR = "javax.servlet.request.ssl_session_id";
    private static final String TLS_SESSION_ID_CLAIM = "tls_sid";

    private static final Logger DEBUG = LoggerFactory.getLogger(JwtProofOfPossession.class);

    private static final int CHALLENGE_STATE = ISAuthConstants.LOGIN_START;
    private static final int RESPONSE_STATE = CHALLENGE_STATE + 1;

    protected static final String CHALLENGE_SIGNING_KEY_ATTR = "challengeSigningKey";
    protected static final String SUBJECT_JWK_SET_ATTR = "subjectJwkSetAttr";
    protected static final String RESPONSE_ENCRYPTION_METHOD_ATTR = "responseEncryptionMethod";
    protected static final String RESPONSE_ENCRYPTION_CIPHER_ATTR = "responseEncryptionCipher";
    protected static final String TLS_SESSION_BINDING_ATTR = "enableTlsSessionBinding";
    protected static final String AUTH_LEVEL_ATTR = "forgerock-am-auth-jwtproofofpossession-auth-level";

    private final AMKeyProvider keyProvider;
    private final ConfirmationKeyLocator confirmationKeyLocator;

    private JWK challengeSigningKey;
    private String subjectJwkSetAttribute;
    private ResponseEncryptionStrategy responseEncryptionStrategy;
    private EncryptionMethod responseEncryptionCipher;

    private Map<String, Object> sharedState;
    private String subject;
    private JWKSet subjectJwkSet;

    private ChallengeResponseVerifier verifier;
    private boolean useTlsSessionBinding;

    protected JwtProofOfPossession(AMKeyProvider keyProvider, ConfirmationKeyType... supportedKeyTypes) {
        this.keyProvider = keyProvider;
        this.confirmationKeyLocator = new ConfirmationKeyLocator(this::getSubjectJwkSet, supportedKeyTypes);
    }

    public JwtProofOfPossession() {
        this(InjectorHolder.getInstance(AMKeyProvider.class), ConfirmationKeyType.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Subject subject, Map sharedState, Map options) {
        this.sharedState = sharedState;

        setAuthLevel(CollectionHelper.getIntMapAttr(options, AUTH_LEVEL_ATTR, 1, DEBUG));

        final String signingKeyId = CollectionHelper.getMapAttr(options, CHALLENGE_SIGNING_KEY_ATTR);
        final KeyPair signingKeyPair = keyProvider.getKeyPair(signingKeyId);
        if (signingKeyPair.getPrivate() instanceof ECPrivateKey) {
            this.challengeSigningKey = new EcJWK((ECPublicKey) signingKeyPair.getPublic(), (ECPrivateKey)
                    signingKeyPair.getPrivate(), KeyUseConstants.SIG, signingKeyId);
        } else if (signingKeyPair.getPrivate() instanceof RSAPrivateKey) {
            this.challengeSigningKey = new RsaJWK(((RSAPublicKey) signingKeyPair.getPublic()),
                    ((RSAPrivateKey) signingKeyPair.getPrivate()), KeyUseConstants.SIG,
                    JwsAlgorithm.RS256.getJwaAlgorithmName(), signingKeyId, null, null, null);
        } else {
            throw new IllegalArgumentException("Invalid signing key");
        }

        this.responseEncryptionStrategy =
                ResponseEncryptionStrategy.valueOf(CollectionHelper.getMapAttr(options, RESPONSE_ENCRYPTION_METHOD_ATTR));
        this.responseEncryptionCipher =
                EncryptionMethod.parseMethod(CollectionHelper.getMapAttr(options, RESPONSE_ENCRYPTION_CIPHER_ATTR));

        if (responseEncryptionCipher == EncryptionMethod.XC20_P1305) {
            // Ensure Bouncy Castle is loaded to support this mode
            if (Security.addProvider(new BouncyCastleProvider()) != -1) {
                DEBUG.debug("JwtProofOfPossession: Registered Bouncy Castle JCE Provider for ChaCha20-Poly1305");
            }
        }

        this.subjectJwkSetAttribute = CollectionHelper.getMapAttr(options, SUBJECT_JWK_SET_ATTR);
        this.useTlsSessionBinding = CollectionHelper.getBooleanMapAttr(options, TLS_SESSION_BINDING_ATTR, true);
    }

    @Override
    public int process(final Callback[] callbacks, final int state) throws LoginException {
        this.subject = (String) sharedState.get(getUserKey());
        if (this.subject == null) {
            throw new AuthLoginException("no subject found in shared state");
        }

        try {
            return processInternal(state, callbacks);
        } catch (LoginException | RuntimeException e) {
            setFailureID(subject);
            throw e;
        }
    }

    private int processInternal(final int state, final Callback...callbacks) throws LoginException {

        switch (state) {
            case CHALLENGE_STATE:
                final JsonValue cnf = (JsonValue) sharedState.get(SHARED_STATE_CONFIRMATION_KEY);
                final JWK confirmationKey = confirmationKeyLocator.resolveConfirmationKey(cnf)
                        .orElseThrow(() -> new AuthLoginException("unable to resolve confirmation key"));

                // Add a restriction to the final issued session token to enforce proof of possession for the life of
                // the session.
                final TokenRestriction tokenRestriction = new ProofOfPossessionTokenRestriction(confirmationKey);
                getLoginState("JwtProofOfPossession.processInternal").setMasterTokenRestriction(tokenRestriction);

                JWK responseEncryptionKeys = responseEncryptionStrategy.getEncryptionKeyPair(this::getSubjectJwkSet);
                final Challenge challenge = Challenge.builder()
                        .audience(subject)
                        .orgDn(getRequestOrg())
                        .signingKey(challengeSigningKey)
                        .responseEncryptionKey(responseEncryptionKeys)
                        .responseEncryptionCipher(responseEncryptionCipher)
                        .claimIfNotNull(TLS_SESSION_ID_CLAIM, getTlsSessionId())
                        .build();

                this.verifier = new ChallengeResponseVerifier(responseEncryptionKeys, confirmationKey, challenge);

                final String realm = DNMapper.orgNameToRealmName(getRequestOrg());
                final HttpCallback callback = challenge.toCallback(realm);

                replaceCallback(RESPONSE_STATE, 0, callback);
                return RESPONSE_STATE;

            case RESPONSE_STATE:
                if (callbacks.length != 1 || !(callbacks[0] instanceof HttpCallback)) {
                    throw new AuthLoginException("invalid callback");
                }
                final HttpCallback response = (HttpCallback) callbacks[0];

                if (response.getAuthorization() == null) {
                    response.setAuthorization(getHttpServletRequest().getHeader(response.getAuthorizationHeader()));
                }

                final JwtClaimsSet verifiedClaims = verifier.verify(response);

                final byte[] actualTlsSessionId = hexDecode(getTlsSessionId());
                if (actualTlsSessionId != null) {
                    final byte[] expectedTlsSessionId = hexDecode(verifiedClaims.getClaim(TLS_SESSION_ID_CLAIM,
                            String.class));
                    if (!MessageDigest.isEqual(expectedTlsSessionId, actualTlsSessionId)) {
                        throw new AuthLoginException("tls session id mismatch");
                    }
                }

                // If there are keys defined in the response then encrypt them and set them to be stored if the
                // user profile is being dynamically created.
                if (verifiedClaims.isDefined("keys")) {
                    final JWKSet registrationKeys = JWKSet.parse(verifiedClaims.toJsonValue());
                    final String encryptedKeys = AccessController.doPrivileged(
                            new EncodeAction(registrationKeys.toJsonString()));

                    setUserAttributes(Collections.singletonMap(subjectJwkSetAttribute,
                            Collections.singleton(encryptedKeys)));
                }
                sharedState.put(SHARED_STATE_POP_VERIFIED_CLAIMS, verifiedClaims);

                return ISAuthConstants.LOGIN_SUCCEED;
            default:
                throw new IllegalStateException("unknown state: " + state);
        }
    }

    private String getTlsSessionId() {
        if (useTlsSessionBinding && getHttpServletRequest().isSecure()) {
            return (String) getHttpServletRequest().getAttribute(TLS_SESSION_ID_ATTR);
        }
        return null;
    }

    private static byte[] hexDecode(String value) {
        try {
            return value == null ? null : DatatypeConverter.parseHexBinary(value);
        } catch (IllegalArgumentException e) {
            DEBUG.error("JwtProofOfPossession: invalid TLS session id '{}': {}", value, e.getMessage());
            return null;
        }
    }

    @Override
    public Principal getPrincipal() {
        return () -> subject;
    }

    @SuppressWarnings("unchecked")
    protected JWKSet getSubjectJwkSet() {
        if (this.subjectJwkSet == null) {
            final AMIdentityRepository identityRepository = getAMIdentityRepository(getRequestOrg());
            final IdSearchControl searchControl = new IdSearchControl();
            searchControl.setReturnAttributes(Collections.singleton(subjectJwkSetAttribute));
            searchControl.setMaxResults(1);
            searchControl.setAllReturnAttributes(false);
            try {
                final IdSearchResults result = identityRepository.searchIdentitiesByUsername(IdType.USER, subject, searchControl);
                if (result.getErrorCode() != IdSearchResults.SUCCESS) {
                    DEBUG.debug("JwtProofOfPossession.getSubjectJwkSet(): error result: {}", result.getErrorCode());
                    return new JWKSet();
                }

                final AMIdentity profile = (AMIdentity) getFirstItem(result.getSearchResults());
                if (profile == null) {
                    DEBUG.debug("JwtProofOfPossession.getSubjectJwkSet(): no identity profile for user: {}", subject);
                    return new JWKSet();
                }
                final String jwkSetStr = (String) getFirstItem(profile.getAttribute(subjectJwkSetAttribute));
                if (jwkSetStr == null) {
                    DEBUG.debug("JwtProofOfPossession.getSubjectJwkSet(): null jwk set for user: {}", subject);
                    return new JWKSet();
                }
                final String decryptedJwkSet = AccessController.doPrivileged(new DecodeAction(jwkSetStr));
                this.subjectJwkSet = JWKSet.parse(decryptedJwkSet);
            } catch (IdRepoException | SSOException e) {
                throw new IllegalStateException("unable to load user subject");
            }
        }
        return this.subjectJwkSet;
    }
}
