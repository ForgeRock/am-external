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
 * Copyright 2021-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.forgerock.openam.auth.nodes.MessageNode.MessageNodeValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceErrorException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;

@RunWith(JUnitParamsRunner.class)
public class MessageNodeValidatorTest {

    private static Realm realm;
    private MessageNodeValidator validator;
    private List<String> configPath;

    private static Object[] validData() {
        return new Object[][]{
                {
                     "Default parameters",
                     new HashMap<String, Set<String>>() {{
                         put("message", Stream.of("[fr]=Une Banane", "[de]=Eine Banane").collect(Collectors.toSet()));
                         put("messageYes", Stream.of("[fr]=Oui", "[de]=Ya").collect(Collectors.toSet()));
                         put("messageNo", Stream.of("[fr]=Non").collect(Collectors.toSet()));
                     }},
                }
        };
    }

    private static Object[] invalidData() {
        return new Object[][]{
                {
                     "Invalid message locale",
                     new HashMap<String, Set<String>>() {{
                         put("message", Stream.of("[fr]=Une Banane", "[de]=Eine Banane", "[jkl]=NOT VALID")
                                         .collect(Collectors.toSet()));
                         put("messageYes", Stream.of("[fr]=Oui", "[de]=Ya").collect(Collectors.toSet()));
                         put("messageNo", Stream.of("[fr]=Non").collect(Collectors.toSet()));
                     }},
                     "Invalid locale provided"
                },
                {
                     "Invalid yes locale",
                     new HashMap<String, Set<String>>() {{
                         put("message", Stream.of("[fr]=Une Banane", "[de]=Eine Banane").collect(Collectors.toSet()));
                         put("messageYes", Stream.of("[fr]=Une Banane", "[de]=Eine Banane", "[jkl]=NOT VALID")
                                 .collect(Collectors.toSet()));
                         put("messageNo", Stream.of("[fr]=Non").collect(Collectors.toSet()));
                     }},
                     "Invalid locale provided"
                },
                {
                     "Invalid no locale",
                     new HashMap<String, Set<String>>() {{
                         put("message", Stream.of("[fr]=Une Banane", "[de]=Eine Banane").collect(Collectors.toSet()));
                         put("messageYes", Stream.of("[fr]=Oui", "[de]=Ya").collect(Collectors.toSet()));
                         put("messageNo", Stream.of("[fr]=Non", "[jkl]=NOT VALID").collect(Collectors.toSet()));
                     }},
                     "Invalid locale provided"
                }
        };
    }

    @Test
    @TestCaseName("{method}: {0}")
    @Parameters(method = "validData")
    public void shouldValidateWithValidInput(String methodName, Map<String, Set<String>> attributes)
            throws ServiceConfigException, ServiceErrorException {
        validator.validate(realm, configPath, attributes);
    }

    @Test
    @TestCaseName("{method}: {0}")
    @Parameters(method = "invalidData")
    public void shouldThrowWithInvalidInput(String methodName, Map<String, Set<String>> attributes,
            String errorMessage) {
        assertThatExceptionOfType(ServiceConfigException.class)
                .isThrownBy(() -> validator.validate(realm, configPath, attributes))
                .withMessage(errorMessage);
    }

    @BeforeClass
    public static void setupClass() {
        RealmTestHelper realmHelper = new RealmTestHelper();
        realmHelper.setupRealmClass();
        realm = realmHelper.mockRealm("realm");
    }

    @Before
    public void setup() {
        validator = new MessageNodeValidator();
        configPath = singletonList(UUID.randomUUID().toString());
    }

}
