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

const TitatoggleWidget = ({ onChange, id, value }) => {
    const handleOnChange = (event) => onChange(event.target.checked);

    return (
        // Disabled as structure is required for Ti-Ta-Toggle
        /* eslint-disable jsx-a11y/label-has-for */
        <div className="checkbox checkbox-slider-primary checkbox-slider checkbox-slider--b">
            <label>
                <input
                    checked={ value }
                    id={ id }
                    onChange={ handleOnChange }
                    type="checkbox"
                    value={ value }
                />
                <span /> {/*This span is required by titatoggle, for adding styles.*/}
            </label>
        </div>
    );
};

TitatoggleWidget.propTypes = {
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.bool.isRequired
};

export default TitatoggleWidget;
