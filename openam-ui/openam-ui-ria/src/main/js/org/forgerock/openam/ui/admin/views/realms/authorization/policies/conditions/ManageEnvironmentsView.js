/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageRulesView",
    "org/forgerock/commons/ui/common/util/Constants"
], ($, _, ManageRulesView, Constants) => {
    var ManageEnvironmentsView = ManageRulesView.extend({
        element: "#environmentContainer",
        envEvents: {
            "change .environment-area .operator > select": "onSelect",
            "mousedown #operatorEnv_0 li.rule:not(.editing)": "setFocus",
            "mousedown #operatorEnv_0 li.operator:not(.editing)": "setFocus",

            "click    #operatorEnv_0 .rule > .item-button-panel > .fa-times": "onDelete",
            "keyup    #operatorEnv_0 .rule > .item-button-panel > .fa-times": "onDelete",
            "click    #operatorEnv_0 .rule > .item-button-panel > .fa-pencil": "toggleEditing",
            "keyup    #operatorEnv_0 .rule > .item-button-panel > .fa-pencil": "toggleEditing",
            "click    #operatorEnv_0 .rule > .item-button-panel > .fa-check": "toggleEditing",
            "keyup    #operatorEnv_0 .rule > .item-button-panel > .fa-check": "toggleEditing",
            "dblclick #operatorEnv_0 li.rule:not(.legacy-condition)": "toggleEditing"
        },
        data: {},
        buttons: {},
        idCount: 0,
        typeAND: {
            "title": "AND",
            "logical": true,
            "config": {
                "properties": {
                    "conditions": {
                        "type": "array",
                        "items": {
                            "type": "any"
                        }
                    }
                }
            }
        },

        render (args, callback) {
            this.idPrefix = "Env_";
            this.property = "condition";
            this.properties = "conditions";
            this.data.conditionName = $.t("console.authorization.policies.edit.addEnvironmentCondition");
            this.data.entity = args.entity;
            this.data.options = args.options;
            this.data.conditions = [];
            this.data.operators = [];

            var self = this;

            _.each(this.data.options.availableEnvironments, (item) => {
                if (item.logical === true) {
                    self.data.operators.push(item);
                } else {
                    self.data.conditions.push(item);
                }

                delete item.config.type;
            });

            if (!_.find(this.data.operators, { title: "AND" })) {
                this.data.operators.push(this.typeAND);
            }

            this.init(args, this.envEvents);
            this.conditionType = Constants.ENVIRONMENT;
            this.setElement(this.element);

            this.idCount = 0;

            this.parentRender(function () {
                this.buttons.addCondition = this.$el.find("a#addCondition");
                this.buttons.addOperator = this.$el.find("a#addOperator");

                if (self.data.operators.length === 0) {
                    this.buttons.addOperator.hide();
                }

                this.buildList();
                this.initSorting();
                this.identifyDroppableLogical();

                if (callback) {
                    callback();
                }
            });
        }
    });

    return new ManageEnvironmentsView();
});
