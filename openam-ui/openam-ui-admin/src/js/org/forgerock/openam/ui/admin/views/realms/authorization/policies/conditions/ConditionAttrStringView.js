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
 * Copyright 2015-2019 ForgeRock AS.
 */

import ConditionAttrBaseView from "./ConditionAttrBaseView";
import ConditionAttrStringTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrString";
import Constants from "org/forgerock/openam/ui/admin/utils/Constants";

export default ConditionAttrBaseView.extend({
    template: ConditionAttrStringTemplate,

    render (data, element, callback) {
        let cssClass = "";

        if (data.title === "startIp" || data.title === "endIp") {
            if (data.schema.title === "IPv4") {
                data.pattern = Constants.IPV4_PATTERN;
            } else if (data.schema.title === "IPv6") {
                data.pattern = Constants.IPV6_PATTERN;
            }
            cssClass = "auto-fill-group";
        } else if (data.value && data.value.type === "number") {
            data.pattern = Constants.NUMBER_PATTERN;
        } else if (data.value && data.value.type === "integer") {
            data.pattern = Constants.INTEGER_PATTERN;
        } else {
            data.pattern = null;
        }

        this.initBasic(data, element, `field-float-pattern data-obj ${cssClass}`);

        this.parentRender(() => {
            if (callback) {
                callback();
            }
        });
    },

    attrSpecificChangeInput () {
        if (this.data.title === "authenticateToRealm") {
            const itemData = this.data.itemData;
            if (itemData.authenticateToRealm.indexOf("/") !== 0) {
                itemData.authenticateToRealm = `/${itemData.authenticateToRealm}`;
            }
        }
    }
});
