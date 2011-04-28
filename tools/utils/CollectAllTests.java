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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vogar.Expectation;
import vogar.ExpectationStore;
import vogar.ModeId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;

public class CollectAllTests extends DescriptionGenerator {

    static final String ATTRIBUTE_RUNNER = "runner";
    static final String ATTRIBUTE_PACKAGE = "appPackageName";
    static final String ATTRIBUTE_NS = "appNameSpace";
    static final String ATTRIBUTE_TARGET = "targetNameSpace";
    static final String ATTRIBUTE_TARGET_BINARY = "targetBinaryName";
    static final String ATTRIBUTE_HOST_SIDE_ONLY = "hostSideOnly";
    static final String ATTRIBUTE_VM_HOST_TEST = "vmHostTest";
    static final String ATTRIBUTE_JAR_PATH = "jarPath";

    static final String JAR_PATH = "LOCAL_JAR_PATH :=";
    static final String TEST_TYPE = "LOCAL_TEST_TYPE :";

    static final int HOST_SIDE_ONLY = 1;
    static final int DEVICE_SIDE_ONLY = 2;
    static final int VM_HOST_TEST = 3;

    private static String runner;
    private static String packageName;
    private static String target;
    private static String xmlName;
    private static int testType;
    private static String jarPath;

    private static Map<String,TestClass> testCases;

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

            if (testType == VM_HOST_TEST) {
                setAttribute(testPackageElem, ATTRIBUTE_VM_HOST_TEST, "true");
                setAttribute(testPackageElem, ATTRIBUTE_JAR_PATH, jarPath);
            }

