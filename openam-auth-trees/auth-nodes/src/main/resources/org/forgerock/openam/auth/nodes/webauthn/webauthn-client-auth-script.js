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
 * Copyright 2018-2020 ForgeRock AS.
 */

if (!window.PublicKeyCredential) {
    document.getElementById('webAuthnOutcome').value = "unsupported";
    document.getElementById("loginButton_0").click();
}

var options = {
    {relyingPartyId}
    challenge: new Int8Array({challenge}).buffer,
    timeout: {timeout},
    userVerification: "{userVerification}",
    {allowCredentials}
};

navigator.credentials.get({ "publicKey" : options })
    .then(function (assertion) {
        var clientData = String.fromCharCode.apply(null, new Uint8Array(assertion.response.clientDataJSON));
        var authenticatorData = new Int8Array(assertion.response.authenticatorData).toString();
        var signature = new Int8Array(assertion.response.signature).toString();
        var rawId = assertion.id;
        var userHandle = String.fromCharCode.apply(null, new Uint8Array(assertion.response.userHandle));
        document.getElementById('webAuthnOutcome').value = clientData + "::" + authenticatorData + "::" + signature + "::" + rawId + "::" + userHandle;
        document.getElementById("loginButton_0").click();
    }).catch(function (err) {
        document.getElementById('webAuthnOutcome').value = "ERROR" + "::" + err;
        document.getElementById("loginButton_0").click();
    });
