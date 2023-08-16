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

import _ from "lodash";
import $ from "jquery";

import ConditionAttrBaseView from "./ConditionAttrBaseView";
import ConditionAttrDayTemplate from "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrDay";

export default ConditionAttrBaseView.extend({
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
        const weekdays = [];
        const self = this;
        let i = 0;
        _.invokeMap(self.days, function () {
            weekdays[i] = {};
            weekdays[i].title = $.t(self.i18n.weekdays.key + this + self.i18n.weekdays.full);
            weekdays[i].value = $.t(self.i18n.weekdays.key + this + self.i18n.weekdays.short);
            i++;
        });
        return weekdays;
    }
});