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
 * Copyright 2025 ForgeRock AS.
 */
/*
 *  Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 *
 */
package org.forgerock.openam.service.datastore;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Singleton;

import org.forgerock.util.Action;

/**
 * Provides a single data store consistency controller that makes use of {@link ReentrantReadWriteLock} to control
 * actions that may wish to interact with system components which need to be in a known and consistent state.
 *
 * @since 6.5.0
 */
@Singleton
final class ReentrantVolatileActionConsistencyController implements VolatileActionConsistencyController {

    private final ReadWriteLock lock;

    ReentrantVolatileActionConsistencyController() {
        lock = new ReentrantReadWriteLock();
    }

    @Override
    public <E extends Exception> void safeExecute(Action<E> action) throws E {
        try {
            lock.readLock().lock();
            action.run();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <E extends Exception> void safeExecuteVolatileAction(Action<E> volatileAction) throws E {
        try {
            lock.writeLock().lock();
            volatileAction.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
