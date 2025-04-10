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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.HTTP_POST;
import static com.sun.identity.saml2.common.SAML2Constants.HTTP_REDIRECT;
import static com.sun.identity.saml2.profile.SPSSOFederate.getSingleSignOnServiceEndpoint;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import com.sun.identity.saml2.common.SOAPCommunicator;
import org.forgerock.guice.core.GuiceExtension;

import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(GuiceExtension.class)
public class SPSSOFederateTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(SOAPCommunicator.class, mock(SOAPCommunicator.class)).build();

    private static final String SHIBBOLETH_1_0_PROFILES_AUTHN_REQUEST
            = "urn:mace:shibboleth:1.0:profiles:AuthnRequest";

    @Test
    void shouldFindEndpointTypeWhenNoPreferredBindingSupplied() {
        // Given
        List<EndpointType> endpointTypes = new ArrayList<>();
        endpointTypes.add(endpointType(SHIBBOLETH_1_0_PROFILES_AUTHN_REQUEST, "authnrequest"));
        endpointTypes.add(endpointType(HTTP_POST, "post"));
        endpointTypes.add(endpointType(HTTP_REDIRECT, "redirect"));

        // When / Then
        assertThat(getSingleSignOnServiceEndpoint(endpointTypes, null).getBinding()).isEqualTo(HTTP_POST);
    }

    @Test
    void shouldNotFindEndpointTypeWhenNoPreferredBindingSuppliedAndNoMatch() {
        // Given
        List<EndpointType> endpointTypes = new ArrayList<>();
        endpointTypes.add(endpointType(SHIBBOLETH_1_0_PROFILES_AUTHN_REQUEST, "authnrequest"));

        // When / Then
        assertThat(getSingleSignOnServiceEndpoint(endpointTypes, null)).isNull();
    }

    @Test
    void shouldFindMatchingEndpointTypeToPreferredBinding() {
        // Given
        List<EndpointType> endpointTypes = new ArrayList<>();
        endpointTypes.add(endpointType(SHIBBOLETH_1_0_PROFILES_AUTHN_REQUEST, "authnrequest"));
        endpointTypes.add(endpointType(HTTP_REDIRECT, "redirect"));
        endpointTypes.add(endpointType(HTTP_POST, "post"));

        // When / Then
        assertThat(getSingleSignOnServiceEndpoint(endpointTypes, HTTP_POST).getBinding()).isEqualTo(HTTP_POST);
    }

    @Test
    void shouldNotFindEndpointTypeWhenPreferredBindingNotAMatch() {
        // Given
        List<EndpointType> endpointTypes = new ArrayList<>();
        endpointTypes.add(endpointType(SHIBBOLETH_1_0_PROFILES_AUTHN_REQUEST, "authnrequest"));
        endpointTypes.add(endpointType(HTTP_REDIRECT, "redirect"));

        // When / Then
        assertThat(getSingleSignOnServiceEndpoint(endpointTypes, HTTP_POST)).isNull();
    }

    private EndpointType endpointType(String binding, String location) {
        EndpointType endpointType = new EndpointType();
        endpointType.setBinding(binding);
        endpointType.setLocation(location);
        return endpointType;
    }
}
