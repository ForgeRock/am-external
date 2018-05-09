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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "templates/admin/views/realms/authorization/policies/PolicyActionsTemplate",
    "templates/admin/views/realms/authorization/policies/PolicyAvailableActionsTemplate",
    "templates/admin/views/realms/authorization/common/ActionsTableTemplate"
], ($, _, AbstractView, UIUtils, PolicyActionsTemplate, PolicyAvailableActionsTemplate, ActionsTableTemplate) => {
    const PolicyActionsView = AbstractView.extend({
        element: "#actions",
        template: PolicyActionsTemplate,
        noBaseTemplate: true,
        events: {
            "click [data-toggle-item]": "changePermission",
            "click [data-action-name]": "selectAction",
            "click button[data-delete]": "deleteItem",
            "keyup button[data-delete]": "deleteItem"
        },

        render (data, callback) {
            _.extend(this.data, data);

            let availableActions = _.cloneDeep(data.options.availableActions);
            const selectedActions = [];

            _.each(data.entity.actionValues, (value, key) => {
                availableActions = _.without(availableActions, _.find(availableActions, { action: key }));
                selectedActions.push({ action: key, value });
            });

            this.data.availableActions = availableActions;
            this.data.selectedActions = selectedActions;

            this.parentRender(function () {
                const d1 = $.Deferred();
                const d2 = $.Deferred();

                this.renderAvailableActions(() => d1.resolve());
                this.renderSelectedActions(() => d2.resolve());

                $.when(d1, d2).done(() => {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        renderAvailableActions (callback) {
            const tpl = PolicyAvailableActionsTemplate({ "items": _.sortBy(this.data.availableActions, "action") });
            this.$el.find("#availableActions").html(tpl);
            if (callback) {
                callback();
            }
        },

        renderSelectedActions (callback) {
            const tpl = ActionsTableTemplate({ "items": _.sortBy(this.data.selectedActions, "action") });
            this.$el.find("#selectedActions").html(tpl);
            this.$el.find("button[data-add-item]").prop("disabled", true);
            if (callback) {
                callback();
            }
        },

        selectAction (e) {
            e.preventDefault();

            const actionName = $(e.target).data("actionName");
            const action = _.find(this.data.options.availableActions, { action: actionName });
            const cloned = _.clone(action);

            this.data.availableActions = _.without(this.data.availableActions,
                _.find(this.data.availableActions, { action: actionName })
            );
            this.renderAvailableActions();
            this.data.selectedActions.push(cloned);
            this.renderSelectedActions();

            this.data.entity.actionValues[action.action] = action.value;
        },

        changePermission (e) {
            const $target = $(e.target);
            const permitted = ($target.val() || $target.find("input").val()) === "true";
            const actionName = $target.closest("tr").find(".action-name").text().trim();

            _.find(this.data.selectedActions, { action: actionName }).value = permitted;

            this.data.entity.actionValues[actionName] = permitted;
        },

        deleteItem (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }
            const $target = $(e.target);
            const actionName = $target.closest("tr").find(".action-name").text().trim();
            const selectedAction = _.find(this.data.selectedActions, { action: actionName });

            this.data.selectedActions = _.without(this.data.selectedActions, selectedAction);
            this.renderSelectedActions();

            delete this.data.entity.actionValues[actionName];

            this.data.availableActions.push(selectedAction);
            this.renderAvailableActions();
        }
    });

    return new PolicyActionsView();
});
