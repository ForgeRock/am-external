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

//@Checkstyle:off
/**
 * This class represents the registered set of TPM device manufacturers. Last updated in line with
 * <a href="https://trustedcomputinggroup.org/wp-content/uploads/TCG-TPM-Vendor-ID-Registry-Version-1.06-Revision-0.94_pub.pdf">
 * TCG TPM Vendor ID Registry Version 1.06 Revision 0.94</a>.
 * <p>
 * Matched via the specification according to
 * <a href="https://www.trustedcomputinggroup.org/wp-content/uploads/Credential_Profile_EK_V2.0_R14_published.pdf">
 * TCG EK Credential Profile Version 2.0 Revision 14</a>.
 */
//@Checkstyle:on
// @Checkstyle:ignore JavadocVariable
// @SuppressWarnings("checkstyle:LineLength")
public enum TpmManufacturerId implements TpmManufacturer {

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

    private final String id;
    private final String manufacturerName;

    /**
     * Constructor for the TPM manufacturer.
     *
     * @param id the manufacturer ID
     * @param manufacturerName the manufacturer name
     */
    TpmManufacturerId(String id, String manufacturerName) {
        this.id = id;
        this.manufacturerName = manufacturerName;
    }

    /**
     * Retrieve the ID of this manufacturer.
     *
     * @return the manufacturer ID
     */
    public String getId() {
        return id;
    }


    /**
     * Retrieve the name of this manufacturer.
     *
     * @return the manufacturer name
     */
    public String getManufacturerName() {
        return manufacturerName;
    }

}
