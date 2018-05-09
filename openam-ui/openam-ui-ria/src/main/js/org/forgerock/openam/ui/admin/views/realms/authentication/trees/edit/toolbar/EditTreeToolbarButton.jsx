/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button } from "react-bootstrap";
import classnames from "classnames";
import PropTypes from "prop-types";
import React from "react";

const EditTreeToolbarButton = (props) => {
    const { className, title, ...restProps } = props;
    return (
        <Button
            { ...restProps }
            className={ classnames(className, {
                "fr-btn-secondary": true
            }) }
            data-title={ title }
        />
    );
};

EditTreeToolbarButton.propTypes = {
    className: PropTypes.string,
    title: PropTypes.string // required by tests
};

export default EditTreeToolbarButton;
