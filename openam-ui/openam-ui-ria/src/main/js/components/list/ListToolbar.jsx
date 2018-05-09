/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, ButtonToolbar } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import AddButton from "components/AddButton";

const ListToolbar = ({ additionalButtons, addButton, isDeleteDisabled, onDelete, numberSelected }) => {
    const numberSelectedText = numberSelected ? `(${numberSelected})` : "";
    return (
        <ButtonToolbar className="page-toolbar">
            <AddButton
                href={ addButton.href }
                title={ addButton.title }
            >
                { addButton.menuItems }
            </AddButton>
            <Button disabled={ isDeleteDisabled } onClick={ onDelete }>
                <i className="fa fa-close" /> { t("common.form.delete") } { numberSelectedText }
            </Button>
            { additionalButtons }
        </ButtonToolbar>
    );
};

ListToolbar.propTypes = {
    addButton: PropTypes.objectOf({
        href: PropTypes.string,
        menuItems: PropTypes.arrayOf({
            href: PropTypes.string.isRequired,
            title: PropTypes.string.isRequired
        }),
        title: PropTypes.string.isRequired
    }).isRequired,
    additionalButtons: PropTypes.node,
    isDeleteDisabled: PropTypes.bool.isRequired,
    numberSelected: PropTypes.number.isRequired,
    onDelete: PropTypes.func.isRequired
};

export default ListToolbar;
