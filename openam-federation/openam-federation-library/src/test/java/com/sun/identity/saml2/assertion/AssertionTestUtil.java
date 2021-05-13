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
 * Copyright 2018 ForgeRock AS.
 */
package com.sun.identity.saml2.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.ArtifactResponse;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;

public class AssertionTestUtil {

    private AssertionFactory assertionFactory;
    private Issuer defaultIssuer;
    private NameID defaultNameId;
    private Subject defaultSubject;
    private ObjectMapper mapper;
    private ObjectMapper plainmapper;

    @BeforeMethod
    public void setUp() throws Exception {

        assertionFactory = AssertionFactory.getInstance();
        defaultIssuer = assertionFactory.createIssuer();
        defaultIssuer.setNameQualifier("name-qualify");
        defaultIssuer.setValue("issuer");
        defaultIssuer.setFormat("noformat");
        defaultSubject = assertionFactory.createSubject();
        defaultNameId = assertionFactory.createNameID();
        defaultNameId.setValue("nameid");
        defaultSubject.setNameID(defaultNameId);

        // JSON Mapper (using same CTS options)
        mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true);

        /**
         * @see http://stackoverflow.com/questions/7105745/how-to-specify-jackson-to-only-use-fields-preferably-globally
         */
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        // @JsonIgnoreProperties( { "mutable" } ) may need to be added later
        plainmapper = new ObjectMapper();
        plainmapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    public void testAssertionShouldSerialize() throws Exception {

        // given
        Assertion assertion = createTestAssertion("assertion-01", defaultIssuer);
        AuthnStatement authn = createTestAuthnStatement();
        ArrayList<AuthnStatement> authnStmts = new ArrayList<>();
        authnStmts.add(authn);
        assertion.setAuthnStatements(authnStmts);
        assertion.setSubject(defaultSubject);

        // when
        String assertionString = mapper.writeValueAsString(assertion);
        Assertion assertion2 = mapper.readValue(assertionString, Assertion.class);

        // then
        assertThat(assertion2.toXMLString()).isXmlEqualTo(assertion.toXMLString());
    }

    @Test
    public void testAssertionSubjectLocalityShouldSerialize() throws Exception {

        Assertion assertion = createTestAssertion("assertion-01", defaultIssuer);
        AuthnStatement authn = createTestAuthnStatement();
        assertion.setSubject(defaultSubject);
        SubjectLocality subjLocality = assertionFactory.createSubjectLocality();
        subjLocality.setAddress("127.0.0.1");
        authn.setSubjectLocality(subjLocality);
        ArrayList<AuthnStatement> authnStmts = new ArrayList<>();
        authnStmts.add(authn);
        assertion.setAuthnStatements(authnStmts);

        // when
        String authnString = mapper.writeValueAsString(authn);
        AuthnStatement authn2 = mapper.readValue(authnString, AuthnStatement.class);
        String assertionString = mapper.writeValueAsString(assertion);
        Assertion assertion2 = mapper.readValue(assertionString, Assertion.class);

        // then
        assertThat(authn.toXMLString()).isXmlEqualTo(authn2.toXMLString());
        assertThat(assertion2.toXMLString()).isXmlEqualTo(assertion.toXMLString());
    }

    @Test
    public void testAssertionAdviceShouldSerialize() throws Exception {

        Assertion assertion = createTestAssertion("assertion-01", defaultIssuer);
        AuthnStatement authn = createTestAuthnStatement();

        // Advice
        Advice advice = assertionFactory.createAdvice();
        AssertionIDRef assertionIDref = assertionFactory.createAssertionIDRef();
        assertionIDref.setValue("idref");
        List<AssertionIDRef> assertionIDRefs = new ArrayList<>();
        assertionIDRefs.add(assertionIDref);
        advice.setAssertionIDRefs(assertionIDRefs);
        advice.setAdditionalInfo(Arrays.asList("<data>advice1</data>","<data>advice2</data>"));
        assertion.setAdvice(advice);

        // Conditions
        AudienceRestriction audRestriction = assertionFactory.createAudienceRestriction();
        audRestriction.setAudience(Arrays.asList("audience-1"));
        Conditions conditions = assertionFactory.createConditions();
        conditions.setAudienceRestrictions(Arrays.asList(audRestriction));
        conditions.setNotBefore(new Date());
        assertion.setConditions(conditions);

        // when
        String assertionString = mapper.writeValueAsString(assertion);
        Assertion assertion2 = mapper.readValue(assertionString, Assertion.class);

        // then
        assertThat(assertion2.toXMLString()).isXmlEqualTo(assertion.toXMLString());
        assertThat(assertion2.getAdvice().getAdditionalInfo().size()).isGreaterThan(0);
    }

