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
package org.forgerock.openam.auth.nodes.webauthn.flows;

import java.security.cert.X509Certificate;
import java.util.List;

import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;

/**
 * Represents https://www.w3.org/TR/webauthn/#attestation-statement
 * A container for ALL possible attestation values.
 */
public class AttestationStatement {

    private List<X509Certificate> attestnCerts;
    private byte[] sig;
    private byte[] certInfo;
    private byte[] pubArea;
    private byte[] ecdaaKeyId;
    private byte[] response;
    private CoseAlgorithm alg;
    private String ver;

    /**
     * The attestation certs.
     * @return the list of certs.
     */
    public List<X509Certificate> getAttestnCerts() {
        return attestnCerts;
    }

    /**
     * The attestation certs.
     * @param attestnCerts the certs.
     */
    public void setAttestnCerts(List<X509Certificate> attestnCerts) {
        this.attestnCerts = attestnCerts;
    }

    /**
     * the signature.
     * @return the signature.
     */
    public byte[] getSig() {
        return sig;
    }

    /**
     * the signature.
     * @param sig the signature.
     */
    public void setSig(byte[] sig) {
        this.sig = sig;
    }

    /**
     * the cert info.
     * @return the cert info.
     */
    public byte[] getCertInfo() {
        return certInfo;
    }

    /**
     * the cert info.
     * @param certInfo the cert info.
     */
    public void setCertInfo(byte[] certInfo) {
        this.certInfo = certInfo;
    }

    /**
     * The pub area.
     * @return the pub area.
     */
    public byte[] getPubArea() {
        return pubArea;
    }

    /**
     * The pub area.
     * @param pubArea the pub area.
     */
    public void setPubArea(byte[] pubArea) {
        this.pubArea = pubArea;
    }

    /**
     * the ecdaa key id.
     * @return the ecdaa key id.
     */
    public byte[] getEcdaaKeyId() {
        return ecdaaKeyId;
    }

    /**
     * the ecdaa key id.
     * @param ecdaaKeyId the ecdaa key id.
     */
    public void setEcdaaKeyId(byte[] ecdaaKeyId) {
        this.ecdaaKeyId = ecdaaKeyId;
    }

    /**
     * the response.
     * @return the response.
     */
    public byte[] getResponse() {
        return response;
    }

    /**
     * The response.
     * @param response the response.
     */
    public void setResponse(byte[] response) {
        this.response = response;
    }

    /**
     * the algorithm.
     * @return the algorithm.
     */
    public CoseAlgorithm getAlg() {
        return alg;
    }

    /**
     * The algorithm.
     * @param alg the algorithm.
     */
    public void setAlg(CoseAlgorithm alg) {
        this.alg = alg;
    }

    /**
     * The version.
     * @return the version.
     */
    public String getVer() {
        return ver;
    }

    /**
     * The version.
     * @param ver the version.
     */
    public void setVer(String ver) {
        this.ver = ver;
    }
}
