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

import { Badge } from "react-bootstrap";
import { t } from "i18next";
import { SortableElement } from "react-sortable-hoc";
import PropTypes from "prop-types";
import React from "react";

import ListItem from "components/inputs/list/sortable/components/ListItem";
import SortableList from "components/inputs/list/sortable/SortableList";

const Item = SortableElement(({ children, ...restProps }) => {
    const isActiveAlias = restProps.position === 0;
    const activeBadge = isActiveAlias
        ? <Badge pullRight>{ t("console.secretStores.edit.mappings.form.aliasField.active") }</Badge>
        : null;

    return (
        <ListItem { ...restProps } bsStyle={ isActiveAlias ? "success" : null }>
            { activeBadge }
            <div style={ { paddingRight: 50 } } >
                { children }
            </div>
        </ListItem>
    );
});

const CustomAliasesField = (props) => {
    const { formData, idSchema, onChange, schema, ...restProps } = props;
    const { items, minItems, uniqueItems } = schema;

    return (
        <SortableList
            components={ { Item } }
            id={ idSchema.$id }
            itemSchema={ items }
            label={ t("console.secretStores.edit.mappings.form.aliasField.label") }
            minItems={ minItems }
            onChange={ onChange }
            placeholder={ t("console.secretStores.edit.mappings.form.aliasField.placeholder") }
            uniqueItems={ uniqueItems }
            values={ formData }
            { ...restProps }
        />
    );
};

CustomAliasesField.defaultProps = {
    formData: []
};

CustomAliasesField.propTypes = {
    formData: PropTypes.arrayOf(PropTypes.any),
    idSchema: PropTypes.objectOf(PropTypes.any),
    onChange: PropTypes.func.isRequired,
    schema: PropTypes.shape({
        items: PropTypes.shape({
            minLength: PropTypes.number
        }).isRequired,
        minItems: PropTypes.number,
        uniqueItems: PropTypes.bool
    }).isRequired
};

export default CustomAliasesField;