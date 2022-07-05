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
 * Copyright 2020-2021 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretBuilder;
import org.forgerock.util.promise.PromiseImpl;
import org.junit.runner.RunWith;

@RunWith(CuppaRunner.class)
@Test
public class ScriptedSecretsTest {

    public static final String INVALID_SECRET_ID = "my.secret";
    public static final String ALMOST_VALID_SECRET_ID = "scriptedsnodelcumber";
    public static final String VALID_SECRET_ID = "scripted.node.my.secret";
    public static final String SECRET_VALUE = "badger";

    private Realm realm;
    private Secrets secrets;
    private ScriptedSecrets scriptedSecrets;
    private String secretId;
    private GenericSecret genericSecret;

    {
        describe(ScriptedSecretsTest.class.getSimpleName(), () -> {
            beforeEach(() -> {
                realm = mock(Realm.class);
                secrets = mock(Secrets.class);
            });
            when("using a valid Secret ID", () -> {
                beforeEach(() -> {
                    secretId = VALID_SECRET_ID;
                });
                when("Secrets API operates as intended", () -> {
                    beforeEach(() -> {
                        genericSecret = new SecretBuilder()
                                .clock(Clock.systemDefaultZone())
                                .password(SECRET_VALUE.toCharArray())
                                .build(Purpose.PASSWORD);
                        secrets = mockSecrets(promisedSecret(genericSecret));
                        scriptedSecrets = new ScriptedSecrets(secrets, realm);
                    });
                    it("returns the secret", () -> {
                        final Secret secretValue = scriptedSecrets.getGenericSecret(secretId);
                        assertThat(secretValue.getAsUtf8()).isEqualTo(SECRET_VALUE);
                    });
                    it("closes the secret", () -> {
                        scriptedSecrets.getGenericSecret(secretId);
                        assertThat(genericSecret.isClosed()).isTrue();
                    });
                    when("Secret ID does not match the allowed template", () -> {
                        it("throws an error", () -> {
                            assertThatThrownBy(() -> scriptedSecrets.getGenericSecret(INVALID_SECRET_ID))
                                    .isInstanceOf(NodeProcessException.class)
                                    .hasMessageContaining("accessible");
                        });
                    });
                    when("Secret ID does not match the allowed template (regex bug)", () -> {
                        it("throws an error", () -> {
                            assertThatThrownBy(() -> scriptedSecrets.getGenericSecret(ALMOST_VALID_SECRET_ID))
                                    .isInstanceOf(NodeProcessException.class)
                                    .hasMessageContaining("accessible");
                        });
                    });
                });
                when("Secrets API fails to resolve Secret ID", () -> {
                    beforeEach(() -> {
                        secrets = mockSecrets(brokenPromise());
                        scriptedSecrets = new ScriptedSecrets(secrets, realm);
                    });
                    it("throws an exception", () -> {
                        assertThatThrownBy(() -> scriptedSecrets.getGenericSecret(secretId))
                                .isInstanceOf(NodeProcessException.class);
                    });
                });
                when("Secrets API blocks forever whilst resolving Secret ID", () -> {
                    beforeEach(() -> {
                        secrets = mockSecrets(neverEndingPromise());
                        scriptedSecrets = new ScriptedSecrets(secrets, realm);
                    });
                    /*
                    Multi-Threaded Test
                    This test requires use of threading to exercise the interrupt behaviour. The mock
                    Secrets API is setup with a Promise that never returns and blocks forever. The
                    request is placed in a thread which is then interrupted at the shutdown of the
                    executor service. We then wait until the service has correctly
                    shutdown before verifying the exception was indeed thrown.
                     */
                    it("throws an exception whilst waiting for the promise to return", () -> {
                        final ExecutorService service = Executors.newFixedThreadPool(1);
                        final AtomicReference<RuntimeException> exceptionHolder = new AtomicReference<>();
                        service.submit(() -> {
                            try {
                                scriptedSecrets.getGenericSecret(secretId);
                            } catch (NodeProcessException npe) {
                                exceptionHolder.set(new RuntimeException(npe));
                            } catch (RuntimeException e) {
                                exceptionHolder.set(e);
                            }
                        });
                        service.shutdownNow();
                        service.awaitTermination(60, TimeUnit.SECONDS);
                        assertThat(exceptionHolder.get()).isNotNull();
                    });
                });
            });
        });
    }

    private PromiseImpl<GenericSecret, NoSuchSecretException> promisedSecret(GenericSecret genericSecret) {
        final PromiseImpl<GenericSecret, NoSuchSecretException> promise = PromiseImpl.create();
        promise.handleResult(genericSecret);
        return promise;
    }

    private static PromiseImpl<GenericSecret, NoSuchSecretException> brokenPromise() {
        final PromiseImpl<GenericSecret, NoSuchSecretException> promise = PromiseImpl.create();
        promise.handleException(new NoSuchSecretException("test"));
        return promise;
    }

    private static PromiseImpl<GenericSecret, NoSuchSecretException> neverEndingPromise() {
        return PromiseImpl.create();
    }

    /**
     * Create a mock {@link Secrets} object which is given a specifically crafted {@link PromiseImpl}.
     * This mock will return the same promise regardless of the secret requested using the
     * {@link SecretsProviderFacade#getActiveSecret(Purpose)} method requested.
     *
     * @param promise Non-null
     * @return A non null mock {@link Secrets}
     */
    private static Secrets mockSecrets(PromiseImpl<GenericSecret, NoSuchSecretException> promise) {
        SecretsProviderFacade mockFacade = mock(SecretsProviderFacade.class);
        given(mockFacade.getActiveSecret(any(Purpose.class))).willReturn(promise);

        final Secrets mockSecrets = mock(Secrets.class);
        given(mockSecrets.getRealmSecrets(any())).willReturn(mockFacade);
        return mockSecrets;
    }
}