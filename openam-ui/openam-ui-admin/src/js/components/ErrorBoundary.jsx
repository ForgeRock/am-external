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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { t } from "i18next";
import debug from "debug";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

/**
 * Error boundary component.
 * Displays a generic error message when an error is thrown from a child component.
 */
class ErrorBoundary extends Component {
    static propTypes = {
        children: PropTypes.node
    };
    state = { hasError: false };

    componentDidCatch (error) {
        this.setState({ hasError: true });

        const logger = debug("forgerock:am:admin:view");
        logger(`Error boundary component caught a view error. ${error}`);
    }

    render () {
        if (this.state.hasError) {
            return (
                <Fragment>
                    <p className="text-center text-primary"><i className="fa fa-frown-o fa-4x" /></p>
                    <h4 className="text-center">{ t("console.common.error.oops") }</h4>
                </Fragment>
            );
        } else {
            return (
                <Fragment>
                    { this.props.children }
                </Fragment>
            );
        }
    }
}

export default ErrorBoundary;
