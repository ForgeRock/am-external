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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import org.forgerock.openam.auth.nodes.webauthn.FidoCertificationLevel;
import org.forgerock.openam.core.realms.Realm;

/**
 * An interface for metadata service factories.
 */
public interface MetadataServiceFactory {

    /**
     * Get the instance of the metadata service.
     *
     * @param realm                  the realm to read the metadata service for
     * @param fidoCertificationLevel the level of FIDO certification to filter by
     * @return the associated metadata service
     * @throws MetadataException on error
     */
    MetadataService getInstance(Realm realm, FidoCertificationLevel fidoCertificationLevel) throws MetadataException;
}
