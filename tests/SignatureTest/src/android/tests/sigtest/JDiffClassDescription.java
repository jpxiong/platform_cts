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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

/**
 * This class represents a class description loaded from JDiff file.
 * And it is used for signature test.
 */
public class JDiffClassDescription {
    // Indicates that the class is an annotation
    private static final int CLASS_MODIFIER_ANNOTATION = 0x00002000;
    // Indicates that the class is an enum
    private static final int CLASS_MODIFIER_ENUM       = 0x00004000;

    // Indicates that the method is a bridge method
    private static final int METHOD_MODIFIER_BRIDGE    = 0x00000040;
    // Indicates that the method is declared to take a variable number of arguments
    private static final int METHOD_MODIFIER_VAR_ARGS  = 0x00000080;
    // Indicates that the method is a synthetic method
    private static final int METHOD_MODIFIER_SYNTHETIC = 0x00001000;

    enum JDiffType {
        INTERFACE, CLASS
    }

    @SuppressWarnings("unchecked")
    private Class mClass;

    private String mPackageName;
    private String mShortClassName;

    /**
     * package name + short class name
     */
    private String mAbsoluteClassName;

    private int mModifier;

    private String mExtendedClass;
    private ArrayList<String> mImplInterface;
    private ArrayList<JDiffField> mJdiffField;
    private ArrayList<JDiffMethod> mJdiffMethod;
    private ArrayList<JDiffConstructor> mJdiffConstructor;

    private Vector<String> mInnerClassList;

    private ResultObserver mResultObserver;
    private JDiffType mClassType;

    public JDiffClassDescription() {
        mImplInterface = new ArrayList<String>();
        mJdiffMethod = new ArrayList<JDiffMethod>();
        mJdiffField = new ArrayList<JDiffField>();
        mJdiffConstructor = new ArrayList<JDiffConstructor>();
    }

    /**
     * Add implemented interface name.
     *
     * @param iname name of interface
     */
    public void addImplInterface(String iname) {
        mImplInterface.add(iname);
    }

    /**
     * Add a field.
     *
     * @param field Object which contains field information
     */
    public void addField(JDiffField field) {
        mJdiffField.add(field);
    }

    /**
     * Add a method.
     *
     * @param method Object which contains method information
     */
    public void addMethod(JDiffMethod method) {
        mJdiffMethod.add(method);
    }

    /**
     * Add a constructor.
     *
     * @param tc Object which contains constructor information
     */
    public void addConstructor(JDiffConstructor tc) {
        mJdiffConstructor.add(tc);
    }

    abstract private static class JDiffElement {
        protected String mName;
        protected int mModifier;

        protected JDiffElement(String name, int modifier) {
            mName = name;
            mModifier = modifier;
        }

    }

    /**
     * Object which contains field information.
     */
    final static class JDiffField extends JDiffElement {
        private String mFieldType;

        public JDiffField(String name, String fieldType, int modifier) {
            super(name, modifier);

            mFieldType = fieldType;
        }

        /**
         * Make a readable string according to the class name specified.
         *
         * @param className The specified class name.
         * @return A readable string to represent this field along with the class name.
         */
        public String toReadableString(String className) {
            return className + "#" + mName + "(" + mFieldType + ")";
        }
    }

    /**
     * Object which contains method information.
     */
    static class JDiffMethod extends JDiffElement {
        protected String mReturnType;
        protected ArrayList<String> mParamList;
        protected ArrayList<String> mExceptionList;

        public JDiffMethod(String name, int modifier, String returnType) {
            super(name, modifier);

            if (returnType == null) {
                mReturnType = "void";
            } else {
                mReturnType = scrubJdiffParamType(returnType);
            }

            mParamList = new ArrayList<String>();
            mExceptionList = new ArrayList<String>();
        }

        /**
         * Add parameter.
         *
         * @param param parameter type
         */
        public void addParam(String param) {
            mParamList.add(scrubJdiffParamType(param));
        }

        /**
         * Add exception.
         *
         * @param exceptionName name of exception
         */
        public void addException(String exceptionName) {
            mExceptionList.add(exceptionName);
        }

