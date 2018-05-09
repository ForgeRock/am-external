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
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrEnum"
], ($, _, ConditionAttrBaseView, ConditionAttrEnumTemplate) => {
    return ConditionAttrBaseView.extend({
        template: ConditionAttrEnumTemplate,

        render (data, element, callback) {
            this.initBasic(data, element, "field-float-select data-obj");

            this.parentRender(() => {
                if (callback) {
                    callback();
                }
            });
        }
    });
});
