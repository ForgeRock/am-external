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
 * Copyright 2019-2020 ForgeRock AS.
 */

import React from "react";
import ReactDOM from "react-dom";

import ChangePasswordContainer
    from "org/forgerock/openam/ui/admin/views/amadmin/changePassword/ChangePasswordContainer";
import Constants from "org/forgerock/openam/ui/admin/utils/Constants";

export default [{
    startEvent: Constants.EVENT_AMADMIN_SECURITY_DIALOG,
    processDescription () {
        const element = document.getElementById("dialog");
        const handleExited = () => {
            ReactDOM.unmountComponentAtNode(element);
        };
        ReactDOM.render(
            React.createElement(ChangePasswordContainer, { onExited: handleExited }),
            element
        );
    }
}];
