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
 * Copyright 2015-2025 Ping Identity Corporation.
 */

import "selectize";
import "popoverclickaway";

import { t } from "i18next";
import _ from "lodash";
import $ from "jquery";
import Handlebars from "handlebars-template-loader/runtime";

import { EditorState, Compartment } from "@codemirror/state";
import { EditorView, keymap, lineNumbers } from "@codemirror/view";
import { defaultKeymap } from "@codemirror/commands";
import { javascript } from "@codemirror/lang-javascript";
import { StreamLanguage } from "@codemirror/language";
import { groovy } from "@codemirror/legacy-modes/mode/groovy";
import { forgerockCmTheme } from "org/forgerock/openam/ui/admin/utils/cm-theme";

import { validate } from "org/forgerock/openam/ui/admin/services/realm/ScriptsService";
import { collapseContexts, lookupContext } from "org/forgerock/openam/ui/admin/utils/scripts/ScriptContextsHelper";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AlertPartial from "partials/alerts/_Alert";
import Base64 from "org/forgerock/commons/ui/common/util/Base64";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import ChangeContextTemplate from "templates/admin/views/realms/scripts/ChangeContextTemplate";
import ChangesPending from "org/forgerock/commons/ui/common/components/ChangesPending";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EditScriptTemplate from "templates/admin/views/realms/scripts/EditScriptTemplate";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import GlobalScriptsService from "org/forgerock/openam/ui/admin/services/global/ScriptsService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewScriptTemplate from "templates/admin/views/realms/scripts/NewScriptTemplate";
import Router from "org/forgerock/commons/ui/common/main/Router";
import Script from "org/forgerock/openam/ui/admin/models/scripts/ScriptModel";
import ScriptContext from "org/forgerock/openam/ui/admin/models/scripts/ScriptContextModel";
import ScriptValidationTemplate from "templates/admin/views/realms/scripts/ScriptValidationTemplate";

Handlebars.registerHelper("concat", (value1, value2) => {
    return value1 + value2;
});

