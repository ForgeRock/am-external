/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

require("chai/register-expect");
require("regenerator-runtime/runtime");

const chai = require("chai");
const sinon = require("sinon");
const sinonChai = require("sinon-chai");
const sinonTest = require("sinon-test");

sinon.test = sinonTest(sinon);

chai.use(sinonChai);

const testsContext = require.context(".", true, /Test.js$/);
testsContext.keys().forEach(testsContext);
