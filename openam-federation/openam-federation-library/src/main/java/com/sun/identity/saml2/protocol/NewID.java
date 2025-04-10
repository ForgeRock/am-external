/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: NewID.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.protocol;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.protocol.impl.NewIDImpl;

/** 
 * This interface identifies the new identifier in an 
 * <code>ManageNameIDRequest</code> message.
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = NewIDImpl.class)
public interface NewID extends XmlSerializable {
    /** 
     * Returns the value of the <code>NewID</code> URI.
     *
     * @return value of the <code>NewID</code> URI.
     */
    public String getValue();

    /**
     * Returns an <code>NewEncryptedID</code> object.
     *
     * @param encryptionConfig The encryption config.
     * @param recipientEntityID Unique identifier of the recipient, it is used as the index to the cached secret key so
     * that the key can be reused for the same recipient; It can be null in which case the secret key will be generated
     * every time and will not be cached and reused. Note that the generation of a secret key is a relatively expensive
     * operation.
     * @return <code>NewEncryptedID</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    NewEncryptedID encrypt(EncryptionConfig encryptionConfig, String recipientEntityID) throws SAML2Exception;
}
