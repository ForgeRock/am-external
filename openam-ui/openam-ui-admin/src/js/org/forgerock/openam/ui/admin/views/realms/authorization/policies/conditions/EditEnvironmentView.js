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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import $ from "jquery";
import _ from "lodash";
import i18next from "i18next";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ArrayAttr from "./ConditionAttrArrayView";
import BooleanAttr from "./ConditionAttrBooleanView";
import DateAttr from "./ConditionAttrDateView";
import DayAttr from "./ConditionAttrDayView";
import EditEnvironmentTemplate from
    "templates/admin/views/realms/authorization/policies/conditions/EditEnvironmentTemplate";
import ListItemTemplate from "templates/admin/views/realms/authorization/policies/conditions/ListItem";
import ObjectAttr from "./ConditionAttrObjectView";
import PoliciesService from "org/forgerock/openam/ui/admin/services/realm/PoliciesService";
import StringAttr from "./ConditionAttrStringView";
import TimeAttr from "./ConditionAttrTimeView";
import EnumAttr from "./ConditionAttrEnumView";

export default AbstractView.extend({
    template: EditEnvironmentTemplate,
    events: {
        "change [data-type-selection]": "changeType"
    },
    data: {},
    i18n: {
        "condition": {
            "key": "console.authorization.policies.edit.conditionTypes.",
            "title": ".title",
            "props": ".props."
        }
    },
    SCRIPT_RESOURCE: "Script",

    render (schema, element, itemID, itemData, callback) {
        const self = this;
        const hiddenData = {};

        this.setElement(element);

        this.data = $.extend(true, [], schema);
        this.data.itemID = itemID;

        _.each(this.data.conditions, (condition) => {
            condition.i18nKey = $.t(self.i18n.condition.key + condition.title + self.i18n.condition.title);
        });

        this.data.conditions = _.sortBy(this.data.conditions, "i18nKey");

        const tpl = this.template(this.data);
        self.$el.append(tpl);
        self.setElement(`#environment_${itemID}`);

        if (itemData) {
            // Temporary fix, the name attribute is being added by the server after the policy is created.
            // TODO: Serverside solution required
            delete itemData.name;

            // Script name is displayed on UI, but script id is saved along with the condition
            if (itemData.type === self.SCRIPT_RESOURCE) {
                hiddenData[itemData.type] = itemData.scriptId;
                self.$el.data("hiddenData", hiddenData);
            }

            self.$el.data("itemData", itemData);
            self.$el.find("select.type-selection:first").val(itemData.type).trigger("change");
            self.createListItem(schema, self.$el);
        }

        self.$el.find("select.type-selection:first").focus();

        self.$el.find(".info-button").hide();

        if (callback) {
            callback();
        }
    },

    createListItem (allEnvironments, item) {
        const self = this;
        let itemToDisplay = null;
        const itemData = item.data().itemData;
        const hiddenData = item.data().hiddenData;
        const mergedData = _.merge({}, itemData, hiddenData);
        let type;

        item.focus(); //  Required to trigger changeInput.
        this.data.conditions = allEnvironments;

        if (mergedData && mergedData.type) {
            type = mergedData.type;
            itemToDisplay = {};
            if (type === self.SCRIPT_RESOURCE) {
                itemToDisplay["console.common.type"] = $.t(self.i18n.condition.key + type +
                    self.i18n.condition.title);
                PoliciesService.getScriptById(mergedData.scriptId).then((script) => {
                    itemToDisplay[`${self.i18n.condition.key}${type}${self.i18n.condition.props}scriptId`] =
                        script.name;
                    self.setListItemHtml(item, itemToDisplay);
                });
            } else {
                _.each(mergedData, (val, key) => {
                    if (key === "type") {
                        itemToDisplay["console.common.type"] =
                            $.t(`${self.i18n.condition.key}${type}${self.i18n.condition.title}`);
                    } else if (key === "authenticationStrategy") {
                        itemToDisplay[`${self.i18n.condition.key}${type}${self.i18n.condition.props}${key}`] =
                            $.t(`${self.i18n.condition.key}${type}${self.i18n.condition.props}${val}`);
                    } else {
                        itemToDisplay[`${self.i18n.condition.key}${type}${self.i18n.condition.props}${key}`] = val;
                    }
                });
                this.setListItemHtml(item, itemToDisplay);
            }
        } else {
            this.setListItemHtml(item, itemToDisplay);
        }
    },

    setListItemHtml (item, itemToDisplay) {
        const self = this;

        const tpl = ListItemTemplate({ data: itemToDisplay });
        item.find(".item-data").html(tpl);
        self.setElement(`#${item.attr("id")}`);
    },

    changeType (e) {
        e.stopPropagation();
        const self = this;
        let itemData = {};
        let hiddenData = {};
        const selectedType = e.target.value;
        const schema = _.find(this.data.conditions, { title: selectedType }) || {};
        const delay = self.$el.find(".field-float-pattern").length > 0 ? 500 : 0;
        let helperText;

        if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
            itemData = this.$el.data().itemData;
            hiddenData = this.$el.data().hiddenData;
        } else {
            itemData = self.setDefaultJsonValues(schema);
            self.$el.data("itemData", itemData);
            self.$el.data("hiddenData", hiddenData);
        }

        if (itemData) {
            self.animateOut();
            helperText = this.getHelperText(schema);

            // setTimeout needed to delay transitions.
            setTimeout(() => {
                self.$el.find(".no-float").remove();
                self.$el.find(".clear-left").remove();

                if (_.isEmpty(helperText)) {
                    self.$el.find(".info-button").hide();
                } else {
                    self.$el.find(".info-button")
                        .show()
                        .attr("data-title", helperText.title.toString())
                        .attr("data-content", helperText.content.toString())
                        .popover();
                }

                if (!self.$el.parents("#dropbox").length || self.$el.hasClass("editing")) {
                    self.buildHTML(itemData, hiddenData, schema).then(() => {
                        self.animateIn();
                    });
                }
            }, delay);
        }
    },

    getHelperText (schema) {
        const helperText = {};
        switch (schema.title) {
            case "IPv4": // fall through
            case "IPv6":
                helperText.title = i18next.t("console.authorization.policies.edit.conditionTypes.ipHelperTitle");
                helperText.content =
                    i18next.t("console.authorization.policies.edit.conditionTypes.ipHelperContent");
                break;
            case "SimpleTime":
                helperText.title =
                    i18next.t("console.authorization.policies.edit.conditionTypes.SimpleTime.helperTitle");
                helperText.content =
                    i18next.t("console.authorization.policies.edit.conditionTypes.SimpleTime.helperContent");
                break;
            case "IdmUser":
                helperText.title =
                    i18next.t("console.authorization.policies.edit.conditionTypes.IdmUser.helperTitle");
                helperText.content =
                    i18next.t("console.authorization.policies.edit.conditionTypes.IdmUser.helperContent");
                break;
            default:
                break;
        }
        return helperText;
    },

    buildHTML (itemData, hiddenData, schema) {
        const self = this;
        const itemDataEl = this.$el.find(".item-data");
        const schemaProps = schema.config.properties;
        let i18nKey;
        let attributesWrapper;
        const htmlBuiltPromise = $.Deferred();

        function buildScriptAttr () {
            new ArrayAttr().render({
                itemData, hiddenData, data: [hiddenData[itemData.type]],
                title: "scriptId", dataSource: "scripts", multiple: false,
                i18nKey: `${self.i18n.condition.key}${schema.title}${self.i18n.condition.props}scriptId`
            }, itemDataEl, htmlBuiltPromise.resolve);
        }

        if (itemData.type === "SimpleTime") {
            attributesWrapper = '<div class="clearfix clear-left" id="conditionAttrTimeDate"></div>';
            new TimeAttr().render({ itemData }, itemDataEl);
            new DayAttr().render({ itemData }, itemDataEl);
            new DateAttr().render({ itemData }, itemDataEl);

            if (!itemData.enforcementTimeZone) {
                itemData.enforcementTimeZone = "GMT";
            }
            new ArrayAttr().render({
                itemData,
                data: [itemData.enforcementTimeZone],
                title: "enforcementTimeZone",
                i18nKey: `${self.i18n.condition.key}${schema.title}${self.i18n.condition.props}enforcementTimeZone`,
                dataSource: "enforcementTimeZone",
                multiple: false
            }, itemDataEl);
            htmlBuiltPromise.resolve();
        } else if (schema.title === self.SCRIPT_RESOURCE) {
            attributesWrapper = '<div class="no-float"></div>';
            if (itemData && itemData.scriptId) {
                PoliciesService.getScriptById(itemData.scriptId).then((script) => {
                    hiddenData[itemData.type] = script.name;
                    buildScriptAttr();
                });
            } else {
                buildScriptAttr();
            }
        } else {
            attributesWrapper = '<div class="no-float"></div>';

            _.map(schemaProps, (value, key) => {
                i18nKey = self.i18n.condition.key + schema.title + self.i18n.condition.props + key;

                switch (value.type) {
                    case "string":
                        if (value.enum) {
                            // Handle the special case where the string has an enum value
                            new EnumAttr().render({
                                envConditionType: schema.title,
                                itemData,
                                data: itemData[key],
                                title: key,
                                i18nKey,
                                schema,
                                value
                            }, itemDataEl);
                            break;
                        }
                        // fall through
                    case "number": // fall through
                    case "integer":
                        new StringAttr().render({
                            itemData,
                            data: itemData[key],
                            title: key,
                            i18nKey,
                            schema,
                            value
                        }, itemDataEl);
                        break;
                    case "boolean":
                        new BooleanAttr().render({
                            itemData,
                            data: value,
                            title: key,
                            i18nKey,
                            selected: itemData[key]
                        }, itemDataEl);
                        break;
                    case "array":
                        new ArrayAttr().render({
                            itemData,
                            data: itemData[key],
                            title: key,
                            i18nKey
                        }, itemDataEl);
                        break;
                    case "object":
                        new ObjectAttr().render({
                            itemData,
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
            self.$el.find(".condition-attr").wrapAll(attributesWrapper);
        });

        return htmlBuiltPromise;
    },

    setDefaultJsonValues (schema) {
        const itemData = { type: schema.title };
        _.map(schema.config.properties, (value, key) => {
            switch (value.type) {
                case "string":
                    if (key === "authenticateToRealm") {
                        itemData[key] = "/";
                    } else if (key !== "startIp" && key !== "endIp") {
                        // OPENAM-5182: we should not submit empty string if IP is missing
                        itemData[key] = "";
                    }
                    break;
                case "number": // fall through
                case "integer":
                    itemData[key] = 0;
                    break;
                case "boolean":
                    itemData[key] = false;
                    break;
                case "array":
                    itemData[key] = [];
                    break;
                case "object":
                    itemData[key] = {};
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
        this.$el.find(".field-float-pattern, .field-float-selectize, .timezone-field")
            .find("label").removeClass("showLabel")
            .next("input").addClass("placeholderText");

        this.$el.find(".field-float-select select:not(.type-selection)").addClass("placeholderText")
            .prev("label").removeClass("showLabel");

        this.$el.removeClass("invalid-rule");
    },

    animateIn () {
        const self = this;
        setTimeout(() => {
            self.$el.find(".field-float-pattern, .field-float-selectize, .timezone-field")
                .find("label").addClass("showLabel")
                .next("input, div input").removeClass("placeholderText").prop("readonly", false);

            self.$el.find(".field-float-select select:not(.type-selection)").removeClass("placeholderText")
                .prop("readonly", false).prev("label").addClass("showLabel");
        }, 10);
    }
});
