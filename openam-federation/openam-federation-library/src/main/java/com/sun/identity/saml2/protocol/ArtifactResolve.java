/**
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
 * $Id: ArtifactResolve.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */



package com.sun.identity.saml2.protocol;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.ArtifactResolveImpl;

/**
 * The <code>ArtifactResolve</code> message is used to request that a SAML
 * protocol message be returned in an <code>ArtifactResponse</code> message
 * by specifying an artifact that represents the SAML protocol message.
 * It has the complex type <code>ArtifactResolveType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ArtifactResolveType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Artifact"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = ArtifactResolveImpl.class)
public interface ArtifactResolve extends RequestAbstract {

    /**
     * Gets the <code>Artifact</code> of the request.
     *
     * @return <code>Artifact</code> of the request.
     * @see #setArtifact(Artifact)
     */
    public Artifact getArtifact();

    /**
     * Sets the <code>Artifact</code> of the request.
     * 
     * @param value new <code>Artifact</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getArtifact()
     */
    public void setArtifact(Artifact value)
	throws SAML2Exception;

}