    // Disabled: Authz/Evidence not implemented in AssertionFactory
    @Test(enabled = false)
    public void testAssertionAuthzDecisionStmtAndEvidence() throws Exception {

        Assertion assertion = createTestAssertion("assertion-01", defaultIssuer);
        AuthnStatement authn = createTestAuthnStatement();
        ArrayList<AuthnStatement> authnStmts = new ArrayList<>();
        authnStmts.add(authn);
        assertion.setAuthnStatements(authnStmts);

        // AssertionURIRefImpl is a String
        // AuthzDecStmt not implemented
        // Evidence  not implemented
        AuthzDecisionStatement authzStmt = assertionFactory.createAuthzDecisionStatement();
        Action action = assertionFactory.createAction();
        action.setValue("action-1");
        action.setNamespace("no-namespace");

        Evidence evidence = assertionFactory.createEvidence();
        AssertionIDRef assertionIDref = assertionFactory.createAssertionIDRef();
        assertionIDref.setValue("idref");
        evidence.setAssertionIDRef(Arrays.asList(assertionIDref));
        authzStmt.setEvidence(evidence);
        authzStmt.setAction(Arrays.asList(action));
        assertion.setAuthzDecisionStatements(Arrays.asList(authzStmt));
        assertion.makeImmutable();

        // when
        String assertionString = mapper.writeValueAsString(assertion);
        Assertion assertion2 = mapper.readValue(assertionString, Assertion.class);

        // then
        assertThat(assertion2.toXMLString()).isXmlEqualTo(assertion.toXMLString());
    }

    @Test
    public void testAssertionSubjectConfirmationShouldSerialize() throws Exception {

        // SubjectConfirmation
        SubjectConfirmation subjconf = assertionFactory.createSubjectConfirmation();
        subjconf.setNameID(defaultNameId);
        subjconf.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
        SubjectConfirmationData subjconfdata = assertionFactory.createSubjectConfirmationData();
        subjconfdata.setAddress("noaddress");
        subjconfdata.setInResponseTo("me");
        subjconfdata.setRecipient("destination");
        subjconfdata.setNotBefore(new Date());
        subjconfdata.setNotOnOrAfter(new Date());
        subjconf.setSubjectConfirmationData(subjconfdata);

        // SubjectConfirmation
        String subjconfXML = subjconf.toXMLString(true, true);
        String subjconfString = mapper.writeValueAsString(subjconf);
        SubjectConfirmation tmpsubjconf = mapper.readValue(subjconfString, SubjectConfirmation.class);
        assertThat(subjconf.toXMLString()).isXmlEqualTo(tmpsubjconf.toXMLString());

        // Subject with SubjectConfirmations
        java.util.List confirmations = java.util.Arrays.asList(subjconf);
        Subject subject = assertionFactory.createSubject();
        subject.setSubjectConfirmation(confirmations);
        String subjectString = mapper.writeValueAsString(subject);
        Subject tmpsubject = mapper.readValue(subjectString, Subject.class);

        // In Assertion
        Assertion assertion = createTestAssertion("assertion-01", defaultIssuer);
        AuthnStatement authn = createTestAuthnStatement();
        ArrayList<AuthnStatement> authnStmts = new ArrayList<>();
        authnStmts.add(authn);
        assertion.setAuthnStatements(authnStmts);
        assertion.setSubject(tmpsubject);
        String assertionString = mapper.writeValueAsString(assertion);
        Assertion assertion2 = mapper.readValue(assertionString, Assertion.class);

        assertThat(subjconf.toXMLString(true, true))
                .isXmlEqualTo(tmpsubjconf.toXMLString(true, true));
        assertThat(subject.toXMLString(true, true))
                .isXmlEqualTo(tmpsubject.toXMLString(true, true));
        assertThat(assertion2.toXMLString()).isXmlEqualTo(assertion.toXMLString());
    }

    @Test
    public void testEncryptedAssertionShouldSerialize() throws Exception {

        EncryptedAssertion encAssertion = createTestEncryptedAssertion();
        String tmpencJson = mapper.writeValueAsString(encAssertion);
        EncryptedAssertion encAssertion2 = mapper.readValue(tmpencJson, EncryptedAssertion.class);
        assertThat(encAssertion2.toXMLString(true, true))
                .isXmlEqualTo(encAssertion.toXMLString(true, true));
    }

