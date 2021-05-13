/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Utils.java,v 1.9 2008/11/10 22:56:59 veiming Exp $
 *
 * Portions Copyright 2013-2019 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.soapbinding;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.forgerock.openam.annotations.Supported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sun.identity.shared.locale.Locale;

/**
 * This class contains utility methods.
 *
 */
@Supported

public class Utils {

    private static Logger debug;
    public static ResourceBundle bundle;

    static {
        bundle = Locale.getInstallResourceBundle("libSOAPBinding");
        debug = LoggerFactory.getLogger(Utils.class);
    }

    /**
     * Converts a value of XML boolean type to Boolean object.
     *
     * @param str a value of XML boolean type
     * @return a Boolean object
     * @throws Exception if there is a syntax error
     */
    @Supported
    public static Boolean StringToBoolean(String str) throws Exception {
        if (str == null) {
            return null;
        }
        
        if (str.equals("true") || str.equals("1")) {
            return Boolean.TRUE;
        }
        
        if (str.equals("false") || str.equals("0")) {
            return Boolean.FALSE;
        }
        
        throw new Exception();
    }

    /**
     * Converts a Boolean object to a String representing XML boolean.
     *
     * @param bool a Boolean object.
     * @return a String representing the boolean value.
     */
    @Supported
    public static String BooleanToString(Boolean bool) {
        if (bool == null) {
            return "";
        }
        
        return bool.booleanValue() ? "1" : "0";
    }

    /**
     * Converts a string value to a QName. The prefix of the string value
     * is resolved to a namespace relative to the element.
     *
     * @param str the String to be converted.
     * @param element the Element object.
     * @return the QName Object.
     */
    @Supported
    public static QName convertStringToQName(String str,Element element) {
        if (str == null) {
            return null;
        }
        
        String prefix = "";
        String localPart;
        int index = str.indexOf(":");
        if (index == -1) {
            localPart = str;
        } else {
            prefix = str.substring(0, index);
            localPart = str.substring(index + 1);
        }
        String ns = getNamespaceForPrefix(prefix, element);
        return new QName(ns, localPart);
    }


    /**
     *  Gets the XML namespace URI that is mapped to the specified prefix, in
     *  the context of the DOM element e
     *
     * @param  prefix  The namespace prefix to map
     * @param  e       The DOM element in which to calculate the prefix binding
     * @return         The XML namespace URI mapped to prefix in the context of e
     */
    public static String getNamespaceForPrefix(String prefix, Element e) {
        return e.lookupNamespaceURI(prefix);
    }
    
    /**
     * Gets localized string from resource bundle.
     *
     * @param key a key to a resource bundle
     * @param params parameters to MessageFormat
     * @return a localized string.
     */
    @Supported
    public static String getString(String key, Object[] params) {
        return MessageFormat.format(bundle.getString(key), params);
    }
}
