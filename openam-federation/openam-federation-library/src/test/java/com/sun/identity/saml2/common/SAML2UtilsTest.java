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
 * Copyright 2014-2021 ForgeRock AS.
 */

package com.sun.identity.saml2.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.IndexedEndpointType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.encode.URLEncDec;

public class SAML2UtilsTest {

    @Test
    public void encodeDecodeTest() {

        // the max length of each random String to encode
        int maxStringLength = 300;
        // the number of encode/decode iterations we want to test
        int randomStringsCount = 3000;
        Random R = new Random();

        int i = 0;
        while (i < randomStringsCount) {
            int size = R.nextInt(maxStringLength);
            // We don't want any 0 length arrays
            while (size == 0) {
                size = R.nextInt(maxStringLength);
            }
            i++;
            String randomString = RandomStringUtils.randomAlphanumeric(size);
            String encoded = SAML2Utils.encodeForRedirect(randomString);
            String decoded = SAML2Utils.decodeFromRedirect(URLEncDec.decode(encoded));
            assertThat(decoded).isEqualTo(randomString);
        }
    }

    @Test
    public void getMappedAttributesTest() {

        List<String> mappings = new ArrayList<>(6);

        mappings.add("invalid entry");
        mappings.add("name1=value");
        mappings.add("name2=\"static value\"");
        mappings.add("name3=\"static cn=value\"");
        mappings.add("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:name4=value");
        mappings.add("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|name5=\"static value\"");

        Map<String, String> mappedAttributes = SAML2Utils.getMappedAttributes(mappings);

        assertThat(mappedAttributes).isNotNull().hasSize(5);
        assertThat(mappedAttributes).containsEntry("name1", "value");
        assertThat(mappedAttributes).containsEntry("name2", "\"static value\"");
        assertThat(mappedAttributes).containsEntry("name3", "\"static cn=value\"");
        assertThat(mappedAttributes).containsEntry("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:name4", "value");
        assertThat(mappedAttributes).containsEntry("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|name5", "\"static value\"");
    }

    @DataProvider(name = "cookies")
    public Object[][] getCookies() {
        String jsessionId = "JSESSIONID=testjsessionidvalue";
        String sessionCookie = "iPlanetDirectoryPro=r3RWxelZgS5tgL8Zgbn46-WfXjw."
                + "*AAJTSQACMDEAAlNLABxNK1hGSHkwL1lHRkhQdFlsVnIzWFpGYWVud2c9AAR0eXBlAANDVFMAAlMxAAA.*";
        return new Object[][]{
                {ImmutableMap.of("Cookie", singletonList(jsessionId))},
                {ImmutableMap.of("Cookie", singletonList(sessionCookie))},
                {ImmutableMap.of("Cookie", singletonList(String.join(";", jsessionId, sessionCookie)))},
                {ImmutableMap.of("Cookie", emptyList())},
                {null}
        };
    }

    /**
     * Perform processing of cookies and verify that no exceptions are thrown.
     * @param headers The set of headers that include 'cookie's
     */
    @Test(dataProvider = "cookies")
    public void processCookiesTest(Map<String, List<?>> headers) {
        HttpServletResponse response = mockHttpServletResponse();
        HttpServletRequest request = mock(HttpServletRequest.class);
        SAML2Utils.processCookies(headers, request, response);
    }

    @Test
    public void shouldVerifyAssertionConsumerServiceLocation() throws SAML2Exception {
        // Given
        String spEntityID = "testEntityId_1";
        String acsUrl = getBaseUrl() + "Consumer/metaAlias/sp1";
        createEntityDescriptor(getDefaultRealm(), spEntityID, acsUrl);
        // When
        assertThatCode(() -> SAML2Utils.verifyAssertionConsumerServiceLocation(getDefaultRealm(), spEntityID,
                mockHttpServletRequest(acsUrl), mockHttpServletResponse()))
                .doesNotThrowAnyException();
    }

    @Test(expectedExceptions = SAML2Exception.class,
            expectedExceptionsMessageRegExp = "Invalid Assertion Consumer Location specified")
    public void shouldFailToVerifyAssertionConsumerServiceLocationGivenNoLocationMatch() throws SAML2Exception {
        // Given
        String spEntityID = "testEntityId_2";
        HttpServletRequest request = mockHttpServletRequest(getBaseUrl() + "Consumer/metaAlias/sp1");
        mockRequestDispatcher(request);
        createEntityDescriptor(getDefaultRealm(), spEntityID, getBaseUrl() + "AuthConsumer/metaAlias/sp1");
        // When
        SAML2Utils.verifyAssertionConsumerServiceLocation(
                getDefaultRealm(), spEntityID, request, mockHttpServletResponse());
    }

    private void generateUrlCombinationTestCase(String[] baseUrls, String[] acsUrls, List<Object[]> params) {
        List<String> testList = new ArrayList<>();
        for (String path : baseUrls) {
            for (String acs : acsUrls) {
                testList.add(path + acs);
            }
        }
        for (String request : testList) {
            for (String acs: testList) {
                params.add(new Object[] { request, acs });
            }
        }
    }

    @DataProvider
    public Object[][] spACSPass() {
        List<Object[]> params = new ArrayList<>();
        final String spACSURL = "Consumer/metaAlias/sp1";
        final String[] spACSURLs = new String[] { spACSURL, spACSURL + "?q=1", spACSURL + "?q=2", spACSURL + "?q2=1" };
        generateUrlCombinationTestCase( new String[] { "http://localhost:8080/am/" }, spACSURLs, params);
        generateUrlCombinationTestCase(new String[] { "http://localhost/am/", "http://localhost:80/am/" },
                spACSURLs, params);
        generateUrlCombinationTestCase(new String[] {"https://localhost/am/", "https://localhost:443/am/" },
                spACSURLs, params);
        return params.toArray(new Object[0][0]);
    }

