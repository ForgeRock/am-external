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
 * $Id: IDPList.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.protocol;

import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.saml2.protocol.impl.IDPListImpl;

/**
 * This interface specifies the identity providers trusted by the requester
 * to authenticate the presenter.
 *
 */
@SupportedAll

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = IDPListImpl.class)
public interface IDPList extends XmlSerializable {

    /**
     * Returns the list of <code>IDPEntry</code> Objects.
     *
     * @return the list <code>IDPEntry</code> objects.
     * @see #setIDPEntries(List)
     */
    public List<IDPEntry> getIDPEntries();

    /**
     * Sets the <code>IDPEntry</code> Object.
     *
     * @param idpEntryList  the  list of <code>IDPEntry</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPEntries
     */
    public void setIDPEntries(List<IDPEntry> idpEntryList) throws SAML2Exception;

    /**
     * Returns the <code>GetComplete</code> Object.
     *
     * @return the <code>GetComplete</code> object.
     * @see #setGetComplete(GetComplete)
     */
    public GetComplete getGetComplete();

    /**
     * Sets the <code>GetComplete</code> Object.
     *
     * @param getComplete the new <code>GetComplete</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getGetComplete
     */

    public void setGetComplete(GetComplete getComplete) throws SAML2Exception;

    /**
     * Makes this object immutable.
     */
    public void makeImmutable() ;

    /**
     * Returns true if object is mutable.
     *
     * @return true if the object is mutable.
     */
    public boolean isMutable();
}
