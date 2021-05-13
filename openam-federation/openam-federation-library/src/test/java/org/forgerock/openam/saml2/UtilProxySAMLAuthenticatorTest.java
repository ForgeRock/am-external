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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.forgerock.openam.saml2.UtilProxySAMLAuthenticator.SAML_2_LOCAL_STORAGE_JWT_ENCRYPTION;
import static org.forgerock.openam.saml2.UtilProxySAMLAuthenticatorLookup.SAML_2_LOCAL_STORAGE_JWT_DECRYPTION;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.jwt.JwtDecryptionHandler;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.assertion.impl.AuthnContextImpl;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;


public class UtilProxySAMLAuthenticatorTest {

    private static final String IDP_ENTITY_ID = "idp.localtest.me";
    private static final String KID = "testKey";
    private final SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");
    private IDPSSOFederateRequest idpSsoRequest;
    private SecretsProviderFacade secretsProviderFacade;
    private Promise<DataEncryptionKey, NoSuchSecretException> encryptionPromise;
    private Promise<DataDecryptionKey, NoSuchSecretException> decryptionPromise;
    private JwtEncryptionOptions jwtEncryptionOptions;

    @BeforeMethod
    public void setUp() throws Exception {
        idpSsoRequest = createIdpSsoRequest();
        secretsProviderFacade = mock(SecretsProviderFacade.class);
        encryptionPromise = getEncryptionPromise();
        decryptionPromise = getDecryptionPromise();
        jwtEncryptionOptions = new JwtEncryptionOptions(
                secretsProviderFacade,
                JweAlgorithm.DIRECT, EncryptionMethod.A256GCM, CompressionAlgorithm.DEF);
    }

    @Test
    public void shouldCreateEncryptedJwtForSamlAuthnRequest() throws Exception {
        UtilProxySAMLAuthenticator utilProxySAMLAuthenticator = new UtilProxySAMLAuthenticator(idpSsoRequest, null,
                null, null, false, jwtEncryptionOptions);

        given(secretsProviderFacade.getActiveSecret(SAML_2_LOCAL_STORAGE_JWT_ENCRYPTION)).willReturn(
                encryptionPromise);
        given(secretsProviderFacade.getNamedSecret(SAML_2_LOCAL_STORAGE_JWT_ENCRYPTION, KID)).willReturn(
                encryptionPromise);
        given(secretsProviderFacade.getActiveSecret(SAML_2_LOCAL_STORAGE_JWT_DECRYPTION)).willReturn(
                decryptionPromise);
        given(secretsProviderFacade.getNamedSecret(SAML_2_LOCAL_STORAGE_JWT_DECRYPTION, KID)).willReturn(
                decryptionPromise);

        //when
        String saml2RequestJwt = utilProxySAMLAuthenticator.createSaml2RequestJwt(idpSsoRequest);

        //then
        EncryptedJwt encryptedJwt = new JwtReconstruction().reconstructJwt(saml2RequestJwt, EncryptedJwt.class);
        assertThat(encryptedJwt.getHeader().get("kid").asString()).isEqualTo(KID);

        SignedJwt decryptedJwt = new JwtDecryptionHandler(jwtEncryptionOptions)
                .decryptJwe(encryptedJwt, SAML_2_LOCAL_STORAGE_JWT_DECRYPTION);
        assertThat(decryptedJwt.getHeader().getAlgorithm()).isEqualTo(JwsAlgorithm.NONE);

        JwtClaimsSet decryptedJwtClaimsSet = decryptedJwt.getClaimsSet();
        assertThat(decryptedJwtClaimsSet.getIssuer()).isEqualTo(IDP_ENTITY_ID);
        assertThat(decryptedJwtClaimsSet.getType()).isEqualTo(SAML2Constants.SAML2_REQUEST_JWT_TYPE);
        assertThat(decryptedJwtClaimsSet.getExpirationTime()).isNotNull();
        assertThat(decryptedJwtClaimsSet.get("authnRequest").asString()).contains("<samlp:AuthnRequest").contains(
                idpSsoRequest.getAuthnRequest().getID());
        assertThat(decryptedJwtClaimsSet.get("authnContext").asString()).contains("<saml:AuthnContext");
        assertThat(decryptedJwtClaimsSet.get("relayState").asString()).isEqualTo(idpSsoRequest.getRelayState());
    }

    private IDPSSOFederateRequest createIdpSsoRequest() throws SAML2Exception {
        IDPSSOFederateRequest idpSsoRequest = new IDPSSOFederateRequest("123", "/", null, "idp", IDP_ENTITY_ID);
        AuthnRequest authnReq = new AuthnRequestImpl();
        String authnID = "authnID";
        authnReq.setID(authnID);
        authnReq.setVersion(SAML2Constants.VERSION_2_0);
        authnReq.setIssueInstant(new Date());
        idpSsoRequest.setAuthnRequest(authnReq);
        AuthnContextImpl matchingAuthnContext = new AuthnContextImpl();
        matchingAuthnContext.setAuthnContextClassRef("blah");
        idpSsoRequest.setMatchingAuthnContext(matchingAuthnContext);
        String relayState = "myRelayState";
        idpSsoRequest.setRelayState(relayState);
        return idpSsoRequest;
    }

    private Promise<DataEncryptionKey, NoSuchSecretException> getEncryptionPromise() throws NoSuchSecretException {
        DataEncryptionKey dataEncryptionKey = new SecretBuilder()
                .secretKey(key)
                .expiresAt(Instant.MAX)
                .stableId(KID)
                .build(Purpose.DATA_ENCRYPTION);
        return Promises.newResultPromise(dataEncryptionKey);
    }

    private Promise<DataDecryptionKey, NoSuchSecretException> getDecryptionPromise() throws NoSuchSecretException {
        DataDecryptionKey dataDecryptionKey = new SecretBuilder()
                .secretKey(key)
                .expiresAt(Instant.MAX)
                .stableId(KID)
                .build(Purpose.DATA_DECRYPTION);
        return Promises.newResultPromise(dataDecryptionKey);
    }
}

