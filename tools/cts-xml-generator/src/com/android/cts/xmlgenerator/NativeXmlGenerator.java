/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cts.xmlgenerator;

import vogar.Expectation;
import vogar.ExpectationStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Generator of TestPackage XML files for native tests.
 *
 * It takes in an input of the following form:
 *
 * class:TestClass1
 * method:testMethod1
 * method:testMethod2
 * class:TestClass2
 * method:testMethod1
 */
class NativeXmlGenerator {

    /** Test package name like "android.nativemedia" to group the tests. */
    private final String mAppPackageName;

    /** Name of the native executable. */
    private final String mName;

    /** Path to output file or null to just dump to standard out. */
    private final String mOutputPath;

    /** ExpectationStore to filter out known failures. */
    private final ExpectationStore mExpectations;

    NativeXmlGenerator(ExpectationStore expectations, String appPackageName, String name,
            String outputPath) {
        mAppPackageName = appPackageName;
        mName = name;
        mOutputPath = outputPath;
        mExpectations = expectations;
    }

    public void writePackageXml() throws IOException {
        OutputStream output = System.out;
        if (mOutputPath != null) {
            File outputFile = new File(mOutputPath);
            File outputDir = outputFile.getParentFile();
            if (!outputDir.exists()) {
                outputDir.mkdirs();
                if (!outputDir.exists()) {
                    System.err.println("Couldn't make output directory: " + outputDir);
                    System.exit(1);
                }
            }
            output = new FileOutputStream(outputFile);
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(output);
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writeTestPackage(writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void writeTestPackage(PrintWriter writer) {
        writer.append("<TestPackage appPackageName=\"")
                .append(mAppPackageName)
                .append("\" name=\"")
                .append(mName)
                .println("\" testType=\"native\" version=\"1.0\">");
        writeTestSuite(writer);
        writer.println("</TestPackage>");
    }

    private void writeTestSuite(PrintWriter writer) {
        /*
         * Given "android.foo.bar.baz"...
         *
         * <TestSuite name="android">
         *   <TestSuite name="foo">
         *     <TestSuite name="bar">
         *       <TestSuite name="baz">
         */
        Scanner scanner = null;
        try {
            scanner = new Scanner(mAppPackageName);
            scanner.useDelimiter("\\.");

            int numLevels = 0;
            for (; scanner.hasNext(); numLevels++) {
                String packagePart = scanner.next();
                writer.append("<TestSuite name=\"").append(packagePart).println("\">");
            }

            writeTestCases(writer);

            for (; numLevels > 0; numLevels--) {
                writer.println("</TestSuite>");
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private void writeTestCases(PrintWriter writer) {
        String currentClassName = null;
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] tokens = line.split(":");
            if (tokens.length > 1) {
                String type = tokens[0];
                String value = tokens[1];
                if ("class".equals(type)) {
                    if (currentClassName != null) {
                        writer.append("</TestCase>");
                    }
                    currentClassName = value;
                    writer.append("<TestCase name=\"").append(value).println("\">");
                } else if ("method".equals(type)) {
                    String fullClassName = mAppPackageName + "." + currentClassName;
                    if (!isKnownFailure(mExpectations, fullClassName, value)) {
                        writer.append("<Test name=\"").append(value).println("\" />");
                    }
                }
            }
        }
        if (currentClassName != null) {
            writer.println("</TestCase>");
        }
    }

    public static boolean isKnownFailure(ExpectationStore expectationStore,
            String className, String methodName) {
        String testName = String.format("%s#%s", className, methodName);
        return expectationStore != null && expectationStore.get(testName) != Expectation.SUCCESS;
    }
}

