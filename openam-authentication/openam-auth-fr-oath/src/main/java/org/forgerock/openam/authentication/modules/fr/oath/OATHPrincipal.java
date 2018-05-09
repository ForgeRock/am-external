/*
 * Copyright 2012-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.fr.oath;

import com.sun.identity.authentication.modules.hotp.HOTPPrincipal;

import java.security.Principal;

public class OATHPrincipal implements Principal, java.io.Serializable {

    private String name;

    /**
     * Public constructor that takes user name
     */
    public OATHPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }

        this.name = name;
    }

    /**
     * Returns the HOTP username for this <code>HOTPPrincipal</code>.
     * <p/>
     * <p/>
     *
     * @return the HOTP username for this <code>HOTPPrincipal</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>HOTPPrincipal</code>.
     * <p/>
     * <p/>
     *
     * @return a string representation of this <code>HOTPPrincipal</code>.
     */
    public String toString() {
        return ("OATHPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>HOTPPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>HOTPPrincipal</code> and the two HOTPPrincipals
     * have the same username.
     * <p/>
     * <p/>
     *
     * @param o Object to be compared for equality with this
     *          <code>HOTPPrincipal</code>.
     * @return true if the specified Object is equal equal to this
     *         <code>HOTPPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof HOTPPrincipal)) {
            return false;
        }
        HOTPPrincipal that = (HOTPPrincipal) o;

        if (this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Returns a hash code for this <code>HOTPPrincipal</code>.
     * <p/>
     * <p/>
     *
     * @return a hash code for this <code>HOTPPrincipal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }
}
