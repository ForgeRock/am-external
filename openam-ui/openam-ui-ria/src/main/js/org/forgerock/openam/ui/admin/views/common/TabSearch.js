/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/common/TabSearch
 */
define([
    "lodash",
    "jquery",
    "backbone",
    "templates/admin/views/common/TabSearchTemplate",
    "selectize"
], (_, $, Backbone, TabSearchTemplate) => {
    function throwOnInvalidOptions (options) {
        if (!options || !_.isObject(options)) {
            throw new Error("[TabSearch] No \"options\" object found.");
        } else {
            if (!options.onChange) {
                throw new Error("[TabSearch] No \"options.onChange\" function found.");
            }
            if (!options.properties) {
                throw new Error("[TabSearch] No \"options.properties\" object found.");
            }
        }
    }

    function createSelectize (element, callback) {
        element.selectize({
            searchField: ["text", "value"],
            onChange (value) {
                const optgroup = this.options[value].optgroup;
                callback(optgroup, value);
                this.clear(true);
            },
            render: {
                item (item) {
                    return `<div>${item.text}</div>`;
                },
                option (item) {
                    return `<div><div>${item.text}</div><span class="text-muted small"><em>${
                        item.value}</em></span></div></div>`;
                },
                optgroup_header (item) { // eslint-disable-line camelcase
                    return `<div class="optgroup-header"><span class="text-primary">${item.label}</span></div>`;
                }
            }
        });

        return element[0].selectize;
    }

    function populateOptionsFromJsonSchemaGroup (properties, selectize) {
        _.each(properties, (group, groupName) => {
            selectize.addOptionGroup(groupName, {
                label: group.title || " "
            });
            _.each(group.properties, (option, key) => {
                selectize.addOption({
                    text: option.title,
                    value: key,
                    optgroup: groupName
                });
            });
        });
        selectize.refreshOptions(false);
    }

    const TabSearch = Backbone.View.extend({
        /**
         * @param  {object} options Contains the options which are passed in
         * @param  {object} options.properties Contains the list of searchable properties
         * @param  {function} options.onChangeCallback. The function that is called when an option is selected,
         */
        initialize (options) {
            throwOnInvalidOptions(options);
            this.options = options;
        },

        render () {
            const data = {
                cssClass:"am-selectize-search",
                placeholder: "common.form.search",
                data: "search"
            };
            const html = TabSearchTemplate(data);
            this.$el.html(html);
            const selectize = createSelectize(this.$el.find("[data-search]"), this.options.onChange);
            populateOptionsFromJsonSchemaGroup(this.options.properties, selectize);

            return this;
        }
    });

    return TabSearch;
});
