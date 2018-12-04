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

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ArrayAttr from "./ConditionAttrArrayView";
import EditSubjectTemplate from "templates/admin/views/realms/authorization/policies/conditions/EditSubjectTemplate";
import ListItemTemplate from "templates/admin/views/realms/authorization/policies/conditions/ListItem";
import StringAttr from "./ConditionAttrStringView";

export default AbstractView.extend({
    template: EditSubjectTemplate,
    events: {
        "change [data-type-selection]": "changeType"
    },
    data: {},
    subjectI18n: {
        "key": "console.authorization.policies.edit.subjectTypes.",
        "title": ".title",
        "props": ".props."
    },
    IDENTITY_RESOURCE: "Identity",

    render (schema, element, itemID, itemData, callback) {
        var self = this;
        this.setElement(element);

        this.data = $.extend(true, [], schema);
        this.data.itemID = itemID;

        _.each(this.data.subjects, (subj) => {
            subj.i18nKey = $.t(self.subjectI18n.key + subj.title + self.subjectI18n.title);
        });

        this.data.subjects = _.sortBy(this.data.subjects, "i18nKey");

        const tpl = this.template(this.data);
        self.$el.append(tpl);

        self.setElement(`#subject_${itemID}`);

        if (itemData) {
            if (itemData.type === self.IDENTITY_RESOURCE) { // client side fix for 'Identity'
                self.$el.data("hiddenData", self.getUIDsFromUniversalValues(itemData.subjectValues));
            }

            self.$el.data("itemData", itemData);
            self.$el.find("select.type-selection:first").val(itemData.type).trigger("change");
        }

        self.$el.find("select.type-selection:first").focus();

        if (callback) {
            callback();
        }
    },

    createListItem (allSubjects, item) {
        var self = this,
            itemToDisplay = null,
            itemData = item.data().itemData,
            hiddenData = item.data().hiddenData,
            type,
            list,
            mergedData;

        mergedData = _.merge({}, itemData, hiddenData);

        item.focus(); //  Required to trigger changeInput.
        this.data.subjects = allSubjects;

        if (mergedData && mergedData.type) {
            type = mergedData.type;
            itemToDisplay = {};

            _.each(mergedData, (val, key) => {
                if (key === "type") {
                    itemToDisplay["console.common.type"] = $.t(self.subjectI18n.key + type +
                        self.subjectI18n.title);
                } else if (type === self.IDENTITY_RESOURCE) {
                    // Do not display the Identities subject values, but display the merged hidden data instead.
                    if (key !== "subjectValues") {
                        list = "";
                        _.forOwn(val, (prop) => {
                            list += `${prop} `;
                        });

                        itemToDisplay[self.subjectI18n.key + type + self.subjectI18n.props + key] = list;
                    }
                } else {
                    itemToDisplay[self.subjectI18n.key + type + self.subjectI18n.props + key] = val;
                }
            });
        }

        const tpl = ListItemTemplate({ data: itemToDisplay });
        item.find(".item-data").html(tpl);
        self.setElement(`#${item.attr("id")}`);
    },

    changeType (e) {
        e.stopPropagation();
        var self = this,
            itemData = {},
            hiddenData,
            selectedType = e.target.value,
            schema = _.find(this.data.subjects, { title: selectedType }) || {},
            delay = self.$el.find(".field-float-pattern").length > 0 ? 500 : 0;

        if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
            itemData = this.$el.data().itemData;
            hiddenData = this.$el.data().hiddenData;
        } else {
            itemData = self.setDefaultJsonValues(schema);
            self.$el.data("itemData", itemData);
            hiddenData = itemData.type === self.IDENTITY_RESOURCE ? { "users": {}, "groups": {} } : {};
            self.$el.data("hiddenData", hiddenData);
        }

        if (itemData) {
            self.animateOut();

            // setTimeout needed to delay transitions.
            setTimeout(() => {
                self.$el.find(".no-float").remove();
                self.$el.find(".clear-left").remove();

                if (!self.$el.parents("#dropbox").length || self.$el.hasClass("editing")) {
                    self.buildHTML(itemData, hiddenData, schema).then(() => {
                        self.animateIn();
                    });
                }
            }, delay);
        }
    },

    buildHTML (itemData, hiddenData, schema) {
        var self = this,
            itemDataEl = this.$el.find(".item-data"),
            schemaProps = schema.config.properties,
            i18nKey,
            htmlBuiltPromise = $.Deferred();

        if (schema.title === self.IDENTITY_RESOURCE) {
            _.each(["users", "groups"], (identityType) => {
                new ArrayAttr().render({
                    itemData,
                    hiddenData,
                    data: hiddenData[identityType],
                    title: identityType,
                    i18nKey: self.subjectI18n.key + schema.title + self.subjectI18n.props + identityType,
                    dataSource: identityType
                }, itemDataEl, htmlBuiltPromise.resolve);
            });
        } else {
            _.map(schemaProps, (value, key) => {
                i18nKey = self.subjectI18n.key + schema.title + self.subjectI18n.props + key;

                switch (value.type) {
                    case "string":
                        new StringAttr().render({
                            itemData,
                            hiddenData,
                            data: itemData[key],
                            title: key,
                            i18nKey
                        }, itemDataEl);
                        break;
                    case "array":
                        new ArrayAttr().render({
                            itemData,
                            hiddenData,
                            data: itemData[key],
                            title: key,
                            i18nKey
                        }, itemDataEl);
                        break;
                    default:
                        break;
                }
            });
            htmlBuiltPromise.resolve();
        }

        htmlBuiltPromise.then(() => {
            self.$el.find(".condition-attr").wrapAll("<div class='no-float'></div>");
        });

        return htmlBuiltPromise;
    },

    setDefaultJsonValues (schema) {
        var itemData = { type: schema.title };
        _.map(schema.config.properties, (value, key) => {
            switch (value.type) {
                case "string":
                    itemData[key] = "";
                    break;
                case "array":
                    itemData[key] = [];
                    break;
                default:
                    console.error("Unexpected data type:", key, value);
                    break;
            }
        });

        return itemData;
    },

    animateOut () {
        // hide all items except the title selector
        this.$el.find(".no-float").fadeOut(500);
        this.$el.find(".clear-left").fadeOut(500);
        this.$el.find(".field-float-pattern, .field-float-selectize")
            .find("label").removeClass("showLabel")
            .next("input, div input").addClass("placeholderText").prop("readonly", true);

        this.$el.removeClass("invalid-rule");
    },

    animateIn () {
        var self = this;
        setTimeout(() => {
            self.$el.find(".field-float-pattern, .field-float-selectize")
                .find("label").addClass("showLabel")
                .next("input, div input").removeClass("placeholderText").prop("readonly", false);
        }, 10);
    },

    getUIDsFromUniversalValues (values) {
        var returnObj = { users: {}, groups: {} },
            endIndex = -1,
            startIndex = String("id=").length;

        _.each(values, (universalid) => {
            endIndex = universalid.indexOf(",ou=");
            if (universalid.indexOf(",ou=user") > -1) {
                returnObj.users[universalid] = universalid.substring(startIndex, endIndex);
            } else if (universalid.indexOf(",ou=group") > -1) {
                returnObj.groups[universalid] = universalid.substring(startIndex, endIndex);
            }
        });

        return returnObj;
    }
});