    @Test
    public void testEncryptedIDShouldSerialize() throws Exception {

        String encIDXML = "<saml:EncryptedID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xenc='http://www.w3.org/2001/04/xmlenc#' xmlns:ds='http://www.w3.org/2000/09/xmldsig#'>  <xenc:EncryptedData>"
                + "<xenc:EncryptionMethod Algorithm=\"http://www.example.com/\">Any text, intermingled with:    <xenc:KeySize>1</xenc:KeySize>    <xenc:OAEPparams>GpM7</xenc:OAEPparams>    <!--any element-->   </xenc:EncryptionMethod>   <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">Any text, intermingled with:    <ds:KeyName>string</ds:KeyName>   </ds:KeyInfo>   <xenc:CipherData>    <xenc:CipherValue>GpM7</xenc:CipherValue> "
                + "</xenc:CipherData> <xenc:EncryptionProperties> <xenc:EncryptionProperty>Any text, intermingled with:...    </xenc:EncryptionProperty>   </xenc:EncryptionProperties>  </xenc:EncryptedData>  <xenc:EncryptedKey>   "
                + "<xenc:EncryptionMethod Algorithm=\"http://www.example.com/\">Any text, intermingled with:    <xenc:KeySize>1</xenc:KeySize>    <xenc:OAEPparams>GpM7</xenc:OAEPparams>"
                + "<!--any element-->   </xenc:EncryptionMethod>   <ds:KeyInfo>Any text, intermingled with:    <ds:KeyName xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">string</ds:KeyName>   </ds:KeyInfo> "
                + "<xenc:CipherData>    <xenc:CipherValue>GpM7</xenc:CipherValue>   </xenc:CipherData>   <xenc:EncryptionProperties>    <xenc:EncryptionProperty>Any text, intermingled with:...    </xenc:EncryptionProperty>   </xenc:EncryptionProperties>   <xenc:ReferenceList> "
                + "<xenc:DataReference URI=\"http://www.example.com/\">     <!--any element-->    </xenc:DataReference>   </xenc:ReferenceList>   <xenc:CarriedKeyName>string</xenc:CarriedKeyName>  </xenc:EncryptedKey></saml:EncryptedID>";

        // Encrypted field need field mappers
        // mapper = new ObjectMapper();
        // mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        EncryptedID encID = assertionFactory.createEncryptedID(encIDXML);
        Subject encsubj = assertionFactory.createSubject();
        encsubj.setEncryptedID(encID);
        String encsubjString = mapper.writeValueAsString(encsubj);
        Subject encsubj2 = mapper.readValue(encsubjString, Subject.class);

        // In Assertion
        Assertion assertion = createTestAssertion("assertion-1", defaultIssuer);
        assertion.setSubject(encsubj);
        String assertionString = mapper.writeValueAsString(assertion);
        Assertion assertion2 = mapper.readValue(assertionString, Assertion.class);

        // then
        assertThat(encsubj2.toXMLString(true, true))
                .isXmlEqualTo(encsubj.toXMLString(true, true));
        assertThat(assertion2.toXMLString(true, true))
                .isXmlEqualTo(assertion.toXMLString(true, true));
    }

