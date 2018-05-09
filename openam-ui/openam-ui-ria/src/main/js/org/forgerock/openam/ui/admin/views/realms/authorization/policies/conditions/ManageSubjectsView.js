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
    var ManageSubjectsView = ManageRulesView.extend({
        element: "#subjectContainer",
        subEvents: {
            "change .subject-area .operator > select": "onSelect",
            "mousedown #operatorSub_0 li.rule:not(.editing)": "setFocus",
            "mousedown #operatorSub_0 li.operator:not(.editing)": "setFocus",

            "click    #operatorSub_0 .rule > .item-button-panel > .fa-times": "onDelete",
            "keyup    #operatorSub_0 .rule > .item-button-panel > .fa-times": "onDelete",
            "click    #operatorSub_0 .rule > .item-button-panel > .fa-pencil": "toggleEditing",
            "keyup    #operatorSub_0 .rule > .item-button-panel > .fa-pencil": "toggleEditing",
            "click    #operatorSub_0 .rule > .item-button-panel > .fa-check": "toggleEditing",
            "keyup    #operatorSub_0 .rule > .item-button-panel > .fa-check": "toggleEditing",
            "dblclick #operatorSub_0 li.rule:not(.legacy-condition)": "toggleEditing"
        },
        data: {},
        buttons: {},
        idCount: 0,
        typeAND: {
            "title": "AND",
            "logical": true,
            "config": {
                "properties": {
                    "subjects": {
                        "type": "array",
                        "items": {
                            "type": "any"
                        }
                    }
                }
            }
        },

        render (args, callback) {
            this.idPrefix = "Sub_";
            this.property = "subject";
            this.properties = "subjects";
            this.data.conditionName = $.t("console.authorization.policies.edit.addSubjectCondition");
            this.data.entity = args.entity;
            this.data.options = args.options;
            this.data.subjects = [];
            this.data.operators = [];

            var self = this;

            _.each(this.data.options.availableSubjects, (item) => {
                if (item.logical === true) {
                    self.data.operators.push(item);
                } else {
                    self.data.subjects.push(item);
                }

                delete item.config.type;
            });

            if (!_.find(this.data.operators, { title: "AND" })) {
                this.data.operators.push(this.typeAND);
            }

            this.init(args, this.subEvents);
            this.conditionType = Constants.SUBJECT;
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

    return new ManageSubjectsView();
});
