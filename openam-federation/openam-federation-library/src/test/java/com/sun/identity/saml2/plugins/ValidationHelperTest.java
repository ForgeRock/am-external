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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.saml2.common.SAML2Exception;

public class ValidationHelperTest {
    private ValidationHelper validationHelper;

    @BeforeEach
    void setUp() {
        validationHelper = new ValidationHelper();
    }

    @Test
    void shouldThrowSaml2ExceptionWhenRealmIsNull() {
        assertThatExceptionOfType(SAML2Exception.class).isThrownBy(() ->
                validationHelper.validateRealm(null)).withMessage("Null realm.");
    }

    @Test
    void shouldThrowSaml2ExceptionWhenSessionIsNull() {
        assertThatExceptionOfType(SAML2Exception.class).isThrownBy(() ->
                validationHelper.validateSession(null)).withMessage("Null single signon token");
    }

    @Test
    void shouldThrowSaml2ExceptionWhenHostedEntityIsNull() {
        assertThatExceptionOfType(SAML2Exception.class).isThrownBy(() ->
                validationHelper.validateHostedEntity(null)).withMessage("Null host entityid.");
    }}