    @Test
    public void protocolResponseShouldSerialize() throws Exception {

        Assertion assertion = assertionFactory.createAssertion();
        assertion.setID("assertion-01");
        assertion.setIssueInstant(new Date());
        assertion.setIssuer(defaultIssuer);
        assertion.setID("b07b804c-7c29-ea16-7300-4f3d6f7928ac");
        assertion.setVersion("2.0");
        AuthnStatement authn = assertionFactory.createAuthnStatement();
        AuthnContext authnCtx = assertionFactory.createAuthnContext();
        Subject subject = assertionFactory.createSubject();
        NameID nameid = assertionFactory.createNameID();
        nameid.setValue("nameid");
        subject.setNameID(nameid);
        SubjectLocality subjLocality = assertionFactory.createSubjectLocality();
        subjLocality.setAddress("127.0.0.1");
        authn.setAuthnInstant(new Date());
        authnCtx.setAuthnContextClassRef(SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT);
        authn.setAuthnContext(authnCtx);
        ArrayList<AuthnStatement> authnStmts = new ArrayList<>();
        authnStmts.add(authn);
        assertion.setAuthnStatements(authnStmts);

        // Response
        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        String responseXML = "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"pfx8b0b9563-d7a0-4b29-d84d-e5ec9c271e30\" Version=\"2.0\" IssueInstant=\"2014-07-17T01:01:48Z\" Destination=\"http://sp.example.com/demo1/index.php?acs\" InResponseTo=\"ONELOGIN_4fee3b046395c4e751011e97f8900b5273d56685\">  <saml:Issuer>http://idp.example.com/metadata.php</saml:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">  <ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>"
                + "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>  <ds:Reference URI=\"#pfx8b0b9563-d7a0-4b29-d84d-e5ec9c271e30\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>"
                + "<ds:DigestValue>F3BLrAssgq+JUgI8Qcsqf2hPJ6o=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>rfeqnuKg94bFXAmv/42CFMRKcwai0G9q2Lan65Li7SpdyNxNFjGl/xI0dyQLY2YwYH27zmuTyeOOt5L+r9r7xSK1gdVj/PePkFqJy/LMrhiWc83Y1vp/5yOKBSt58r2gyXOXe0pQ/hBFJvLcdbZONfjTFn8DCP/FoYh0XlIMu+A=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIICajCCAdOgAwIBAgIBADANBgkqhkiG9w0BAQ0FADBSMQswCQYDVQQGEwJ1czETMBEGA1UECAwKQ2FsaWZvcm5pYTEVMBMGA1UECgwMT25lbG9naW4gSW5jMRcwFQYDVQQDDA5zcC5leGFtcGxlLmNvbTAeFw0xNDA3MTcxNDEyNTZaFw0xNTA3MTcxNDEyNTZaMFIxCzAJBgNVBAYTAnVzMRMwEQYDVQQIDApDYWxpZm9ybmlhMRUwEwYDVQQKDAxPbmVsb2dpbiBJbmMxFzAVBgNVBAMMDnNwLmV4YW1wbGUuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDZx+ON4IUoIWxgukTb1tOiX3bMYzYQiwWPUNMp+Fq82xoNogso2bykZG0yiJm5o8zv/sd6pGouayMgkx/2FSOdc36T0jGbCHuRSbtia0PEzNIRtmViMrt3AeoWBidRXmZsxCNLwgIV6dn2WpuE5Az0bHgpZnQxTKFek0BMKU/d8wIDAQABo1AwTjAdBgNVHQ4EFgQUGHxYqZYyX7cTxKVODVgZwSTdCnwwHwYDVR0jBBgwFoAUGHxYqZYyX7cTxKVODVgZwSTdCnwwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQ0FAAOBgQByFOl+hMFICbd3DJfnp2Rgd/dqttsZG/tyhILWvErbio/DEe98mXpowhTkC04ENprOyXi7ZbUqiicF89uAGyt1oqgTUCD1VsLahqIcmrzgumNyTwLGWo17WDAa1/usDhetWAMhgzF/Cnf5ek0nK00m0YZGyc4LzgD0CROMASTWNg==</ds:X509Certificate>"
                + "</ds:X509Data></ds:KeyInfo></ds:Signature>  <samlp:Status>    <samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>  </samlp:Status>  <saml:Assertion xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" ID=\"pfx96c78a4a-c4dc-6453-fd2d-95e74d678255\" Version=\"2.0\" IssueInstant=\"2014-07-17T01:01:48Z\">    <saml:Issuer>http://idp.example.com/metadata.php</saml:Issuer>"
                + "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">  <ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>    <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>  <ds:Reference URI=\"#pfx96c78a4a-c4dc-6453-fd2d-95e74d678255\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>"
                + "m5ldcVmOnWYUFKecyClH2aqBnfI=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>"
                + "Pzjres6GhIHlqw4SJsvuUGflF79WklPLr8GzeyIOoMEDl+dfsZUx82qKGFymNl3Cx4Ju8ET/zDe4YNIzEJwi5NjsGvvX7ISu5w/rKlwtmaxT+rsDiCn+xXThQJ2S2iUW+zNHH7c8kqhaKX9Fwm+gYNH80s00c/+Z28ZlwCOq0pg=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIICajCCAdOgAwIBAgIBADANBgkqhkiG9w0BAQ0FADBSMQswCQYDVQQGEwJ1czETMBEGA1UECAwKQ2FsaWZvcm5pYTEVMBMGA1UECgwMT25lbG9naW4gSW5jMRcwFQYDVQQDDA5zcC5leGFtcGxlLmNvbTAeFw0xNDA3MTcxNDEyNTZaFw0xNTA3MTcxNDEyNTZaMFIxCzAJBgNVBAYTAnVzMRMwEQYDVQQIDApDYWxpZm9ybmlhMRUwEwYDVQQKDAxPbmVsb2dpbiBJbmMxFzAVBgNVBAMMDnNwLmV4YW1wbGUuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDZx+ON4IUoIWxgukTb1tOiX3bMYzYQiwWPUNMp+Fq82xoNogso2bykZG0yiJm5o8zv/sd6pGouayMgkx/2FSOdc36T0jGbCHuRSbtia0PEzNIRtmViMrt3AeoWBidRXmZsx CNLwgIV6dn2WpuE5Az0bHgpZnQxTKFek0BMKU/d8wIDAQABo1AwTjAdBgNVHQ4EFgQUGHxYqZYyX7cTxKVODVgZwSTdCnwwHwYDVR0jBBgwFoAUGHxYqZYyX7cTxKVODVgZwSTdCnwwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQ0FAAOBgQByFOl+hMFICbd3DJfnp2Rgd/dqttsZG/tyhILWvErbio/DEe98mXpowhTkC04ENprOyXi7ZbUqiicF89uAGyt1oqgTUCD1VsLahqIcmrzgumNyTwLGWo17WDAa1/usDhetWAMhgzF/Cnf5ek0nK00m0YZGyc4LzgD0CROMASTWNg==</ds:X509Certificate></ds:X509Data></ds:KeyInfo>"
                + "</ds:Signature>    <saml:Subject>      <saml:NameID SPNameQualifier=\"http://sp.example.com/demo1/metadata.php\" Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">_ce3d2948b4cf20146dee0a0b3dd6f69b6cf86f62d7</saml:NameID>      <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">        <saml:SubjectConfirmationData NotOnOrAfter=\"2024-01-18T06:21:48Z\" Recipient=\"http://sp.example.com/demo1/index.php?acs\" InResponseTo=\"ONELOGIN_4fee3b046395c4e751011e97f8900b5273d56685\"/>      </saml:SubjectConfirmation>    </saml:Subject>    <saml:Conditions NotBefore=\"2014-07-17T01:01:18Z\" NotOnOrAfter=\"2024-01-18T06:21:48Z\">      <saml:AudienceRestriction>        <saml:Audience>http://sp.example.com/demo1/metadata.php</saml:Audience>"
                + "</saml:AudienceRestriction>    </saml:Conditions>    <saml:AuthnStatement AuthnInstant=\"2014-07-17T01:01:48Z\" SessionNotOnOrAfter=\"2024-07-17T09:01:48Z\" SessionIndex=\"_be9967abd904ddcae3c0eb4189adbe3f71e327cf93\">      <saml:AuthnContext>        <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef>"
                + "</saml:AuthnContext>    </saml:AuthnStatement>    <saml:AttributeStatement>      <saml:Attribute Name=\"uid\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">        <saml:AttributeValue xsi:type=\"xs:string\">test</saml:AttributeValue>      </saml:Attribute>      <saml:Attribute Name=\"mail\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">        <saml:AttributeValue xsi:type=\"xs:string\">test@example.com</saml:AttributeValue>      </saml:Attribute>      <saml:Attribute Name=\"eduPersonAffiliation\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">        <saml:AttributeValue xsi:type=\"xs:string\">users</saml:AttributeValue>        <saml:AttributeValue xsi:type=\"xs:string\">examplerole1</saml:AttributeValue>      </saml:Attribute>    </saml:AttributeStatement>  </saml:Assertion></samlp:Response>";
        Response response = protocolFactory.createResponse(responseXML);
        String responseString = mapper.writeValueAsString(response);
        Response response2 = mapper.readValue(responseString, Response.class);

        assertThat(response2.toXMLString(true, true))
                .isXmlEqualTo(response2.toXMLString(true, true));

    }