        /**
         * Make a readable string according to the class name specified.
         *
         * @param className The specified class name.
         * @return A readable string to represent this method along with the class name.
         */
        public String toReadableString(String className) {
            return className + "#" + mName + "(" + convertParamList(mParamList) + ")";
        }

        /**
         * Convert parameter array to one string
         *
         * @param params parameter array
         * @return converted parameter string
         */
        private static String convertParamList(final ArrayList<String> params) {

            StringBuffer paramList = new StringBuffer();

            if (params != null) {
                for (String str : params) {
                    paramList.append(str + ", ");
                }
                if (params.size() > 0) {
                    paramList.delete(paramList.length() - 2, paramList.length());
                }
            }

            return paramList.toString();
        }

    }

    /**
     * Object which contains constructor information.
     */
    final static class JDiffConstructor extends JDiffMethod {
        public JDiffConstructor(String name, int modifier) {
            super(name, modifier, null);
        }

        public JDiffConstructor(String name, String[] param, int modifier) {
            super(name, modifier, null);

            for (int i = 0; i < param.length; i++) {
                addParam(param[i]);
            }
        }
    }

    /**
     * Check test class' name, modifier, fields, constructors, and methods.
     */
    public void checkSignatureCompliance() {
        checkClassCompliance();
        if (mClass != null) {
            checkFieldsCompliance();
            checkConstructorCompliance();
            checkMethodCompliance();
        }
        mImplInterface.clear();
        mJdiffMethod.clear();
        mJdiffField.clear();
        mJdiffConstructor.clear();
    }

