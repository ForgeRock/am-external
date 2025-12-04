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
 * Copyright 2011-2025 Ping Identity Corporation.
 */

import $ from "jquery";
import Spinner from "spin.js";

const SpinnerManager = {};

SpinnerManager.showSpinner = function () {
    if (SpinnerManager.spinner) {
        SpinnerManager.hideSpinner();
    }

    SpinnerManager.spinner = new Spinner().spin(document.getElementById("wrapper"));
    $(".spinner").position({
        of: $(window),
        my: "center center",
        at: "center center"
    });

    $("#wrapper").attr("aria-busy", true);
};

SpinnerManager.hideSpinner = function () {
    if (SpinnerManager.spinner) {
        SpinnerManager.spinner.stop();
    }

    $("#wrapper").attr("aria-busy", false);
};

export default SpinnerManager;
