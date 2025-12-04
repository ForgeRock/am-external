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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.statement;

import java.util.List;

/**
 * The {@link MetadataStatement} class models the contents of the Base64 payload.
 * <p>
 * For more details, see the
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-statement-v3.0-ps-20210518.html">
 * FIDO Metadata Statements</a> specification.
 * <p>
 * <b>Note:</b> there are many fields in the specification, however for the purposes
 * of this initial implementation, we only need access to one of these fields.
 */
//@Checkstyle:off JavadocType
public record MetadataStatement(String aaid, String aaguid,
                                List<String> attestationRootCertificates, String description, String protocolFamily,
                                List<String> authenticationAlgorithms, List<String> publicKeyAlgAndEncodings,
                                List<String> attestationTypes) {
}
