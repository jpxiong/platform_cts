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

package android.tests.sigtest;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.tests.sigtest.JDiffClassDescription.JDiffConstructor;
import android.tests.sigtest.JDiffClassDescription.JDiffField;
import android.tests.sigtest.JDiffClassDescription.JDiffMethod;

import com.android.internal.util.XmlUtils;

/**
 * Entry class for signature test.
 */
public class SignatureTest {
    private static final String TAG_ROOT = "api";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_CLASS = "class";
    private static final String TAG_INTERFACE = "interface";
    private static final String TAG_IMPLEMENTS = "implements";
    private static final String TAG_CONSTRUCTOR = "constructor";
    private static final String TAG_METHOD = "method";
    private static final String TAG_PARAM = "parameter";
    private static final String TAG_EXCEPTION = "exception";
    private static final String TAG_FIELD = "field";

    private static final String MODIFIER_ABSTRACT = "abstract";
    private static final String MODIFIER_FINAL = "final";
    private static final String MODIFIER_NATIVE = "native";
    private static final String MODIFIER_PRIVATE = "private";
    private static final String MODIFIER_PROTECTED = "protected";
    private static final String MODIFIER_PUBLIC = "public";
    private static final String MODIFIER_STATIC = "static";
    private static final String MODIFIER_SYNCHRONIZED = "synchronized";
    private static final String MODIFIER_TRANSIENT = "transient";
    private static final String MODIFIER_VOLATILE = "volatile";
    private static final String MODIFIER_VISIBILITY = "visibility";

    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_EXTENDS = "extends";
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_RETURN = "return";

    private static ArrayList<String> mDebugArray = new ArrayList<String>();

    private HashSet<String> mKeyTagSet;

    private JDiffMethod mCurrentJdiffMethod;

    private ArrayList<ResultObserver> mReportObserverList;

    private JDiffClassDescription mCurrentClass;

    public SignatureTest() {

        mReportObserverList = new ArrayList<ResultObserver>();
        mCurrentClass = new JDiffClassDescription();
        mKeyTagSet = new HashSet<String>();
        mKeyTagSet.addAll(Arrays.asList(new String[] {
                TAG_PACKAGE, TAG_CLASS, TAG_INTERFACE, TAG_IMPLEMENTS, TAG_CONSTRUCTOR,
                TAG_METHOD, TAG_PARAM, TAG_EXCEPTION, TAG_FIELD }));
    }

