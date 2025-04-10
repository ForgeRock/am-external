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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import PropTypes from "prop-types";
import React from "react";
import { Button } from "react-bootstrap";
import { t } from "i18next";

import AddButton from "components/AddButton";
import CallToAction from "components/CallToAction";

const ListCallToAction = ({ addButton, description, title, showImport = false, onImport }) => {
    const importButton = showImport && (
        <Button
            bsStyle="primary"
            className="ml-2"
            onClick={ onImport }
        >
            <i className="fa fa-upload" /> { t("common.form.import") }
        </Button>
    );
    return (
        <CallToAction>
            <p className="text-primary"><i className="fa fa-list-alt fa-4x" /></p>
            <h2>{ title }</h2>
            <p className="panel-description text-muted">{ description }</p>
            <AddButton
                href={ addButton.href }
                onClick={ addButton.handleOnClick }
                title={ addButton.title }
            >
                { addButton.menuItems }
            </AddButton>
            { importButton }
        </CallToAction>
    );
};

ListCallToAction.propTypes = {
    addButton: PropTypes.shape({
        handleOnClick: PropTypes.func,
        href: PropTypes.string,
        menuItems: PropTypes.arrayOf(PropTypes.shape({
            href: PropTypes.string.isRequired,
            title: PropTypes.string.isRequired
        })),
        title: PropTypes.string.isRequired
    }).isRequired,
    description: PropTypes.string.isRequired,
    onImport: PropTypes.func,
    showImport: PropTypes.bool,
    title: PropTypes.string.isRequired
};

export default ListCallToAction;
