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

import PropTypes from "prop-types";
import React from "react";

import SortableList from "components/inputs/list/sortable/SortableList";

const CustomArrayField = (props) => {
    const { formData, idSchema, schema, ...restProps } = props;
    const { items, minItems, uniqueItems, title } = schema;

    return (
        <SortableList
            id={ idSchema.$id }
            itemSchema={ items }
            label={ title }
            minItems={ minItems }
            uniqueItems={ uniqueItems }
            values={ formData }
            { ...restProps }
        />
    );
};

CustomArrayField.propTypes = {
    formData: PropTypes.arrayOf(PropTypes.any),
    idSchema: PropTypes.objectOf(PropTypes.any),
    onChange: PropTypes.func.isRequired,
    schema: PropTypes.shape({
        items: PropTypes.shape({
            minLength: PropTypes.number,
            properties: PropTypes.objectOf(PropTypes.any),
            type: PropTypes.string.isRequired
        }).isRequired,
        minItems: PropTypes.number,
        uniqueItems: PropTypes.bool,
        title: PropTypes.string.isRequired
    }).isRequired
};

export default CustomArrayField;
