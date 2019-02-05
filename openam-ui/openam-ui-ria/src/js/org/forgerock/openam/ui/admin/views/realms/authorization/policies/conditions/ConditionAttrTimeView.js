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
 * Copyright 2015-2018 ForgeRock AS.
 */

import "clockPicker";

import _ from "lodash";
import $ from "jquery";

import ConditionAttrBaseView from "./ConditionAttrBaseView";
import ConditionAttrTimeTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrTime";

export default ConditionAttrBaseView.extend({
    template: ConditionAttrTimeTemplate,

    clickClockPicker (e) {
        e.stopPropagation();
        $(e.currentTarget).prev("input").clockpicker("show");
    },

    render (data, element, callback) {
        this.initBasic(data, element, "pull-left attr-group");

        this.events["click [data-clock]"] = _.bind(this.clickClockPicker, this);

        this.parentRender(function () {
            this.initClockPickers();

            if (callback) {
                callback();
            }
        });
    },

    initClockPickers () {
        this.$el.find(".clockpicker").each(function () {
            var clock = $(this);
            clock.clockpicker({
                placement: "top",
                autoclose: true,
                afterDone () {
                    clock.trigger("change");
                }
            });
        });
    }
});