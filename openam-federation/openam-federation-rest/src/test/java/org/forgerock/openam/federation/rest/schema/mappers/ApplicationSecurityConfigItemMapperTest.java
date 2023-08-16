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
package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.ApplicationSecurityConfigItem;
import org.testng.annotations.Test;

public class ApplicationSecurityConfigItemMapperTest {

    private ApplicationSecurityConfigItemMapper mapper = new ApplicationSecurityConfigItemMapper();

    @Test
    public void shouldMapUrl() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("url=badger"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getUrl()).isEqualTo("badger");
    }

    @Test
    public void shouldMapType() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("type=weasel"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getType()).isEqualTo("weasel");
    }

    @Test
    public void shouldMapPubKeyAlias() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("pubkeyalias=otter"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getPubKeyAlias()).isEqualTo("otter");
    }

    @Test
    public void shouldMapEncryptionAlgorithm() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("encryptionalgorithm=mink"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getEncryptionAlgorithm()).isEqualTo("mink");
    }

    @Test
    public void shouldMapEncryptionKeyStrength() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("encryptionkeystrength=ferret"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getEncryptionKeyStrength()).isEqualTo("ferret");
    }

    @Test
    public void shouldMapSecret() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("secret=marten"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getSecret()).isEqualTo("marten");
    }

    @Test
    public void shouldMapComplexItems() {
        List<ApplicationSecurityConfigItem> mapped = mapper.map(singletonList("url=badger|type=weasel|"
                + "pubkeyalias=otter|encryptionalgorithm=mink|encryptionkeystrength=ferret|secret=marten"), ROOT);

        assertThat(mapped).isNotNull().hasSize(1);
        ApplicationSecurityConfigItem item = mapped.get(0);
        assertThat(item.getUrl()).isEqualTo("badger");
        assertThat(item.getType()).isEqualTo("weasel");
        assertThat(item.getPubKeyAlias()).isEqualTo("otter");
        assertThat(item.getEncryptionAlgorithm()).isEqualTo("mink");
        assertThat(item.getEncryptionKeyStrength()).isEqualTo("ferret");
        assertThat(item.getSecret()).isEqualTo("marten");
    }
}