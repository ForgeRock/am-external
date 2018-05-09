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
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrDay"
], ($, _, ConditionAttrBaseView, ConditionAttrDayTemplate) => {
    return ConditionAttrBaseView.extend({
        template: ConditionAttrDayTemplate,
        i18n: {
            "weekdays": { "key": "console.authorization.common.weekdays.", "full": ".full", "short": ".short" }
        },
        days: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],

        render (data, element, callback) {
            this.initBasic(data, element, "pull-left attr-group");

            this.data.weekdays = this.getWeekDays();
            this.parentRender(() => {
                if (callback) {
                    callback();
                }
            });
        },

        getWeekDays () {
            var weekdays = [], i = 0, self = this;
            _.invoke(self.days, function () {
                weekdays[i] = {};
                weekdays[i].title = $.t(self.i18n.weekdays.key + this + self.i18n.weekdays.full);
                weekdays[i].value = $.t(self.i18n.weekdays.key + this + self.i18n.weekdays.short);
                i++;
            });
            return weekdays;
        }
    });
});
