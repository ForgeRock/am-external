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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

import { Button, ButtonToolbar } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import AddButton from "components/AddButton";

const ListToolbar = ({
    additionalButtons,
    addButton,
    isDeleteDisabled,
    showExport = false,
    showImport = false,
    onDelete,
    onExport,
    onImport,
    numberSelected
}) => {
    const numberSelectedText = numberSelected ? `(${numberSelected})` : "";
    const exportButton = showExport && (
        <Button disabled={ numberSelected === 0 } onClick={ onExport }>
            <i className="fa fa-download" /> { t("common.form.export") }
        </Button>
    );
    const importButton = showImport && (
        <Button onClick={ onImport }>
            <i className="fa fa-upload" /> { t("common.form.import") }
        </Button>
    );

    return (
        <ButtonToolbar className="page-toolbar">
            <AddButton
                href={ addButton.href }
                onClick={ addButton.handleOnClick }
                title={ addButton.title }
            >
                { addButton.menuItems }
            </AddButton>
            { importButton }
            { exportButton }
            <Button disabled={ isDeleteDisabled } onClick={ onDelete }>
                <i className="fa fa-close" /> { t("common.form.delete") } { numberSelectedText }
            </Button>
            { additionalButtons }
        </ButtonToolbar>
    );
};

ListToolbar.propTypes = {
    addButton: PropTypes.shape({
        handleOnClick: PropTypes.func,
        href: PropTypes.string,
        menuItems: PropTypes.arrayOf(PropTypes.shape({
            href: PropTypes.string.isRequired,
            title: PropTypes.string.isRequired
        })),
        title: PropTypes.string.isRequired
    }).isRequired,
    additionalButtons: PropTypes.node,
    isDeleteDisabled: PropTypes.bool.isRequired,
    numberSelected: PropTypes.number.isRequired,
    onDelete: PropTypes.func.isRequired,
    onExport: PropTypes.func,
    onImport: PropTypes.func,
    showExport: PropTypes.bool,
    showImport: PropTypes.bool
};

export default ListToolbar;
