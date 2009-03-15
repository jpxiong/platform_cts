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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.runner.BaseTestRunner;

/**
 * Unit test runner running host side unit test.
 *
 */
public class HostUnitTestRunner extends BaseTestRunner{
    private static String JAR_SUFFIX = ".jar";

    private TestCase mTestCase;

    public HostUnitTestRunner() {
        mTestCase = null;
    }

    /**
     * Run the specified test.
     *
     * @param jarPath The jar file.
     * @param testPkgName The package name.
     * @param testClassName The class name.
     * @param testMethodName The method name.
     * @return The test result.
     */
    public TestResult runTest(final String jarPath, final String testPkgName,
            String testClassName, String testMethodName)
            throws MalformedURLException, ClassNotFoundException {

        TestResult result = new TestResult();
        loadTestCase(jarPath, testPkgName, testClassName, testMethodName);

        if (mTestCase != null) {
            mTestCase.run(result);
        }
        return result;
    }

    /**
     * Load test case via test class name and test method.
     *
     * @param testClassName The class name.
     * @param testMethodName The method name.
     */
    @SuppressWarnings("unchecked")
    public TestCase loadTestCase(final String jarPath,
            final String testPkgName, final String testClassName,
            final String testMethodName) throws MalformedURLException,
            ClassNotFoundException {

        Log.d("jarPath=" + jarPath + ",testPkgName=" + testPkgName
                + ",testClassName=" + testClassName);

        Class testClass = null;
        if ((jarPath != null) && (jarPath.endsWith(JAR_SUFFIX))) {
            testClass = loadClass(jarPath, testPkgName, testClassName);
        } else {
            testClass = Class.forName(testPkgName + "." + testClassName);
        }

        if ((testMethodName != null) && TestCase.class.isAssignableFrom(testClass)) {
            mTestCase = buildTestMethod(testClass, testMethodName);
        }

        return mTestCase;
    }

    /**
     * Load class from jar file.
     *
     * @param jarPath The jar file.
     * @param testPkgName The package name.
     * @param testClassName The class name.
     * @return The class.
     */
    @SuppressWarnings("unchecked")
    public Class loadClass(final String jarPath,
            final String testPkgName, final String testClassName)
            throws MalformedURLException, ClassNotFoundException {

        URL urls[] = {};
        JarFileLoader cl = new JarFileLoader(urls);
        cl.addFile(jarPath);
        Class testClass = cl.loadClass(testPkgName + "." + testClassName);
        Log.d("succeed in load jarred class: " + jarPath + "." + testPkgName
                + "." + testClassName);

        return testClass;
    }

    /**
     * Build test method.
     *
     * @param testClass The test class.
     * @param testMethodName The method name.
     * @return The test case.
     */
    @SuppressWarnings("unchecked")
    private TestCase buildTestMethod(Class testClass,
            String testMethodName) {
        try {
            TestCase testCase = (TestCase) testClass.newInstance();
            testCase.setName(testMethodName);
            return testCase;
        } catch (IllegalAccessException e) {
            runFailed("Could not access test class. Class: "
                    + testClass.getName());
        } catch (InstantiationException e) {
            runFailed("Could not instantiate test class. Class: "
                    + testClass.getName());
        }

        return null;
    }

    /** {@inheritDoc} */
    public void testStarted(String testName) {

    }

    /** {@inheritDoc} */
    public void testEnded(String testName) {

    }

    /** {@inheritDoc} */
    public void testFailed(int status, Test test, Throwable t) {
    }

    /** {@inheritDoc} */
    protected void runFailed(String message) {
        throw new RuntimeException(message);
    }

    /**
     * Jar file loader.
     *
     */
    class JarFileLoader extends URLClassLoader {
        public JarFileLoader(URL[] urls) {
            super(urls);
        }

        /**
         * Add jar file path.
         *
         * @param path the path to the specified jar file.
         */
        public void addFile(String path) throws MalformedURLException {
            String urlPath = "jar:file://" + path + "!/";
            addURL(new URL(urlPath));
        }
    }
}
