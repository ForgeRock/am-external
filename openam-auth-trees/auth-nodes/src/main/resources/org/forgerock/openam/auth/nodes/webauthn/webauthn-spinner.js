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

/*
 * Note:
 *
 * When a ConfirmationCallback is used (e.g. during recovery code use), the XUI does not render a loginButton. However
 * the webAuthn script needs to call loginButton.click() to execute the appropriate data reformatting prior to sending
 * the request into AM. Here we query whether the loginButton exists and add it to the DOM if it doesn't.
 */

var newLocation = document.getElementById("wrapper");

var script = "<div class=\"form-group\">\n" +
    "<div class=\"panel panel-default\">\n" +
    "    <div class=\"panel-body text-center\">\n" +
    "    <h4 class=\"awaiting-response\"><i class=\"fa fa-circle-o-notch fa-spin text-primary\"></i> %1$s </h4>\n" +
    "    </div>\n" +
    "</div>\n";

if (!document.getElementById("loginButton_0")) {
    script += "<input id=\"loginButton_0\" role=\"button\" type=\"submit\" hidden>";
} else {
    document.getElementById("loginButton_0").style.visibility='hidden';
}

script += "</div>";

newLocation.getElementsByTagName("fieldset")[0].innerHTML += script;
document.body.appendChild(newLocation);
