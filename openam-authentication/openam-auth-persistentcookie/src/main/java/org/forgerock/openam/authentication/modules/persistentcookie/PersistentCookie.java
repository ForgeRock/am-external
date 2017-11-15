/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import org.forgerock.openam.authentication.modules.common.AbstractLoginModuleBinder;

/**
 * Sub type of the AbstractLoginModuleBinder which binds the AMLoginModule with the PersistentCookieAuthModule
 * authentication logic.
 *
 * @author Phill Cunnington phill.cunnington@forgerock.com
 */
public class PersistentCookie extends AbstractLoginModuleBinder {

    /**
     * Constructs an instance of the PersistentCookie.
     */
    public PersistentCookie() {
        super(new PersistentCookieAuthModule());
    }
}
