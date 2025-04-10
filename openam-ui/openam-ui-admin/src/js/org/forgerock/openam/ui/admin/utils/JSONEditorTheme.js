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
import JSONEditor from "exports-loader?window.JSONEditor!json-editor";

const JSONEditorTheme = {};

const buildToggleSwitch = (checkbox) => {
    const div = document.createElement("div");
    const label = document.createElement("label");
    const span = document.createElement("span");

    checkbox.style.opacity = "0";
    label.className = "am-toggle-switch";

    label.appendChild(checkbox);
    label.appendChild(span);

    div.appendChild(label);

    return div;
};

JSONEditorTheme.getTheme = function (gridColWidth1, gridColWidth2) {
    // Magic number 12 is the number of colomns in the bootstrap grid.
    const theme = JSONEditor.AbstractTheme.extend({
        getSelectInput (options) {
            const input = this._super(options);
            input.className += "form-control";

            return input;
        },

        setSelectOptions (selectGroup, options, titles) {
            const select = selectGroup.getElementsByTagName("select")[0] || selectGroup;
            let option = null;
            let i;

            titles = titles || [];
            select.innerHTML = "";

            for (i = 0; i < options.length; i++) {
                option = document.createElement("option");
                option.setAttribute("value", options[i]);
                option.textContent = titles[i] || options[i];
                select.appendChild(option);
            }
        },

        setGridColumnSize () {
            // JSONEditor grid system not used, so overridden here.
        },

        afterInputReady (input) {
            if (input.controlgroup) {
                return;
            }
            input.controlgroup = this.closest(input, ".form-group");
            if (this.closest(input, ".compact")) {
                input.controlgroup.style.marginBottom = 0;
            }
        },

        getTextareaInput (placeholder) {
            const el = document.createElement("textarea");
            el.className = "form-control";
            if (placeholder) {
                el.setAttribute("placeholder", placeholder);
            }
            return el;
        },
        getFormInputField (type, placeholder) {
            const input = this._super(type);
            if (type !== "checkbox") {
                input.className += "form-control";
            }
            if (type === "password") {
                input.setAttribute("spellcheck", "false");
            }
            if (placeholder) {
                input.setAttribute("placeholder", placeholder);
            }
            input.setAttribute("autocomplete", "off");
            return input;
        },

        getFormInputLabel (text) {
            const el = document.createElement("label");
            el.appendChild(document.createTextNode(text));
            el.className += ` control-label col-sm-${gridColWidth2}`;
            return el;
        },

        getFormControl (label, input, description, inheritanceButton) {
            const group = document.createElement("div");
            const div = document.createElement("div");

            group.className = "form-group";

            if (label && $(input).prop("type") === "checkbox") {
                input = buildToggleSwitch(input);
            }

            if (label) {
                label.className += ` control-label col-sm-${gridColWidth2}`;
                group.appendChild(label);
            }

            const nodeName = input.nodeName.toLowerCase();
            if (["div", "input", "select", "textarea"].includes(nodeName)) {
                // All Inputs need to be wrapped in a div with the BS grid class added.
                div.className += `col-sm-${gridColWidth1}`;
                div.appendChild(input);
                group.appendChild(div);
            } else {
                group.appendChild(input);
            }

            if (inheritanceButton) {
                group.appendChild(inheritanceButton);
            }

            if (description) {
                group.appendChild(description);
            }
            return group;
        },

        getCheckboxLabel (text) {
            return this.getFormInputLabel(text);
        },

        getIndentedPanel () {
            return document.createElement("div");
        },

        getFormInputDescription (text, additional) {
            return this.getDescription(text, additional);
        },

        getDescription (text, additional) {
            const el = document.createElement("div");
            const parseHtml = document.implementation.createHTMLDocument("");
            let content = "";
            let classNames = "";
            let alertType = "";

            if (_.has(additional, "alert")) {
                alertType = _.get(additional, "alert");
                classNames = `col-sm-offset-${gridColWidth2} col-sm-${gridColWidth1}`;
                content = `<div><p></p><div class='alert ${alertType}' role='alert'>${text}</div></div>`;
            } else {
                classNames = `col-sm-offset-${gridColWidth2} col-sm-${gridColWidth1} help-block`;
                content = `<div class='wordwrap'>${text}</div>`;
            }

            el.className = classNames;
            parseHtml.body.innerHTML = content;
            el.appendChild(parseHtml.body.getElementsByTagName("div")[0]);

            return el;
        },

        getHeaderButtonHolder () {
            return this.getButtonHolder();
        },

        getButtonHolder () {
            const el = document.createElement("div");
            el.className = "btn-group";
            return el;
        },

        getButton (text, icon, title) {
            const el = this._super(text, icon, title);
            el.className += "btn btn-default";
            return el;
        },

        getInlineButton (text, icon, title) {
            const el = this._super(text, icon, title);
            el.className += "btn btn-link delete-row-item";
            return el;
        },

        getTable () {
            const el = document.createElement("table");
            el.className = "table table-bordered";
            el.style.width = "auto";
            el.style.maxWidth = "none";
            return el;
        },

        getGridRow () {
            const el = document.createElement("div");
            el.className = "form-horizontal";
            return el;
        },

        addInputError (input, text) {
            if (!input.controlgroup) {
                return;
            }
            input.controlgroup.className += " has-error";
            if (input.errmsg) {
                input.errmsg.style.display = "";
            } else {
                input.errmsg = document.createElement("p");
                input.errmsg.className =
                    `help-block errormsg col-sm-offset-${gridColWidth2} col-sm-${gridColWidth1}`;
                input.controlgroup.appendChild(input.errmsg);
            }

            input.errmsg.textContent = text;
        },

        removeInputError (input) {
            if (!input.errmsg) {
                return;
            }
            input.errmsg.style.display = "none";
            input.controlgroup.className = input.controlgroup.className.replace(/\s?has-error/g, "");
        },

        getTabHolder () {
            const el = document.createElement("div");
            el.innerHTML = "<div class=tabs 'list-group col-md-2'></div><div class='col-md-10'></div>";
            el.className = "rows";
            return el;
        },

        getTab (text) {
            const el = document.createElement("a");
            el.className = "list-group-item";
            el.setAttribute("href", "#");
            el.appendChild(text);
            return el;
        },

        markTabActive (tab) {
            tab.className += " active";
        },

        markTabInactive (tab) {
            tab.className = tab.className.replace(/\s?active/g, "");
        },

        getProgressBar () {
            const min = 0;
            const max = 100;
            const start = 0;
            const container = document.createElement("div");
            const bar = document.createElement("div");

            container.className = "progress";
            bar.className = "progress-bar";
            bar.setAttribute("role", "progressbar");
            bar.setAttribute("aria-valuenow", start);
            bar.setAttribute("aria-valuemin", min);
            bar.setAttribute("aria-valuenax", max);
            bar.innerHTML = `${start}%`;
            container.appendChild(bar);

            return container;
        },

        updateProgressBar (progressBar, progress) {
            if (!progressBar) {
                return;
            }

            const bar = progressBar.firstChild;
            const percentage = `${progress}%`;
            bar.setAttribute("aria-valuenow", progress);
            bar.style.width = percentage;
            bar.innerHTML = percentage;
        },

        updateProgressBarUnknown (progressBar) {
            if (!progressBar) {
                return;
            }

            const bar = progressBar.firstChild;
            progressBar.className = "progress progress-striped active";
            bar.removeAttribute("aria-valuenow");
            bar.style.width = "100%";
            bar.innerHTML = "";
        },

        getFirstColumnWrapper () {
            const wrapper = document.createElement("div");
            wrapper.className = `col-sm-${gridColWidth1}`;
            return wrapper;
        },

        getSecondColumnWrapper () {
            const wrapper = document.createElement("div");
            wrapper.className = `col-sm-offset-1 col-sm-${(gridColWidth2 - 1)}`;
            return wrapper;
        },

        addError (element) {
            $(element).addClass("has-error");
        },

        removeError (element) {
            $(element).removeClass("has-error");
        },

        addBorder (element) {
            element.style.border = "solid 1px rgb(204, 204, 204)";
            element.style.paddingTop = "15px";
            element.style.marginBottom = "15px";
        },

        getHeader (text) {
            const el = document.createElement("h3");

            el.className = "block-header";
            el.setAttribute("data-header", true);
            if (typeof text === "string") {
                el.textContent = text;
            }
            return el;
        },

        getMapHeader (text) {
            const el = document.createElement("div"); const header = document.createElement("label");
            el.appendChild(header);
            if (typeof text === "string") {
                header.textContent = text;
            }
            el.style.display = "inline-block";
            return el;
        },

        getKeyFormInputField () {
            return this.getFormInputField("text", $.t("common.form.key"));
        },

        getValueFormInputField () {
            return this.getFormInputField("text", $.t("common.form.value"));
        },

        getInputId () {
            return _.uniqueId();
        },

        getInheritanceButton (valueIsInherited, path, hideInheritance) {
            if (hideInheritance) {
                return null;
            }

            const button = document.createElement("button");
            button.type = "button";
            button.className = "btn fr-btn-secondary am-btn-single-icon";
            button.setAttribute("data-inherit-value", valueIsInherited);
            button.setAttribute("data-schemapath", path);
            button.dataToggle = "button";
            button.title = $.t("common.form.inheritValue");
            const icon = document.createElement("i");
            icon.className = "fa fa-unlock";
            if (valueIsInherited) {
                button.className += " active";
                icon.className = "fa fa-lock";
            }
            button.appendChild(icon);
            return button;
        },

        getSwitcher () {
            const el = document.createElement("div");
            return el;
        },

        getModal () {
            const el = document.createElement("div");
            el.className = "form-group";
            return el;
        }
    });

    return theme;
};

export default JSONEditorTheme;
