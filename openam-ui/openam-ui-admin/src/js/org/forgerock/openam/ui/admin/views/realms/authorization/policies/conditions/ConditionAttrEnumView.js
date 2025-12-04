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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

import "selectize";

import _ from "lodash";
import $ from "jquery";

import ConditionAttrEnumTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrEnum";
import ConditionAttrBaseView from
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView";

const CONDITION_TYPE_I18N_PATH_SEGMENT = "console.authorization.policies.edit.conditionTypes.";

export default ConditionAttrBaseView.extend({
    template: ConditionAttrEnumTemplate,

    render (data, element, callback) {
        this.initBasic(data, element, "field-float-selectize data-obj");

        console.log("data: ", data);

        this.parentRender(function () {
            const view = this;
            let title = "";
            let itemData;
            let options;
            let $item;
            let enumValues;
            let type;
            let conditionType;

            this.$el.find("select.selectize").each(function () {
                $item = $(this);
                type = $item.parent().find("label").data().title;
                conditionType = view.data.envConditionType;
                enumValues = view.data.value.enum;

                options = {};

                _.extend(options, {
                    placeholder: $.t("common.form.select"),
                    valueField: "value",
                    labelField: "label",
                    options: _.map(enumValues, (key) => {
                        return {
                            value: key,
                            label: $.t(`${CONDITION_TYPE_I18N_PATH_SEGMENT}${conditionType}.props.${key}`)
                        };
                    }),
                    items: [view.data.itemData[type]],
                    create: false,
                    onChange (value) {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                        itemData[title] = value ? value : "";
                    }
                });

                _.extend(options, {
                    plugins: ["restore_on_backspace"]
                });
                $item.selectize(options);
            });

            if (callback) {
                callback();
            }
        });
    }
});