    /**
     * Check whether the method parsed from JDiff xml file and Java
     * reflection are compliant.
     */
    private void checkMethodCompliance() {
        Method[] methods = mClass.getDeclaredMethods();
        for (JDiffMethod method : mJdiffMethod) {
            try {
                // this is because jdiff think a method in an interface is not abstract
                if (JDiffType.INTERFACE.equals(mClassType)) {
                    method.mModifier |= Modifier.ABSTRACT;
                }

                Method m = lookupMethod(method, methods);
                if (m == null) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_METHOD,
                                                  method.toReadableString(mAbsoluteClassName));
                } else {
                    if (m.isVarArgs()) {
                        method.mModifier |= METHOD_MODIFIER_VAR_ARGS;
                    }
                    if (m.isBridge()) {
                        method.mModifier |= METHOD_MODIFIER_BRIDGE;
                    }
                    if (m.isSynthetic()) {
                        method.mModifier |= METHOD_MODIFIER_SYNTHETIC;
                    }

                    // FIXME: A workaround to fix the final mismatch on enumeration
                    if (mClass.isEnum() && method.mName.equals("values")) {
                        return;
                    }

                    if (m.getModifiers() != method.mModifier) {
                        mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_METHOD,
                                                      method.toReadableString(mAbsoluteClassName));
                    }
                }
            } catch (Exception e) {
                SignatureTestLog.e("Got exception when checking method compliance", e);
                mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                                              method.toReadableString(mAbsoluteClassName));
            }
        }
    }

    /**
     * Lookup a method from constant pool by reflection.
     *
     * @param method to be tested method
     * @param methods all methods which are from java reflection
     * @return available test method
     */
    @SuppressWarnings("unchecked")
    private Method lookupMethod(JDiffMethod method, Method[] methods) {
        boolean found = false;

        for (Method m : methods) {
            if (method.mName.equals(m.getName())) {
                String jdiffReturnType = method.mReturnType;
                String reflectionReturnType = typeToString(m.getGenericReturnType());
                ArrayList<String> jdiffParamList = method.mParamList;
                if (jdiffReturnType.equals(reflectionReturnType)) {
                    Type[] params = m.getGenericParameterTypes();
                    if (jdiffParamList.size() == params.length) {
                        // Possible match
                        found = true;
                        for (int i = 0; i < jdiffParamList.size(); i++) {
                            if (!compareParam(jdiffParamList.get(i), typeToString(params[i]))) {
                                found = false;
                                break;
                            }
                        }

                        if (found) {
                            return m;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Compare the param from jdiff and param from reflection.
     *
     * @param jdiffParam param parsed from the jdiff xml file.
     * @param reflectionParam param got from the Java reflection.
     * @return True if the two params match, otherwise return false.
     */
    private static boolean compareParam(String jdiffParam, String reflectionParam) {
        if (jdiffParam == null || reflectionParam == null) {
            return false;
        }

        // Most things aren't varargs, so just do a simple compare
        // first.
        if (jdiffParam.equals(reflectionParam)) {
            return true;
        }

        // Check for varargs.  jdiff reports varargs as ..., while
        // reflection reports them as []
        int jdiffParamEndOffset = jdiffParam.indexOf("...");
        int reflectionParamEndOffset = reflectionParam.indexOf("[]");
        if (jdiffParamEndOffset != -1 && reflectionParamEndOffset != -1) {
            jdiffParam = jdiffParam.substring(0, jdiffParamEndOffset);
            reflectionParam = reflectionParam.substring(0, reflectionParamEndOffset);
            return jdiffParam.equals(reflectionParam);
        }

        return false;
    }

    /**
     * Check whether the constructor parsed from JDiff xml file and
     * Java reflection are compliant.
     */
    @SuppressWarnings("unchecked")
    private void checkConstructorCompliance() {
        for (JDiffConstructor con : mJdiffConstructor) {
            try {
                Constructor c = lookupConstructor(con, mClass);
                if (c == null) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_METHOD,
                                                  con.toReadableString(mAbsoluteClassName));
                } else {
                    if (c.isVarArgs()) {// some method's parameter are variable args
                        con.mModifier |= METHOD_MODIFIER_VAR_ARGS;
                    }
                    if (c.getModifiers() != con.mModifier) {
                        mResultObserver.notifyFailure(
                                SignatureTestActivity.FAILURE_TYPE.MISMATCH_METHOD,
                                con.toReadableString(mAbsoluteClassName));
                    }
                }
            } catch (Exception e) {
                SignatureTestLog.e("Got exception when checking constructor compliance", e);
                mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                                              con.toReadableString(mAbsoluteClassName));
            }
        }
    }

    /**
     * Search available constructor.
     *
     * @param jdiffDes constructor which is to be tested
     * @param cons all constructors which are from java reflection
     * @return available constructor
     */
    @SuppressWarnings("unchecked")
    private static Constructor lookupConstructor(JDiffConstructor jdiffDes,
            Class clazz) {
        for (Constructor c : clazz.getDeclaredConstructors()) {
            Type[] params = c.getGenericParameterTypes();
            boolean isStaticClass = ((clazz.getModifiers() & Modifier.STATIC) != 0);

            int startParamOffset = 0;
            int numberOfParams = params.length;

            // non-static inner class -> skip implicit parent pointer
            // as first arg
            if (clazz.isMemberClass() && !isStaticClass && params.length >= 1) {
                startParamOffset = 1;
                --numberOfParams;
            }

            ArrayList<String> jdiffParamList = jdiffDes.mParamList;
            if (jdiffParamList.size() == numberOfParams) {
                boolean isFound = true;
                // i counts jdiff params, j counts reflected params
                int i = 0;
                int j = startParamOffset;
                while (i < jdiffParamList.size()) {
                    if (!compareParam(jdiffParamList.get(i), typeToString(params[j]))) {
                        isFound = false;
                        break;
                    }
                    ++i;
                    ++j;
                }
                if (isFound) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Check all fields in test class.
     */
    private void checkFieldsCompliance() {
        Field[] fields = mClass.getDeclaredFields();

        for (JDiffField field : mJdiffField) {
            try {
                Field f = lookupField(fields, field);
                if (f == null) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_FIELD,
                                                  field.toReadableString(mAbsoluteClassName));
                } else {
                    if (f.getModifiers() != field.mModifier
                        || !f.getType().getCanonicalName().equals(field.mFieldType)) {
                        mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_FIELD,
                                                      field.toReadableString(mAbsoluteClassName));
                    }
                }
            } catch (Exception e) {
                SignatureTestLog.e("Got exception when checking field compliance", e);
                mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                                              field.toReadableString(mAbsoluteClassName));
            }
        }
    }

    /**
     * Lookup field from constant pool by reflection.
     *
     * @param fields all fields which are from java reflection
     * @param field field which is to be tested
     * @return available test field
     */
    private static Field lookupField(Field[] fields, JDiffField field){
        for (Field f : fields) {
            if (f.getName().equals(field.mName)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Check class information.
     */
    @SuppressWarnings("unchecked")
    private void checkClassCompliance() {
        try {
            mClass = null;
            mAbsoluteClassName = mPackageName + "." + mShortClassName;

            if (mShortClassName.indexOf(".") != -1) {
                // The class is an inner class
                mClass = lookupClass();
            } else {
                try {
                    mClass = Class.forName(mAbsoluteClassName);
                } catch (ClassNotFoundException e) {
                    // Ignore the exception, will handle it later
                    if (mAbsoluteClassName.equals("java.util.prefs.Preferences")) {
                        //FIXME: A workaround to fix the loading error for java.util.prefs.Preferences
                        SignatureTestLog.d("java.util.prefs.Preferences workaround hit");
                        return;
                    }
                } catch (java.lang.ExceptionInInitializerError e) {
                    // FIXME: This is a temp workaround to fix the ExceptionInInitializerError
                    SignatureTestLog.d("ExceptionInInitializerError workaround hit");
                    return;
                }
            }

            if (mClass == null) {
                // No class found, notify the observer according to the class type
                if (JDiffType.INTERFACE.equals(mClassType)) {
                    if (mAbsoluteClassName.equals("android.widget.PopupWindow.OnDismissListener")) {
                        // FIXME: A workaround to fix the visibility problem of
                        //          android.widget.PopupWindow.OnDismissListener.
                        SignatureTestLog.d("android.widget.PopupWindow.OnDismissListener workaround hit");
                        return;
                    }
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_INTERFACE,
                                                  mAbsoluteClassName);
                } else {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_CLASS,
                                                  mAbsoluteClassName);
                }

                return;
            }

            int realModifer = mClass.getModifiers();
            if (isAnnotation()) {
                realModifer &= ~CLASS_MODIFIER_ANNOTATION;
            }

            if (mClass.isInterface()) {
                realModifer &= ~Modifier.INTERFACE;
            }
            if (isEnumType() && mClass.isEnum()) {
                realModifer &= ~CLASS_MODIFIER_ENUM;
            }
            if (realModifer != mModifier || (isEnumType() != mClass.isEnum())) {
                logMismatchInterfaceSignature(mAbsoluteClassName);
                return;
            }

            if (mClass.isAnnotation()) {
                // check annotation
                boolean found  = false;
                for (String inter : mImplInterface) {
                    if ("java.lang.annotation.Annotation".equals(inter))
                    {
                        found = true;
                    }
                }

                if (!found) {
                    logMismatchInterfaceSignature(mAbsoluteClassName);
                }
            } else {
                // check father class
                Class superClass = mClass.getSuperclass();
                if (mExtendedClass != null || superClass != null) {
                    if (superClass == null
                        || !superClass.getCanonicalName().equals(mExtendedClass)){

                        if (mAbsoluteClassName.equals("android.hardware.SensorManager")) {
                            // FIXME: Please see Issue 1496822 for more information
                        } else {
                            logMismatchInterfaceSignature(mAbsoluteClassName);
                            return;
                        }
                    }
                }

                // check implements interface
                Class[] interfaces = mClass.getInterfaces();
                HashSet<String> interFaceSet = new HashSet<String>();
                for (Class c : interfaces) {
                    interFaceSet.add(c.getCanonicalName());
                }

                for (String inter : mImplInterface) {
                    if (!interFaceSet.contains(inter)) {
                        logMismatchInterfaceSignature(mAbsoluteClassName);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            SignatureTestLog.e("Got exception when checking field compliance", e);
            mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                                          mAbsoluteClassName);
        }
    }

    private void logMismatchInterfaceSignature(String classFullName) {
        if (JDiffType.INTERFACE.equals(mClassType)) {
            mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_INTERFACE,
                    classFullName);
        } else {
            mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_CLASS,
                    classFullName);
        }
    }

    /**
     * Whether this class is enum.
     *
     * @return true if this class is enum
     */
    private boolean isEnumType() {
        return "java.lang.Enum".equals(mExtendedClass);
    }

    /**
     * Search available class which is the corresponding test class.
     *
     * @return available class if found, else return null.
     */
    @SuppressWarnings("unchecked")
    private Class lookupClass() {
        mInnerClassList = new Vector<String>();
        String[] strArray = mShortClassName.split("\\.");
        for (String str : strArray) {
            mInnerClassList.add(str);
        }
        String fullName = mPackageName + "." + mInnerClassList.remove(0);
        try {
            Class c = Class.forName(fullName);
            return lookupClass(c, fullName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Search aim class
     *
     * @param c class which contains test class
     * @param fullName class' full name
     * @return The aim class, or null if not found.
     */
    @SuppressWarnings("unchecked")
    private Class lookupClass(Class c, String fullName) {
        if (mInnerClassList.size() == 0){
            return null;
        }

        fullName += "." + mInnerClassList.remove(0);
        Class[]cs = c.getClasses();
        for (int i = 0; i < cs.length; i++) {
            String className = cs[i].getCanonicalName();
            if (fullName.equals(className)) {
                return mInnerClassList.size() == 0 ? cs[i] : lookupClass(cs[i], className);
            }
        }

        return null;
    }

    /**
     * Whether this class is annotation.
     *
     * @return true if this class is Annotation.
     */
    private boolean isAnnotation() {
        if (mImplInterface.contains("java.lang.annotation.Annotation")) {
            return true;
        }
        return false;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public void setClassName(String className) {
        mShortClassName = className;
    }

    public String getClassName() {
        return mShortClassName;
    }

    public void setModifier(int modifier) {
        mModifier = modifier;
    }

    public void setType(JDiffType type) {
        mClassType = type;
    }

    public void setExtendsClass(String extendsClass) {
        mExtendedClass = extendsClass;
    }

    public void registerResultObserver(ResultObserver resultObserver) {
        mResultObserver = resultObserver;
    }

    /**
     * A helper function for typeToString that converts WildcardType
     * array into a jdiff compatible string.
     *
     * @param types array of types to format.
     * @return the jdiff formatted string.
     */
    private static String concatWildcardTypes(Type[] types) {
        StringBuffer sb = new StringBuffer();
        int elementNum = 0;
        for (Type t : types) {
            sb.append(typeToString(t));
            if (++elementNum < types.length) {
                sb.append(" & ");
            }
        }
        return sb.toString();
    }

    /**
     * Converts a Type into a jdiff compatible String.  The returned
     * types from this function should match the same Strings that
     * jdiff is providing to us.
     *
     * @param type the type to convert.
     * @return the jdiff formatted string.
     */
    private static String typeToString(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;

            StringBuffer sb = new StringBuffer();
            sb.append(typeToString(pt.getRawType()));
            sb.append("<");

            int elementNum = 0;
            Type[] types = pt.getActualTypeArguments();
            for (Type t : types) {
                sb.append(typeToString(t));
                if (++elementNum < types.length) {
                    sb.append(", ");
                }
            }

            sb.append(">");
            return sb.toString();
        } else if (type instanceof TypeVariable) {
            return ((TypeVariable) type).getName();
        } else if (type instanceof Class) {
            return ((Class) type).getCanonicalName();
        } else if (type instanceof GenericArrayType) {
            String typeName = typeToString(((GenericArrayType) type).getGenericComponentType());
            return typeName + "[]";
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] lowerBounds = wt.getLowerBounds();
            if (lowerBounds.length == 0) {
                String name = "? extends " + concatWildcardTypes(wt.getUpperBounds());

                // Special case for ?
                if (name.equals("? extends java.lang.Object")) {
                    return "?";
                } else {
                    return name;
                }
            } else {
                String name = concatWildcardTypes(wt.getUpperBounds()) +
                        " super " +
                        concatWildcardTypes(wt.getLowerBounds());
                // Another special case for ?
                name = name.replace("java.lang.Object", "?");
                return name;
            }
        } else {
            throw new RuntimeException("Got an unknown java.lang.Type");
        }
    }

    /**
     * Cleans up jdiff parameters to canonicalize them.
     *
     * @param paramType the parameter from jdiff.
     * @return the scrubbed version of the parameter.
     */
    private static String scrubJdiffParamType(String paramType) {
        // <? extends java.lang.Object and <?> are the same, so
        // canonicalize them to one form.
        return paramType.replace("<? extends java.lang.Object>", "<?>");
    }
}
