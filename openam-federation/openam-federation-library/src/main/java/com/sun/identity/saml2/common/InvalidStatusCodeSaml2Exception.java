/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.common;


/**
 * This class is an extension point for proxying saml2 firstlevel and secondlevel status code related exceptions.
 */
public class InvalidStatusCodeSaml2Exception extends SAML2Exception {
    private String firstlevelStatuscode;
    private String secondlevelStatuscode;

    /**
     * Constructs a new <code>InvalidStatusCodeSaml2Exception</code> without a nested <code>Throwable</code>.
     * @param firstlevelStatuscode SAML2 firstlevel status code can be one of the below
     * urn:oasis:names:tc:SAML:2.0:status:Success, urn:oasis:names:tc:SAML:2.0:status:Requester,
     * urn:oasis:names:tc:SAML:2.0:status:Responder and urn:oasis:names:tc:SAML:2.0:status:VersionMismatch
     * @param secondlevelStatuscode System entities are free to define more specific status codes by defining
     * appropriate URI references.
     *
     */
    public InvalidStatusCodeSaml2Exception(String firstlevelStatuscode, String secondlevelStatuscode){
        super(SAML2Utils.BUNDLE_NAME, "invalidStatusCodeInResponse", null);
        this.firstlevelStatuscode = firstlevelStatuscode;
        this.secondlevelStatuscode = secondlevelStatuscode;
    }

    /**
     * Returns first level status code.
     *
     * @return firstlevelStatuscode.
     */
    public String getFirstlevelStatuscode() {
        return firstlevelStatuscode;
    }

    /**
     * Returns second level status code.
     *
     * @return secondlevelStatuscode.
     */
    public String getSecondlevelStatuscode() {
        return secondlevelStatuscode;
    }
}

