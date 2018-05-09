/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import java.security.Principal;
import java.util.Objects;

/**
 * A social auth specific Principal representation.
 */
public class SocialAuthPrincipal implements Principal {

    private final String name;

    /**
     * Public constructor that takes user name.
     *
     * @param name name of the principal to represent
     */
    public SocialAuthPrincipal(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SocialAuthPrincipal that = (SocialAuthPrincipal) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
