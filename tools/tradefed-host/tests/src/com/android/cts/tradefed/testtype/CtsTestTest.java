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

import com.android.cts.tradefed.build.StubCtsBuildHelper;
import com.android.ddmlib.testrunner.TestIdentifier;
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
    private StubCtsBuildHelper mStubBuildHelper;
    private ITestPackageDef mMockPackageDef;
    private IRemoteTest mMockTest;

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
        mStubBuildHelper = new StubCtsBuildHelper();
        mMockPackageDef = EasyMock.createMock(ITestPackageDef.class);
        mMockTest = EasyMock.createMock(IRemoteTest.class);

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
        mCtsTest.setBuildHelper(mStubBuildHelper);
        // turn off device collection for simplicity
        mCtsTest.setCollectDeviceInfo(false);


    }

    /**
     * Test normal case {@link CtsTest#run(java.util.List)} when running a plan.
     */
    @SuppressWarnings("unchecked")
    public void testRun_plan() throws DeviceNotAvailableException, ParseException {
        setParsePlanExceptations();

        setCreateAndRunTestExpectations();

        replayMocks();
        mCtsTest.run(mMockListener);
        verifyMocks();
    }

    /**
     * Test normal case {@link CtsTest#run(java.util.List)} when running a package.
     */
    @SuppressWarnings("unchecked")
    public void testRun_package() throws DeviceNotAvailableException {
        mCtsTest.addPackageName(PACKAGE_NAME);

        setCreateAndRunTestExpectations();

        replayMocks();
        mCtsTest.run(mMockListener);
        verifyMocks();
    }

    /**
     * Test a resumed run
     */
    @SuppressWarnings("unchecked")
    public void testRun_resume() throws DeviceNotAvailableException {
        mCtsTest.addPackageName(PACKAGE_NAME);

        setCreateAndRunTestExpectations();
        // abort the first run
        EasyMock.expectLastCall().andThrow(new DeviceNotAvailableException());

        // now expect test to be resumed
        mMockTest.run((ITestInvocationListener)EasyMock.anyObject());
        EasyMock.expect(mMockPackageDef.getName()).andReturn(PACKAGE_NAME);
        EasyMock.expect(mMockPackageDef.getDigest()).andReturn("digest");

        replayMocks();
        try {
            mCtsTest.run(mMockListener);
            fail("Did not throw DeviceNotAvailableException");
        } catch (DeviceNotAvailableException e) {
            // expected
        }
        // now resume, and expect same test's run method to be called again
        mCtsTest.run(mMockListener);
        verifyMocks();
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
        mMockPackageDef.setClassName(className, methodName);

        setCreateAndRunTestExpectations();

        replayMocks();
        mCtsTest.run(mMockListener);
        verifyMocks();
    }

    /**
     * Test {@link CtsTest#run(java.util.List)} when --excluded-package is specified
     */
    public void testRun_excludedPackage() throws DeviceNotAvailableException, ParseException {
        mCtsTest.setPlanName(PLAN_NAME);
        mMockPlanParser.parse((InputStream)EasyMock.anyObject());
        Collection<String> uris = new ArrayList<String>(1);
        uris.add(PACKAGE_NAME);
        EasyMock.expect(mMockPlanParser.getTestUris()).andReturn(uris);

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
        TestFilter filter = new TestFilter();
        EasyMock.expect(mMockPlanParser.getExcludedTestFilter(PACKAGE_NAME)).andReturn(
                filter);
        mMockPackageDef.setExcludedTestFilter(filter);
    }

    /**
     * Set EasyMock expectations for creating and running a package with PACKAGE_NAME
     */
    private void setCreateAndRunTestExpectations() throws DeviceNotAvailableException {
        EasyMock.expect(mMockRepo.getTestPackage(PACKAGE_NAME)).andReturn(mMockPackageDef);
        EasyMock.expect(mMockPackageDef.createTest((File)EasyMock.anyObject())).andReturn(
                mMockTest);
        EasyMock.expect(mMockPackageDef.getTests()).andReturn(new ArrayList<TestIdentifier>());
        EasyMock.expect(mMockPackageDef.getUri()).andStubReturn(PACKAGE_NAME);
        EasyMock.expect(mMockPackageDef.getName()).andReturn(PACKAGE_NAME);
        EasyMock.expect(mMockPackageDef.getDigest()).andReturn("digest");

        mMockTest.run((ITestInvocationListener)EasyMock.anyObject());
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
        EasyMock.replay(mMockRepo, mMockPlanParser, mMockDevice, mMockListener, mMockPackageDef,
                mMockTest);
        EasyMock.replay(mocks);
    }

    private void verifyMocks(Object... mocks) {
        EasyMock.verify(mMockRepo, mMockPlanParser, mMockDevice, mMockListener, mMockPackageDef,
                mMockTest);
        EasyMock.verify(mocks);
    }
}
