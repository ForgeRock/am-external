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
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";
import Select from "react-select";

import DropdownIndicator from "./components/DropdownIndicator";
import Option from "./components/options/DefaultOption";
import styles from "./components/styles";

/*
 * This custom component wraps the default react-select v2 componet and add the "react-select-multi" class name
 * which is then targeted within our functional tests. For more information @see https://react-select.com/components
 */
const MultiSelect = (props) => (
    <Select
        className={ "react-select-multi" }
        components={ { DropdownIndicator, Option } }
        isMulti
        placeholder={ t("common.form.select") }
        styles={ styles }
        { ...props }
    />
);

MultiSelect.propTypes = {
    inputId: PropTypes.string.isRequired, // Required for functional tests and accessibility
    onChange: PropTypes.func.isRequired
};

export default MultiSelect;