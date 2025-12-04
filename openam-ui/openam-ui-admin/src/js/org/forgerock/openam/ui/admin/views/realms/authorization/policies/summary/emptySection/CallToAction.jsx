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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import { Button } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

class CallToAction extends Component {
    handleCallToActionClick = () => {
        this.props.onClick(this.props.sectionName);
    };

    render () {
        return (
            <div className="text-center">
                <Button bsStyle="link" onClick={ this.handleCallToActionClick }>
                    <i className="fa fa-plus" /> { t("common.form.addItem", { item: this.props.sectionName }) }
                </Button>
            </div>
        );
    }
}

CallToAction.propTypes = {
    onClick: PropTypes.func.isRequired,
    sectionName: PropTypes.string.isRequired
};

export default CallToAction;
