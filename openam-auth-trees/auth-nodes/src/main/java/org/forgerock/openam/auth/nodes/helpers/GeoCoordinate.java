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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.helpers;

/**
 * Helper methods for Geo Coordinate.
 */
public final class GeoCoordinate {

    private GeoCoordinate() {
        // prevent instantiation
    }

    /**
     * Calculate the distance between 2 locations.
     *
     * @param fromLat The from Latitude
     * @param fromLon The from Longitude
     * @param toLat   The to Latitude
     * @param toLon   The to Longitude
     * @return The distance between 2 location in km
     */
    public static double distance(double fromLat, double fromLon, double toLat, double toLon) {
        if ((fromLat == toLat) && (fromLon == toLon)) {
            return 0;
        } else {
            double diffLon = fromLon - toLon;
            double dist = Math.sin(Math.toRadians(fromLat)) * Math.sin(Math.toRadians(toLat))
                    + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat)) * Math
                    .cos(Math.toRadians(diffLon));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            return dist * 1.609344;
        }
    }
}
