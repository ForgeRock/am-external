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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.validators;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.sun.identity.sm.ServiceAttributeValidator;

public class SessionPropertyNameValidatorTest {
    private ServiceAttributeValidator validator;

    @BeforeEach
    public void beforeEach() {
        List<String> systemPropertyNames = asList("x", "y");
        validator = new SessionPropertyNameValidator(systemPropertyNames);
    }

    @Test
    @DisplayName(value = "the input is valid it returns true")
    public void testTheInputIsValidItReturnsTrue() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("a", "b"))).isTrue();
    }

    @Test
    @DisplayName(value = "there are no values it returns false")
    public void testThereAreNoValuesItReturnsFalse() throws Exception {
        assertThat(validator.validate(Collections.emptySet())).isFalse();
    }

    @Test
    @DisplayName(value = "one of the values is a system property it returns false")
    public void testOneOfTheValuesIsASystemPropertyItReturnsFalse() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("a", "x"))).isFalse();
    }
}
