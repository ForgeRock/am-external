package org.forgerock.openam.auth.nodes.jwt;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.forgerock.openam.auth.nodes.HmacProvider;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

public class PersistentJwtStringSupplierTest extends GuiceTestCase {

    @Mock
    AuthKeyFactory authKeyFactory;

    private PersistentJwtStringSupplier persistentJwtStringSupplier;
    private PersistentJwtStringSupplier persistentJwtStringSupplierWithMockAuthKeyFactory;
    private String hmacKey;

    @BeforeMethod
    public void before() throws FileNotFoundException, SMSException, SSOException {
        initMocks(this);
        persistentJwtStringSupplier = InjectorHolder.getInstance(PersistentJwtStringSupplier.class);
        PersistentJwtProvider persistentJwtProvider = InjectorHolder.getInstance(PersistentJwtProvider.class);
        persistentJwtStringSupplierWithMockAuthKeyFactory = new PersistentJwtStringSupplier(new JwtBuilderFactory(),
                persistentJwtProvider, authKeyFactory);
        hmacKey = HmacProvider.generateSigningKey(HmacProvider.KeySize.BITS_256);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testCreateJwtStringWithNullHmacKey() throws Exception {
        persistentJwtStringSupplier.createJwtString("orgname", getEmptyAuthContext(), 40, 20, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testCreateJwtStringWithEmptyHmacKey() throws Exception {
        persistentJwtStringSupplier.createJwtString("orgname", getEmptyAuthContext(), 40, 20, "");
    }

    @Test
    public void testGetUpdatedJwtWithNullJwtCookie() throws Exception {
        String jwtString = persistentJwtStringSupplier.getUpdatedJwt(null, "orgName", "key", 10);
        assertThat(jwtString).isNull();
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testBuildEncryptedJwtStringCatchesFileNotFoundException() throws Exception {
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenThrow(new FileNotFoundException());
        persistentJwtStringSupplierWithMockAuthKeyFactory.createJwtString("orgname", getEmptyAuthContext(), 40, 20,
                hmacKey);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testBuildEncryptedJwtStringCatchesSMSException() throws Exception {
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenThrow(new SMSException());
        persistentJwtStringSupplierWithMockAuthKeyFactory.createJwtString("orgname", getEmptyAuthContext(), 40, 20,
                hmacKey);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testBuildEncryptedJwtStringCatchesSSOException() throws Exception {
        when(authKeyFactory.getPublicAuthKey(any(), any())).thenThrow(new SSOException("test exception"));
        persistentJwtStringSupplierWithMockAuthKeyFactory.createJwtString("orgname", getEmptyAuthContext(), 40, 20,
                hmacKey);
    }

    private Map<String, String> getEmptyAuthContext() {
        return new HashMap<>();
    }
}