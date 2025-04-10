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

package com.sun.identity.saml2.profile;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.forgerock.guice.core.GuiceTestCase;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.jaxb.assertion.AttributeType;
import com.sun.identity.shared.xml.XMLUtils;

public class AttributeQueryUtilTest extends GuiceTestCase {

    @Test
    void shouldEscapeSpecialCharactersInAttributeValues() throws Exception {
        // Given
        Document doc = XMLUtils.newDocument();
        Element valueElement = doc.createElement("saml:AttributeValue");
        valueElement.setTextContent("<oops>wat</oops>");
        AttributeType inputAttribute = new AttributeType();
        inputAttribute.setName("test");
        inputAttribute.getAttributeValue().add(valueElement);

        // When
        List<Attribute> result = AttributeQueryUtil.convertAttributes(singletonList(inputAttribute));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).toXMLString()).doesNotContain("<oops>wat</oops>");
    }

}