    @Test
    public void protocolResponseStatusShouldSerialize() throws Exception {

        // Response
        Assertion assertion = createTestAssertion("assertion-1", defaultIssuer);
        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        Response response = protocolFactory.createResponse();
        response.setID("response-1");
        response.setVersion("2.0");
        response.setIssueInstant(new Date());
        Status status = protocolFactory.createStatus();
        StatusCode statuscode = protocolFactory.createStatusCode();
        statuscode.setValue("statuscode");
        status.setStatusCode(statuscode);
        response.setStatus(status);
        response.setAssertion(Arrays.asList(assertion));
        response.setInResponseTo("me");
        Extensions extensions = ProtocolFactory.getInstance().createExtensions();
        extensions.setAny(Arrays.asList("123", "234"));
        response.setExtensions(extensions);
        String responseString = mapper.writeValueAsString(response);
        Response response2 = mapper.readValue(responseString, Response.class);

        assertThat(response2.toXMLString(true, true))
                .isXmlEqualTo(response2.toXMLString(true, true));

    }

    @Test
    public void protocolResponseEncryptedAssertionShouldSerialize() throws Exception {

        EncryptedAssertion encAssertion = createTestEncryptedAssertion();
        Assertion assertion = createTestAssertion("assertion-1", defaultIssuer);
        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        Response response = protocolFactory.createResponse();
        response.setID("response-1");
        response.setVersion("2.0");
        response.setIssueInstant(new Date());
        Status status = protocolFactory.createStatus();
        StatusCode statuscode = protocolFactory.createStatusCode();
        statuscode.setValue("statuscode");
        status.setStatusCode(statuscode);
        response.setStatus(status);
        response.setAssertion(Arrays.asList(assertion));
        response.setEncryptedAssertion(Arrays.asList(encAssertion));
        response.setInResponseTo("me");

        String responseString = mapper.writeValueAsString(response);
        Response response2 = mapper.readValue(responseString, Response.class);

        assertThat(response2.toXMLString(true, true))
                .isXmlEqualTo(response2.toXMLString(true, true));

    }

