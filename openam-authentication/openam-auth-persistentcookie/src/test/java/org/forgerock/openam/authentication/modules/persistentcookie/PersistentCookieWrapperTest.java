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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.INSTANCE_NAME_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.utils.AMKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class PersistentCookieWrapperTest {

    private static final String REALM_PATH = "/realms/root/realms/foo/realms/bar";
    private static final String ANY_STRING = "any_string";
    private static final boolean ANY_BOOLEAN = false;
    private ServletJwtSessionModule jwtSessionModule;
    private AMKeyProvider amKeyProvider;
    private PersistentCookieModuleWrapper persistentCookieWrapper;

    @BeforeEach
    void setUp() throws Exception {

        jwtSessionModule = mock(ServletJwtSessionModule.class);
        amKeyProvider = mock(AMKeyProvider.class);
        RealmLookup realmLookup = mock(RealmLookup.class);

        persistentCookieWrapper = new PersistentCookieModuleWrapper(jwtSessionModule, realmLookup);

        given(amKeyProvider.getPrivateKeyPass()).willReturn("PRIVATE_KEY_PASS");
        given(amKeyProvider.getKeystoreType()).willReturn("KEYSTORE_TYPE");
        given(amKeyProvider.getKeystoreFilePath()).willReturn("KEYSTORE_FILE_PATH");
        given(amKeyProvider.getKeystorePass()).willReturn("KEYSTORE_PASS".toCharArray());

        Realm realm = mock(Realm.class);
        given(realmLookup.lookup(anyString())).willReturn(realm);
        given(realm.asRoutingPath()).willReturn(REALM_PATH);
    }

    @Test
    void generatesConfigWithCorrectValuesUsingSecretsApi() throws Exception {
        Map<String, Object> config = persistentCookieWrapper.generateConfig(ANY_STRING, ANY_STRING, ANY_BOOLEAN,
                ANY_STRING, ANY_BOOLEAN, ANY_BOOLEAN, ANY_STRING, Sets.newHashSet(ANY_STRING),
                "instance_name");

        assertThat(config).hasSize(10);
        assertThat(config).doesNotContainValue(null);

        assertThat(config)
                .contains(entry(INSTANCE_NAME_KEY, "instance_name"))
                .contains(entry(JwtSessionModule.JWT_ISSUER, REALM_PATH + "/modules/instance_name"));
    }
}
