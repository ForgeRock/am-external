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
 * Copyright 2014-2018 ForgeRock AS.
 */

import "selectize";

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import SubjectAttributesTemplate from
    "templates/admin/views/realms/authorization/policies/attributes/SubjectAttributesTemplate";

const SubjectResponseAttributesView = AbstractView.extend({
    element: "#userAttrs",
    template: SubjectAttributesTemplate,
    noBaseTemplate: true,
    attrType: "User",

    render (args, callback) {
        var self = this,
            attr;

        this.data.selectedUserAttributes = args[0];
        this.data.allUserAttributes = [];

        _.each(args[1], (propertyName) => {
            attr = {};
            attr.propertyName = propertyName;
            attr.selected = _.find(self.data.selectedUserAttributes, (obj) => {
                return obj.propertyName === propertyName;
            });
            self.data.allUserAttributes.push(attr);
        });

        this.parentRender(() => {
            self.initSelectize();

            if (callback) {
                callback();
            }
        });
    },

    getAttrs () {
        var data = [],
            attr,
            self = this;

        _.each(this.data.selectedUserAttributes, (value) => {
            attr = {};
            attr.type = self.attrType;
            attr.propertyName = value.propertyName || value;
            data.push(attr);
        });

        data = _.sortBy(data, "propertyName");

        return data;
    },

    initSelectize () {
        var self = this;

        this.$el.find(".selectize").each(function () {
            $(this).selectize({
                plugins: ["restore_on_backspace"],
                delimiter: false,
                persist: false,
                create: false,
                hideSelected: true,
                onChange (value) {
                    self.data.selectedUserAttributes = value;
                }
            });
        });
    }
});

export default new SubjectResponseAttributesView();
