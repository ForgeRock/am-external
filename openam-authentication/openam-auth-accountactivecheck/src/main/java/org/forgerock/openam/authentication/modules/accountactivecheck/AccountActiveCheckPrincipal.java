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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.accountactivecheck;

import java.security.Principal;

/**
 * Representation of a principal used by the AccountActiveCheck authentication module.
 */
public class AccountActiveCheckPrincipal implements Principal, java.io.Serializable {
    private String name;
    
    public AccountActiveCheckPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Returns the user name for this <code>AccountActiveCheckPrincipal</code>.
     *
     * @return the user name for this <code>AccountActiveCheckPrincipal</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>AccountActiveCheckPrincipal</code>.
     *
     * @return a string representation of this <code>AccountActiveCheckPrincipal</code>.
     */
    public String toString() {
        return("DataStorePrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>AccountActiveCheckPrincipal</code>
     * for equality.  Returns <code>true</code> if the given object is also a
     * <code>AccountActiveCheckPrincipal</code> and the two AccountActiveCheckPrincipal
     * have the same user name.
     *
     * @param o Object to be compared for equality with this
     *        <code>AccountActiveCheckPrincipal</code>.
     * @return <code>true</code> if the specified Object is equal equal to this
     *         <code>AccountActiveCheckPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }
 
        if (!(o instanceof AccountActiveCheckPrincipal)) {
            return false;
        }

        AccountActiveCheckPrincipal that = (AccountActiveCheckPrincipal)o;
        return this.getName().equals(that.getName());
    }
 
    /**
     * Returns a hash code for this <code>AccountActiveCheckPrincipal</code>.
     *
     * @return a hash code for this <code>AccountActiveCheckPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
