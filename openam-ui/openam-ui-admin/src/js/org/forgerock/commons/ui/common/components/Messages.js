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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";
import debug from "debug";

import ConfigMessages from "config/messages";

const Messages = {
    configuration: {
        messages: ConfigMessages
    }
};

Messages.TYPE_SUCCESS = "success";
Messages.TYPE_INFO = "info";
Messages.TYPE_WARNING = "warning";
Messages.TYPE_DANGER = "error";

const MessagesView = Backbone.View.extend({
    list: [],
    el: "#messages",
    events: {
        "click div": "removeAndNext"
    },
    delay: 2500,
    timer: null,

    displayMessageFromConfig (event) {
        const _this = Messages.messages;
        if (typeof event === "object") {
            if (typeof event.key === "string") {
                _this.addMessage({
                    message: $.t(Messages.configuration.messages[event.key].msg, event),
                    type: Messages.configuration.messages[event.key].type
                });
            }
        } else if (typeof event === "string") {
            _this.addMessage({
                message: $.t(Messages.configuration.messages[event].msg),
                type: Messages.configuration.messages[event].type
            });
        }
    },
    /**
     * @param {Object} msg contains the message object
     * @param {string} msg.type - The type of mesage to be displayed. TYPE_SUCCESS, TYPE_INFO, TYPE_WARNING
     * or TYPE_DANGER
     * @param {string} [msg.messages] - The message to be displayed.
     * @param {Object} [msg.response] - The responce from a rest call will be parsed to extract the message
     * @param {boolean} [msg.escape] - If set to true the displayed message will be escaped. This is useful when
     * the message could contain unsafe strings.
     */
    addMessage (msg) {
        const logger = debug("forgerock:am:admin:view:message");
        const invalidMsg = !msg.message && !msg.response;
        if (invalidMsg) {
            logger("Expected object to contain a 'response' or 'message' parameter");
            return;
        }
        let i; const _this = Messages.messages;
        if (!msg.message && msg.response && typeof msg.response.responseJSON === "object" &&
            typeof msg.response.responseJSON.message === "string") {
            msg.message = msg.response.responseJSON.message;
        }
        if (!msg.message && msg.response && typeof msg.response.message === "string") {
            msg.message = msg.response.message;
        }
        for (i = 0; i < _this.list.length; i++) {
            if (_this.list[i].message === msg.message) {
                return;
            }
        }
        if (msg.escape !== false) {
            msg.message = _.escape(msg.message);
        }
        _this.list.push(msg);
        if (_this.list.length <= 1) {
            _this.showMessage(msg);
        }
    },

    nextMessage () {
        const _this = Messages.messages;
        _this.list.shift();
        if (_this.list.length > 0) {
            _this.showMessage();
        }
    },

    removeAndNext () {
        const _this = Messages.messages;
        window.clearTimeout(Messages.messages.timer);
        _this.$el.find("div").fadeOut(300, function () {
            $(this).remove();
            _this.nextMessage();
        });
    },

    hideMessages () {
        const _this = Messages.messages;
        window.clearTimeout(_this.timer);
        window.clearTimeout(_this.removeAndNext.timer);
        _this.$el.find("div").fadeOut(0, function () {
            $(this).remove();
            _this.list = [];
        });
    },

    showMessage () {
        const _this = this;
        let alertClass = "alert-info";
        let alertIcon = "alert-message-icon";
        const delay = _this.delay + (this.list[0].message.length * 20);

        switch (this.list[0].type) {
            case Messages.TYPE_DANGER:
                alertClass = "alert-danger";
                break;
            case Messages.TYPE_SUCCESS:
                alertClass = "alert-success";
                alertIcon = "fa-check-circle-o";
                break;
            case Messages.TYPE_WARNING:
                alertClass = "alert-warning";
                break;
            case Messages.TYPE_INFO:
                alertClass = "alert-info";
                break;
            default:
                alertClass = "alert-info";
                break;
        }

        if (this.hasNavigation()) {
            _this.$el.addClass("logged-user");
        } else {
            _this.$el.removeClass("logged-user");
        }

        this.$el.append(`<div role='alert' class='alert-system alert-message alert ${alertClass
        }'><i class='fa ${alertIcon}'></i><span class='message'>${this.list[0].message
        }</span></div>`);
        this.$el.find("div:last").fadeIn(300, () => {
            _this.timer = window.setTimeout(_this.removeAndNext, delay);
        });
    },

    clearMessages () {
        const _this = Messages.messages;
        if (_this.list.length > 1) {
            _this.list = [_this.list[1]];
        }
    },

    hasNavigation () {
        return $("#mainNavHolder").length > 0;
    }

});

Messages.messages = new MessagesView();

Messages.addMessage = function (msg) {
    Messages.messages.addMessage(msg);
};

Messages.hideMessages = function () {
    Messages.messages.hideMessages();
};
export default Messages;
