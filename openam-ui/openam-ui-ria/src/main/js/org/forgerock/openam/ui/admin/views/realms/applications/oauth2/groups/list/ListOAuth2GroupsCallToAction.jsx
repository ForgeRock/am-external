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
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 */

import React, { PropTypes } from "react";
import { Button } from "react-bootstrap";
import { t } from "i18next";

import CallToAction from "components/CallToAction";

const ListOAuth2GroupsCallToAction = ({ href }) => (
    <CallToAction>
        <p className="text-primary"><i className="fa fa-list-alt fa-4x" /></p>
        <h2>{ t("console.applications.oauth2.groups.list.callToAction.title") }</h2>
        <p className="panel-description text-muted">
            { t("console.applications.oauth2.groups.list.callToAction.description") }
        </p>
        <p>
            <Button bsStyle="primary" href={ href }>
                <i className="fa fa-plus" /> { t("console.applications.oauth2.groups.list.callToAction.button") }
            </Button>
        </p>
    </CallToAction>
);

ListOAuth2GroupsCallToAction.propTypes = {
    href: PropTypes.string.isRequired
};

export default ListOAuth2GroupsCallToAction;