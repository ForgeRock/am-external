/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.meta;

import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public class SAML2MetaUtilsTest {
    
    private static final String PATH_SEPARATOR = "/";
    private static final String TEST_ENTITY = "someEntity";
    private static final String PREFIX = PATH_SEPARATOR + "abcdefg";
    private static final String TEST_SUB_REALM = "subsub";
    
    public SAML2MetaUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    @Test
    public void testGetMetaDataByURI_DefaultRealm() {
        final String uri = PREFIX 
                + PATH_SEPARATOR + SAML2MetaManager.NAME_META_ALIAS_IN_URI
                + PATH_SEPARATOR + TEST_ENTITY;
        final String result = SAML2MetaUtils.getMetaAliasByUri(uri);
        assertEquals(result, PATH_SEPARATOR + TEST_ENTITY);
    }
    
    @Test
    public void testGetMetaDataByURI_SubRealm() {
        final String uri = PREFIX 
                + PATH_SEPARATOR + SAML2MetaManager.NAME_META_ALIAS_IN_URI 
                + PATH_SEPARATOR + TEST_SUB_REALM 
                + PATH_SEPARATOR +  TEST_ENTITY;
        final String result = SAML2MetaUtils.getMetaAliasByUri(uri);
        assertEquals(result, PATH_SEPARATOR + TEST_SUB_REALM + PATH_SEPARATOR + TEST_ENTITY);        
    }    
    
}
