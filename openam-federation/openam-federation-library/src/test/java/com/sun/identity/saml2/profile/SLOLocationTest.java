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
 * Copyright 2013-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.HTTP_POST;
import static com.sun.identity.saml2.common.SAML2Constants.HTTP_REDIRECT;
import static com.sun.identity.saml2.common.SAML2Constants.SOAP;
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
public class SLOLocationTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(SOAPCommunicator.class, mock(SOAPCommunicator.class)).build();

    @Test
    void sameBindingReturnedWhenAvailable() {
        List<EndpointType> endpoints = new ArrayList<>();
        endpoints.add(endpointFor(HTTP_REDIRECT, "redirect"));
        endpoints.add(endpointFor(HTTP_POST, "post"));
        endpoints.add(endpointFor(SOAP, "soap"));
        EndpointType result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(HTTP_REDIRECT);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(HTTP_POST);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result.getBinding()).isEqualTo(SOAP);
    }

    @Test
    void asynchronousBindingIsPreferredOverSynchronous() {
        List<EndpointType> endpoints = new ArrayList<>();
        endpoints.add(endpointFor(HTTP_POST, "post"));
        endpoints.add(endpointFor(SOAP, "soap"));
        EndpointType result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(HTTP_POST);
        endpoints.set(0, endpointFor(HTTP_REDIRECT, "redirect"));
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(HTTP_REDIRECT);
    }

    @Test
    void asynchronousBindingsAreNotReturnedWhenRequestingSynchronous() {
        List<EndpointType> endpoints = new ArrayList<EndpointType>();
        endpoints.add(endpointFor(HTTP_REDIRECT, "redirect"));
        endpoints.add(endpointFor(HTTP_POST, "post"));
        EndpointType result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result).isNull();
    }

    @Test
    void nullReturnedIfNoBindingAvailable() {
        List<EndpointType> endpoints = new ArrayList<>();
        EndpointType result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result).isNull();
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result).isNull();
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result).isNull();
    }

    @Test
    void synchronousBindingReturnedIfNoAsynchronousAvailable() {
        List<EndpointType> endpoints = new ArrayList<>();
        endpoints.add(endpointFor(SOAP, "soap"));
        EndpointType result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(SOAP);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(SOAP);
    }

    private EndpointType endpointFor(String binding, String location) {
        EndpointType ret = new EndpointType();
        ret.setBinding(binding);
        ret.setLocation(location);
        return ret;
    }
}
