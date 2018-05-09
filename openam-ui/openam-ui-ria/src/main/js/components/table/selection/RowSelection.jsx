/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

import HeaderSelection from "./HeaderSelection";

/**
* Table row selection component.
* @param {object} props Object properties passed to the component
* @see https://github.com/AllenFang/react-bootstrap-table/blob/master/examples/js/selection/custom-multi-select-table.js
* @returns {object} react row selection component
*/
const RowSelection = (props) => {
    const { checked, disabled, onChange, rowIndex } = props;
    const onSelect = (event) => { onChange(event, rowIndex); };

    if (rowIndex === "Header") {
        return (
            <div className="checkbox">
                <HeaderSelection { ...props } />
                <label htmlFor={ `checkbox${rowIndex}` } />
            </div>
        );
    } else {
        return (
            <div className="checkbox">
                <input
                    checked={ checked }
                    disabled={ disabled }
                    id={ `checkbox${rowIndex}` }
                    name={ `checkbox${rowIndex}` }
                    onChange={ onSelect }
                    type="checkbox"
                />
                <label htmlFor={ `checkbox${rowIndex}` } />
            </div>
        );
    }
};

RowSelection.propTypes = {
    checked: PropTypes.bool.isRequired,
    disabled: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired,
    rowIndex: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
    ])
};

export default RowSelection;
