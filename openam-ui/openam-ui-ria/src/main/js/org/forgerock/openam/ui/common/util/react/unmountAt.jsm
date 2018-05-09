/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import ReactDOM from "react-dom";

/**
 * Removes a mounted React component from the DOM and cleans up its event handlers and state.
 * @param {element} element The DOM element that contains the react component
 */
const unmountAt = (element) => {
    if (element) {
        ReactDOM.unmountComponentAtNode(element);
    }
};

export default unmountAt;
