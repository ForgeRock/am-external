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

import React, { Component, PropTypes } from "react";

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
