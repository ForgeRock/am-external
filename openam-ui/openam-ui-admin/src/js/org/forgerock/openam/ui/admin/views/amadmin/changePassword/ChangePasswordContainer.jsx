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
 * Copyright 2019 ForgeRock AS.
 */

import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { updatePassword } from "org/forgerock/openam/ui/admin/services/amadmin/AmadminService";
import ChangePasswordModal from "./ChangePasswordModal";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

class ChangePasswordContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            showModal: true,
            isFormEnabled: true
        };
    }

    handleClose = () => {
        this.setState({ showModal: false });
    };

    handleSave = async ({ olduserpassword, userpassword }) => {
        this.setState({ isFormEnabled: false });
        try {
            await updatePassword({ olduserpassword, userpassword });
            this.setState({ showModal: false });
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        } catch (error) {
            this.setState({ isFormEnabled: true });
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    };

    validate ({ userpassword, confirmationPassword }, errors) {
        if (confirmationPassword && userpassword !== confirmationPassword) {
            errors.confirmationPassword.addError(t("common.form.validation.confirmationMatchesPassword"));
        }
        return errors;
    }

    render () {
        const schema = {
            "$schema": "http://json-schema.org/draft-07/schema#",
            type: "object",
            properties: {
                olduserpassword: {
                    title: t("common.user.currentPassword"),
                    propertyOrder: 1,
                    type: "string",
                    _isPassword: true
                },
                userpassword: {
                    title: t("common.user.newPassword"),
                    minLength: 8,
                    propertyOrder: 2,
                    type: "string",
                    _isPassword: true
                },
                confirmationPassword: {
                    title: t("common.user.confirmNewPassword"),
                    propertyOrder: 3,
                    type: "string",
                    _isPassword: true
                }
            },
            required: ["olduserpassword", "userpassword", "confirmationPassword"]
        };

        return (
            <ChangePasswordModal
                isFormEnabled={ this.state.isFormEnabled }
                onClose={ this.handleClose }
                onExited={ this.props.onExited }
                onSave={ this.handleSave }
                schema={ schema }
                show={ this.state.showModal }
                validate={ this.validate }
                { ...this.props }
            />
        );
    }
}

ChangePasswordContainer.propTypes = {
    onExited: PropTypes.func.isRequired
};

export default ChangePasswordContainer;
