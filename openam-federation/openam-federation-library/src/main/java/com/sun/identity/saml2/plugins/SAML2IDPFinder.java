/**
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
 * $Id: SAML2IDPFinder.java,v 1.3 2008/12/03 00:34:10 hengming Exp $
 *
 * Portions Copyrighted 2019-2022 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins;

import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.openam.saml2.plugins.IDPFinder;

/**
 * This interface <code>SAML2IDPFinder</code> is used to find a list of 
 * preferred Identity Authenticating providers to service the authentication
 * request.
 *
 * @deprecated since AM 7.3.0 Implement use-case specific {@link IDPFinder} implementations instead.
 */ 
@SupportedAll
@Deprecated(forRemoval = true, since = "7.3.0")
public interface SAML2IDPFinder extends IDPFinder {
}
