/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { HelpBlock } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

const HtmlFormattedHelpBlock = ({ children, ...restProps }) => {
    return (
        <HelpBlock { ...restProps }>
            <small
                dangerouslySetInnerHTML={ // eslint-disable-line react/no-danger
                    {
                        __html: children
                    }
                }
            />
        </HelpBlock>
    );
};

HtmlFormattedHelpBlock.propTypes = {
    children: PropTypes.string.isRequired
};

export default HtmlFormattedHelpBlock;
