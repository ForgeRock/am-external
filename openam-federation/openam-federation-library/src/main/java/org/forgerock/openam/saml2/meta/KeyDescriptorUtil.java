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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.meta;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.sun.identity.saml2.jaxb.xmlsig.KeyInfoType;
import com.sun.identity.saml2.jaxb.xmlsig.ObjectFactory;
import com.sun.identity.saml2.jaxb.xmlsig.X509DataType;

/**
 * Utility class that provides util methods helpful for the population of key descriptor objects.
 *
 * @since 7.0.0
 */
public final class KeyDescriptorUtil {

    private static final ObjectFactory SIG_OBJECT_FACTORY = new ObjectFactory();

    private KeyDescriptorUtil() {
        throw new AssertionError("Util class should not be instantiated");
    }

    /**
     * Creates KeyInfoType object the given certificates content.
     *
     * @param certificate The X509Certificate.
     * @return The KeyInfoType with the certificate content.
     */
    public static KeyInfoType createKeyInfoFromCertificate(X509Certificate certificate) {
        X509DataType x509Data = SIG_OBJECT_FACTORY.createX509DataType();
        x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName()
                .add(SIG_OBJECT_FACTORY.createX509DataTypeX509Certificate(encodeCertificate(certificate)));
        KeyInfoType keyInfo = SIG_OBJECT_FACTORY.createKeyInfoType();
        keyInfo.getContent().add(SIG_OBJECT_FACTORY.createX509Data(x509Data));
        return keyInfo;
    }

    private static byte[] encodeCertificate(X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Unable to encode certificate", e);
        }
    }

}
