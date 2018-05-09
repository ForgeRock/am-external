/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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