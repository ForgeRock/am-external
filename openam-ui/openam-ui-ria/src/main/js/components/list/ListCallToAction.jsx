/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

import AddButton from "components/AddButton";
import CallToAction from "components/CallToAction";

const ListCallToAction = ({ addButton, description, title }) => {
    return (
        <CallToAction>
            <p className="text-primary"><i className="fa fa-list-alt fa-4x" /></p>
            <h2>{ title }</h2>
            <p className="panel-description text-muted">{ description }</p>
            <AddButton
                href={ addButton.href }
                title={ addButton.title }
            >
                { addButton.menuItems }
            </AddButton>
        </CallToAction>
    );
};

ListCallToAction.propTypes = {
    addButton: PropTypes.objectOf({
        href: PropTypes.string,
        menuItems: PropTypes.arrayOf({
            href: PropTypes.string.isRequired,
            title: PropTypes.string.isRequired
        }),
        title: PropTypes.string.isRequired
    }).isRequired,
    description: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired
};

export default ListCallToAction;
