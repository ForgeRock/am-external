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
 * Copyright 2011-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.adaptive;

import java.io.Serializable;
import java.security.Principal;

public class AdaptivePrincipal implements Principal, Serializable {

    private String name;
    private static final long serialVersionUID = 1000L;

    /**
     * TODO-JAVADOC
     */
    public AdaptivePrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /**
     * Returns the VALid user name for this <code>VALid</code>.
     *
     * @return the VALid user name for this <code>VALid</code>.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>AdaptivePrincipal</code>.
     *
     * @return a string representation of this <code>AdaptivePrincipal</code>.
     */
    @Override
    public String toString() {
        return "AdaptivePrincipal:  " + name;
    }

    /**
     * Compares the specified Object with this <code>AdaptivePrincipal</code>
     * for equality.  Returns <code>true</code> if the given object is also a
     * <code>AdaptivePrincipal</code> and the two <code>AdaptivePrincipal</code>s
     * have the same user name.
     *
     * @param obj Object to be compared for equality with this
     *        <code>AdaptivePrincipal</code>.
     *
     * @return <code>true</code> if the specified Object is equal equal to this
     *         <code>AdaptivePrincipal</code>.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AdaptivePrincipal other = (AdaptivePrincipal) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
