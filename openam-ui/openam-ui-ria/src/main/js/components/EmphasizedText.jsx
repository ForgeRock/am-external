/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { map } from "lodash";
import PropTypes from "prop-types";
import React from "react";

/**
 * This function creates an array of snippets, with the odd array snippets being the ones with matched characters,
 * and the even snippets being the characters inbetween. The matched snippets are then wrapped in the <strong> element.
 * @param {string} children The string to which the emphasized text will be applied.
 * @param {string} match The characters of the string to emphasize.
 * @returns {Array<String|ReactElement>} An array of alternating strings and react elements.
 * @example
 * Given the string "/applications",
 *      a match of "a" will return the snippet array ["/", "a", "pplic", "a", "tions"]
 *      a match of "/AP" will return the snippet array ["", "/ap", "plications"]
 */
function emphasizeMatchingText (children, match) {
    const isOdd = (number) => (number % 2) === 1;
    const snippets = children.split(new RegExp(`(${match})`, "gi"));
    return map(snippets, (snippet, index) => {
        return isOdd(index) ? <strong>{snippet}</strong> : snippet;
    });
}

const EmphasizedText = ({ children, match }) => {
    if (match) {
        return <span>{ emphasizeMatchingText(children, match) }</span>;
    } else {
        return <span>{ children }</span>;
    }
};

EmphasizedText.propTypes = {
    children: PropTypes.string.isRequired,
    match: PropTypes.string
};

export default EmphasizedText;
