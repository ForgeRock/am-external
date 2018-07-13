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
 * $Id: FSRemoteException.java,v 1.2 2008/06/25 05:46:40 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.common;

/**
 * This class is an extension point for IDFF related exceptions.
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSRemoteException extends Exception {
    /**
     * Creates an <code>FSRemoteException</code> with no message.
     */
    public FSRemoteException () {
	super();
    }

    /**
     * Creates an <code>FSRemoteException</code> with a message.
     *
     * @param s exception message.
     */
    public FSRemoteException (String s) {
	super(s);
    }
}

