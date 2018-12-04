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
 * Copyright 2018 ForgeRock AS.
 */

if (!window.PublicKeyCredential) {
    document.getElementById('webAuthnOutcome').value = "unsupported";
    document.getElementById("loginButton_0").click();
}

var publicKey = {
    challenge: new Uint8Array({challenge}).buffer,
    // Relying Party:
    rp: {
        name: "{relyingPartyName}",
        id: "{relyingPartyId}"
    },
    // User:
    user: {
        id: Uint8Array.from("{userId}", c=>c.charCodeAt(0)),
        name: "{userName}",
        displayName: "{userName}"
    },
    pubKeyCredParams: [
        {pubKeyCredParams}
    ],
    attestation: "{attestationPreference}",
    timeout: {timeout},
    excludeCredentials: [{excludeCredentials}],
    authenticatorSelection: {authenticatorSelection}
};

navigator.credentials.create({publicKey})
    .then(function (newCredentialInfo) {
        var rawId = newCredentialInfo.id;
        var clientData = String.fromCharCode.apply(null, new Uint8Array(newCredentialInfo.response.clientDataJSON));
        var keyData = new Int8Array(newCredentialInfo.response.attestationObject).toString();
        document.getElementById('webAuthnOutcome').value = clientData + "::" + keyData + "::" + rawId;
        document.getElementById("loginButton_0").click();
    }).catch(function (err) {
        document.getElementById('webAuthnOutcome').value = "ERROR" + "::" + err;
        document.getElementById("loginButton_0").click();
    });