    /**
     * Signature test entry point.
     */
    public void start(XmlPullParser parser) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(parser, TAG_ROOT);
        int type;
        while (true) {
            type = XmlPullParser.START_DOCUMENT;
            while ((type=parser.next()) != XmlPullParser.START_TAG
                       && type != XmlPullParser.END_DOCUMENT
                       && type != XmlPullParser.END_TAG) {
                ;
            }

            if (type == XmlPullParser.END_TAG) {
                if (TAG_CLASS.equals(parser.getName())
                        || TAG_INTERFACE.equals(parser.getName())) {
                    mCurrentClass.checkSignatureCompliance();
                }
                continue;
            }

            if (type == XmlPullParser.END_DOCUMENT) {
                break;
            }

            String tagname = parser.getName();
            if (!mKeyTagSet.contains(tagname)) {
                continue;
            }

            if (tagname.equals(TAG_PACKAGE)) {
                mCurrentClass.setPackageName(parser.getAttributeValue(null,
                        ATTRIBUTE_NAME));
            } else if (tagname.equals(TAG_CLASS)) {
                loadClassInfo(parser, false);
            } else if (tagname.equals(TAG_INTERFACE)) {
                loadClassInfo(parser, true);
            } else if (tagname.equals(TAG_IMPLEMENTS)) {
                loadImplementationInfo(parser);
            } else if (tagname.equals(TAG_CONSTRUCTOR)) {
                loadConstructorInfo(parser);
            } else if (tagname.equals(TAG_METHOD)) {
                loadMethodInfo(parser);
            } else if (tagname.equals(TAG_PARAM)) {
                loadParamInfo(parser);
            } else if (tagname.equals(TAG_EXCEPTION)) {
                loadExceptionInfo(parser);
            } else if (tagname.equals(TAG_FIELD)) {
                loadFieldInfo(parser);
            } else {
                throw new RuntimeException(
                        "unknow tag exception:" + tagname);
            }
        }
    }

    public static void log(final String msg) {
        mDebugArray.add(msg);
    }

    public void registerResultObserver(ResultObserver resultObserver) {
        mCurrentClass.registerResultObserver(resultObserver);
    }

    public void addReportObserver(ResultObserver observer) {
        mReportObserverList.add(observer);
    }

    public void removeReportObserver(ResultObserver observer) {
        mReportObserverList.remove(observer);
    }

    public void clearReportObserverList() {
        mReportObserverList.clear();
    }

    /**
     * Load field information from xml to memory.
     */
    private void loadFieldInfo(XmlPullParser parser) {
        String fieldName = parser.getAttributeValue(null, ATTRIBUTE_NAME);
        String fieldType = parser.getAttributeValue(null, ATTRIBUTE_TYPE);
        int modifier = jdiffModifierToReflectionFormat(parser);
        mCurrentClass.addField(new JDiffField(fieldName, fieldType, modifier));
    }

    /**
     * Load exception information from xml to memory.
     *
     * @param parser The XmlPullParser which carries the xml information.
     */
    private void loadExceptionInfo(XmlPullParser parser) {
        mCurrentJdiffMethod.addException(parser.getAttributeValue(null, ATTRIBUTE_TYPE));
    }

    /**
     * Load parameter information from xml to memory.
     *
     * @param parser The XmlPullParser which carries the xml information.
     */
    private void loadParamInfo(XmlPullParser parser) {
        mCurrentJdiffMethod.addParam(parser.getAttributeValue(null, ATTRIBUTE_TYPE));
    }

    /**
     * Load method information from xml to memory.
     *
     * @param parser The XmlPullParser which carries the xml information.
     */
    private void loadMethodInfo(XmlPullParser parser) {
        String methodName = parser.getAttributeValue(null, ATTRIBUTE_NAME);
        String returnType = parser.getAttributeValue(null, ATTRIBUTE_RETURN);
        int modifier = jdiffModifierToReflectionFormat(parser);
        mCurrentJdiffMethod = new JDiffMethod(methodName, modifier, returnType);
        mCurrentClass.addMethod(mCurrentJdiffMethod);
    }

    /**
     * Load constructor information from xml to memory.
     *
     * @param parser The XmlPullParser which carries the xml information.
     */
    private void loadConstructorInfo(XmlPullParser parser) {
//      SignatureTestLog.d("load constructor info >>> ");
        int modifier = jdiffModifierToReflectionFormat(parser);

        mCurrentJdiffMethod = new JDiffConstructor(mCurrentClass.getClassName(), modifier);

        mCurrentClass.addConstructor((JDiffConstructor) mCurrentJdiffMethod);
    }

    /**
     * Load implementation information to memory.
     *
     * @param parser The XmlPullParser which carries the xml information.
     */
    private void loadImplementationInfo(XmlPullParser parser) {
        mCurrentClass.addImplInterface(parser.getAttributeValue(null, ATTRIBUTE_NAME));
    }

    /**
     * Load class or interface information to memory.
     *
     * @param parser The XmlPullParser which carries the xml information.
     * @param isInterface true if the current class is an interface, otherwise is false.
     */
    private void loadClassInfo(XmlPullParser parser, boolean isInterface) {
        String className = parser.getAttributeValue(null, ATTRIBUTE_NAME);
        mCurrentClass.setClassName(className);
        mCurrentClass.setModifier(jdiffModifierToReflectionFormat(parser));
        mCurrentClass.setType(isInterface ? JDiffClassDescription.JDiffType.INTERFACE :
            JDiffClassDescription.JDiffType.CLASS);
        mCurrentClass.setExtendsClass(parser.getAttributeValue(null, ATTRIBUTE_EXTENDS));
    }

    /**
     * Convert string modifier to int modifier.
     *
     * @param key modifier name
     * @param value modifier value
     * @return converted modifier value
     */
    private static int modifierDescriptionToReflectedType(String key, String value){
        if (key.equals(MODIFIER_ABSTRACT)) {
            return value.equals("true") ? Modifier.ABSTRACT : 0;
        } else if (key.equals(MODIFIER_FINAL)) {
            return value.equals("true") ? Modifier.FINAL : 0;
        } else if (key.equals(MODIFIER_NATIVE)) {
            return value.equals("true") ? Modifier.NATIVE : 0;
        } else if (key.equals(MODIFIER_STATIC)) {
            return value.equals("true") ? Modifier.STATIC : 0;
        } else if (key.equals(MODIFIER_SYNCHRONIZED)) {
            return value.equals("true") ? Modifier.SYNCHRONIZED : 0;
        } else if (key.equals(MODIFIER_TRANSIENT)) {
            return value.equals("true") ? Modifier.TRANSIENT : 0;
        } else if (key.equals(MODIFIER_VOLATILE)) {
            return value.equals("true") ? Modifier.VOLATILE : 0;
        } else if (key.equals(MODIFIER_VISIBILITY)) {
            if (value.equals(MODIFIER_PRIVATE)) {
                throw new RuntimeException(
                        "should not be private method/field here");
            } else if (value.equals(MODIFIER_PROTECTED)) {
                return Modifier.PROTECTED;
            } else if (value.equals(MODIFIER_PUBLIC)) {
                return Modifier.PUBLIC;
            } else if ("".equals(value)) {
                // If the visibility is "", it means it has no modifier.
                // which is package private. We should return 0 for this modifier.
                return 0;
            } else {
                throw new RuntimeException(
                        "Unknow modifier:" + value);
            }
        }
        return 0;
    }

    /**
     * Transfer string modifier to int one.
     *
     * @param parser XML resource parser
     * @return converted modifier
     */
    private static int jdiffModifierToReflectionFormat(XmlPullParser parser){
        int modifier = 0;
        for (int i = 0;i < parser.getAttributeCount();i++) {
            modifier |= modifierDescriptionToReflectedType(parser.getAttributeName(i),
                    parser.getAttributeValue(i));
        }
        return modifier;
    }
}
