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
 * Copyright 2016-2018 ForgeRock AS.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const UnauthorizedView = AbstractView.extend({
    template: "common/UnauthorizedTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    events: {
        "click #goBack" () {
            window.history.go(-1);
        },
        "click #logout" () {
            Configuration.gotoFragment = `#${URIUtils.getCurrentFragment()}`;
        }
    }
});

export default new UnauthorizedView();
