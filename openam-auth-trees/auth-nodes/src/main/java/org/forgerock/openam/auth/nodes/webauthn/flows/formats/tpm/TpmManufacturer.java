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
 * Copyright 2020-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

/**
 * This class represents the registered set of TPM device manufacturers. Last updated in line with
 * https://trustedcomputinggroup.org/wp-content/uploads/TCG-TPM-Vendor-ID-Registry-Version-1.06-Revision-0.94_pub.pdf
 *
 * Matched via the specification according to
 * https://www.trustedcomputinggroup.org/wp-content/uploads/Credential_Profile_EK_V2.0_R14_published.pdf
 */
// @Checkstyle:ignore JavadocVariable
public enum TpmManufacturer {

    AMD("id:414D4400", "AMD"),
    ANT("id:414E5400", "Ant Group"),
    ATML("id:41544D4C", "Atmel"),
    BRCM("id:4252434D", "Broadcom"),
    CSCO("id:4353434F", "Cisco"),
    FLYS("id:464C5953", "Flyslice Technologies"),
    ROCC("id:524F4343", "Fuzhou Rockchip"),
    GOOG("id:474F4F47", "Google"),
    HPI("id:48504900", "HPI"),
    HPE("id:48504500", "HPE"),
    HISI("id:48495349", "Huawei"),
    IBM("id:49424D00", "IBM"),
    IFX("id:49465800", "Infineon"),
    INTC("id:494E5443", "Intel"),
    LEN("id:4C454E00", "Lenovo"),
    MSFT("id:4D534654", "Microsoft"),
    NSM("id:4E534D20", "National Semiconductor"),
    NTZ("id:4E545A00", "Nationz"),
    NTC("id:4E544300", "Nuvoton Technology"),
    QCOM("id:51434F4D", "Qualcomm"),
    SMSN("id:534D534E", "Samsung"),
    SNS("id:534E5300", "Sinosun"),
    SMSC("id:534D5343", "SMSC"),
    STM("id:53544D20", "ST Microelectronics"),
    TXN("id:54584E00", "Texas Instruments"),
    WEC("id:57454300", "Winbond");

    private final String value;
    private final String name;

    TpmManufacturer(String value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Retrieve the name of this manufacturer.
     *
     * @return The manufacturer.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve the alg information from the int lookup.
     *
     * @param lookup The int of the alg information to look up.
     * @return The enum associated with that alg.
     */
    public static TpmManufacturer getTpmManufacturer(String lookup) {
        for (TpmManufacturer t : values()) {
            if (t.value.equals(lookup)) {
                return t;
            }
        }
        return null;
    }

}
