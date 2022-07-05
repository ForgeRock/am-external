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

package com.sun.identity.xacml.context.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SubjectImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Subject " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "SubjectCategory=\"urn:test\">" +
                        "<xacml-context:Attribute AttributeId=\"urn:testattr\" Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Subject>" },
                { true, false, "<xacml-context:Subject " +
                        "SubjectCategory=\"urn:test\">" +
                        "<xacml-context:Attribute AttributeId=\"urn:testattr\" Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Subject>" },
                { false, false, "<Subject SubjectCategory=\"urn:test\">" +
                        "<Attribute AttributeId=\"urn:testattr\" Issuer=\"testIssuer\">" +
                        "<AttributeValue>a</AttributeValue></Attribute></Subject>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectImpl subject = new SubjectImpl();
        subject.setSubjectCategory(URI.create("urn:test"));
        AttributeImpl attr = new AttributeImpl();
        attr.setIssuer("testIssuer");
        attr.setAttributeId(URI.create("urn:testattr"));
        attr.setAttributeStringValues(List.of("a"));
        subject.setAttributes(List.of(attr));

        // When
        String xml = subject.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}