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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HttpServletResponseHelper}.
 */
public class HttpServletResponseHelperTest {

    private HttpServletResponse response;
    private HttpServletResponseHelper responseHelper;

    @BeforeEach
    void setup() {
        response = mock(HttpServletResponse.class);
        responseHelper = new HttpServletResponseHelper(response);
    }

    @Test
    void shouldGetHeaderSuccessfully() {
        // Given
        String headerName = "Content-Type";
        String headerValue = "application/json";
        when(response.getHeader(headerName)).thenReturn(headerValue);

        // When
        String result = responseHelper.getHeader(headerName);

        // Then
        assertThat(result).isEqualTo(headerValue);
        verify(response).getHeader(headerName);
    }

    @Test
    void shouldReturnNullWhenHeaderNotFound() {
        // Given
        String headerName = "NonExistentHeader";
        when(response.getHeader(headerName)).thenReturn(null);

        // When
        String result = responseHelper.getHeader(headerName);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenHeaderNameIsNull() {
        // When
        String result = responseHelper.getHeader(null);

        // Then
        assertThat(result).isNull();
        verifyNoInteractions(response);
    }

    @Test
    void shouldGetHeadersSuccessfully() {
        // Given
        String headerName = "Set-Cookie";
        List<String> headerValues = Arrays.asList("session=abc123", "token=xyz789", "lang=en");
        when(response.getHeaders(headerName)).thenReturn(headerValues);

        // When
        List<String> result = responseHelper.getHeaders(headerName);

        // Then
        assertThat(result).containsExactly("session=abc123", "token=xyz789", "lang=en");
        verify(response).getHeaders(headerName);
    }

    @Test
    void shouldReturnEmptyListWhenHeadersNotFound() {
        // Given
        String headerName = "NonExistentHeader";
        when(response.getHeaders(headerName)).thenReturn(null);

        // When
        List<String> result = responseHelper.getHeaders(headerName);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenResponseIsNull() {
        // Given
        HttpServletResponseHelper helperWithNullResponse = new HttpServletResponseHelper(null);

        // When
        List<String> result = helperWithNullResponse.getHeaders("testHeader");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetHeaderNamesSuccessfully() {
        // Given
        List<String> headerNames = Arrays.asList("Content-Type", "Content-Length", "Set-Cookie");
        when(response.getHeaderNames()).thenReturn(headerNames);

        // When
        List<String> result = responseHelper.getHeaderNames();

        // Then
        assertThat(result).containsExactly("Content-Type", "Content-Length", "Set-Cookie");
        verify(response).getHeaderNames();
    }

    @Test
    void shouldReturnEmptyListWhenNoHeaderNames() {
        // Given
        when(response.getHeaderNames()).thenReturn(null);

        // When
        List<String> result = responseHelper.getHeaderNames();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenResponseIsNullForHeaderNames() {
        // Given
        HttpServletResponseHelper helperWithNullResponse = new HttpServletResponseHelper(null);

        // When
        List<String> result = helperWithNullResponse.getHeaderNames();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldAddHeaderSuccessfully() {
        // Given
        String headerName = "X-Custom-Header";
        String headerValue = "customValue";

        // When
        responseHelper.addHeader(headerName, headerValue);

        // Then
        verify(response).addHeader(headerName, headerValue);
    }

    @Test
    void shouldNotAddHeaderWhenNameIsNull() {
        // Given
        String headerValue = "customValue";

        // When
        responseHelper.addHeader(null, headerValue);

        // Then
        verifyNoInteractions(response);
    }

    @Test
    void shouldNotAddHeaderWhenValueIsNull() {
        // Given
        String headerName = "X-Custom-Header";

        // When
        responseHelper.addHeader(headerName, null);

        // Then
        verifyNoInteractions(response);
    }

    @Test
    void shouldNotAddHeaderWhenResponseIsNull() {
        // Given
        HttpServletResponseHelper helperWithNullResponse = new HttpServletResponseHelper(null);
        String headerName = "X-Custom-Header";
        String headerValue = "customValue";

        // When
        helperWithNullResponse.addHeader(headerName, headerValue);

        // Then - no exception thrown
    }

    @Test
    void shouldSetHeaderSuccessfully() {
        // Given
        String headerName = "Content-Type";
        String headerValue = "text/html";

        // When
        responseHelper.setHeader(headerName, headerValue);

        // Then
        verify(response).setHeader(headerName, headerValue);
    }

    @Test
    void shouldNotSetHeaderWhenNameIsNull() {
        // Given
        String headerValue = "text/html";

        // When
        responseHelper.setHeader(null, headerValue);

        // Then
        verifyNoInteractions(response);
    }

    @Test
    void shouldNotSetHeaderWhenValueIsNull() {
        // Given
        String headerName = "Content-Type";

        // When
        responseHelper.setHeader(headerName, null);

        // Then
        verifyNoInteractions(response);
    }

    @Test
    void shouldNotSetHeaderWhenResponseIsNull() {
        // Given
        HttpServletResponseHelper helperWithNullResponse = new HttpServletResponseHelper(null);
        String headerName = "Content-Type";
        String headerValue = "text/html";

        // When
        helperWithNullResponse.setHeader(headerName, headerValue);

        // Then - no exception thrown
    }

    @Test
    void shouldSendRedirectSuccessfully() throws IOException {
        // Given
        String location = "https://example.com/redirect";

        // When
        responseHelper.sendRedirect(location);

        // Then
        verify(response).sendRedirect(location);
    }

    @Test
    void shouldThrowIOExceptionWhenRedirectFails() throws IOException {
        // Given
        String location = "https://example.com/redirect";
        doThrow(new IOException("Redirect failed")).when(response).sendRedirect(location);

        // When/Then
        assertThatThrownBy(() -> responseHelper.sendRedirect(location))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Redirect failed");
    }

    @Test
    void shouldNotSendRedirectWhenLocationIsNull() throws IOException {
        // When
        responseHelper.sendRedirect(null);

        // Then
        verifyNoInteractions(response);
    }

    @Test
    void shouldNotSendRedirectWhenResponseIsNull() throws IOException {
        // Given
        HttpServletResponseHelper helperWithNullResponse = new HttpServletResponseHelper(null);
        String location = "https://example.com/redirect";

        // When
        helperWithNullResponse.sendRedirect(location);

        // Then - no exception thrown
    }
}

