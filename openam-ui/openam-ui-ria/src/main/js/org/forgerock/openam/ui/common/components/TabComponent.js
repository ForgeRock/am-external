/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backbone",
    "templates/common/components/tab/TabComponentTemplate",
    "templates/common/components/tab/TabComponentBodyTemplate",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Backbone, TabComponentTemplate, TabComponentBodyTemplate) => {
    function has (attribute, tab) {
        if (!tab[attribute]) {
            throw new TypeError(`[TabComponent] Expected all items within 'tabs' to have a '${attribute}' attribute.`);
        }
        if (!_.isString(tab[attribute])) {
            throw new TypeError(`[TabComponent] Expected all items within 'tabs' to have String '${attribute}'s.`);
        }

        return true;
    }

    const TabComponent = Backbone.View.extend({
        template: TabComponentTemplate,
        bodyTemplate: TabComponentBodyTemplate,
        events: {
            "show.bs.tab ul.nav.nav-tabs a": "handleTabClick"
        },
        initialize (options) {
            if (!(options.tabs instanceof Array)) {
                throw new TypeError("[TabComponent] \"tabs\" argument is not an Array.");
            }
            if (_.isEmpty(options.tabs)) {
                throw new TypeError("[TabComponent] \"tabs\" argument is an empty Array.");
            }
            _(options.tabs)
                .each(_.partial(has, "id"))
                .each(_.partial(has, "title"))
                .value();

            this.options = options;
        },
        getBody () {
            return this.tabBody;
        },
        getBodyElement () {
            return this.$el.find("[data-tab-panel]");
        },
        getFooter () {
            return this.options.tabFooter;
        },
        getFooterElement () {
            return this.$el.find("[data-tab-footer]");
        },
        getTabId () {
            return this.currentTabId;
        },
        setTabId (id) {
            this.currentTabId = id;
            this.$el.find(`[data-tab-id="${id}"]`).tab("show");
        },
        handleTabClick (event) {
            this.currentTabId = $(event.currentTarget).data("tab-id");

            this.options.tabFooter = this.options.createFooter(this.currentTabId);
            this.tabBody = this.options.createBody(this.currentTabId);

            const html = this.bodyTemplate(this.options);
            this.$el.find("[data-tab-component-panel]").html(html);

            if (this.tabBody) {
                this.tabBody.setElement(this.getBodyElement());
                this.tabBody.render();
            }

            if (this.options.tabFooter) {
                this.options.tabFooter.setElement(this.getFooterElement());
                this.options.tabFooter.render();
            }
        },
        render () {
            const html = this.template(this.options);
            this.$el.html(html);
            this.$el.find(".tab-menu .nav-tabs").tabdrop();
            this.setTabId(this.options.tabs[0].id);
            return this;
        }
    });

    return TabComponent;
});