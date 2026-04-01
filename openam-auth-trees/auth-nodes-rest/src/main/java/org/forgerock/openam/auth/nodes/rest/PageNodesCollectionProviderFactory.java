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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.rest;

import org.forgerock.api.enums.Stability;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.ServiceSchema;

/**
 * Interface for Guice to inject {@link PageNodesCollectionProvider} instances.
 */
public interface PageNodesCollectionProviderFactory {
    /**
     * Creates a new instance of {@link PageNodesCollectionProvider}.
     * @param instanceSchema the schema of the instance to be created
     * @param rootSchema the root schema of the service
     * @param stability the stability of the service schema
     * @return a new instance of {@link PageNodesCollectionProvider}
     */
    PageNodesCollectionProvider create(@Assisted("schema") ServiceSchema instanceSchema,
            @Assisted("rootSchema") ServiceSchema rootSchema, Stability stability);
}
