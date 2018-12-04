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
 * Copyright 2016-2018 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.INSTANCE_NAME_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.openam.utils.AMKeyProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

public class PersistentCookieWrapperTest {

    private static final String ANY_STRING = "any_string";
    private static final boolean ANY_BOOLEAN = false;
    private ServletJwtSessionModule jwtSessionModule;
    private AMKeyProvider amKeyProvider;
    private PersistentCookieModuleWrapper persistentCookieWrapper;

    @BeforeMethod
    public void setUp() {

        jwtSessionModule = mock(ServletJwtSessionModule.class);
        amKeyProvider = mock(AMKeyProvider.class);

        persistentCookieWrapper = new PersistentCookieModuleWrapper(jwtSessionModule);

        given(amKeyProvider.getPrivateKeyPass()).willReturn("PRIVATE_KEY_PASS");
        given(amKeyProvider.getKeystoreType()).willReturn("KEYSTORE_TYPE");
        given(amKeyProvider.getKeystoreFilePath()).willReturn("KEYSTORE_FILE_PATH");
        given(amKeyProvider.getKeystorePass()).willReturn("KEYSTORE_PASS".toCharArray());
    }

    @Test
    public void generatesConfigWithCorrectValuesUsingSecretsApi() {
        Map<String, Object> config = persistentCookieWrapper.generateConfig(ANY_STRING, ANY_STRING, ANY_BOOLEAN,
                ANY_STRING, ANY_BOOLEAN, ANY_BOOLEAN, ANY_STRING, Sets.newHashSet(ANY_STRING),
                "instance_name");

        assertThat(config).hasSize(9);
        assertThat(config).doesNotContainValue(null);

        assertThat(config)
                .contains(entry(INSTANCE_NAME_KEY, "instance_name"));
    }
}
