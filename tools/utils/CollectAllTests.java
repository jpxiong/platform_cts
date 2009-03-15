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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.textui.TestRunner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CollectAllTests extends DescriptionGenerator {

    static final String ATTRIBUTE_RUNNER = "runner";
    static final String ATTRIBUTE_PACKAGE = "appPackageName";
    static final String ATTRIBUTE_NS = "appNameSpace";
    static final String ATTRIBUTE_TARGET = "target";
    static final String ATTRIBUTE_HOST_SIDE_ONLY = "hostSideOnly";
    static final String ATTRIBUTE_JAR_PATH = "jarPath";

    static final String JAR_PATH = "LOCAL_JAR_PATH :=";
    static final String TEST_TYPE = "LOCAL_TEST_TYPE :";

    static final int HOST_SIDE_ONLY = 1;
    static final int DEVICE_SIDE_ONLY = 2;

    private static String runner;
    private static String packageName;
    private static String target;
    private static String xmlName;
    private static int testType;
    private static String jarPath;

    private static Map<String,TestClass> testCases;
    private static Set<String> failed = new HashSet<String>();

    private static class MyXMLGenerator extends XMLGenerator {

        MyXMLGenerator(String outputPath) throws ParserConfigurationException {
            super(outputPath);

            Node testPackageElem = mDoc.getDocumentElement();

            setAttribute(testPackageElem, ATTRIBUTE_NAME, xmlName);
            setAttribute(testPackageElem, ATTRIBUTE_RUNNER, runner);
            setAttribute(testPackageElem, ATTRIBUTE_PACKAGE, packageName);
            setAttribute(testPackageElem, ATTRIBUTE_NS, packageName);

            if (testType == HOST_SIDE_ONLY) {
                setAttribute(testPackageElem, ATTRIBUTE_HOST_SIDE_ONLY, "true");
                setAttribute(testPackageElem, ATTRIBUTE_JAR_PATH, jarPath);
            }

            if (!packageName.equals(target)) {
                setAttribute(testPackageElem, ATTRIBUTE_TARGET, target);
                }
            }
    }

    private static String OUTPUTFILE = "";
    private static String MANIFESTFILE = "";
    private static String TESTSUITECLASS = "";
    private static String ANDROID_MAKE_FILE = "";

    private static Test TESTSUITE;

    static XMLGenerator xmlGenerator;

    public static void main(String[] args) {
        if (args.length > 2) {
            OUTPUTFILE = args[0];
            MANIFESTFILE = args [1];
            TESTSUITECLASS = args[2];
            if (args.length > 3) {
                ANDROID_MAKE_FILE = args[3];
            }
        } else {
            System.out.println("usage: \n" +
                "\t... CollectAllTests <output-file> <manifest-file> <testsuite-class-name> <makefile-file>");
            return;
        }

        if (ANDROID_MAKE_FILE.length() > 0) {
            testType = getTestType(ANDROID_MAKE_FILE);
        }

        Document manifest = null;
        try {
            manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(MANIFESTFILE));
        } catch (Exception e) {
            System.err.println("cannot open manifest");
            e.printStackTrace();
            return;
        }

        Element documentElement = manifest.getDocumentElement();

        documentElement.getAttribute("package");

        xmlName = new File(OUTPUTFILE).getName();
        runner = getElementAttribute(documentElement, "instrumentation", "android:name");
        packageName = documentElement.getAttribute("package");
        target = getElementAttribute(documentElement, "instrumentation", "android:targetPackage");

        Class<?> testClass = null;
        try {
            testClass = Class.forName(TESTSUITECLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("test class not found");
            e.printStackTrace();
            return;
        }

        Method method = null;
        try {
            method = testClass.getMethod("suite", new Class<?>[0]);
        } catch (SecurityException e) {
            System.err.println("failed to get suite method");
            e.printStackTrace();
            return;
        } catch (NoSuchMethodException e) {
            System.err.println("failed to get suite method");
            e.printStackTrace();
            return;
        }

        try {
            TESTSUITE = (Test) method.invoke(null, (Object[])null);
        } catch (IllegalArgumentException e) {
            System.err.println("failed to get suite method");
            e.printStackTrace();
            return;
        } catch (IllegalAccessException e) {
            System.err.println("failed to get suite method");
            e.printStackTrace();
            return;
        } catch (InvocationTargetException e) {
            System.err.println("failed to get suite method");
            e.printStackTrace();
            return;
        }

        try {
            xmlGenerator = new MyXMLGenerator(OUTPUTFILE + ".xml");
        } catch (ParserConfigurationException e) {
            System.err.println("Can't initialize XML Generator");
        }

        testCases = new LinkedHashMap<String, TestClass>();
        CollectAllTests cat = new CollectAllTests();
        cat.compose();

        if (!failed.isEmpty()) {
            System.err.println("The following classes have no default constructor");
            for (Iterator<String> iterator = failed.iterator(); iterator.hasNext();) {
                String type = iterator.next();
                System.err.println(type);
            }
        }

        for (Iterator<TestClass> iterator = testCases.values().iterator(); iterator.hasNext();) {
            TestClass type = iterator.next();
            xmlGenerator.addTestClass(type);
        }

        try {
            xmlGenerator.dump();
        } catch (Exception e) {
            System.err.println("cannot dump xml");
            e.printStackTrace();
        }
    }

    private static int getTestType(String makeFileName) {

        int type = DEVICE_SIDE_ONLY;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(makeFileName));
            String line;

            while ((line =reader.readLine())!=null) {
                if (line.startsWith(TEST_TYPE)) {
                    type = HOST_SIDE_ONLY;
                } else if (line.startsWith(JAR_PATH)) {
                    jarPath = line.substring(JAR_PATH.length(), line.length()).trim();
                }
            }
            reader.close();
        } catch (IOException e) {
        }

        return type;
    }

    private static Element getElement(Element element, String tagName) {
        NodeList elements = element.getElementsByTagName(tagName);
        if (elements.getLength() > 0) {
            return (Element) elements.item(0);
        } else {
            return null;
        }
    }

    private static String getElementAttribute(Element element, String elementName, String attributeName) {
        Element e = getElement(element, elementName);
        if (e != null) {
            return e.getAttribute(attributeName);
        } else {
            return "";
        }
    }

    public void compose() {
        System.out.println("Collecting all junit tests...");
        new TestRunner() {
            @Override
            protected TestResult createTestResult() {
                return new TestResult() {
                    @Override
                    protected void run(TestCase test) {
                        addToTests(test);
                    }
                };
            }

            @Override
            public TestResult doRun(Test test) {
                return super.doRun(test);
            }
        }.doRun(TESTSUITE);
    }

    private void addToTests(TestCase test) {

        String testClassName = test.getClass().getName();
        String testName = test.getName();
        if (!testName.startsWith("test")) {
            try {
                test.runBare();
            } catch (Throwable e) {
                e.printStackTrace();
                return;
            }
        }
        TestClass testClass = null;
        if (testCases.containsKey(testClassName)) {
            testClass = testCases.get(testClassName);
        } else {
            testClass = new TestClass(testClassName, new ArrayList<TestMethod>());
            testCases.put(testClassName, testClass);
        }

        testClass.mCases.add(new TestMethod(testName, "", "", null));

        try {
            test.getClass().getConstructor(new Class<?>[0]);
        } catch (SecurityException e) {
            failed.add(test.getClass().getName());
        } catch (NoSuchMethodException e) {
            failed.add(test.getClass().getName());
        }
    }
}
