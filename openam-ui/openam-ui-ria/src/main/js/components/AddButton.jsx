/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, ButtonGroup, DropdownButton, MenuItem } from "react-bootstrap";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React from "react";

const AddButton = ({ children, href, title }) => {
    if (children) {
        const menuItems = map(children, (item) => (
            <MenuItem href={ item.href }>
                { item.title }
            </MenuItem>
        ));
        return (
            <ButtonGroup>
                <DropdownButton
                    bsStyle="primary"
                    disabled={ isEmpty(menuItems) }
                    title={ title }
                >
                    { menuItems }
                </DropdownButton>
            </ButtonGroup>
        );
    } else {
        return (
            <ButtonGroup>
                <Button
                    bsStyle="primary"
                    href={ href }
                >
                    <i className="fa fa-plus" /> { title }
                </Button>
            </ButtonGroup>
        );
    }
};

AddButton.propTypes = {
    children: PropTypes.arrayOf({
        href: PropTypes.string.isRequired,
        title: PropTypes.string.isRequired
    }),
    href: PropTypes.string,
    title: PropTypes.string.isRequired
};

export default AddButton;
