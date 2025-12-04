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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

if (!window.PublicKeyCredential) {
    document.getElementById('webAuthnOutcome').value = "unsupported";
    document.getElementById("loginButton_0").click();
}

var publicKey = {
    challenge: new Int8Array({challenge}).buffer,
    // Relying Party:
    rp: {
        {relyingPartyId}
        name: "{relyingPartyName}"
    },
    // User:
    user: {
        id: Uint8Array.from("{userId}", function (c) { return c.charCodeAt(0) }),
        name: "{userName}",
        displayName: "{displayName}"
    },
    pubKeyCredParams: {pubKeyCredParams},
    attestation: "{attestationPreference}",
    timeout: {timeout},
    excludeCredentials: [{excludeCredentials}],
    authenticatorSelection: {authenticatorSelection},
    extensions: {extensions}
};

navigator.credentials.create({publicKey: publicKey})
    .then(function (newCredentialInfo) {
        var rawId = newCredentialInfo.id;
        var clientData = String.fromCharCode.apply(null, new Uint8Array(newCredentialInfo.response.clientDataJSON));
        var keyData = new Int8Array(newCredentialInfo.response.attestationObject).toString();
        var outcome = {
            legacyData: clientData + "::" + keyData + "::" + rawId,
            authenticatorAttachment: newCredentialInfo.authenticatorAttachment
        }
        document.getElementById('webAuthnOutcome').value = JSON.stringify(outcome);
        document.getElementById("loginButton_0").click();
    }).catch(function (err) {
        var outcome = {
            error: String(err)
        }
        document.getElementById('webAuthnOutcome').value = JSON.stringify(outcome);
        document.getElementById("loginButton_0").click();
    });
