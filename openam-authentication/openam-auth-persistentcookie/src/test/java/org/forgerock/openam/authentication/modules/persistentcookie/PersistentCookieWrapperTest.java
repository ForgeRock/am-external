/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.forgerock.guava.common.collect.Sets;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.openam.utils.AMKeyProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

public class PersistentCookieWrapperTest {

    private static final String KEY_ALIAS = "KEY_ALIAS";
    private static final String ANY_STRING = "any_string";
    private static final boolean ANY_BOOLEAN = false;
    private ServletJwtSessionModule jwtSessionModule;
    private AMKeyProvider amKeyProvider;
    private PersistentCookieModuleWrapper persistentCookieWrapper;

    @BeforeMethod
    public void setUp() {

        jwtSessionModule = mock(ServletJwtSessionModule.class);
        amKeyProvider = mock(AMKeyProvider.class);

        persistentCookieWrapper = new PersistentCookieModuleWrapper(jwtSessionModule, amKeyProvider) {

            @Override
            protected ServiceConfigManager getServiceConfigManager() throws SSOException, SMSException {
                ServiceConfigManager serviceConfigManager = mock(ServiceConfigManager.class);
                ServiceConfig serviceConfig = mock(ServiceConfig.class);
                given(serviceConfig.getAttributes()).willReturn(Collections.singletonMap("iplanet-am-auth-key-alias",
                                (Set<String>)Sets.newHashSet(KEY_ALIAS)));
                given(serviceConfigManager.getOrganizationConfig(anyString(), anyString())).willReturn(serviceConfig);
                return serviceConfigManager;
            }
        };

        given(amKeyProvider.getPrivateKeyPass()).willReturn("PRIVATE_KEY_PASS");
        given(amKeyProvider.getKeystoreType()).willReturn("KEYSTORE_TYPE");
        given(amKeyProvider.getKeystoreFilePath()).willReturn("KEYSTORE_FILE_PATH");
        given(amKeyProvider.getKeystorePass()).willReturn("KEYSTORE_PASS".toCharArray());
    }

    @Test
    public void generatesConfigWithCorrectValues() throws Exception {
        Map<String, Object> config = persistentCookieWrapper.generateConfig(ANY_STRING, ANY_STRING, ANY_BOOLEAN,
                ANY_STRING, ANY_BOOLEAN, ANY_BOOLEAN, ANY_STRING, Sets.newHashSet(ANY_STRING), ANY_STRING);

        assertThat(config).hasSize(13);
        assertThat(config).doesNotContainValue(null);

        assertEquals(config.get(JwtSessionModule.KEY_ALIAS_KEY), KEY_ALIAS);

        assertEquals(config.get(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY), "PRIVATE_KEY_PASS");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_TYPE_KEY), "KEYSTORE_TYPE");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_FILE_KEY), "KEYSTORE_FILE_PATH");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_PASSWORD_KEY), "KEYSTORE_PASS");
    }

}
