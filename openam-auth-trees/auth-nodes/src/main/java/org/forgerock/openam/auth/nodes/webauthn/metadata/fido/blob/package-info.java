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

/**
 * The BLOB package contains the classes required to model the MDS BLOB to the level of
 * granularity required for parsing and working with the trust anchor certificates
 * that are defined within it.
 * <p>
 * All information contained in these classes has been derived from the official FIDO
 * Metadata Service specification.
 * </p>
 * <p>
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html">
 * FIDO Alliance Implementation Draft 18 May 2021</a>
 * </p>
 * <p>
 * <b>Note:</b> The classes in this package represent a subset of the entire dictionary
 * which enough for this utility to function as intended.
 * </p>
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob;
