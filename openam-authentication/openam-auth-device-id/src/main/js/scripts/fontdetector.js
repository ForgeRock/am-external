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
var fontDetector = (function () {
    /**
     * JavaScript code to detect available availability of a
     * particular font in a browser using JavaScript and CSS.
     *
     * Author : Lalit Patel
     * Website: http://www.lalit.org/lab/javascript-css-font-detect/
     * License: Apache Software License 2.0
     *          http://www.apache.org/licenses/LICENSE-2.0
     * Version: 0.15 (21 Sep 2009)
     *          Changed comparision font to default from sans-default-default,
     *          as in FF3.0 font of child element didn't fallback
     *          to parent element if the font is missing.
     * Version: 0.2 (04 Mar 2012)
     *          Comparing font against all the 3 generic font families ie,
     *          'monospace', 'sans-serif' and 'sans'. If it doesn't match all 3
     *          then that font is 100% not available in the system
     * Version: 0.3 (24 Mar 2012)
     *          Replaced sans with serif in the list of baseFonts
     */
    /*
     * Portions Copyrighted 2013-2025 Ping Identity Corporation.
     */
    var detector = {}, baseFonts, testString, testSize, h, s, defaultWidth = {}, defaultHeight = {}, index;

    // a font will be compared against all the three default fonts.
    // and if it doesn't match all 3 then that font is not available.
    baseFonts = ['monospace', 'sans-serif', 'serif'];

    //we use m or w because these two characters take up the maximum width.
    // And we use a LLi so that the same matching fonts can get separated
    testString = "mmmmmmmmmmlli";

    //we test using 72px font size, we may use any size. I guess larger the better.
    testSize = '72px';

    h = document.getElementsByTagName("body")[0];

    // create a SPAN in the document to get the width of the text we use to test
    s = document.createElement("span");
    s.style.fontSize = testSize;
    s.innerHTML = testString;
    for (index in baseFonts) {
        //get the default width for the three base fonts
        s.style.fontFamily = baseFonts[index];
        h.appendChild(s);
        defaultWidth[baseFonts[index]] = s.offsetWidth; //width for the default font
        defaultHeight[baseFonts[index]] = s.offsetHeight; //height for the defualt font
        h.removeChild(s);
    }

    detector.detect = function(font) {
        var detected = false, index, matched;
        for (index in baseFonts) {
            s.style.fontFamily = font + ',' + baseFonts[index]; // name of the font along with the base font for fallback.
            h.appendChild(s);
            matched = (s.offsetWidth !== defaultWidth[baseFonts[index]] || s.offsetHeight !== defaultHeight[baseFonts[index]]);
            h.removeChild(s);
            detected = detected || matched;
        }
        return detected;
    };

    return detector;
}());
