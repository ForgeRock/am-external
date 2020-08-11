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

import "bootstrap-datetimepicker";

import ConditionAttrBaseView from "./ConditionAttrBaseView";
import ConditionAttrDateTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrDate";

export default ConditionAttrBaseView.extend({
    template: ConditionAttrDateTemplate,

    render (data, element, callback) {
        this.initBasic(data, element, "pull-left attr-group");

        this.parentRender(function () {
            this.initDatePickers();

            if (callback) {
                callback();
            }
        });
    },

    initDatePickers () {
        const options = {
            format: "YYYY:MM:DD",
            useCurrent: false,
            icons: {
                previous: "fa fa-chevron-left",
                next: "fa fa-chevron-right"
            }
        };
        const startDate = this.$el.find("#startDate");
        const endDate = this.$el.find("#endDate");

        startDate.datetimepicker(options);
        endDate.datetimepicker(options);

        startDate.on("dp.change", (e) => {
            endDate.data("DateTimePicker").minDate(e.date);
        });

        endDate.on("dp.change", (e) => {
            startDate.data("DateTimePicker").maxDate(e.date);
        });
    }
});
