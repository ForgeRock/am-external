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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.cose;

/**
 * https://tools.ietf.org/html/rfc8152#section-13.1
 * A mapping between int value representations of cryptographic curves and the curve parameters.
 */
public enum CoseCurve {

    /**
     * the p256 curve.
     */
    P256("P-256", 1, "prime256v1"),
    /**
     * the p384 curve.
     */
    P384("P-384", 2, "prime384v1"),
    /**
     * the p521 curve.
     */
    P521("P-521", 3, "prime521v1"),
    /**
     * the ed25519 curve.
     */
    ED25519("Ed25519", 6, "ed25519");

    private String name;
    private int coseNumber;
    private String specName;

    /**
     * The constructor.
     *
     * @param name the algorithm name.
     * @param coseNumber the value representing the algorithm.
     */
    CoseCurve(String name, int coseNumber, String specName) {
        this.name = name;
        this.coseNumber = coseNumber;
        this.specName = specName;
    }

    /**
     * Get the algorithm from the magic int value.
     *
     * @param coseNumber the int value of the algorithm defined by COSE.
     * @return the enum representing the algorithm.
     */
    public static CoseCurve fromCoseNumber(int coseNumber) {
        for (CoseCurve coseCurve : CoseCurve.values()) {
            if (coseCurve.coseNumber == coseNumber) {
                return coseCurve;
            }
        }
        return null;
    }

    /**
     * Get the name of the algorithm.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the numerical value representing the algorithm.
     * @return the value.
     */
    public int getCoseNumber() {
        return coseNumber;
    }

    /**
     * Get the exact algorithm name used by security providers.
     * @return the algorithm name.
     */
    public String getSpecName() {
        return specName;
    }

    @Override
    public String toString() {
        return specName;
    }
}
