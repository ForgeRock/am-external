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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";

import ConditionAttrBaseView from "./ConditionAttrBaseView";
import ConditionAttrBooleanTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrBoolean";

export default ConditionAttrBaseView.extend({
    template: ConditionAttrBooleanTemplate,

    render (data, element, callback) {
        this.initBasic(data, element, "field-float-pattern data-obj button-field");

        this.events["click [data-btn]"] = _.bind(this.buttonControlClick, this);
        this.events["keyup [data-btn]"] = _.bind(this.buttonControlClick, this);

        this.parentRender(() => {
            if (callback) {
                callback();
            }
        });
    },

    buttonControlClick (e) {
        if (e.type === "keyup" && e.keyCode !== 13) {
            return;
        }

        const btn = $(e.currentTarget);
        const btnGroup = btn.parent(".btn-group");

        if (btnGroup.hasClass("active")) {
            return;
        }

        const parentBtnGroup = btnGroup.parent(".btn-group");
        const label = parentBtnGroup.prev("label").data().title;

        const secondBtnGroup = parentBtnGroup.find(".btn-group.active");

        this.data.itemData[label] = btn.data("val");

        secondBtnGroup.removeClass("active");
        btnGroup.addClass("active");
    }
});
