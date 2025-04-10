/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAML2ServiceProviderAdapter.java,v 1.5 2008/08/19 19:11:15 veiming Exp $
 *
 * Portions Copyrighted 2013-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins;

import org.forgerock.openam.annotations.SupportedAll;

import org.forgerock.openam.saml2.plugins.SPAdapter;

/**
 * The <code>SAML2ServiceProviderAdapter</code> abstract class provides methods
 * that could be extended to perform user specific logics during SAMLv2 
 * protocol processing on the Service Provider side. The implementation class
 * could be configured on a per-service provider basis in the extended
 * metadata configuration.   
 * <p>
 * A singleton instance of this <code>SAML2ServiceProviderAdapter</code>
 * class will be used per Service Provider during runtime, so make sure 
 * implementation of the methods are thread safe.
 *
 * @deprecated since AM 7.3.0 Implement use-case specific {@link SPAdapter} implementations instead.
 *
 */
@Deprecated(forRemoval = true, since = "7.3.0")
@SupportedAll
public abstract class SAML2ServiceProviderAdapter implements SPAdapter {

}
