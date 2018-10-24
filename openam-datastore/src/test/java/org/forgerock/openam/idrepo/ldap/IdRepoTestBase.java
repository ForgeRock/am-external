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
 * Copyright 2013-2018 ForgeRock AS.
 */

package org.forgerock.openam.idrepo.ldap;

import static org.mockito.Mockito.*;

import javax.annotation.Nullable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.util.Map;
import java.util.stream.Stream;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.sm.ldap.ConfigAuditorFactory;
import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.AuditEventPublisherImpl;
import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.auditors.SMSAuditor;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldif.LdifEntryReader;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

@PrepareForTest(value = {IdRepoListener.class, WebtopNaming.class})
public abstract class IdRepoTestBase extends PowerMockTestCase {

    protected static final String TEST1_GROUP = "test1";
    protected static final String TEST1_GROUP_DN = "cn=test1,ou=groups,dc=openam,dc=forgerock,dc=org";
    protected static final String TEST_USER1 = "testuser1";
    protected static final String DEMO = "demo";
    protected static final String USER0 = "user.0";
    protected static final String USER0_DN = "uid=user.0,ou=people,dc=openam,dc=forgerock,dc=org";
    protected static final String DEMO_DN = "uid=demo,ou=people,dc=openam,dc=forgerock,dc=org";
    protected MemoryBackend memoryBackend;
    protected IdRepoListener idRepoListener;
    protected DJLDAPv3Repo idrepo = new DJLDAPv3Repo() {
        @Override
        protected org.forgerock.openam.sm.datalayer.api.ConnectionFactory<Connection>
        createConnectionFactory(String username, char[] password, int maxPoolSize) {
            return LdapConnectionFactoryProvider.wrapExistingConnectionFactory(new FakeConnectionFactory());
        }

        @Override
        protected org.forgerock.openam.sm.datalayer.api.ConnectionFactory<Connection>
        createPasswordConnectionFactory(String username, char[] password, int maxPoolSize) {
            return LdapConnectionFactoryProvider.wrapExistingConnectionFactory(new FakeConnectionFactory());
        }


        @Override
        protected Schema getSchema() throws IdRepoException {
            return Schema.getCoreSchema().asStrictSchema();
        }
    };

    public static class TestGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ShutdownManager.class).toInstance(mock(ShutdownManager.class));
            bind(ConfigAuditorFactory.class).toInstance(new ConfigAuditorFactory() {
                @Override
                public SMSAuditor create(SSOToken runAs, @Assisted("realm") @Nullable String realm, @Assisted("objectId") String objectId, Map initialState) {
                    return mock(SMSAuditor.class);
                }
            });
            bind(AuditEventPublisher.class).toInstance(mock(AuditEventPublisherImpl.class));
            bind(AuditEventBuilder.class).toInstance(mock(AuditEventBuilder.class));
            bind(AuditServiceProvider.class).toInstance(mock(AuditServiceProvider.class));
        }
    }

    @BeforeClass
    public void setUpSuite() throws Exception {
        InjectorConfiguration.setGuiceModuleLoader(() -> Stream.of(TestGuiceModule.class));
        PowerMockito.mockStatic(WebtopNaming.class);
        idRepoListener = PowerMockito.mock(IdRepoListener.class);
        when(WebtopNaming.getAMServerID()).thenReturn("01");
        when(WebtopNaming.getSiteID(eq("01"))).thenReturn("02");
        memoryBackend = new MemoryBackend(new LdifEntryReader(getClass().getResourceAsStream(getLDIFPath())));
    }

    @BeforeMethod
    public void resetMocks() {
        reset(idRepoListener);
    }

    @AfterClass
    public void tearDown() {
        idrepo.shutdown();
    }

    protected abstract String getLDIFPath();

    private class FakeConnectionFactory implements ConnectionFactory {
        private final ConnectionFactory cf = Connections.newInternalConnectionFactory(memoryBackend::handleRequest);

        @Override
        public void close() {
            cf.close();
        }

        @Override
        public Promise<Connection, LdapException> getConnectionAsync() {
            return cf.getConnectionAsync();
        }

        @Override
        public Connection getConnection() throws LdapException {
            return cf.getConnection();
        }
    }

    protected Callback[] getCredentials(String username, String password) {
        NameCallback nc = new NameCallback("dummy");
        nc.setName(username);
        PasswordCallback pc = new PasswordCallback("dummy", false);
        pc.setPassword(password.toCharArray());
        return new Callback[] { nc, pc };
    }

    protected String getIdRepoExceptionMessage(String code, Object... args) {
        return new IdRepoException(IdRepoBundle.BUNDLE_NAME, code, args).getMessage();
    }
}
