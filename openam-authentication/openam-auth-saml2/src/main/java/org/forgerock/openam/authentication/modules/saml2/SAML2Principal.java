/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.saml2;

import java.security.Principal;

/**
 * SAML2Principal object.
 */
final class SAML2Principal implements Principal {

    private String name;

    /**
     * Public constructor that takes user name.
     */
    public SAML2Principal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }

        this.name = name;
    }

    /**
     * Returns the SAML2 username for this <code>SAML2Principal</code>.
     * <p/>
     *
     * @return the SAML2 username for this <code>SAML2Principal</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this <code>SAML2Principal</code>.
     * <p/>
     *
     * @return a string representation of this <code>SAML2Principal</code>.
     */
    public String toString() {
        return ("SAML2Principal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>SAML2Principal</code>
     * for equality.  Returns true if the given object is also a
     * <code>SAML2Principal</code> and the two SAML2Principals
     * have the same username.
     * <p/>
     * <p/>
     *
     * @param o Object to be compared for equality with this
     *          <code>SAML2Principal</code>.
     * @return true if the specified Object is equal equal to this
     *         <code>SAML2Principal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof SAML2Principal)) {
            return false;
        }
        SAML2Principal that = (SAML2Principal) o;

        return this.getName().equals(that.getName());
    }

    /**
     * Returns a hash code for this <code>SAML2Principal</code>.
     * <p/>
     *
     * @return a hash code for this <code>SAML2Principal</code>.
     */
    public int hashCode() {
        return name.hashCode();
    }

}
