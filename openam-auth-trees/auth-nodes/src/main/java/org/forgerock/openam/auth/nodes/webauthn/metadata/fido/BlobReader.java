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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido;

import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.MetadataBlobPayload;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

//@Checkstyle:off LineLength
/**
 * Reads the FIDO Alliance Metadata Service Binary Large Object (BLOB) and returns its contents in JSON format.
 * <p>
 * This implementation follows the
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#metadata-blob-object-processing-rules">
 * FIDO Alliance Metadata Service</a> specification.
 */
//@Checkstyle:on LineLength
public class BlobReader {
    private final Gson gson = new Gson();

    /**
     * Given the Base64 encoded {@link Jwt}, parse the {@link MetadataBlobPayload} from
     * payload of the JWT.
     * <p>
     * The payload of the {@link Jwt} is accessed as the {@link Jwt#getClaimsSet()}
     * which is defined as containing a JSON Object as per
     * <a href="https://tools.ietf.org/html/rfc7519#section-4">RFC 7519 section 4. Jwt Claims</a>.
     *
     * @param mdsJwt non-null, non-empty payload.
     * @return a non-null {@link MetadataBlobPayload} object hierarchy.
     *
     * @throws InvalidPayloadException If there was an unexpected error in parsing
     * the payload.
     */
    public MetadataBlobPayload readBlob(Jwt mdsJwt) throws InvalidPayloadException {
        try {
            return gson.fromJson(mdsJwt.getClaimsSet().toString(), MetadataBlobPayload.class);
        } catch (JsonSyntaxException e) {
            throw new InvalidPayloadException("Failed to parse JSON", e);
        }
    }
}
