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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.rest;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TREES_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

@ExtendWith(MockitoExtension.class)
public class AuthTreeHandlerTest {

    @Mock
    private SmsRouteTree smsRouteTree;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private ServiceSchemaManager ssm;
    @Mock
    private ServiceSchema organisationSchema;
    @Mock
    private AuthTreesCollectionProviderFactory authTreesCollectionProviderFactory;
    @Mock
    private SchemaType schemaType;

    @InjectMocks
    private AuthTreeHandler authTreeHandler;

    @Test
    void shouldHandleAuthTreeService() {
        assertThat(authTreeHandler.handles("authenticationTreesService")).isTrue();
    }

    @Test
    void shouldNotHandleOtherServices() {
        assertThat(authTreeHandler.handles("otherService")).isFalse();
    }

    @Test
    void shouldRegisterServiceIfServiceNameAuthTree() throws Exception {
        //given
        String serviceName = "authenticationTreesService";
        SSOToken ssoToken = mock(SSOToken.class);
        SmsRouteTree treeSmsRouteTree = mock(SmsRouteTree.class);
        given(coreWrapper.getAdminToken()).willReturn(ssoToken);
        given(coreWrapper.getServiceSchemaManager(AUTH_TREES_SERVICE_NAME, ssoToken))
                .willReturn(ssm);
        given(ssm.getOrganizationSchema()).willReturn(organisationSchema);
        given(smsRouteTree.handles(serviceName)).willReturn(treeSmsRouteTree);
        given(authTreesCollectionProviderFactory.create(any())).willReturn(mock(AuthTreesCollectionProvider.class));
        SmsRouteTree.Route route = mock(SmsRouteTree.Route.class);
        given(treeSmsRouteTree.addRoute(eq(STARTS_WITH), eq("trees"), any()))
                .willReturn(route);

        //when
        var serviceRoute = authTreeHandler.registerService(smsRouteTree, schemaType, serviceName);

        //then
        assertThat(serviceRoute).isNotNull();
        verify(treeSmsRouteTree).addRoute(eq(STARTS_WITH), eq("trees"), any());
    }
}