    @Test
    public void protocolArtifactResponseShouldSerialize() throws Exception {

        // Protocol ArtifactResponse
        ProtocolFactory protocolFactory = ProtocolFactory.getInstance();
        ArtifactResponse response = protocolFactory.createArtifactResponse();
        response.setVersion("2.0");
        response.setInResponseTo("me");
        response.setIssueInstant(new Date());
        response.setID("artresp-1");
        response.setDestination("destination");
        Status status = protocolFactory.createStatus();
        StatusCode statuscode = protocolFactory.createStatusCode();
        statuscode.setValue("statuscode");
        status.setStatusCode(statuscode);
        response.setStatus(status);
        String responseString = mapper.writeValueAsString(response);
        ArtifactResponse response2 = mapper.readValue(responseString, ArtifactResponse.class);

        assertThat(response2.toXMLString(true, true))
                .isXmlEqualTo(response.toXMLString(true, true));
    }

    @Test(enabled = false)
    public void testAuthzDecisionStatementCanCreate() throws Exception {

        // AuthzDecisionStatement not yet implemented
        String authzXML =
                "<saml:AuthzDecisionStatement Resource=\"http://www.example.com/\" Decision=\"Permit\">"
                        + "<saml:Action Namespace=\"http://www.example.com/\">string</saml:Action>"
                        + "<saml:Evidence> <saml:AssertionIDRef>NCName</saml:AssertionIDRef></saml:Evidence>"
                        + "</saml:AuthzDecisionStatement>";
        AuthzDecisionStatement authz = assertionFactory.createAuthzDecisionStatement(authzXML);
        assertThat(authz).isNotNull();
    }

    @Test
    public void testAssertionSubjectConfirmationDataContentListShouldSerialize() throws Exception {

        SubjectConfirmation subjconf = assertionFactory.createSubjectConfirmation();
        subjconf.setNameID(defaultNameId);
        subjconf.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
        SubjectConfirmationData subjconfdata = assertionFactory.createSubjectConfirmationData();
        subjconfdata.setAddress("noaddress");
        subjconfdata.setInResponseTo("me");
        subjconfdata.setRecipient("destination");
        subjconfdata.setNotBefore(new Date());
        org.w3c.dom.Element xmlelement =
                javax.xml.parsers.DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .newDocument()
                        .createElement("TAGNAME");
        xmlelement.setAttribute("ABC", "123");
        subjconfdata.setContent(Arrays.asList("<!-- EMPTY -->", xmlelement));
        subjconf.setSubjectConfirmationData(subjconfdata);
        String subjconfJson = mapper.writeValueAsString(subjconf);
        SubjectConfirmation tmpsubjconf = mapper.readValue(subjconfJson, SubjectConfirmation.class);

        assertThat(tmpsubjconf.toXMLString())
                .isXmlEqualTo(subjconf.toXMLString());
    }

    private Assertion createTestAssertion(String id, Issuer issuer) throws SAML2Exception {
        Assertion assertion = assertionFactory.createAssertion();
        assertion.setID(id);
        assertion.setIssueInstant(new Date());
        assertion.setIssuer(issuer);
        assertion.setVersion("2.0");
        return assertion;
    }

