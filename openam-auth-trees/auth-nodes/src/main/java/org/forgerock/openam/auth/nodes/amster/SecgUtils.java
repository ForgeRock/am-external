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
 * Copyright (C)2009 - SSHJ Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Portions copyrighted 2016-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.amster;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

/**
 * Utilities for converting between {@link ECPoint} and octet string according to the SEC1 paper published by the
 * Standards for Efficient Cryptography Group. Obtained with thanks under the Apache 2 license from the SSHJ project
 * (versions 0.15.0-0.19.0) maintainers.
 *
 * @see <a href="https://github.com/hierynomus/sshj/blob/bc41908694d9982b0705951f6f05704b30fc5d7c/src/main/java/com
 * /hierynomus/sshj/secg/SecgUtils.java">Original source</a>
 * @see <a href="http://www.secg.org/sec1-v2.pdf">SEC1 paper</a>
 */
public final class SecgUtils {

    private SecgUtils() {
    }

    /**
     * SECG 2.3.4 Octet String to ECPoint.
     *
     * @param m the octet string.
     * @param curve the Elliptic Curve.
     * @return the ECPoint.
     */
    public static ECPoint getDecoded(byte[] m, EllipticCurve curve) throws GeneralSecurityException {
        int elementSize = getElementSize(curve);
        if (m.length != 2 * elementSize + 1 || m[0] != 0x04) {
            throw new GeneralSecurityException("Invalid 'f' for Elliptic Curve " + curve.toString());
        }
        byte[] xBytes = new byte[elementSize];
        byte[] yBytes = new byte[elementSize];
        System.arraycopy(m, 1, xBytes, 0, elementSize);
        System.arraycopy(m, 1 + elementSize, yBytes, 0, elementSize);
        return new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes));
    }

    /**
     * SECG 2.3.3 ECPoint to Octet String.
     *
     * @param point the ECPoint.
     * @param curve the Elliptic Curve.
     * @return the octet string.
     */
    public static byte[] getEncoded(ECPoint point, EllipticCurve curve) {
        int elementSize = getElementSize(curve);
        byte[] m = new byte[2 * elementSize + 1];
        m[0] = 0x04;

        byte[] xBytes = stripLeadingZeroes(point.getAffineX().toByteArray());
        byte[] yBytes = stripLeadingZeroes(point.getAffineY().toByteArray());
        System.arraycopy(xBytes, 0, m, 1 + elementSize - xBytes.length, xBytes.length);
        System.arraycopy(yBytes, 0, m, 1 + 2 * elementSize - yBytes.length, yBytes.length);
        return m;
    }

    private static byte[] stripLeadingZeroes(byte[] bytes) {
        int start = 0;
        while (bytes[start] == 0x0) {
            start++;
        }

        return Arrays.copyOfRange(bytes, start, bytes.length);
    }

    private static int getElementSize(EllipticCurve curve) {
        int fieldSize = curve.getField().getFieldSize();
        return (fieldSize + 7) / 8;
    }

}
