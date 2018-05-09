package org.forgerock.openam.auth.nodes.jwt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.FileNotFoundException;

import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

public class PersistentJwtProviderTest extends GuiceTestCase {

    private PersistentJwtProvider persistentJwtProvider;

    @Mock
    AuthKeyFactory mockAuthKeyFactory;

    @BeforeMethod
    public void before() {
        initMocks(this);
        persistentJwtProvider = new PersistentJwtProvider(mockAuthKeyFactory, new JwtReconstruction());
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtBadKeySSOException() throws Exception {
        when(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).thenThrow(new SSOException("test exception"));
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtBadKeySMSException() throws Exception {
        when(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).thenThrow(new SMSException("test exception"));
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtBadKeyFileNotFoundException() throws Exception {
        when(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).thenThrow(new FileNotFoundException("test exception"));
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtNulls() throws Exception {
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }
}