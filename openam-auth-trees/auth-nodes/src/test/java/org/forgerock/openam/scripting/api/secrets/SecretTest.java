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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.junit.runner.RunWith;

import java.time.Clock;

@RunWith(CuppaRunner.class)
@Test
public class SecretTest {

    private final SecretBuilder builder = new SecretBuilder();
    private GenericSecret genericSecret;

    {
        describe(Secret.class.getSimpleName(), () -> {
            when("A null secret is provided", () -> {
                it("fails with error", () -> {
                    assertThatThrownBy(() -> new Secret(null))
                            .isInstanceOf(NullPointerException.class);
                });
            });
            when("A non-null secret is provided", () -> {
                beforeEach(() -> {
                    genericSecret = builder.password("badger".toCharArray())
                            .clock(Clock.systemDefaultZone())
                            .build(Purpose.PASSWORD);
                });
                it("returns the contained secret in utf8", () -> {
                    final Secret secret = new Secret(genericSecret);
                    assertThat(secret.getAsUtf8()).isEqualTo("badger");
                });
                it("returns the contained secret in bytes", () -> {
                    final Secret secret = new Secret(genericSecret);
                    assertThat(secret.getAsBytes()).isEqualTo("badger".getBytes());
                });
            });
        });
    }
}