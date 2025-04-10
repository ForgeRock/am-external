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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatus;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatusType;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.StatusReport;

/**
 * A class containing information about an authenticator and its authentication statuses as provided by the
 * Metadata Service.
 */
public class AuthenticatorDetails {
    private final AuthenticatorStatus maxCertificationStatus;
    private final Set<AuthenticatorStatus> securityStatuses;
    private final Set<AuthenticatorStatus> infoStatuses;

    /**
     * Constructor.
     *
     * @param reports a list of {@link StatusReport} objects that contain the authentication statuses of the
     *                authenticator
     */
    public AuthenticatorDetails(final List<StatusReport> reports) {
        Map<AuthenticatorStatusType, List<AuthenticatorStatus>> collect = reports.stream()
                .map(StatusReport::status)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(AuthenticatorStatus::getType));

        this.maxCertificationStatus =
                collect.getOrDefault(AuthenticatorStatusType.CERTIFICATION, Collections.emptyList())
                        .stream()
                        .max(AuthenticatorStatus::compareTo)
                        .orElse(AuthenticatorStatus.NOT_FIDO_CERTIFIED);

        this.securityStatuses =
                new HashSet<>(collect.getOrDefault(AuthenticatorStatusType.SECURITY, Collections.emptyList()));
        this.infoStatuses = new HashSet<>(collect.getOrDefault(AuthenticatorStatusType.INFO, Collections.emptyList()));
    }

    /**
     * Constructor.
     *
     * @param maxCertificationStatus the maximum {@link AuthenticatorStatus} attributed to the authenticator
     * @param securityStatuses a set of authentication statuses that are a result of security notifications against the
     *                         authenticator
     * @param infoStatuses a set of authentication statuses that are considered extra information about the
     *                     authenticator
     */
    public AuthenticatorDetails(final AuthenticatorStatus maxCertificationStatus,
            final Set<AuthenticatorStatus> securityStatuses, final Set<AuthenticatorStatus> infoStatuses) {
        this.maxCertificationStatus = maxCertificationStatus;
        this.securityStatuses = securityStatuses;
        this.infoStatuses = infoStatuses;
    }

    /**
     * Return the maximum {@link AuthenticatorStatus} attributed to the authenticator.
     *
     * @return the maximum {@link AuthenticatorStatus} attributed to the authenticator
     */
    public AuthenticatorStatus getMaxCertificationStatus() {
        return maxCertificationStatus;
    }

    /**
     * Return a set of authentication statuses that are a result of security notifications against the authenticator.
     *
     * @return the authentication statuses that are a result of security notifications against the authenticator
     */
    public Set<AuthenticatorStatus> getSecurityStatuses() {
        return securityStatuses;
    }

    /**
     * Return  a set of authentication statuses that are considered extra information about the authenticator.
     *
     * @return the authentication statuses that are considered extra information about the authenticator
     */
    public Set<AuthenticatorStatus> getInfoStatuses() {
        return infoStatuses;
    }
}
