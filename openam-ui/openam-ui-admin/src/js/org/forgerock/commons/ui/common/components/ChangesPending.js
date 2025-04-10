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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import ChangesPendingTemplate from "templates/common/ChangesPendingTemplate";

const ChangesPending = {};
const ChangesPendingWidget = AbstractView.extend({
    template: ChangesPendingTemplate,
    noBaseTemplate: true,
    events: {
        "click .undo-changes": "undo"
    },

    /**
         * @param {Object} args
         * @param {Function} [callback] - Called after render
         */
    render (args, callback) {
        const defaults = {
            alertClass: "alert-warning",
            icon: "fa-exclamation-circle",
            title: $.t("templates.user.ChangesPendingTemplate.changesPending"),
            message: "",
            undo: false,
            undoMsg: $.t("templates.user.ChangesPendingTemplate.undo"),
            watchedObj: {},
            changes: null,
            undoCallback: _.noop
        };

        this.data = _.extend(defaults, _.cloneDeep(args));
        this.element = args.element;

        if (!this.data.watchedProperties) {
            this.data.watchedProperties = _.keys(this.data.watchedObj);
        }

        if (!this.data.changes) {
            this.data.changes = _.cloneDeep(this.data.watchedObj);
        }

        this.parentRender(_.bind(function () {
            this.checkChanges();

            if (callback) {
                callback();
            }
        }, this));
    },

    /**
         * We need this clean up for when a view is rerendered and the original element
         * is no longer present. This will set a new element and redeclare all associated events.
         * @param {Object} [el] - jQuery element to render widget to
         */
    reRender (el) {
        this.$el = el;
        this.data.element = el;
        this.render(this.data, _.noop);
        this.delegateEvents();
    },

    /**
         * When undo is clicked the changes object will be overwritten and the undo callback called.
         * @param {Object} e
         */
    undo (e) {
        e.preventDefault();
        this.data.changes = _.cloneDeep(this.data.watchedObj);
        if (this.data.undoCallback) {
            this.data.undoCallback(_.pick(_.cloneDeep(this.data.watchedObj), this.data.watchedProperties));
        }
    },

    /**
         * Call to update widget with new data that has yet to be saved.
         * @param {Object} changes - The object that contains changes from the base object this.data.watchedObj
         */
    makeChanges (changes) {
        this.data.changes = _.cloneDeep(changes);
        this.checkChanges();
    },

    /**
         * Call to save your changes over to the watchedObj.
         */
    saveChanges () {
        this.data.watchedObj = _.cloneDeep(this.data.changes);
        this.checkChanges();
    },

    /**
         * Called when a change is made to this.data.changes or this.data.watchedObj
         */
    checkChanges () {
        $(this.element).toggle(this.isChanged());
    },

    /**
         * Called to check if changes were done
         */
    isChanged () {
        const isChanged = _.some(this.data.watchedProperties, _.bind(function (prop) {
            return !this.compareObjects(prop, this.data.watchedObj, this.data.changes);
        }, this));

        return isChanged;
    },

    /**
         * Compares two objects for equality on a given property.
         * @param {string} property
         * @param {Object} obj1
         * @param {Object} obj2
         * @returns {boolean} whether two passed objects are equal
         */
    compareObjects (property, obj1, obj2) {
        const val1 = _.cloneDeep(obj1[property]);
        const val2 = _.cloneDeep(obj2[property]);
        const deleteEmptyProperties = function (obj) {
            _.each(obj, (prop, key) => {
                if (_.isEmpty(prop) && !_.isNumber(prop) && !_.isBoolean(prop)) {
                    delete obj[key];
                }
            });
        };

        if (_.isObject(val1) && _.isObject(val2)) {
            deleteEmptyProperties(val1);
            deleteEmptyProperties(val2);
        } else if (!val1 && !val2 && val1 === val2) {
            return true;
        }

        return _.isEqual(val1, val2);
    }
});

/**
 * @param {Object} args
 * @param {Object} args.element - the jQuery element to render the widget to
 * @param {Object} args.watchedObj - the object containing properties to watch for changes
 * @param {Array} [args.watchedProperties=_.keys(args.watchedObj)] - a list of property names to watch for changes,
 *                                                                   defaults to watchedObj keys if not present
 * @param {boolean} [args.undo=false] - enables the undo functionality
 * @param {string} [args.undoMsg="Undo Changes"] - the text on the undo link
 * @param {string} [args.undoCallback] - The function called when the undo link is clicked
 * @param {string} [args.title="Changes Pending"] - bolded title of the widget
 * @param {string} [args.message=""] - additional message
 * @param {string} [args.icon="fa-exclamation-circle"] - font awesome icon classname
 * @param {string} [args.alertClass="alert-warning"] - the classname to apply to the whole widget
 * @param {Object} [args.changes] - contains the changes from watchedObj, this is usually not set manually
 * @param {Object} [callback] - called after render
 * @returns {Object} - An instance of the changes pending widget
 */
ChangesPending.watchChanges = function (args, callback) {
    const widget = new ChangesPendingWidget();
    widget.render(args, callback);
    return widget;
};

export default ChangesPending;
