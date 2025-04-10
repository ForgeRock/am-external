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

package com.sun.identity.saml2.protocol.impl;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.test.bazel.ResourceFinder.findPathToResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import com.sun.identity.saml2.assertion.AudienceRestriction;

import org.junit.jupiter.api.Test;
import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.ProxyRestriction;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.impl.AudienceRestrictionImpl;
import com.sun.identity.saml2.assertion.impl.BaseIDImpl;
import com.sun.identity.saml2.assertion.impl.ConditionsImpl;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.assertion.impl.OneTimeUseImpl;
import com.sun.identity.saml2.assertion.impl.ProxyRestrictionImpl;
import com.sun.identity.saml2.assertion.impl.SubjectConfirmationImpl;
import com.sun.identity.saml2.assertion.impl.SubjectImpl;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.RequesterID;
import com.sun.identity.saml2.protocol.Scoping;

public class AuthnRequestImplTest {
    private static final String ACS_URL = "https://acs.example.com/foo";

    @Test
    void shouldProduceCorrectXml() throws Exception {
        // Given
        final long referenceTimestamp = 1622626177000L; // Wed Jun 02 10:29:37 BST 2021

        // Create a representative AuthnRequest object with most attributes set to provide decent coverage of XML
        // serialisation.
        AuthnRequestImpl request = new AuthnRequestImpl();
        request.setID("testId");
        request.setVersion(SAML2Constants.VERSION_2_0);
        request.setIssueInstant(new Date(referenceTimestamp));
        request.setAssertionConsumerServiceIndex(1);
        request.setForceAuthn(true);
        request.setAssertionConsumerServiceURL(ACS_URL);
        request.setIsPassive(false);
        request.setAttributeConsumingServiceIndex(2);
        request.setProtocolBinding(SAML2Constants.HTTP_POST);
        request.setProviderName("testProvider");
        request.setDestination("https://foo.example.com/bar");

        NameIDPolicy nameIDPolicy = new NameIDPolicyImpl();
        nameIDPolicy.setAllowCreate(true);
        nameIDPolicy.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        nameIDPolicy.setSPNameQualifier("test");
        request.setNameIDPolicy(nameIDPolicy);

        Scoping scoping = new ScopingImpl();
        scoping.setProxyCount(3);
        IDPList idpList = new IDPListImpl();
        IDPEntry idp1 = new IDPEntryImpl();
        idp1.setName("idp1");
        idp1.setLoc("https://idp1.example.com/idp1");
        idp1.setProviderID("urn:idp:1");
        IDPEntry idp2 = new IDPEntryImpl();
        idp2.setName("idp2");
        idp2.setLoc("https://idp2.example.net/idp2");
        idp2.setProviderID("urn:idp:2");
        idpList.setIDPEntries(List.of(idp1, idp2));
        GetCompleteImpl getComplete = new GetCompleteImpl();
        getComplete.setValue("https://foo.example.com/idps");
        idpList.setGetComplete(getComplete);
        scoping.setIDPList(idpList);
        RequesterID requesterID = new RequesterIDImpl();
        requesterID.setValue("https://requester.example.com/");
        scoping.setRequesterIDs(List.of(requesterID));
        request.setScoping(scoping);

        Subject subject = new SubjectImpl();
        BaseID baseID = new BaseIDImpl();
        baseID.setSPNameQualifier("testSPNameQualifier");
        baseID.setNameQualifier("testNameQualifier");
        subject.setBaseID(baseID);
        SubjectConfirmation subjectConfirmation = new SubjectConfirmationImpl();
        subjectConfirmation.setBaseID(baseID);
        subjectConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
        subject.setSubjectConfirmation(List.of(subjectConfirmation));
        request.setSubject(subject);

        RequestedAuthnContext requestedAuthnContext = new RequestedAuthnContextImpl();
        requestedAuthnContext.setAuthnContextClassRef(List.of("a", "b"));
        requestedAuthnContext.setComparison("exact");
        request.setRequestedAuthnContext(requestedAuthnContext);

        Conditions conditions = new ConditionsImpl();
        conditions.setNotBefore(new Date(referenceTimestamp - 3600000L));
        conditions.setNotOnOrAfter(new Date(referenceTimestamp + 3600000L));
        AudienceRestriction audienceRestriction = new AudienceRestrictionImpl();
        audienceRestriction.setAudience(List.of("https://a.b.com/", "https://b.c.com/"));
        conditions.setAudienceRestrictions(List.of(audienceRestriction));
        ProxyRestriction proxyRestriction = new ProxyRestrictionImpl();
        proxyRestriction.setAudience(List.of("https://proxy1.com", "https://proxy2.com"));
        proxyRestriction.setCount(2);
        conditions.setProxyRestrictions(List.of(proxyRestriction));
        conditions.setOneTimeUses(List.of(new OneTimeUseImpl()));
        request.setConditions(conditions);

        Issuer issuer = new IssuerImpl();
        issuer.setValue("foo");
        issuer.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        request.setIssuer(issuer);

        // When
        String xml = request.toXMLString(true, true);

        // Then
        Path path = findPathToResource(this.getClass(), "/saml2/example-authnrequest.xml");
        // Skip copyright header:
        String expected = Files.readAllLines(path).stream().skip(8).collect(joining("\n"));
        assertThat(xml).isEqualToIgnoringWhitespace(expected);
    }
}