    @Test(dataProvider = "spACSPass")
    public void shouldVerifyAssertionConsumerServiceLocation(String requestUrl, String acsUrl)  throws SAML2Exception {
        // Given
        String spEntityID = String.format("testEntityId_1_%s_%s", requestUrl, acsUrl);
        createEntityDescriptor(getDefaultRealm(), spEntityID, acsUrl);

        // When
        assertThatCode(() -> SAML2Utils.verifyAssertionConsumerServiceLocation(getDefaultRealm(), spEntityID,
                mockHttpServletRequest(requestUrl), mockHttpServletResponse()))
                .doesNotThrowAnyException();
    }

    @DataProvider
    public Object[][] spACSFail() {
        final String spACSURL1 = getBaseUrl() + "Consumer/metaAlias/sp1";
        final String spACSURL2 = getBaseUrl() + "AuthConsumer/metaAlias/sp1";
        final String wrongHostRequestACSURL1 = "http://otherhost/am/" + "Consumer/metaAlias/sp1";
        final String wrongHostRequestACSURL2 = "http://otherhost/am/" + "AuthConsumer/metaAlias/sp1";
        return new Object[][]{
                { spACSURL1, spACSURL2},
                { wrongHostRequestACSURL1, spACSURL1 },
                { wrongHostRequestACSURL1, spACSURL2 },
                { spACSURL2, spACSURL1 },
                { wrongHostRequestACSURL2, spACSURL1 },
                { wrongHostRequestACSURL2, spACSURL2 }
        };
    }

    @Test(dataProvider = "spACSFail",
            expectedExceptions = SAML2Exception.class,
            expectedExceptionsMessageRegExp = "Invalid Assertion Consumer Location specified"
    )
    public void shouldFailToVerifyAssertionConsumerServiceLocationGivenNoLocationMatch(
            String requestURL, String acsUrl) throws SAML2Exception {
        // Given
        String spEntityID = String.format("testEntityId_2_%s_%s", requestURL, acsUrl);
        HttpServletRequest request = mockHttpServletRequest(requestURL);
        mockRequestDispatcher(request);
        createEntityDescriptor(getDefaultRealm(), spEntityID, acsUrl);

        // When
        SAML2Utils.verifyAssertionConsumerServiceLocation(
                getDefaultRealm(), spEntityID, request, mockHttpServletResponse());
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Request URL in request cannot be null")
    public void shouldFailToVerifyAssertionConsumerServiceLocationGivenEmptyRequestUrl() throws SAML2Exception {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        // When
        SAML2Utils.verifyAssertionConsumerServiceLocation(
                getDefaultRealm(), "testEntityId_3", request, mockHttpServletResponse());
    }

    private String getBaseUrl() {
        return "http://localhost:8080/am/";
    }

    private String getDefaultRealm() {
        return "/";
    }

    private void mockRequestDispatcher(HttpServletRequest request) {
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);
        given(request.getRequestDispatcher("/saml2/jsp/saml2error.jsp?errorcode=invalidACSLocation" +
                "&httpstatuscode=400&errormessage=Invalid%20Assertion%20Consumer%20Location%20specified"))
                .willReturn(requestDispatcher);
    }

    private HttpServletResponse mockHttpServletResponse() {
        return mock(HttpServletResponse.class);
    }

    private HttpServletRequest mockHttpServletRequest(String requestUrl) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRequestURL()).willReturn(new StringBuffer(requestUrl));
        return request;
    }

    private void createEntityDescriptor(String realm, String spEntityID, String acsUrl) throws SAML2MetaException {
        SAML2MetaManager saml2MetaManager = SAML2Utils.getSAML2MetaManager();
        EntityDescriptorElement entityDescriptorElement =
                createTestEntityDescriptorElement(spEntityID, acsUrl);
        saml2MetaManager.createEntityDescriptor(realm, entityDescriptorElement);
    }

    private EntityDescriptorElement createTestEntityDescriptorElement(String spEntityID, String acsUrl) {
        SPSSODescriptorType spssoDescriptorType = new SPSSODescriptorType();
        spssoDescriptorType.setExtensions(new ExtensionsType());
        IndexedEndpointType indexedEndpointType = new IndexedEndpointType();
        indexedEndpointType.setLocation(acsUrl);
        spssoDescriptorType.getAssertionConsumerService().add(indexedEndpointType);
        EntityDescriptorElement entityDescriptorElement = new EntityDescriptorElement(new EntityDescriptorType());
        entityDescriptorElement.getValue()
                .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor().add(spssoDescriptorType);
        entityDescriptorElement.getValue().setEntityID(spEntityID);
        return entityDescriptorElement;
    }

    @Test
    public void testParseSamlFragment() throws Exception {
        List<Node> nodes = SAML2Utils.parseSAMLFragment("<saml:Extension>foo</saml:Extension><samlp:Artifact/>");
        assertThat(nodes).hasSize(2);
        assertThat(nodes).allMatch(Element.class::isInstance);
        assertThat(nodes.get(0).getLocalName()).isEqualTo("Extension");
        assertThat(nodes.get(0).getNamespaceURI()).isEqualTo("urn:oasis:names:tc:SAML:2.0:assertion");
        assertThat(nodes.get(0).getTextContent()).isEqualTo("foo");
        assertThat(nodes.get(1).getLocalName()).isEqualTo("Artifact");
        assertThat(nodes.get(1).getNamespaceURI()).isEqualTo("urn:oasis:names:tc:SAML:2.0:protocol");
    }
}
