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

package com.sun.identity.saml2.assertion.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.sun.identity.shared.xml.XMLUtils;

import org.w3c.dom.Element;

/**
 *  The <code>SubjectConfirmationDataContentFieldSerializer</code> serialize
 *  the XML Elements as String
 */
class SubjectConfirmationDataContentFieldSerializer extends StdSerializer {

    public SubjectConfirmationDataContentFieldSerializer() {
        this(null);
    }

    public SubjectConfirmationDataContentFieldSerializer(Class<?> t) {
        super(t);
    }

    @Override
    public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (obj == null) return;
        if (obj instanceof Element) {
            String xmlString = XMLUtils.print((Element) obj);
            jsonGenerator.writeString(xmlString);
        } else if (obj instanceof String) {
            jsonGenerator.writeString((String) obj);
        }
    }
}
