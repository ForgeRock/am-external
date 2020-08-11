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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.service.datastore;

import org.forgerock.guice.core.InjectorHolder;

import com.sun.identity.setup.SetupListener;

/**
 * Setup listener that register ldap data store service listener after the setup us complete.
 *
 * This is required to be done as a setup listener so that we know everything has been
 * initialized prior to attempting to register listeners.
 *
 * The cycle that causes this problem is as such:
 *
 * on startup SMSEntry uses a static block to init itself, in doing so, it calls initSMSObject()
 * which tries to construct a SMSObject. The SMSObject in turn attempts to load the DataStoreService
 * which then attempts to register itself as a listener. However, to do this it wants an SMSNotificationManager,
 * whose constructor wants a reference to SMSEntry...
 *
 * and so we get an error that adding the listener on first startup is null.
 * As such a SetupListener is a way to ensure we don't risk entering this loop.
 */
public class LdapDataStoreListenerRegister implements SetupListener {

    @Override
    public void setupComplete() {
        InjectorHolder.getInstance(DataStoreServiceRegister.class).register();
    }
}
