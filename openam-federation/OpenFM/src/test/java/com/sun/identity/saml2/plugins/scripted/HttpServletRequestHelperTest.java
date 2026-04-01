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
 * Copyright 2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static java.util.Collections.enumeration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HttpServletRequestHelper}.
 */
public class HttpServletRequestHelperTest {

    private HttpServletRequest request;
    private HttpServletRequestHelper requestHelper;

    @BeforeEach
    void setup() {
        request = mock(HttpServletRequest.class);
        requestHelper = new HttpServletRequestHelper(request);
    }

    @Test
    void shouldGetAttributeSuccessfully() {
        // Given
        String attributeName = "testAttribute";
        String attributeValue = "testValue";
        when(request.getAttribute(attributeName)).thenReturn(attributeValue);

        // When
        Object result = requestHelper.getAttribute(attributeName);

        // Then
        assertThat(result).isEqualTo(attributeValue);
        verify(request).getAttribute(attributeName);
    }

    @Test
    void shouldReturnNullWhenAttributeNotFound() {
        // Given
        String attributeName = "nonExistentAttribute";
        when(request.getAttribute(attributeName)).thenReturn(null);

        // When
        Object result = requestHelper.getAttribute(attributeName);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenAttributeNameIsNull() {
        // When
        Object result = requestHelper.getAttribute(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldSetAttributeSuccessfully() {
        // Given
        String attributeName = "testAttribute";
        String attributeValue = "testValue";

        // When
        requestHelper.setAttribute(attributeName, attributeValue);

        // Then
        verify(request).setAttribute(attributeName, attributeValue);
    }

    @Test
    void shouldNotSetAttributeWhenNameIsNull() {
        // Given
        String attributeValue = "testValue";

        // When
        requestHelper.setAttribute(null, attributeValue);

        // Then - no exception thrown and no interaction with request
    }

    @Test
    void shouldGetHeaderSuccessfully() {
        // Given
        String headerName = "Content-Type";
        String headerValue = "application/json";
        when(request.getHeader(headerName)).thenReturn(headerValue);

        // When
        String result = requestHelper.getHeader(headerName);

        // Then
        assertThat(result).isEqualTo(headerValue);
        verify(request).getHeader(headerName);
    }

    @Test
    void shouldReturnNullWhenHeaderNotFound() {
        // Given
        String headerName = "NonExistentHeader";
        when(request.getHeader(headerName)).thenReturn(null);

        // When
        String result = requestHelper.getHeader(headerName);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenHeaderNameIsNull() {
        // When
        String result = requestHelper.getHeader(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldGetHeadersSuccessfully() {
        // Given
        String headerName = "Accept";
        List<String> headerValues = Arrays.asList("application/json", "application/xml", "text/html");
        when(request.getHeaders(headerName)).thenReturn(enumeration(headerValues));

        // When
        List<String> result = requestHelper.getHeaders(headerName);

        // Then
        assertThat(result).containsExactly("application/json", "application/xml", "text/html");
        verify(request).getHeaders(headerName);
    }

    @Test
    void shouldReturnNullWhenHeadersNotFound() {
        // Given
        String headerName = "NonExistentHeader";
        when(request.getHeaders(headerName)).thenReturn(null);

        // When
        List<String> result = requestHelper.getHeaders(headerName);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenHeadersNameIsNull() {
        // When
        List<String> result = requestHelper.getHeaders(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldGetParameterSuccessfully() {
        // Given
        String paramName = "userId";
        String paramValue = "12345";
        when(request.getParameter(paramName)).thenReturn(paramValue);

        // When
        String result = requestHelper.getParameter(paramName);

        // Then
        assertThat(result).isEqualTo(paramValue);
        verify(request).getParameter(paramName);
    }

    @Test
    void shouldReturnNullWhenParameterNotFound() {
        // Given
        String paramName = "nonExistentParam";
        when(request.getParameter(paramName)).thenReturn(null);

        // When
        String result = requestHelper.getParameter(paramName);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenParameterNameIsNull() {
        // When
        String result = requestHelper.getParameter(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldGetParameterValuesSuccessfully() {
        // Given
        String paramName = "roles";
        String[] paramValues = {"admin", "user", "developer"};
        when(request.getParameterValues(paramName)).thenReturn(paramValues);

        // When
        String[] result = requestHelper.getParameterValues(paramName);

        // Then
        assertThat(result).containsExactly("admin", "user", "developer");
        verify(request).getParameterValues(paramName);
    }

    @Test
    void shouldReturnNullWhenParameterValuesNotFound() {
        // Given
        String paramName = "nonExistentParam";
        when(request.getParameterValues(paramName)).thenReturn(null);

        // When
        String[] result = requestHelper.getParameterValues(paramName);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenParameterValuesNameIsNull() {
        // When
        String[] result = requestHelper.getParameterValues(null);

        // Then
        assertThat(result).isNull();
    }
}

