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
 * Copyright 2021 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.common.SAML2Exception;

public class ValidationHelperTest {
    private ValidationHelper validationHelper;

    @BeforeMethod
    public void setUp() {
        validationHelper = new ValidationHelper();
    }

    @Test
    public void shouldThrowSaml2ExceptionWhenRealmIsNull() {
        assertThatExceptionOfType(SAML2Exception.class).isThrownBy(() ->
                validationHelper.validateRealm(null)).withMessage("Null realm.");
    }

    @Test
    public void shouldThrowSaml2ExceptionWhenSessionIsNull() {
        assertThatExceptionOfType(SAML2Exception.class).isThrownBy(() ->
                validationHelper.validateSession(null)).withMessage("Null single signon token");
    }

    @Test
    public void shouldThrowSaml2ExceptionWhenHostedEntityIsNull() {
        assertThatExceptionOfType(SAML2Exception.class).isThrownBy(() ->
                validationHelper.validateHostedEntity(null)).withMessage("Null host entityid.");
    }}