    private AuthnStatement createTestAuthnStatement() throws SAML2Exception {
        AuthnStatement authn = assertionFactory.createAuthnStatement();
        authn.setAuthnInstant(new Date());
        authn.setSessionNotOnOrAfter(new Date());
        AuthnContext authnCtx = assertionFactory.createAuthnContext();
        authnCtx.setAuthnContextClassRef(SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT);
        authn.setAuthnContext(authnCtx);
        return authn;
    }

    private EncryptedAssertion createTestEncryptedAssertion() throws SAML2Exception {

        String encAssertionXML = "<saml:EncryptedAssertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
                + "<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\" Type=\"http://www.w3.org/2001/04/xmlenc#Element\">"
                + "<xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/>"
                + "<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"><xenc:EncryptedKey><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><xenc:CipherData>"
                + "<xenc:CipherValue>H2ovc4sLLlyqJQeLlrMaiTLhcb9sDC933MWtOgyiN7VeYmjnVQKLY+Cvx3t3rAg8IlOcPn2SOpUppMdgBpxhx1Wm5+i3Uhko4ax/f/5KVDBFGuxYikj0bRiRSIyA4YJboow7tavcoS7j3sWahds0uHS/COuWYYFXPtobdD2mY=</xenc:CipherValue>"
                + "</xenc:CipherData></xenc:EncryptedKey></dsig:KeyInfo>"
                + "<xenc:CipherData><xenc:CipherValue>tGOTBEq3d8Tqj4at2yoz0UfffqU9rahCvGKohBlzf7hiYLeVfh3ZmiB5SBVQuRyDmhjdOPvi0mJdjJ7h/sQwFENJXz8gXi0F/+PkJ5XMXmAKWdPAhI5pbXAEV+7uU9r6+s7Hyzh1/5jwmb4lukLeD4IOeYxzDkZH6KqNCG8DhJp6iWL"
                + "kGgAV+gdyYZ82t2ZSXTSceVX9TYqF8DnX3NB8P/BZiLUGH+NjxOseWN0kwZOTs0z60Az7wgPWgvJPDjzDEF7bykJ7ko61qRlaxQCOU4pHcMOFIbLQi73kQZ54Bzvamj9MuWqV4Cn7l9MQ1lfqdc2OEuoZ3QeclTESI4E9DYVePQ+RaEQ3ETnGNvz4zZYRD/h9RMMycczJOlraOXw0y"
                + "rHXLf6pzn4wzFnATmSPov+TAumhdcRfv59i0Relv3N0H397WE341mtOMKxFl5absDVAS/J1kalVtmEWcrM6jOzPT+6VWaXgaXHCNl+/cM8Ufy0U+bz4Tj39xbXgCYKWluPCg3pUAfUDsQc9gkNyuBC5du1vyU5+tEH1PuYxGfHyk9KLgxa2N0gSSkx52A5UPTv6AQ9HzB37eRRuP8p"
                + "3GIlOYiHgwhiYQ6fkakaWQzhG4os96R7+XlLHRZ3qyzsB8VA2mIwyrWeMn94WBIbPKLfn1ZztJjR17Hwy5zOZ20dTlMLvp5xdakix+p6k+4Iegz1OqCij6ZAY5KZ8oqp/i88MNNe1oyUNY5sfDYf/U3AEv2uBzpb3sRwYD0/5DEY6df9CgyvfjrrcGggAxnHKmXkjN5LW3/hmUzl/W"
                + "orAxdBbJr2TjHSpAEzBYLMngoNIvE8jkjOdH20M2/D7PQBkXofKsJu+z95fdEsI8LQg342wK1xsQb3dSXvx7WYDSnapi6aVOhol4/WhMicyIN+2sJxxQz9cjzbWZQPQ3b/WiovJoOXjua9/ILFy16ck3BLlbUYijduGmaPJDlMypT6XiHI2xMdLAmsXrlnlr8dE0TbEUKVLoTGgzrJ"
                + "iKgG4CvSjmLhQYfBP7TfB4QaMYnA2648d8jS7GuXfSiNXrw72lnOT28tPcV3iYZgMH/qKQC9M9Vxx0wBBeIp1RhEVy3GhdmBymHESxjcRTcxG0zKe/WmWEuA1QN/SGO/3ltPn2IyJI2jhqUO3WWKapxGo3UT6aB/2ULvs9CBwpZDTuMfqLIq7b0NGx+umkapHm6oqbrtDRKGC/dIHp"
                + "mYs+gPy2Y48tyRJigy3Rd6vaZE7FVofundUA1mAYoSgoVzL2ab8whruESEmVIJVxk/RS3t3hYC+GOq5p6Cv3TWHRwUpcjKhrw5KIUlrZRRhcipJO3GDJ8NEFXB3A+RvgVpOPeuafE6HTKOFY5fDZwGwsgm8rG37+yI6VKSRuoLzNkZl4wApGLlcYmy35uhh0QMNpcmkvLVsrWaHUYs"
                + "0ZN8JBxeGtepTBycBxhzxlJLkYqFsgjz0/qnuPCt+1w+wewzL0TWMfd87BvfcFaFQHu8rjUpO1hlLTQV343ixnvyTfQ4cBkh4lWDAa3ZF5NHPVuyABZpI07zldUglNpQYoh326V3OIQFvB6iUPLqQnk2g3NdrMB2E3qCiKxIb8j3vTiQgPaPZw2WNacdmRTw3dRHHtStS/qCA1CLby"
                + "Kyc35r8gVLFVaRdeV0K155SdJO9uXYE3OcAOjzwGKxo+ygMj898gx5Mg7EK4rhnZ8RW/5TVimfCL/AynIfBeuYhhVOKC1VN/iMlXNqtwyi8u6d6kllstIP1aZuzA+OniS7xUKclcExAuCix5JlkKAzPcKuwwTJf21H0CWAZcRBa+Htsby42P7Q3vQDEuYFVtG3zL0ge9q3y2iHXcrd"
                + "sOcgleoYkEkOdO16U90ZLCxxzo24S5rDZBg65Xdu1XE87Px2pLOssce5vPn3dsFoamj2PIvKmdFuFO40SGMc/ilKR4yRxn4RUJb9Na74oT+IFEBS5ASUMwyWAO6VsHKlW8J5CK01nnY6GSbvFQyHesi2Om3iYGE8OTIaBo9aW+zc1lloXZOeZv/G75WYl1D4FwUxHLNJxzkgwDDaqS"
                + "viyFjeby1zoLMj3uvcHW7X0ja0KjbR3FLwko7hPP2wj6rB8wgmXQCZmedTNjxVMUFunSZglJbTOSjmjWDELNLRoxXl1I8wKeiMKNQU5XQhoqJOZEqCYKYhubi39QPyad0iYy1ua4Eql9ZkQEj0IL3Hlya33AIvRvRz4V4Sibq0WZaJ2B+GMOs6T6hswRyu0KkXXMPNqkc/ouubtH/A"
                + "S08trE8ynW5iaTgitxsgPZZnb62GGweIy5MdojHBRDzr20a6MD1tG/PBahT583baiePgm064HB6nsW3B5AQhsbxsT8ZzZAfr1CSEwXDfY66LqNgTmdpZyk/JoE05g7r4iP2kzABq9JP2dW1iLtLakXRW1dS+QeAXwo0zhu2SHX0jgYDG38ve4l5LuBQb652sTmw/wZ7UIQ/qGLZWiu"
                + "ASTpAXR1fH3rHzky+mtkFylwrtYoIGPVrGD8xfa9OGar+YbkFYrL/0wZ/nSSLT05M9CVNGuPtGBG2BruSKwt6jv3SjNO6TX+PA5sm1B2NLgAn8hm4FKuNstaBm4FsyJGQxNV6qQUkg9pz2tIbV2LDRvZTA4INn6mDCTY/JgngvqGrR4naeRZLvaJRhXUbzveH3CCsmI03jXYDyHZII"
                + "R1ZZFV8bvimpCc+Dvu2i7P5VFRJwalsAsfim6RflQ40XIanfVNhR8mgKK2CrRBOFiUokMY09X2sqkktqZUnruU1Y3nA5Kfa/rzUC4+KYIBqxQvY8vJvJCylO5jJrjBQZWFuPll/+Y7u7z0DBoLWBgN7S864gXvSUDY+zLLxpPi9+yQt0xcxqq5+FcHXmsu4EaJo2OuTD6kuyS5p8ZI"
                + "NWD4u7x3eVLDkeUWao=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData></saml:EncryptedAssertion>";

        EncryptedAssertion encAssertion = assertionFactory.createEncryptedAssertion(encAssertionXML);
        return encAssertion;
    }

}
