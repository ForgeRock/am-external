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

if (!window.PublicKeyCredential) {
    document.getElementById('webAuthnOutcome').value = "unsupported";
    document.getElementById("loginButton_0").click();
}

var options = {
    {relyingPartyId}
    challenge: new Int8Array({challenge}).buffer,
    timeout: {timeout},
    userVerification: "{userVerification}",
    extensions: {extensions},
    {allowCredentials}
};

navigator.credentials.get({ "publicKey" : options })
    .then(function (assertion) {
        var clientData = String.fromCharCode.apply(null, new Uint8Array(assertion.response.clientDataJSON));
        var authenticatorData = new Int8Array(assertion.response.authenticatorData).toString();
        var signature = new Int8Array(assertion.response.signature).toString();
        var rawId = assertion.id;
        var userHandle = String.fromCharCode.apply(null, new Uint8Array(assertion.response.userHandle));
        var outcome = {
            legacyData: clientData + "::" + authenticatorData + "::" + signature + "::" + rawId + "::" + userHandle,
            authenticatorAttachment: assertion.authenticatorAttachment
        }
        document.getElementById('webAuthnOutcome').value = JSON.stringify(outcome);
        document.getElementById("loginButton_0").click();
    }).catch(function (err) {
        var outcome = {
            error: String(err)
        }
        document.getElementById('webAuthnOutcome').value = JSON.stringify(outcome);
        var allowRecoveryCode = 'true' === "{allowRecoveryCode}";
        if (allowRecoveryCode) {
            var loginButton = document.getElementById("loginButton_0");
            if (loginButton) {
                var prev = loginButton.previousElementSibling;
                if (prev && prev.nodeName == "DIV") {
                    prev.getElementsByTagName("div")[0].innerHTML = "<i class=\"fa fa-times-circle text-primary\"> "
                        + err + "</i>";
                }
            }
        } else {
            document.getElementById("loginButton_0").click();
        }
    });
