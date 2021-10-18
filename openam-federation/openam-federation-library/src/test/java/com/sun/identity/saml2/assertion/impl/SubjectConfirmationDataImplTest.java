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

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.sun.identity.shared.xml.XMLUtils;

public class SubjectConfirmationDataImplTest {

    @Test
    public void shouldEscapeSpecialCharactersInXmlInResponseTo() throws Exception {
        // Given
        String inResponseTo = "foo\" oops=\"bar";
        SubjectConfirmationDataImpl subjectConfirmationData = new SubjectConfirmationDataImpl();
        subjectConfirmationData.setInResponseTo(inResponseTo);

        // When
        String xml = subjectConfirmationData.toXMLString(true, true);
        Document doc = XMLUtils.toDOMDocument(xml, null);

        // Then
        assertThat(doc.getDocumentElement().hasAttribute("oops")).isFalse();
        assertThat(doc.getDocumentElement().getAttribute("InResponseTo")).isEqualTo(inResponseTo);
    }
}