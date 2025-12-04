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

var newLocation = document.getElementById("wrapper");
var oldHtml = newLocation.getElementsByTagName("fieldset")[0].innerHTML;
newLocation.getElementsByTagName("fieldset")[0].innerHTML = "<div class=\"panel panel-default\">\n" +
    "    <div class=\"panel-body text-center\">\n" +
    "        <h3>%1$s</h3>\n" +
    "        <h4>%4$s</h4>\n" +
    "    </div>\n" +
    "<div class=\"text-center\">\n" +
    "%5$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%6$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%7$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%8$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%9$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%10$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%11$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%12$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%13$s\n" +
    "</div>\n" +
    "<div class=\"text-center\">\n" +
    "%14$s\n" +
    "</div>\n" +
    "<div class=\"panel-body text-center\">\n" +
    "        <p>%2$s <em>%3$s</em></p>\n" +
    "</div>\n" +
    "</div>" + oldHtml;
document.body.appendChild(newLocation);


