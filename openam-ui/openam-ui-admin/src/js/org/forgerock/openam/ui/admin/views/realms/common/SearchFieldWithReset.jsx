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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { Button, ControlLabel, FormControl, FormGroup, InputGroup } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

const SearchFieldWithReset = ({ value, label, onClear, isResetEnabled, ...restProps }) => (
    <FormGroup className="am-search-field-with-reset" controlId="searchFieldWithReset">
        <ControlLabel srOnly>{ label }</ControlLabel>
        <InputGroup>
            <FormControl
                { ...restProps }
                value={ value }
            />
            <i className="fa fa-search" />
            <InputGroup.Button>
                <Button
                    disabled={ !isResetEnabled }
                    onClick={ onClear }
                >
                    <i className="fa fa-times" />
                </Button>
            </InputGroup.Button>
        </InputGroup>
    </FormGroup>
);

SearchFieldWithReset.defaultProps = {
    placeholder: t("common.form.filter"),
    isResetEnabled: false
};

SearchFieldWithReset.propTypes = {
    isResetEnabled: PropTypes.bool,
    label: PropTypes.string.isRequired,
    onBlur: PropTypes.func,
    onChange: PropTypes.func,
    onClear: PropTypes.func.isRequired,
    onKeyPress: PropTypes.func,
    placeholder: PropTypes.string,
    value: PropTypes.string.isRequired
};

export default SearchFieldWithReset;
