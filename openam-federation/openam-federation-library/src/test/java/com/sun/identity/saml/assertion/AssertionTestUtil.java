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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml.assertion;

import static com.sun.identity.saml.common.SAMLConstants.AUTH_METHOD_PASSWORD_URI;
import static com.sun.identity.saml.common.SAMLConstants.CONFIRMATION_METHOD_BEARER;
import static org.forgerock.openam.utils.Time.newDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AssertionTestUtil {

    static String getSignedWSFedAssertionXml() throws Exception {
        List<String> targets = new ArrayList<>();
        targets.add("urn:federation:MicrosoftOnline");
        AudienceRestrictionCondition arc = new AudienceRestrictionCondition(targets);

        Subject sub = new Subject(new NameIdentifier("TestUser", "", "http://schemas.xmlsoap.org/claims/UPN"));
        sub.setSubjectConfirmation(new SubjectConfirmation(CONFIRMATION_METHOD_BEARER));

        Set<Statement> statements = new HashSet<Statement>(1);
        statements.add(new AuthenticationStatement(AUTH_METHOD_PASSWORD_URI, newDate(), sub, null, null));

        Date issueInstant = newDate();
        long skewPeriod = 600000L;
        Date notBefore = new Date(issueInstant.getTime() - skewPeriod);
        Date notAfter = new Date(issueInstant.getTime() + skewPeriod);

        Conditions cond = new Conditions(notBefore, notAfter, null, arc);
        Assertion assertion = new Assertion("abcdefg123456", "openam-wsfed-idp", issueInstant, cond, statements);
        assertion.signXML("defaultkey");

        return assertion.toString(true, true);
    }
}
