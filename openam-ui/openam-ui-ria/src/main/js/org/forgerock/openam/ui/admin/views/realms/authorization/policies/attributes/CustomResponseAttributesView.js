/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "templates/admin/views/realms/authorization/policies/attributes/CustomAttributesTemplate"
], (AbstractView, CustomAttributesTemplate) => {
    var CustomResponseAttributesView = AbstractView.extend({
        element: "#customAttrs",
        template: CustomAttributesTemplate,
        noBaseTemplate: true,

        render (customAttributes, callback) {
            this.data.customAttributes = customAttributes;

            this.parentRender(() => {
                if (callback) {
                    callback();
                }
            });
        },

        getAttrs () {
            return this.data.customAttributes;
        }
    });

    return new CustomResponseAttributesView();
});
