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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView",
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrString"
], ($, _, Constants, ConditionAttrBaseView, ConditionAttrStringTemplate) => {
    return ConditionAttrBaseView.extend({
        template: ConditionAttrStringTemplate,

        render (data, element, callback) {
            var cssClass = "";

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
                var itemData = this.data.itemData;
                if (itemData.authenticateToRealm.indexOf("/") !== 0) {
                    itemData.authenticateToRealm = `/${itemData.authenticateToRealm}`;
                }
            }
        }
    });
});
