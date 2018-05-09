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
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListEditingView",
    "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesPatternsTemplate"
], ($, _, AbstractView, StripedListEditingView, ResourceTypesPatternsTemplate) => {
    function ResourceTypePatternsView () {
    }

    // TODO: Rename StripedListEditingView or rewrite to bootstrap table
    ResourceTypePatternsView.prototype = new StripedListEditingView();

    ResourceTypePatternsView.prototype.render = function (entity, actions, el, callback) {
        this.data = {};
        this.entity = entity;

        this.data.items = actions || [];

        this.baseRender(this.data,
            ResourceTypesPatternsTemplate,
            el, callback);
    };

    ResourceTypePatternsView.prototype.getPendingItem = function () {
        return this.$el.find("[data-editing-input]").val().toString().trim();
    };

    ResourceTypePatternsView.prototype.isValid = function (e) {
        return this.getPendingItem(e) !== "";
    };

    ResourceTypePatternsView.prototype.isExistingItem = function (itemPending, itemFromCollection) {
        return itemPending === itemFromCollection;
    };

    ResourceTypePatternsView.prototype.getCollectionWithout = function (e) {
        const itemName = $(e.target).parents("li").data("item-name").toString();
        return _.without(this.data.items, itemName);
    };

    ResourceTypePatternsView.prototype.updateEntity = function () {
        this.entity.patterns = this.data.items;
    };

    return ResourceTypePatternsView;
});
