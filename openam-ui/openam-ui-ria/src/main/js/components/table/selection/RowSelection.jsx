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

import React, { PropTypes } from "react";

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
    rowIndex: React.PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number
    ])
};

export default RowSelection;
