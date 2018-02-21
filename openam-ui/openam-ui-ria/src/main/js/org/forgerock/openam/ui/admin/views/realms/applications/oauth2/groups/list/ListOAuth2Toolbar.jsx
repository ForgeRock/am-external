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
 * Copyright 2017 ForgeRock AS.
 */

import { Button, ButtonToolbar } from "react-bootstrap";
import { t } from "i18next";
import React, { PropTypes } from "react";

const ListOAuth2Toolbar = ({ isDeleteDisabled, onDelete, newHref, numberSelected }) => (
    <ButtonToolbar className="page-toolbar">
        <Button bsStyle="primary" href={ newHref }>
            <i className="fa fa-plus" /> { t("console.applications.oauth2.groups.list.callToAction.button") }
        </Button>

        <Button disabled={ isDeleteDisabled } onClick={ onDelete }>
            <i className="fa fa-close" /> { t("common.form.delete") } { numberSelected ? `(${numberSelected})` : "" }
        </Button>
    </ButtonToolbar>
);

ListOAuth2Toolbar.propTypes = {
    isDeleteDisabled: PropTypes.bool.isRequired,
    newHref: PropTypes.string.isRequired,
    numberSelected: PropTypes.number.isRequired,
    onDelete: PropTypes.func.isRequired
};

export default ListOAuth2Toolbar;
