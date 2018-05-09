/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/utils/form/setActiveTab
  */
define([
], () =>
/**
      * Sets active tab whose ID indicated in the variable view.activeTabId.
      * @param  {Object} view Backbone view with tabs
      * @param  {string} view.activeTabId ID tab which you want to make active
      */
    (view) => {
        if (view && view.activeTabId) {
            view.$el.find(`.nav-tabs a[href="${view.activeTabId}"]`).tab("show");
        }
    }
);
