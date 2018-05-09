/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "doTimeout"
], ($, _, AbstractView, EventManager, Constants) =>
    AbstractView.extend({
        noBaseTemplate: true,
        events: {
            "click [data-add-item]": "addItem",
            "keyup [data-add-item]": "addItem",
            "keyup [data-editing-input]": "addItem",
            "click span[data-delete]": "deleteItem",
            "keyup span[data-delete]": "deleteItem"
        },

        baseRender (data, tpl, el, callback) {
            this.data = data;
            this.data.options = {};

            this.template = tpl;
            this.element = el;

            this.renderParent(callback);
        },

        renderParent (callback) {
            this.parentRender(function () {
                delete this.data.options.justAdded;

                this.flashDomItem(this.$el.find(".text-success"), "text-success");

                if (callback) {
                    callback();
                }
            });
        },

        addItem (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                this.toggleAddButton(this.isValid(e));
                return;
            }

            const pending = this.getPendingItem(e); // provide child implementation
            let duplicateIndex = -1;
            let counter = 0;

            if (!this.isValid(e)) { // provide child implementation
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidItem");
                this.flashDomItem(this.$el.find(".editing"), "text-danger");
                return;
            }

            _.each(this.data.items, (item) => {
                if (this.isExistingItem(pending, item)) { // provide child implementation
                    duplicateIndex = counter;
                    return;
                }
                counter++;
            });

            if (duplicateIndex >= 0) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateItem");
                this.flashDomItem(this.$el.find(`.list-group-item:eq(${duplicateIndex})`), "text-danger");
            } else {
                this.data.items.push(pending);
                this.data.options.justAdded = pending;
                if (this.updateEntity) {
                    // provide child implementation
                    this.updateEntity();
                }
                this.renderParent(() => {
                    this.$el.find(".editing input[type=text]").focus();
                });
            }
        },

        deleteItem (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            this.data.items = this.getCollectionWithout(e); // provide child implementation
            if (this.updateEntity) {
                this.updateEntity(); // provide child implementation
            }
            this.renderParent();
        },

        flashDomItem (item, className) {
            item.addClass(className);
            $.doTimeout(_.uniqueId(className), 2000, () => {
                item.removeClass(className);
            });
        },

        toggleAddButton (enabled) {
            this.$el.find("[data-add-item]").prop("disabled", !enabled);
        }
    })
);
