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
 * Copyright 2018 ForgeRock AS.
 */
import { createAction, handleActions } from "redux-actions";

import throwOnEmpty from "../../action/payload/creators/throwOnEmpty";
import throwOnNotArray from "../../action/payload/creators/throwOnNotArray";

const arrayMultiType = (branch) => {
    // Types
    const ADD_OR_UPDATE = `${branch}/ADD_OR_UPDATE`;
    const REMOVE = `${branch}/REMOVE`;
    const SET = `${branch}/SET`;

    // Actions
    const addOrUpdate = createAction(ADD_OR_UPDATE, throwOnEmpty);
    const remove = createAction(REMOVE, throwOnEmpty);
    const set = createAction(SET, throwOnNotArray);

    // Selectors
    const withCompositeId = (state) => state.map((instance) => ({
        ...instance,
        _compositeId: `${instance._id}${instance._type._id}`
    }));

    // Reducer
    const initialState = [];
    const reducer = handleActions({
        [ADD_OR_UPDATE]: (state, action) => {
            const newState = state.filter((instance) => {
                return instance._id !== action.payload._id || instance._type._id !== action.payload._type._id;
            });
            newState.push(action.payload);
            return newState;
        },
        [REMOVE]: (state, action) =>
            state.filter((instance) => `${instance._id}${instance._type._id}` !== action.payload),
        [SET]: (state, action) => action.payload
    }, initialState);

    return {
        addOrUpdate,
        reducer,
        remove,
        set,
        withCompositeId
    };
};

export default arrayMultiType;