export default AbstractView.extend({
    initialize () {
        AbstractView.prototype.initialize.call(this);
        this.model = null;
        this.contextModel = null;
        this.languageConf = new Compartment();
    },
    partials: {
        "alerts/_Alert": AlertPartial
    },
    events: {
        "click [data-upload-script]": "uploadScript",
        "change [name=upload]": "readUploadedFile",
        "change [name=evaluatorVersion]": "onVersionChange",
        "click [data-validation-script]": "validateScript",
        "click [data-change-context]": "openDialog",
        "change input[name=language]": "onChangeLanguage",
        "click [data-save]": "submitForm",
        "click [data-delete]": "onDeleteClick",
        "click [data-show-fullscreen]": "editFullScreen",
        "click [data-exit-fullscreen]": "exitFullScreen",
        "change [data-field]": "checkChanges",
        "keyup [data-field]": "checkChanges"
    },

    render (args, callback) {
        let uuid = null;

        this.data.realmPath = args[0];

        // As we interrupt render to update the model, we need to remember the callback
        if (callback) {
            this.renderCallback = callback;
        }

        // Realm location is the first argument, second one is the script uuid
        if (args.length === 2) {
            uuid = args[1];
        }

        this.contextsPromise = GlobalScriptsService.scripts.getAllContexts();
        this.defaultContextPromise = GlobalScriptsService.scripts.getDefaultGlobalContext();
        this.contextSchemaPromise = GlobalScriptsService.scripts.getSchema();
        this.languageSchemaPromise = GlobalScriptsService.scripts.getContextSchema();

        if (uuid) {
            this.data.headerActions = [
                { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" }
            ];
            this.template = EditScriptTemplate;
            this.model = new Script({ _id: uuid });
            const self = this;
            this.listenTo(this.model, "sync", (model) => {
                self.contextModel = new ScriptContext({ _id: model.attributes.context });
                this.listenTo(this.contextModel, "sync", self.renderAfterSyncModel);
                self.contextModel.fetch();
            });
            this.model.fetch();
        } else {
            this.template = NewScriptTemplate;
            this.newEntity = true;
            this.model = new Script();
            this.contextModel = new ScriptContext();
            this.renderAfterSyncModel();
        }
    },

    remove () {
        if (this.scriptEditor) {
            this.scriptEditor.destroy();
        }
        $(window).off("hashchange", this.cleanupAfterCodemirror);
        AbstractView.prototype.remove.call(this);
    },

    /**
     * So the uuid can be omitted to the render function for two reasons:
     * 1. need to create a new script
     * 2. the render function is called from the function onModelSync
     * Then there is a conflict in the function syncModel.
     * In the first case we should to create a new model, in second case is not create.
     * So the render function is divided into two parts, so as not to cause a re-check and avoid the second case.
     */
    renderAfterSyncModel () {
        const self = this;

        this.data.entity = _.pick(this.model.attributes,
            "uuid", "name", "description", "language", "context", "evaluatorVersion", "script");
        this.data.context = _.pick(this.contextModel.attributes, "subContexts", "evaluatorVersions");

        if (this.data.contexts) {
            self.languageSchemaPromise.then((langSchema) => {
                self.langSchema = langSchema;
                self.renderScript();
            });
        } else {
            Promise.all([
                self.contextsPromise,
                self.defaultContextPromise,
                self.contextSchemaPromise,
                self.languageSchemaPromise
            ]).then(([contexts, defaultContext, contextSchema, languageSchema]) => {
                self.data.contexts = contexts.result;
                self.data.defaultContext = defaultContext.defaultContext;
                self.addContextNames(self.data.contexts, contextSchema);
                self.data.scriptTypes = collapseContexts(self.data.contexts);
                self.langSchema = languageSchema;
                self.renderScript();
            });
        }

        self.cleanupAfterCodemirror = _.bind(self.cleanupAfterCodemirror, self);
        window.addEventListener("hashchange", self.cleanupAfterCodemirror);
    },

    cleanupAfterCodemirror () {
        // When codemirror is in a full screen mode, it adds "overflow: hidden" to the document. Pressing back
        // button while in full screen doesn't remove the style, hence breaking the scrolling for the app
        const editorWrapper = this.$el.find(".cm-editor").parent();
        if (editorWrapper.hasClass("fullscreen")) {
            this.toggleFullScreen(false);
        }
    },

    renderScript () {
        const self = this;
        let context;

        if (this.model.id) {
            context = this.getContext(self);
            this.data.contextName = context.name;
            this.data.evaluatorVersions = this.getEvaluatorVersions(context, this.data.entity.language);
            this.data.languages = this.addLanguageNames(context.languages.filter((lang) =>
                this.data.entity.evaluatorVersion === "1.0" || lang === "JAVASCRIPT"));
        } else {
            this.data.languages = [];
        }

        this.parentRender(function () {
            if (this.newEntity) {
                this.$el.find("#context").selectize({
                    sortField: "text"
                });
            } else {
                this.changesPendingWidget = ChangesPending.watchChanges({
                    element: this.$el.find(".script-changes-pending"),
                    watchedObj: this.data.entity,
                    undo: !this.newEntity,
                    undoCallback (changes) {
                        _.extend(self.data.entity, changes);
                        const context = self.getContext(self);
                        self.data.contextName = context.name;
                        self.data.evaluatorVersions = self.data.context.evaluatorVersions[this.data.entity.language];
                        self.data.languages = self.addLanguageNames(context.languages);
                        self.reRenderView();
                    },
                    alertClass: "alert-warning alert-sm"
                });

                this.showUploadButton();
                this.initScriptEditor();
            }

            if (this.renderCallback) {
                this.renderCallback();
            }
        });
    },

    reRenderView () {
        this.parentRender(function () {
            this.showUploadButton();
            this.initScriptEditor();

            this.changesPendingWidget.makeChanges(this.data.entity);
            this.changesPendingWidget.reRender(this.$el.find(".script-changes-pending"));
        });
    },

    checkChanges () {
        this.updateFields();
        if (this.newEntity) {
            this.toggleSaveButton(this.checkRequiredFields());
        } else {
            this.changesPendingWidget.makeChanges(this.data.entity);
        }
    },

    updateFields () {
        const self = this;
        const app = this.data.entity;
        const previousContext = app.context;
        const dataFields = this.$el.find("[data-field]");
        let dataField;

        _.each(dataFields, (field) => {
            dataField = field.getAttribute("data-field");

            if (field.type === "radio") {
                if (field.checked) {
                    app[dataField] = field.value;
                }
            } else {
                app[dataField] = field.value.trim();
            }
        });

        const subTypes = lookupContext(this.data.entity.context);
        if (subTypes) {
            app.context = app.evaluatorVersion === "1.0" ? subTypes.legacyContext : subTypes.nextGenContext;
        }

        if (this.newEntity) {
            if (previousContext !== app.context || subTypes) {
                self.toggleSaveButton(false);

                this.updateContextModel(app.context)
                    .then(_.bind(self.changeContext, self))
                    .then(_.bind(self.disableVersionButtons, self))
                    .then(_.bind(self.updateInfoButton, self))
                    .then(_.bind(self.ensureVersionValid, self))
                    .then(() => {
                        self.toggleSaveButton(self.checkRequiredFields());
                    });
            }
        } else if (this.scriptEditor) {
            app.script = this.scriptEditor.state.doc.toString();
        }
    },

    checkRequiredFields () {
        return this.data.entity.name && this.data.entity.context && this.data.entity.language &&
            this.data.entity.evaluatorVersion;
    },

    disableVersionButtons () {
        const evs = this.data.evaluatorVersions;
        $("#radio-legacy").prop("disabled", !evs.includes("1.0"));
        $("label[for=radio-legacy]").toggleClass("disabled", !evs.includes("1.0"));
        $("#radio-nextgen").prop("disabled", !evs.includes("2.0"));
        $("label[for=radio-nextgen]").toggleClass("disabled", !evs.includes("2.0"));
    },

    updateInfoButton () {
        const scriptTypeName = $("#context").text().trim();
        const scriptType = this.data.scriptTypes.find((type) => type.name === scriptTypeName);
        const subTypes = lookupContext(scriptType._id);
        const infoButton = $("#evaluatorVersionInfo");
        if (subTypes) {
            infoButton.removeClass("hidden");
            infoButton.popoverclickaway({
                container: "#content",
                html: true,
                placement: "right",
                update: true,
                content: `<p>${t("console.scripts.edit.scriptType")} will differ based on selected 
${t("console.scripts.edit.evaluatorVersion")}</p>
<ul>
<li>${t("console.scripts.edit.version.1.0")}: <code>${this.getContextById(subTypes.legacyContext).name}</code></li>
<li>${t("console.scripts.edit.version.2.0")}: <code>${this.getContextById(subTypes.nextGenContext).name}</code></li>
</ul>`
            });
        } else {
            infoButton.addClass("hidden");
        }
    },

    ensureVersionValid () {
        const evs = this.data.evaluatorVersions;
        const ev = this.data.entity.evaluatorVersion;
        if (!evs.includes(ev)) {
            this.data.entity.evaluatorVersion = evs[0];
            this.updateVersionButtons(evs[0] === "1.0" ? "radio-legacy" : "radio-nextgen");
        }
    },

    submitForm (e) {
        e.preventDefault();

        const self = this;
        const nonModifiedAttributes = _.clone(this.model.attributes);

        this.updateFields();

        _.extend(this.model.attributes, { description: "" }, this.data.entity);
        const savePromise = this.model.save();

        if (savePromise) {
            savePromise.then(() => {
                if (self.newEntity) {
                    Router.routeTo(Router.configuration.routes.realmsScriptEdit, {
                        args: [encodeURIComponent(self.data.realmPath), self.model.id],
                        trigger: true
                    });
                } else {
                    Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                }
            });
        } else {
            _.extend(this.model.attributes, nonModifiedAttributes);
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
        }
    },

    validateScript () {
        if (!this.scriptEditor) {
            return;
        }
        const scriptText = this.scriptEditor.state.doc.toString();
        const language = this.data.entity.language;
        const self = this;

        const script = {
            script: Base64.encodeUTF8(scriptText),
            language
        };

        validate(script).then((result) => {
            const tpl = ScriptValidationTemplate(result);
            self.$el.find("#validation").html(tpl);
        });
    },

    uploadScript () {
        this.$el.find("[name=upload]").trigger("click");
    },

    readUploadedFile (e) {
        const self = this;
        const file = e.target.files[0];
        const reader = new FileReader();

        reader.onload = (function () {
            return function (e) {
                self.scriptEditor.dispatch({
                    changes: { from: 0, to: self.scriptEditor.state.doc.length, insert: e.target.result }
                });
            };
        }(file));

        reader.readAsText(file);
    },

    onVersionChange (e) {
        this.updateVersionButtons(e.target.id);
    },

    updateVersionButtons (activeButtonId) {
        this.$el.find(".versionSelect").removeClass("active");
        this.$el.find(`label[for=${activeButtonId}]`).addClass("active");
        this.$el.find(`#${activeButtonId}`).prop("checked", true);
    },

    openDialog () {
        const self = this;

        if (this.data.defaultContext) {
            self.renderDialog();
        } else {
            this.defaultContextPromise.then((context) => {
                self.data.defaultContext = context.defaultContext;
                self.renderDialog();
            });
        }
    },

    renderDialog () {
        BootstrapDialog.show(this.constructDialogOptions());
    },

    constructDialogOptions () {
        const self = this;
        const footerButtons = [];
        const options = {
            type: BootstrapDialog.TYPE_DANGER,
            title: t("console.scripts.edit.dialog.title"),
            cssClass: "script-change-context",
            message: $("<div></div>"),
            onshow () {
                const dialog = this;
                const tpl = ChangeContextTemplate(self.data);
                dialog.message.append(tpl);
            }
        };

        footerButtons.push({
            label: t("common.form.cancel"),
            cssClass: "btn-default",
            action (dialog) {
                dialog.close();
            }
        }, {
            label: t("common.form.change"),
            cssClass: "btn-danger",
            action (dialog) {
                const checkedItem = dialog.$modalContent.find("[name=changeContext]:checked");
                const newContext = checkedItem.val();
                const newContextName = checkedItem.parent().text().trim();
                if (self.data.entity.context !== newContext) {
                    self.data.entity.context = newContext;
                    self.data.contextName = newContextName;
                    self.updateContextModel(newContext)
                        .then(_.bind(self.changeContext, self))
                        .then(_.bind(self.reRenderView, self));
                }
                dialog.close();
            }
        });

        options.buttons = footerButtons;

        return options;
    },

    getContext (self) {
        return this.getContextById(self.data.entity.context);
    },

    getContextById (id) {
        return _.find(this.data.contexts, (context) => {
            return context._id === id;
        });
    },

    getEvaluatorVersions (context, language) {
        if (lookupContext(context._id)) {
            return ["1.0", "2.0"];
        }
        return this.data.context.evaluatorVersions[language];
    },

    changeContext () {
        const self = this;
        const selectedContext = this.getContext(self);
        let defaultScript;
        const promise = $.Deferred();

        this.data.languages = this.addLanguageNames(selectedContext.languages);

        if (!selectedContext.defaultScript || selectedContext.defaultScript === "[Empty]") {
            this.data.entity.script = "";
            this.data.entity.language = this.data.languages[0].id;
            this.data.evaluatorVersions = this.getEvaluatorVersions(selectedContext, this.data.entity.language);
            promise.resolve();
        } else {
            defaultScript = new Script({ _id: selectedContext.defaultScript });
            this.listenTo(defaultScript, "sync", (model) => {
                if (self.data.languages.length === 1) {
                    self.data.entity.language = self.data.languages[0].id;
                } else {
                    self.data.entity.language = model.attributes.language;
                }
                self.data.evaluatorVersions = this.getEvaluatorVersions(selectedContext, self.data.entity.language);
                self.data.entity.script = model.attributes.script;
                promise.resolve();
            });
            defaultScript.fetch();
        }

        return promise;
    },

    updateContextModel (contextName) {
        const promise = $.Deferred();

        const self = this;
        this.contextModel = new ScriptContext({ _id: contextName });
        this.listenTo(this.contextModel, "sync", (model) => {
            self.data.context = _.pick(model.attributes, "subContexts", "evaluatorVersions");
            promise.resolve();
        });
        this.contextModel.fetch();

        return promise;
    },

    initScriptEditor () {
        const scriptTextArea = this.$el.find("#script")[0];
        const editorDiv = document.createElement("div");

        scriptTextArea.parentElement.insertBefore(editorDiv, scriptTextArea);
        scriptTextArea.style.display = "none";

        const extensions = [
            ...forgerockCmTheme,
            lineNumbers(),
            keymap.of(defaultKeymap),
            this.languageConf.of(this.getLanguageSupport(this.data.entity.language)),
            EditorView.updateListener.of((update) => {
                if (update.docChanged) {
                    this.checkChanges();
                }
            })
        ];

        this.scriptEditor = new EditorView({
            state: EditorState.create({
                doc: scriptTextArea.value,
                extensions
            }),
            parent: editorDiv
        });
    },

    onChangeLanguage (e) {
        this.changeLanguage(e.target.value);
    },

    changeLanguage (language) {
        this.data.entity.language = language;
        this.scriptEditor.dispatch({ effects: this.languageConf.reconfigure(this.getLanguageSupport(language)) });
    },

    getLanguageSupport (language) {
        switch (language.toUpperCase()) {
            case "GROOVY":
                return StreamLanguage.define(groovy);
            case "JAVASCRIPT":
            default:
                return javascript();
        }
    },

    showUploadButton () {
        // Show the Upload button for modern browsers only. Documented feature.
        // File: Chrome 13; Firefox (Gecko) 3.0 (1.9) (non standard), 7 (7) (standard); Internet Explorer 10.0;
        //       Opera 11.5; Safari (WebKit) 6.0
        // FileReader: Firefox (Gecko) 3.6 (1.9.2);	Chrome 7; Internet Explorer 10; Opera 12.02; Safari 6.0.2
        if (window.File && window.FileReader && window.FileList) {
            this.$el.find("[data-upload-scripts]").show();
        }
    },

    /**
     * Update context's array using translation from Schema.
     * @param  {Array} contexts Array with script contexts
     * @param  {object} schema Script schema with translations
     */
    addContextNames (contexts, schema) {
        let i;
        let index;
        const length = contexts.length;
        if (schema && schema.properties && schema.properties.defaultContext) {
            for (i = 0; i < length; i++) {
                index = _.indexOf(schema.properties.defaultContext["enum"], contexts[i]._id);
                contexts[i].name = schema.properties.defaultContext.options.enum_titles[index];
            }
        }
    },

    /**
     * Merge script IDs from Context and translation from Schema to the Language array.
     * @param  {Array} languages Language IDs from Context
     * @returns {Array} result combined array
     */
    addLanguageNames (languages) {
        let result;
        let i;
        const length = languages.length;
        let index;
        if (this.langSchema && this.langSchema.properties && this.langSchema.properties.languages &&
            this.langSchema.properties.languages.items) {
            result = [];
            for (i = 0; i < length; i++) {
                index = _.indexOf(this.langSchema.properties.languages.items["enum"], languages[i]);
                result[i] = {
                    id: languages[i],
                    name: this.langSchema.properties.languages.items.options.enum_titles[index]
                };
            }
        }
        return result;
    },

    onDeleteClick (e) {
        e.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({ type: t("console.scripts.edit.script") },
            _.bind(this.deleteScript, this));
    },

    deleteScript () {
        const self = this;
        const onSuccess = function () {
            Router.routeTo(Router.configuration.routes.realmsScripts, {
                args: [encodeURIComponent(self.data.realmPath)],
                trigger: true
            });
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        };

        this.model.destroy({
            success: onSuccess,
            wait: true
        });
    },

    editFullScreen () {
        this.toggleFullScreen(true);
    },

    exitFullScreen () {
        this.toggleFullScreen(false);
    },

    toggleFullScreen (fullScreen) {
        const editorWrapper = this.$el.find(".cm-editor").parent();
        editorWrapper.toggleClass("fullscreen", fullScreen);
        this.$el.find(".full-screen-bar").toggle(fullScreen);
        if (this.scriptEditor) {
            this.scriptEditor.focus();
        }
    },

    toggleSaveButton (flag) {
        this.$el.find("[data-save]").prop("disabled", !flag);
    }
});
