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

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmsAttestException;

/**
 * This class represents a method of, and the result of parsing the webAuthn-passed 'certInfo' field into an
 * object containing information about the attestation process.
 */
final class TpmsAttest {

    // _LNG is short for "length", as in the number of bytes this item takes up
    private static final int CLOCK_INFO_LNG = 17;
    private static final int FIRMWARE_VERSION_LNG = 8;

    final int magic;
    final TpmSt type;
    final byte[] qualifiedSigner;
    final byte[] extraData;
    final byte[] clockInfo;
    final byte[] firmwareVersion;
    final TpmAttested attested;

    private TpmsAttest(int magic, TpmSt type, byte[] qualifiedSigner, byte[] extraData, byte[] clockInfo,
                        byte[] firmwareVersion, TpmAttested attested) {
        this.magic = magic;
        this.type = type;
        this.qualifiedSigner = qualifiedSigner;
        this.extraData = extraData;
        this.clockInfo = clockInfo;
        this.firmwareVersion = firmwareVersion;
        this.attested = attested;
    }

    static TpmsAttest toTpmsAttest(byte[] certificateInfo) throws InvalidTpmsAttestException {
        try (DataInputStream certInfo = new DataInputStream(new ByteArrayInputStream(certificateInfo))) {
            int magic = certInfo.readInt();
            TpmSt type = TpmSt.getType(certInfo.readUnsignedShort());

            int qualifiedSignerLength = certInfo.readShort();
            byte[] qualifiedSigner = new byte[qualifiedSignerLength];
            certInfo.read(qualifiedSigner, 0, qualifiedSignerLength);

            int extraDataLength = certInfo.readShort();
            byte[] extraData = new byte[extraDataLength];
            certInfo.read(extraData, 0, extraDataLength);

            byte[] clockInfo = new byte[CLOCK_INFO_LNG];
            certInfo.read(clockInfo, 0, CLOCK_INFO_LNG);

            byte[] firmwareVersion = new byte[FIRMWARE_VERSION_LNG];
            certInfo.read(firmwareVersion, 0, FIRMWARE_VERSION_LNG);

            int attestedNameLength = certInfo.readShort();
            byte[] attestedName = new byte[attestedNameLength];
            certInfo.read(attestedName, 0, attestedNameLength);

            int attestedQualifiedNameLength = certInfo.readShort();
            byte[] attestedQualifiedName = new byte[attestedQualifiedNameLength];
            certInfo.read(attestedQualifiedName, 0, attestedQualifiedNameLength);

            TpmAttested attested = TpmAttested.toTpmAttested(attestedName, attestedQualifiedName);

            if (certInfo.read() != -1) {
                throw new InvalidTpmsAttestException("Bytes remaining in certInfo after parsing tpmsAttest!");
            }

            return new TpmsAttest(magic, type, qualifiedSigner, extraData, clockInfo, firmwareVersion, attested);

        } catch (IOException ioe) {
            throw new InvalidTpmsAttestException("Unable to parse certInfo by spec", ioe);
        }
    }
}
