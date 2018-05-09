/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView",
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrDate",
    "bootstrap-datetimepicker"
], ($, _, ConditionAttrBaseView, ConditionAttrDateTemplate) => {
    return ConditionAttrBaseView.extend({
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
            var options = {
                    format: "YYYY:MM:DD",
                    useCurrent: false,
                    icons: {
                        previous: "fa fa-chevron-left",
                        next: "fa fa-chevron-right"
                    }
                },
                startDate = this.$el.find("#startDate"),
                endDate = this.$el.find("#endDate");

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
});
