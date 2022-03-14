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

package org.forgerock.openam.auth.nodes.validators;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private static final Map.Entry<String, Set<String>> DEFAULT_MESSAGE = Map.entry("message",
            Set.of("[fr]=Une Banane", "[de]=Eine Banane"));
    private static final Map.Entry<String, Set<String>> DEFAULT_YES = Map.entry("messageYes",
            Set.of("[fr]=Oui", "[de]=Ya"));
    private static final Map.Entry<String, Set<String>> DEFAULT_NO = Map.entry("messageNo",
            Set.of("[fr]=Non"));

    private static Object[] validData() {
        return new Object[][]{
                {
                        "Default parameters",
                        Map.ofEntries(
                                DEFAULT_MESSAGE,
                                DEFAULT_YES,
                                DEFAULT_NO
                        ),
                }
        };
    }

    private static Object[] invalidData() {
        return new Object[][]{
                {
                        "Invalid message locale",
                        Map.ofEntries(
                                Map.entry("message", Set.of("[fr]=Une Banane", "[de]=Eine Banane", "[jkl]=NOT VALID")),
                                DEFAULT_YES,
                                DEFAULT_NO
                        ),
                        "Invalid locale provided"
                },
                {
                        "Invalid yes locale",
                        Map.ofEntries(
                                DEFAULT_MESSAGE,
                                Map.entry("messageYes",
                                        Set.of("[fr]=Une Banane", "[de]=Eine Banane", "[jkl]=NOT VALID")),
                                DEFAULT_NO
                        ),
                        "Invalid locale provided"
                },
                {
                        "Invalid no locale",
                        Map.ofEntries(
                                DEFAULT_MESSAGE,
                                DEFAULT_YES,
                                Map.entry("messageNo",
                                        Set.of("[fr]=Une Banane", "[de]=Eine Banane", "[jkl]=NOT VALID"))
                        ),
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
