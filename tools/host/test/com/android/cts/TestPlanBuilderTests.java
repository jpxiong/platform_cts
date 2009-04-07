/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

/**
 * Test the test plan builder.
 *
 */
public class TestPlanBuilderTests extends CtsTestBase {
    public static boolean DEBUG = false;

    private String mTestPackageBinaryName = "CtsTestPackage";

    /** {@inheritDoc} */
    @Override
    public void tearDown() {
        HostConfig.getInstance().removeTestPacakges();
        deleteTestPackage(mTestPackageBinaryName);

        super.tearDown();
    }

    /**
     * Test getting test suite name list contained within a test package.
     */
    public void testGetTestSuiteNames() throws IOException, NoSuchAlgorithmException {
        final String appPackageName = "com.google.android.cts";
        final String fullName = mTestPackageBinaryName + ".CtsTest";
        final String caseName = "CtsTestHello";
        final String testName = "testHello";

        final String descriptionConfigStr = "<TestPackage name=\"" + mTestPackageBinaryName + "\""
                + " appPackageName=\"" + appPackageName + "\""
                + " version=\"1.0\" AndroidFramework=\"Android 1.0\""
                + " runner=\"android.test.InstrumentationTestRunner\" >\n"
                + "  <Description>something extracted from java doc</Description>\n"
                + "  <TestSuite name=\"com.google\">\n"
                + "     <TestCase name=\"CtsTestHello\" priority=\"mandatory\">\n"
                + "         <Description>" + "something extracted from java doc"
                + "         </Description>\n"
                + "         <!-- Test Methods -->\n"
                + "         <Test method=\"testHello\"" + " type=\"automatic\"" + "/>\n"
                + "     </TestCase>\n"
                + "     <TestSuite name=\"TestSuiteName\">\n"
                + "         <TestCase name=\"TestCaseName\" priority=\"mandatory\">\n"
                + "             <Description>" + "something extracted from java doc"
                + "             </Description>\n"
                + "             <!-- Test Methods -->\n"
                + "             <Test method=\"testName1\"" + " type=\"automatic\"" + "/>\n"
                + "         </TestCase>\n"
                + "     </TestSuite>\n"
                + "  </TestSuite>\n"
                + "</TestPackage>\n";

        HostConfig.getInstance().removeTestPacakges();
        createTestPackage(descriptionConfigStr, mTestPackageBinaryName);

        HostConfig.getInstance().loadTestPackages();
        Collection<TestPackage> testPackages = HostConfig.getInstance().getTestPackages();
        assertEquals(1, testPackages.size());
        TestPackage testPackage = testPackages.iterator().next();

        String suiteFullName = "com.google";
        TestSuite suite = testPackage.getTestSuiteByName(suiteFullName);
        assertEquals(suiteFullName, suite.getFullName());

        String caseFullName = "com.google.CtsTestHello";
        TestCase cazz = testPackage.getTestCaseByName(caseFullName);
        assertEquals(caseFullName, cazz.getFullName());

        List<String> testNames = testPackage.getAllTestNames(caseFullName);
        String testFullName = "com.google.CtsTestHello#testHello";
        assertEquals(1, testNames.size());
        assertEquals(testFullName, testNames.get(0));

        suiteFullName = "com.google.TestSuiteName";
        suite = testPackage.getTestSuiteByName(suiteFullName);
        assertEquals(suiteFullName, suite.getFullName());

        caseFullName = "com.google.TestSuiteName.TestCaseName";
        cazz = testPackage.getTestCaseByName(caseFullName);
        assertEquals(caseFullName, cazz.getFullName());

        testNames = testPackage.getAllTestNames(caseFullName);
        testFullName = "com.google.TestSuiteName.TestCaseName#testName1";
        assertEquals(1, testNames.size());
        assertEquals(testFullName, testNames.get(0));
    }

    /**
     * Test building test plan.
     */
    public void testPlanBuilder() throws IOException,
            ParserConfigurationException,
            TransformerFactoryConfigurationError, TransformerException, NoSuchAlgorithmException {

        if (DEBUG) {
            final String appPackageName = "com.google";
            final String suiteName1 = "com.google.SuiteName1";
            final String caseName1 = "CtsTestHello";
            final String testName1 = "testHello";

            final String cName2 = "CtsTestHello2";
            final String testName2 = "testHello2";
            final String testName3 = "testHello3";
            final String testName4 = "testHello4";
            final String testName5 = "testHello5";
            final String nestedSuiteName1 = "com.google";
            final String nestedSuiteName2 = "CtsTest.SuiteName2";

            final String descriptionConfigStr =
                    "<TestPackage name=\"" + mTestPackageBinaryName + "\""
                    + " appPackageName=\"" + appPackageName + "\""
                    + " version=\"1.0\" AndroidFramework=\"Android 1.0\""
                    + " runner=\"android.test.InstrumentationTestRunner\" >\n"
                    + " <Description>something extracted from java doc</Description>\n"
                    + " <TestSuite name=\"" + suiteName1 + "\"" + ">\n"
                    + "     <TestCase name=\"" + caseName1 + "\"" + " category=\"mandatory\">\n"
                    + "         <Description>" + "something extracted from java doc"
                    + "         </Description>\n"
                    + "         <!-- Test Methods -->\n"
                    + "         <Test method=\"" + testName1 + "\" type=\"automatic\"" + "/>\n"
                    + "     </TestCase>\n"
                    + " </TestSuite>\n"
                    + " <TestSuite name=\"" + nestedSuiteName1+ "\"" + ">\n"
                    + "     <TestSuite name=\"" + nestedSuiteName2+ "\"" + ">\n"
                    + "         <TestCase name=\"" + cName2 + "\"" + " priority=\"mandatory\">\n"
                    + "             <Test method=\"" + testName2 +"\" type=\"automatic\" />\n"
                    + "             <Test method=\"" + testName3 +"\" type=\"automatic\" />\n"
                    + "             <Test method=\"" + testName4 +"\" type=\"automatic\" />\n"
                    + "             <Test method=\"" + testName5 +"\" type=\"automatic\" />\n"
                    + "         </TestCase>\n"
                    + "     </TestSuite>\n"
                    + " </TestSuite>\n"
                    + "</TestPackage>\n";

            createTestPackage(descriptionConfigStr, mTestPackageBinaryName);
            HostConfig.getInstance().loadTestPackages();
            ArrayList<String> packageNames = new ArrayList<String>();
            packageNames.add(appPackageName);
            PlanBuilder planBuilder = new PlanBuilder(packageNames);
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            planBuilder.setInputStream(in);
            HashMap<String, ArrayList<String>> results = planBuilder.doSelect();
            String planName = "plan_test";
            if (results != null) {
                Log.i("result=" + results.toString());
                ArrayList<String> excludedList = results.get(appPackageName);
                if (excludedList != null) {
                    for(String str : excludedList) {
                        Log.i(str);
                    }
                } else {
                    Log.i("Nothing is excluded at all!");
                }

                String planPath =
                              HostConfig.getInstance().getPlanRepository().getPlanPath(planName);
                TestSessionBuilder.getInstance().serialize(planName, packageNames, results);
                Log.i("planPath=" + planPath);
            } else {
                Log.i("Selected nothing for the plan of "
                        + planName + ". The plan is not created!");
            }
        }
    }
}