            if (!packageName.equals(target)) {
                setAttribute(testPackageElem, ATTRIBUTE_TARGET, target);
                setAttribute(testPackageElem, ATTRIBUTE_TARGET_BINARY, target);
            }
        }
    }

    private static String OUTPUTFILE;
    private static String MANIFESTFILE;
    private static String JARFILE;
    private static String LIBCORE_EXPECTATION_DIR;
    private static String ANDROID_MAKE_FILE = "";

    static XMLGenerator xmlGenerator;
    private static ExpectationStore libcoreVogarExpectationStore;
    private static ExpectationStore ctsVogarExpectationStore;

    public static void main(String[] args) {
        if (args.length >= 3 && args.length <= 5) {
            OUTPUTFILE = args[0];
            MANIFESTFILE = args[1];
            JARFILE = args[2];
            if (args.length >= 4) {
                LIBCORE_EXPECTATION_DIR = args[3];
                if (args.length >= 5) {
                    ANDROID_MAKE_FILE = args[4];
                }
            }
        } else {
            System.err.println("usage: CollectAllTests <output-file> <manifest-file> <jar-file>"
                               + "[expectation-dir [makefile-file]]");
            System.exit(1);
        }

        if (ANDROID_MAKE_FILE.length() > 0) {
            testType = getTestType(ANDROID_MAKE_FILE);
        }

        Document manifest = null;
        try {
            manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new FileInputStream(MANIFESTFILE));
        } catch (Exception e) {
            System.err.println("cannot open manifest " + MANIFESTFILE);
            e.printStackTrace();
            System.exit(1);
        }

        Element documentElement = manifest.getDocumentElement();

        documentElement.getAttribute("package");

        xmlName = new File(OUTPUTFILE).getName();
        runner = getElementAttribute(documentElement, "instrumentation", "android:name");
        packageName = documentElement.getAttribute("package");
        target = getElementAttribute(documentElement, "instrumentation", "android:targetPackage");

        try {
            xmlGenerator = new MyXMLGenerator(OUTPUTFILE + ".xml");
        } catch (ParserConfigurationException e) {
            System.err.println("Can't initialize XML Generator " + OUTPUTFILE + ".xml");
            System.exit(1);
        }

        try {
            libcoreVogarExpectationStore
                    = VogarUtils.provideExpectationStore(LIBCORE_EXPECTATION_DIR);
            ctsVogarExpectationStore = VogarUtils.provideExpectationStore(CTS_EXPECTATION_DIR);
        } catch (IOException e) {
            System.err.println("Can't initialize vogar expectation store from "
                               + LIBCORE_EXPECTATION_DIR);
            e.printStackTrace(System.err);
            System.exit(1);
        }

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(JARFILE);
        } catch (Exception e) {
            System.err.println("cannot open jarfile " + JARFILE);
            e.printStackTrace();
            System.exit(1);
        }

        testCases = new LinkedHashMap<String, TestClass>();

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String name = jarEntry.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            String className
                    = name.substring(0, name.length() - ".class".length()).replace('/', '.');
            try {
                Class<?> klass = Class.forName(className,
                                               false,
                                               CollectAllTests.class.getClassLoader());
                if (!TestCase.class.isAssignableFrom(klass)) {
                    continue;
                }
                if (Modifier.isAbstract(klass.getModifiers())) {
                    continue;
                }
                if (!Modifier.isPublic(klass.getModifiers())) {
                    continue;
                }
                try {
                    klass.getConstructor(new Class<?>[] { String.class } );
                    addToTests(klass.asSubclass(TestCase.class));
                    continue;
                } catch (NoSuchMethodException e) {
                }
                try {
                    klass.getConstructor(new Class<?>[0]);
                    addToTests(klass.asSubclass(TestCase.class));
                    continue;
                } catch (NoSuchMethodException e) {
                }
            } catch (ClassNotFoundException e) {
                System.out.println("class not found " + className);
                e.printStackTrace();
                System.exit(1);
            }
        }

        for (Iterator<TestClass> iterator = testCases.values().iterator(); iterator.hasNext();) {
            TestClass type = iterator.next();
            xmlGenerator.addTestClass(type);
        }

        try {
            xmlGenerator.dump();
        } catch (Exception e) {
            System.err.println("cannot dump xml to " + OUTPUTFILE + ".xml");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int getTestType(String makeFileName) {

        int type = DEVICE_SIDE_ONLY;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(makeFileName));
            String line;

            while ((line =reader.readLine())!=null) {
                if (line.startsWith(TEST_TYPE)) {
                    if (line.indexOf(ATTRIBUTE_VM_HOST_TEST) >= 0) {
                        type = VM_HOST_TEST;
                    } else {
                        type = HOST_SIDE_ONLY;
                    }
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

    private static String getElementAttribute(Element element,
                                              String elementName,
                                              String attributeName) {
        Element e = getElement(element, elementName);
        if (e != null) {
            return e.getAttribute(attributeName);
        } else {
            return "";
        }
    }

    private static String getKnownFailure(final Class<? extends TestCase> testClass,
            final String testName) {
        return getAnnotation(testClass, testName, KNOWN_FAILURE);
    }

    private static boolean isKnownFailure(final Class<? extends TestCase> testClass,
            final String testName) {
        return getAnnotation(testClass, testName, KNOWN_FAILURE) != null;
    }

    private static boolean isBrokenTest(final Class<? extends TestCase> testClass,
            final String testName)  {
        return getAnnotation(testClass, testName, BROKEN_TEST) != null;
    }

    private static boolean isSuppressed(final Class<? extends TestCase> testClass,
            final String testName)  {
        return getAnnotation(testClass, testName, SUPPRESSED_TEST) != null;
    }

    private static boolean hasSideEffects(final Class<? extends TestCase> testClass,
            final String testName) {
        return getAnnotation(testClass, testName, SIDE_EFFECT) != null;
    }

    private static String getAnnotation(final Class<? extends TestCase> testClass,
            final String testName, final String annotationName) {
        try {
            Method testMethod = testClass.getMethod(testName, (Class[])null);
            Annotation[] annotations = testMethod.getAnnotations();
            for (Annotation annot : annotations) {

                if (annot.annotationType().getName().equals(annotationName)) {
                    String annotStr = annot.toString();
                    String knownFailure = null;
                    if (annotStr.contains("(value=")) {
                        knownFailure =
                            annotStr.substring(annotStr.indexOf("=") + 1,
                                    annotStr.length() - 1);

                    }

                    if (knownFailure == null) {
                        knownFailure = "true";
                    }

                    return knownFailure;
                }

            }

        } catch (java.lang.NoSuchMethodException e) {
        }

        return null;
    }

    private static void addToTests(Class<? extends TestCase> test) {
        Class testClass = test;
        Set<String> testNames = new HashSet<String>();
        while (TestCase.class.isAssignableFrom(testClass)) {
            Method[] testMethods = testClass.getDeclaredMethods();
            for (Method testMethod : testMethods) {
                String testName = testMethod.getName();
                if (testNames.contains(testName)) {
                    continue;
                }
                if (!testName.startsWith("test")) {
                    continue;
                }
                if (testMethod.getParameterTypes().length != 0) {
                    continue;
                }
                if (!testMethod.getReturnType().equals(Void.TYPE)) {
                    continue;
                }
                if (!Modifier.isPublic(testMethod.getModifiers())) {
                    continue;
                }
                testNames.add(testName);
                addToTests(test, testName);
            }
            testClass = testClass.getSuperclass();
        }
    }

    private static void addToTests(Class<? extends TestCase> test, String testName) {

        String testClassName = test.getName();
        String knownFailure = getKnownFailure(test, testName);

        if (isKnownFailure(test, testName)) {
            System.out.println("ignoring known failure: " + test + "#" + testName);
            return;
        } else if (isBrokenTest(test, testName)) {
            System.out.println("ignoring broken test: " + test + "#" + testName);
            return;
        } else if (isSuppressed(test, testName)) {
            System.out.println("ignoring suppressed test: " + test + "#" + testName);
            return;
        } else if (hasSideEffects(test, testName)) {
            System.out.println("ignoring test with side effects: " + test + "#" + testName);
            return;
        } else if (VogarUtils.isVogarKnownFailure(libcoreVogarExpectationStore,
                                                  testClassName,
                                                  testName)) {
            System.out.println("ignoring libcore expectation known failure: " + test
                               + "#" + testName);
            return;
        } else if (VogarUtils.isVogarKnownFailure(ctsVogarExpectationStore,
                                                  testClassName,
                                                  testName)) {
            System.out.println("ignoring cts expectation known failure: " + test
                               + "#" + testName);
            return;
        }

        TestClass testClass = null;
        if (testCases.containsKey(testClassName)) {
            testClass = testCases.get(testClassName);
        } else {
            testClass = new TestClass(testClassName, new ArrayList<TestMethod>());
            testCases.put(testClassName, testClass);
        }

        testClass.mCases.add(new TestMethod(testName, "", "", knownFailure, false, false));
    }
}
