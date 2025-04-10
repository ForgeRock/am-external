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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { filter, isEmpty, isString } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";
import Select, { components } from "react-select";

import defaultStyles from "./components/styles";
import SingleValue from "./components/SingleValue";
import Option from "./components/options/DefaultOption";

const DropdownIndicator = (props) => (
    components.DropdownIndicator && (
        <components.DropdownIndicator { ...props }>
            <i className="fa fa-search" />
        </components.DropdownIndicator>
    )
);

const searchAllStrings = ({ data }, rawInput) => {
    if (isEmpty(rawInput)) {
        return true;
    }

    const input = rawInput.toLowerCase();
    const filteredStrings = filter(data, (property) => {
        return isString(property) ? property.toLowerCase().includes(input) : false;
    });

    return !isEmpty(filteredStrings);
};

/*
 * This custom component wraps the default react-select v2 componet and add the "react-select-single-search" class name
 * which is then targeted within our functional tests. For more information @see https://react-select.com/components
 */
const SearchSingleSelect = ({ components, options, styles, ...restProps }) => (
    <Select
        backspaceRemovesValue
        className={ "react-select-single-search" }
        components={ { DropdownIndicator, Option, SingleValue, ...components } }
        filterOption={ searchAllStrings }
        isClearable
        options={ options }
        placeholder={ t("common.form.search") }
        styles={ {
            ...defaultStyles,
            ...styles
        } }
        { ...restProps }
    />
);

SearchSingleSelect.propTypes = {
    components: PropTypes.objectOf(PropTypes.func),
    inputId: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    options: PropTypes.arrayOf(PropTypes.any),
    styles: PropTypes.objectOf(PropTypes.any)
};

export default SearchSingleSelect;
