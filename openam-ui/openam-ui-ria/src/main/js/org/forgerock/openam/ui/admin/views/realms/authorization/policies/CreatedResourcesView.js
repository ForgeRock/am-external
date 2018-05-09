/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
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
    "templates/admin/views/realms/authorization/policies/CreatedResourcesTemplate",
    "templates/admin/views/realms/authorization/policies/PopulateResourceTemplate",
    "autosizeInput",
    "doTimeout"
], ($, _, AbstractView, EventManager, Constants, CreatedResourcesTemplate, PopulateResourceTemplate) => {
    const CreatedResourcesView = AbstractView.extend({
        element: "#editResources",
        template: CreatedResourcesTemplate,
        noBaseTemplate: true,
        events: {
            "click [data-add-resource]": "addResource",
            "keyup [data-add-resource]": "addResource",
            "keyup #resourceBuilder input": "addResource",
            "click span[data-delete]": "deleteResource",
            "keyup span[data-delete]": "deleteResource",
            "click [data-remove-pending]": "removePendingResource",
            "click [data-show-editing]" : "showEditingResources"
        },

        render (args, callback) {
            _.extend(this.data, args);

            if (this.data.entity.resources) {
                this.data.entity.resources = _.sortBy(this.data.entity.resources);
            } else {
                this.data.entity.resources = [];
            }

            var self = this;

            this.parentRender(() => {
                this.$el.find(".selectize").selectize({
                    sortField: {
                        field: "text",
                        direction: "asc"
                    },
                    onChange (value) {
                        self.data.options.newPattern = value;

                        const content = PopulateResourceTemplate(self.data);
                        var resources = self.$el.find("#populateResource");
                        resources.html(content);

                        resources.find("input").autosizeInput({ space: 19 });
                        resources.find("input:eq(0)").focus().select();
                    }
                });

                delete self.data.options.justAdded;
                self.flashDomItem(self.$el.find(".text-success"), "text-success");

                if (callback) { callback(); }
            });
        },

        validate (inputs) {
            // This is very simple native validation for supporting browsers for now.
            // More complexity to come later.
            let allInputsValid = true;

            _.find(inputs, (input) => {
                if (_.isFunction(input.checkValidity) && !input.checkValidity()) {
                    allInputsValid = false;
                    return;
                }
            });

            return allInputsValid;
        },

        addResource (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var resourceBuilder = this.$el.find("#resourceBuilder"),
                resourceStr = resourceBuilder.data().resource.replace("-*-", "̂"),
                inputs = resourceBuilder.find("input"),
                strLength = resourceStr.length,
                resource = "",
                count = 0,
                i,
                duplicateIndex;

            if (this.validate(inputs) === false) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidItem");
                this.flashDomItem(this.$el.find(".editing"), "text-danger");
                return;
            }

            for (i = 0; i < strLength; i++) {
                if (resourceStr[i] === "*") {
                    resource += inputs[count].value;
                    count++;
                } else if (resourceStr[i] === "̂") {
                    resource += inputs[count].value === "̂" ? "-*-" : inputs[count].value;
                    count++;
                } else {
                    resource += resourceStr[i];
                }
            }

            duplicateIndex = _.indexOf(this.data.entity.resources, resource);

            if (duplicateIndex >= 0) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateItem");
                this.flashDomItem(this.$el.find(`#createdResources ul li:eq(${duplicateIndex})`), "text-danger");
            } else {
                this.data.entity.resources.push(resource);
                this.data.options.justAdded = resource;
                this.render(this.data);
            }
        },

        removePendingResource (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            this.data.options.newPattern = null;
            this.render(this.data);
        },

        deleteResource (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }
            var resource = $(e.currentTarget).parent().data().resource;
            this.data.entity.resources = _.without(this.data.entity.resources, resource);
            this.render(this.data);
        },

        showEditingResources () {
            this.$el.find("[data-show-editing]").prop("disabled", true);
            this.$el.find("li.editing").removeClass("hidden");
        },

        flashDomItem (item, className) {
            item.addClass(className);
            $.doTimeout(_.uniqueId(className), 2000, () => {
                item.removeClass(className);
            });
        }
    });

    return new CreatedResourcesView();
});
