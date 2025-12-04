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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.forgerock.openam.annotations.SupportedAll;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * Common super-interface for all SAML elements that can be serialized into XML. Implementations must implement
 * either the {@link #toDocumentFragment(Document, boolean, boolean)} method (preferred) or the
 * {@link #toXMLString(boolean, boolean)} method (only for compatibility).
 */
@SupportedAll
public interface XmlSerializable {
    /**
     * Serializes the element into an XML {@link DocumentFragment}. A default implementation is provided for
     * compatibility with legacy code that implements {@link #toXMLString()}, but it is highly recommended to override
     * this method.
     *
     * @param document the parent {@link Document} to create the document fragment from.
     * @param includeNSPrefix whether to include a namespace prefix in the document elements.
     * @param declareNS whether to declare any namespaces or assume that they are already declared.
     * @return the XML document fragment representing this SAML2 element.
     * @throws SAML2Exception if the element cannot be serialized for any reason.
     */
    default DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        String xml = toXMLString();
        DocumentFragment documentFragment = document.createDocumentFragment();
        if (xml != null) {
            List<Node> nodes = SAML2Utils.parseSAMLFragment(xml);
            for (Node node : nodes) {
                documentFragment.appendChild(node);
            }
        }
        return documentFragment;
    }

    /**
     * Returns an XML String representation of this element.
     * @param includeNSPrefix Determines whether or not the namespace qualifier is prepended to the Element when
     *                        converted.
     * @param declareNS Determines whether or not the namespace is declared within the Element.
     * @return A String representation.
     * @exception SAML2Exception if something went wrong during conversion
     * @deprecated Use {@link #toDocumentFragment(Document, boolean, boolean)} instead.
     */
    @Deprecated
    default String toXMLString(boolean includeNSPrefix, boolean declareNS) throws SAML2Exception {
        try {
            Document document = XMLUtils.newDocument();
            DocumentFragment fragment = toDocumentFragment(document, includeNSPrefix, declareNS);
            return XMLUtils.print(fragment);
        } catch (ParserConfigurationException e) {
            throw new SAML2Exception(e);
        }
    }

    /**
     * Returns an XML String representation of this element.
     *
     * @return A String representation.
     * @exception SAML2Exception if something went wrong during conversion.
     * @deprecated Use {@link #toDocumentFragment(Document, boolean, boolean)} instead.
     */
    @Deprecated
    default String toXMLString() throws SAML2Exception {
        // The old implementation defaulted to true, false here. The new implementation opts to always generate
        // namespace declarations because the XML processor will eliminate any redundant ones anyway.
        return toXMLString(true, true);
    }
}
