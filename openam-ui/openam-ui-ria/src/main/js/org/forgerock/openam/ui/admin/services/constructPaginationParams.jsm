/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/constructPaginationParams
 */

/**
 * Assembles the pagination query parameter string from the passed in pagination object returned by
 * the react-bootstrap-table.
 * @param {Object} [pagination] The pagination object
 * @param {number} pagination.sizePerPage The number of results per page.
 * @param {number} pagination.pagedResultsOffset The paged results offset.
 * @param {string} pagination.sortKey The sort key.
 * @param {string} pagination.sortDirection The sort direction.
 * @returns {string} Returns the pagination query string or empty string.
 */
const constructPaginationParams = (pagination) => {
    if (pagination) {
        const { sizePerPage, pagedResultsOffset, sortKey, sortDirection } = pagination;
        const pagedResultsOffsetParam = pagedResultsOffset ? `&_pagedResultsOffset=${pagedResultsOffset}` : "";
        const sizePerPageParam = `&_pageSize=${sizePerPage}`;
        const sortParams = sortKey && sortDirection ? `&_sortKeys=${encodeURIComponent(sortDirection)}${sortKey}` : "";

        return `${pagedResultsOffsetParam}${sizePerPageParam}${sortParams}`;
    } else {
        return "";
    }
};

export default constructPaginationParams;
