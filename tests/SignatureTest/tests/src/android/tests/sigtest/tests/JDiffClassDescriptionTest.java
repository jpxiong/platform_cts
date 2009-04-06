/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.tests.sigtest.tests;

import android.test.InstrumentationTestCase;
import android.tests.sigtest.ResultObserver;
import android.tests.sigtest.SignatureTestActivity;
import android.tests.sigtest.JDiffClassDescription;
import junit.framework.TestCase;
import java.lang.reflect.Modifier;

/**
 * Test class for JDiffClassDescription.
 */
public class JDiffClassDescriptionTest extends InstrumentationTestCase {

    private class NoFailures implements ResultObserver {
        public void notifyFailure(SignatureTestActivity.FAILURE_TYPE type, String name) {
            JDiffClassDescriptionTest.this.fail("Saw unexpected test failure: " + name + " failure type: " + type);
        }
    }

    private class ExpectFailure implements ResultObserver {
        private SignatureTestActivity.FAILURE_TYPE expectedType;
        private boolean failureSeen;

        public ExpectFailure(SignatureTestActivity.FAILURE_TYPE expectedType) {
            this.expectedType = expectedType;
        }

        public void notifyFailure(SignatureTestActivity.FAILURE_TYPE type, String name) {
            if (type == expectedType) {
                if (failureSeen) {
                    JDiffClassDescriptionTest.this.fail("Saw second test failure: " + name + " failure type: " + type);
                } else {
                    // We've seen the error, mark it and keep going
                    failureSeen = true;
                }
            } else {
                JDiffClassDescriptionTest.this.fail("Saw unexpected test failure: " + name + " failure type: " + type);
            }
        }
    }

    /**
     * Create the JDiffClassDescription for "NormalClass".
     *
     * @return the new JDiffClassDescription
     */
    private JDiffClassDescription createNormalClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        return clz;
    }

    public void testNormalClassCompliance() {
        JDiffClassDescription clz = createNormalClass();
        clz.checkSignatureCompliance();
    }

    public void testMissingClass() {
        ExpectFailure observer = new ExpectFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_CLASS);
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data",
                                                              "NoSuchClass",
                                                              observer);
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.checkSignatureCompliance();
    }

    public void testSimpleConstructor() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", Modifier.PUBLIC);
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
    }
    public void testOneArgConstructor() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", Modifier.PRIVATE);
        constructor.addParam("java.lang.String");
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
    }
    public void testConstructorThrowsException() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", Modifier.PROTECTED);
        constructor.addParam("java.lang.String");
        constructor.addParam("java.lang.String");
        constructor.addException("android.tests.sigtest.tests.data.NormalException");
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
    }
    public void testPackageProtectedConstructor() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", 0);
        constructor.addParam("java.lang.String");
        constructor.addParam("java.lang.String");
        constructor.addParam("java.lang.String");
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
    }


    public void testStaticMethod() {
        JDiffClassDescription clz = createNormalClass();
        clz.addMethod(new JDiffClassDescription.JDiffMethod("staticMethod", Modifier.STATIC | Modifier.PUBLIC, "void"));
        clz.checkSignatureCompliance();
    }
    public void testSyncMethod() {
        JDiffClassDescription clz = createNormalClass();
        clz.addMethod(new JDiffClassDescription.JDiffMethod("syncMethod", Modifier.SYNCHRONIZED | Modifier.PUBLIC, "void"));
        clz.checkSignatureCompliance();
    }
    public void testPackageProtectMethod() {
        JDiffClassDescription clz = createNormalClass();
        clz.addMethod(new JDiffClassDescription.JDiffMethod("packageProtectedMethod", 0, "boolean"));
        clz.checkSignatureCompliance();
    }
    public void testPrivateMethod() {
        JDiffClassDescription clz = createNormalClass();
        clz.addMethod(new JDiffClassDescription.JDiffMethod("privateMethod", Modifier.PRIVATE, "void"));
        clz.checkSignatureCompliance();
    }
    public void testProtectedMethod() {
        JDiffClassDescription clz = createNormalClass();
        clz.addMethod(new JDiffClassDescription.JDiffMethod("protectedMethod", Modifier.PROTECTED, "java.lang.String"));
        clz.checkSignatureCompliance();
    }
    public void testThrowsMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("throwsMethod", Modifier.PUBLIC, "void");
        method.addException("android.tests.sigtest.tests.data.NormalException");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
    }
    public void testNativeMethod() {
        JDiffClassDescription clz = createNormalClass();
        clz.addMethod(new JDiffClassDescription.JDiffMethod("nativeMethod", Modifier.PUBLIC | Modifier.NATIVE, "void"));
        clz.checkSignatureCompliance();
    }

    public void testFinalField() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("FINAL_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.FINAL));
        clz.checkSignatureCompliance();
    }
    public void testStaticField() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("STATIC_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.STATIC));
        clz.checkSignatureCompliance();
    }
    public void testVolatileFiled() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("VOLATILE_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.VOLATILE));
        clz.checkSignatureCompliance();
    }
    public void testTransientField() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("TRANSIENT_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.TRANSIENT));
        clz.checkSignatureCompliance();
    }
    public void testPacakgeField() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("PACAKGE_FIELD", "java.lang.String", 0));
        clz.checkSignatureCompliance();
    }
    public void testPrivateField() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("PRIVATE_FIELD", "java.lang.String", Modifier.PRIVATE));
        clz.checkSignatureCompliance();
    }
    public void testProtectedField() {
        JDiffClassDescription clz = createNormalClass();
        clz.addField(new JDiffClassDescription.JDiffField("PROTECTED_FIELD", "java.lang.String", Modifier.PROTECTED));
        clz.checkSignatureCompliance();
    }

    public void testInnerClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass.InnerClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        clz.addField(new JDiffClassDescription.JDiffField("innerClassData", "java.lang.String", Modifier.PRIVATE));
        clz.checkSignatureCompliance();
    }
    public void testInnerInnerClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass.InnerClass.InnerInnerClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        clz.addField(new JDiffClassDescription.JDiffField("innerInnerClassData", "java.lang.String", Modifier.PRIVATE));
        clz.checkSignatureCompliance();
    }
    public void testInnerInterface() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass.InnerInterface", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.INTERFACE);
        clz.setModifier(Modifier.PUBLIC);
        clz.addMethod(new JDiffClassDescription.JDiffMethod("doSomething", Modifier.PUBLIC, "void"));
        clz.checkSignatureCompliance();
    }

    public void testInterface() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalInterface", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.INTERFACE);
        clz.setModifier(Modifier.PUBLIC);
        clz.addMethod(new JDiffClassDescription.JDiffMethod("doSomething", Modifier.PUBLIC, "void"));
        clz.checkSignatureCompliance();
    }
    public void testFinalClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "FinalClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC | Modifier.FINAL);
        clz.checkSignatureCompliance();
    }
}
