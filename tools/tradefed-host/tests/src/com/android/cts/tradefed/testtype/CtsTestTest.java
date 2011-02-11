/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.cts.tradefed.testtype;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import org.easymock.EasyMock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

/**
 * Unit tests for {@link CtsTest}.
 */
public class CtsTestTest extends TestCase {

    private static final String PACKAGE_NAME = "test-uri";
    /** the test fixture under test, with all external dependencies mocked out */
    private CtsTest mCtsTest;
    private ITestCaseRepo mMockRepo;
    private IPlanXmlParser mMockPlanParser;
    private ITestDevice mMockDevice;
    private ITestInvocationListener mMockListener;

    private static final String PLAN_NAME = "CTS";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMockRepo = EasyMock.createMock(ITestCaseRepo.class);
        mMockPlanParser = EasyMock.createMock(IPlanXmlParser.class);
        mMockDevice = EasyMock.createMock(ITestDevice.class);
        mMockListener = EasyMock.createNiceMock(ITestInvocationListener.class);

        mCtsTest = new CtsTest() {
            @Override
            ITestCaseRepo createTestCaseRepo() {
                return mMockRepo;
            }

            @Override
            IPlanXmlParser createXmlParser() {
                return mMockPlanParser;
            }

            @Override
            InputStream createXmlStream(File xmlFile) throws FileNotFoundException {
                // return empty stream, not used
                return new ByteArrayInputStream(new byte[0]);
            }
        };
        mCtsTest.setDevice(mMockDevice);
        // not used, but needs to be non-null
        mCtsTest.setTestCaseDir(new File("tmp"));
        mCtsTest.setTestPlanDir(new File("tmp"));
        // turn off device collection for simplicity
        mCtsTest.setCollectDeviceInfo(false);
    }

    /**
     * Test normal case {@link CtsTest#run(java.util.List)} when running a plan.
     */
    @SuppressWarnings("unchecked")
    public void testRun_plan() throws DeviceNotAvailableException, ParseException {
        setParsePlanExceptations();

        ITestPackageDef mockPackageDef = EasyMock.createMock(ITestPackageDef.class);
        IRemoteTest mockTest = EasyMock.createMock(IRemoteTest.class);
        EasyMock.expect(mMockRepo.getTestPackage(PACKAGE_NAME)).andReturn(mockPackageDef);
        EasyMock.expect(mockPackageDef.createTest((File)EasyMock.anyObject(),
                (String)EasyMock.anyObject(), (String)EasyMock.anyObject())).andReturn(mockTest);
        mockTest.run((ITestInvocationListener)EasyMock.anyObject());

        replayMocks(mockTest, mockPackageDef);
        mCtsTest.run(mMockListener);
        verifyMocks(mockTest, mockPackageDef);
    }

    /**
     * Test normal case {@link CtsTest#run(java.util.List)} when running a package.
     */
    @SuppressWarnings("unchecked")
    public void testRun_package() throws DeviceNotAvailableException {
        mCtsTest.addPackageName(PACKAGE_NAME);
        ITestPackageDef mockPackageDef = EasyMock.createMock(ITestPackageDef.class);
        IRemoteTest mockTest = EasyMock.createMock(IRemoteTest.class);
        EasyMock.expect(mMockRepo.getTestPackage(PACKAGE_NAME)).andReturn(mockPackageDef);
        EasyMock.expect(mockPackageDef.createTest((File)EasyMock.anyObject(),
                (String)EasyMock.anyObject(), (String)EasyMock.anyObject())).andReturn(mockTest);
        mockTest.run((ITestInvocationListener)EasyMock.anyObject());

        replayMocks(mockTest, mockPackageDef);
        mCtsTest.run(mMockListener);
        verifyMocks(mockTest, mockPackageDef);
    }

    /**
     * Test normal case {@link CtsTest#run(java.util.List)} when running a class.
     */
    @SuppressWarnings("unchecked")
    public void testRun_class() throws DeviceNotAvailableException {
        final String className = "className";
        final String methodName = "methodName";
        mCtsTest.setClassName(className);
        mCtsTest.setMethodName(methodName);


        EasyMock.expect(mMockRepo.findPackageForTest(className)).andReturn(PACKAGE_NAME);
        ITestPackageDef mockPackageDef = EasyMock.createMock(ITestPackageDef.class);
        EasyMock.expect(mMockRepo.getTestPackage(PACKAGE_NAME)).andReturn(mockPackageDef);
        IRemoteTest mockTest = EasyMock.createMock(IRemoteTest.class);
        EasyMock.expect(mockPackageDef.createTest((File)EasyMock.anyObject(),
                EasyMock.eq(className), EasyMock.eq(methodName))).andReturn(mockTest);
        mockTest.run((ITestInvocationListener)EasyMock.anyObject());

        replayMocks(mockTest, mockPackageDef);
        mCtsTest.run(mMockListener);
        verifyMocks(mockTest, mockPackageDef);
    }

    /**
     * Test {@link CtsTest#run(java.util.List)} when --excluded-package is specified
     */
    public void testRun_excludedPackage() throws DeviceNotAvailableException, ParseException {
        setParsePlanExceptations();

        mCtsTest.addExcludedPackageName(PACKAGE_NAME);

        // PACKAGE_NAME would normally be run, but it has been excluded. Expect nothing to happen
        replayMocks();
        mCtsTest.run(mMockListener);
        verifyMocks();
    }

    /**
     * Set EasyMock expectations for parsing {@link #PLAN_NAME}
     */
    private void setParsePlanExceptations() throws ParseException {
        mCtsTest.setPlanName(PLAN_NAME);
        mMockPlanParser.parse((InputStream)EasyMock.anyObject());
        Collection<String> uris = new ArrayList<String>(1);
        uris.add(PACKAGE_NAME);
        EasyMock.expect(mMockPlanParser.getTestUris()).andReturn(uris);
    }

    /**
     * Test {@link CtsTest#run(java.util.List)} when --plan and --package options have not been
     * specified
     */
    public void testRun_nothingToRun() throws DeviceNotAvailableException {
        try {
            mCtsTest.run(mMockListener);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Test {@link CtsTest#run(ITestInvocationListener))} when --plan and --package options have
     * been specified.
     */
    public void testRun_packagePlan() throws DeviceNotAvailableException {
        mCtsTest.setPlanName(PLAN_NAME);
        mCtsTest.addPackageName(PACKAGE_NAME);
        try {
            mCtsTest.run(mMockListener);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Test {@link CtsTest#run(java.util.List)} when --plan and --class options have been
     * specified
     */
    public void testRun_planClass() throws DeviceNotAvailableException {
        mCtsTest.setPlanName(PLAN_NAME);
        mCtsTest.setClassName("class");
        try {
            mCtsTest.run(mMockListener);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Test {@link CtsTest#run(java.util.List)} when --package and --class options have been
     * specified
     */
    public void testRun_packageClass() throws DeviceNotAvailableException {
        mCtsTest.addPackageName(PACKAGE_NAME);
        mCtsTest.setClassName("class");
        try {
            mCtsTest.run(mMockListener);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Test {@link CtsTest#run(java.util.List)} when --plan, --package and --class options have been
     * specified
     */
    public void testRun_planPackageClass() throws DeviceNotAvailableException {
        mCtsTest.setPlanName(PLAN_NAME);
        mCtsTest.addPackageName(PACKAGE_NAME);
        mCtsTest.setClassName("class");
        try {
            mCtsTest.run(mMockListener);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private void replayMocks(Object... mocks) {
        EasyMock.replay(mMockRepo, mMockPlanParser, mMockDevice, mMockListener);
        EasyMock.replay(mocks);
    }

    private void verifyMocks(Object... mocks) {
        EasyMock.verify(mMockRepo, mMockPlanParser, mMockDevice, mMockListener);
        EasyMock.verify(mocks);
    }
}
