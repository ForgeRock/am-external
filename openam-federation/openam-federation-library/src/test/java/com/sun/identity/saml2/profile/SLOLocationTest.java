/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static com.sun.identity.saml2.common.SAML2Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.sun.identity.saml2.jaxb.metadata.SingleLogoutServiceElement;
import com.sun.identity.saml2.jaxb.metadata.impl.SingleLogoutServiceElementImpl;

@Test
public class SLOLocationTest {

    public void sameBindingReturnedWhenAvailable() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(HTTP_REDIRECT, "redirect"));
        endpoints.add(endpointFor(HTTP_POST, "post"));
        endpoints.add(endpointFor(SOAP, "soap"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(HTTP_REDIRECT);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(HTTP_POST);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result.getBinding()).isEqualTo(SOAP);
    }

    public void asynchronousBindingIsPreferredOverSynchronous() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(HTTP_POST, "post"));
        endpoints.add(endpointFor(SOAP, "soap"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(HTTP_POST);
        endpoints.set(0, endpointFor(HTTP_REDIRECT, "redirect"));
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(HTTP_REDIRECT);
    }

    public void asynchronousBindingsAreNotReturnedWhenRequestingSynchronous() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(HTTP_REDIRECT, "redirect"));
        endpoints.add(endpointFor(HTTP_POST, "post"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result).isNull();
    }

    public void nullReturnedIfNoBindingAvailable() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result).isNull();
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result).isNull();
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, SOAP);
        assertThat(result).isNull();
    }

    public void synchronousBindingReturnedIfNoAsynchronousAvailable() {
        List<SingleLogoutServiceElement> endpoints = new ArrayList<SingleLogoutServiceElement>();
        endpoints.add(endpointFor(SOAP, "soap"));
        SingleLogoutServiceElement result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_REDIRECT);
        assertThat(result.getBinding()).isEqualTo(SOAP);
        result = LogoutUtil.getMostAppropriateSLOServiceLocation(endpoints, HTTP_POST);
        assertThat(result.getBinding()).isEqualTo(SOAP);
    }

    private SingleLogoutServiceElement endpointFor(String binding, String location) {
        SingleLogoutServiceElement ret = new SingleLogoutServiceElementImpl();
        ret.setBinding(binding);
        ret.setLocation(location);
        return ret;
    }
}
