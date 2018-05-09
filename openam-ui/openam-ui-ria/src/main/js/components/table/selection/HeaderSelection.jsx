/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import PropTypes from "prop-types";
import React, { Component } from "react";

class HeaderSelection extends Component {
    constructor (props) {
        super(props);
        this.setRef = this.setRef.bind(this);
    }

    componentDidMount () {
        this.update(this.props.checked);
    }

    componentWillReceiveProps (props) {
        this.update(props.checked);
    }

    update (checked) {
        this.input.indeterminate = checked === "indeterminate";
    }

    setRef (input) {
        this.input = input;
    }

    render () {
        return (
            <input
                checked={ this.props.checked }
                className="react-bs-select-all"
                id={ `checkbox${this.props.rowIndex}` }
                name={ `checkbox${this.props.rowIndex}` }
                onChange={ this.props.onChange }
                ref={ this.setRef }
                type="checkbox"
            />
        );
    }
}

HeaderSelection.propTypes = {
    checked: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired,
    rowIndex: PropTypes.string.isRequired
};

export default HeaderSelection;
