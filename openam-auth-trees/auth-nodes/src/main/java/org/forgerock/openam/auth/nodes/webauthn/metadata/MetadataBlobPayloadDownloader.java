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

import java.io.IOException;
import java.security.cert.CertPath;

import javax.inject.Inject;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobReader;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobVerifier;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.InvalidPayloadException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayload;
import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;

/**
 * {@link MetadataBlobPayloadDownloader} will attempt to resolve the location of the {@link Jwt} and
 * present the contents of it to the user, based on the
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html">FIDO
 * Metadata Service V3 Specification</a>.
 */
class MetadataBlobPayloadDownloader {
    private final ResourceResolver resolver;
    private final JwtReconstruction jwtReconstruction;
    private final BlobReader reader;
    private final BlobVerifier blobVerifier;

    /**
     * Constructor.
     * @param blobVerifier         the blob verifier
     * @param resolver             the resource resolver
     * @param blobReader           the blob reader
     * @param jwtReconstruction    the jwt reconstruction
     */
    @Inject
    MetadataBlobPayloadDownloader(BlobVerifier blobVerifier, ResourceResolver resolver, BlobReader blobReader,
            JwtReconstruction jwtReconstruction) {
        this.blobVerifier = blobVerifier;
        this.resolver = resolver;
        this.reader = blobReader;
        this.jwtReconstruction = jwtReconstruction;
    }

    /**
     * After resolving the locations of the provided parameters, attempt
     * to resolve the {@link SignedJwt} and perform certificate verification against the
     * resolved certificates that make up the {@link CertPath}, returning a list of metadata entries.
     *
     * @param mdsBlobEndpoint      the details as to where to find the blob
     * @param trustAnchorValidator used to validate the blob certificates
     * @return the downloaded {@link MetadataBlobPayloadDownloader}
     * @throws MetadataException if there is any error in processing, then the cause of
     *                           the error will be established to as specific an error as possible to assist
     *                           the user with debugging the issue
     */
    public MetadataBlobPayload downloadMetadataPayload(String mdsBlobEndpoint,
            TrustAnchorValidator trustAnchorValidator) throws MetadataException {
        // With the validated parameters provided, resolve the location of the JWT
        SignedJwt jwt;
        try {
            String jwtString = new String(resolveJwt(mdsBlobEndpoint));
            jwt = jwtReconstruction.reconstructJwt(jwtString, SignedJwt.class);
        } catch (JwtReconstructionException e) {
            throw new MetadataException("Failed to reconstruct the JWT", e);
        }

        if (!blobVerifier.verify(jwt, mdsBlobEndpoint, trustAnchorValidator)) {
            throw new MetadataException("Failed to verify the validity of the MDS BLOB");
        }

        final MetadataBlobPayload payload;
        try {
            payload = reader.readBlob(jwt);
        } catch (InvalidPayloadException e) {
            throw new MetadataException("Failed to list the contents of the MDS BLOB", e);
        }

        return payload;
    }

    /**
     * Resolve the MDS JWT Endpoint parameter.
     * <p>
     * This parameter can be either a file path, or a URL. This function needs to determine
     * which is the case and then correctly read the contents of this resource.
     *
     * @param mdsBlobEndpoint non-null
     * @return the non-null, possibly empty byte[] data of the JWT
     * @throws MetadataException if there was any unexpected error whilst attempting to resolve the JWT from the
     *                           provided resource
     */
    private byte[] resolveJwt(String mdsBlobEndpoint) throws MetadataException {
        try {
            return resolver.resolveAsBytes(mdsBlobEndpoint);
        } catch (IOException e) {
            throw new MetadataException(e);
        }
    }
}
