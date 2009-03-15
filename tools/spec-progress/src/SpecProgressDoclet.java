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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.TypeVariable;

/**
 * Provides a Doclet for checking the correctness and completeness of the
 * Android core library JavaDoc (aka "the spec"). It generates an HTML-based
 * report vaguely similar to the standard JavaDoc output. The following rules
 * are currently implemented:
 * 
 * Each package must have a package.html doc, and all classes must be documented
 * as described below.
 * 
 * Each class must have an individual doc and all members (fields, constructors,
 * methods) must be documented as described below. All type parameters on class 
 * level need to be documented.
 * 
 * Each member must have an individual doc.
 * 
 * Each executable member (constructor or method) must have a "@param" tag
 * describing each declared parameter. "@param" tags for non-existing parameters
 * are not allowed.
 * 
 * Each method that has a non-void return type must have at least one "@return"
 * tag. A method that has a void return type must not have a "@return" tag.
 * 
 * Each executable member must have a "@throws" tag for each declared exception
 * that does not extend java.lang.RuntimeException or java.lang.Error. This
 * tag may refer to a superclass of the exception actually being thrown. Each
 * exception specified by a "@throws" tag must actually be declared by the
 * member, unless it extends java.lang.RuntimeException or java.lang.Error.
 * Again, the exception being thrown might be more specific than the one
 * documented.
 * 
 * Methods that override or implement another method are allowed to be
 * undocumented, resulting in the inherited documentation being used. If such a
 * method is documented anyway, it must have the complete documentation as
 * described above.
 * 
 * Elements that have a "@hide" JavaDoc tag are not considered part of the
 * official API and hence are not required to be documented.
 * 
 * Based on checking the above rules, the Doclet assigns statuses to individual
 * documentation elements as follows:
 * 
 * Red: the element violates at least one of the above rules.
 * 
 * Yellow: the element fulfills all the above rules, but neither it nor one of
 * its parent elements (class, package) has been marked with the
 * "@since Android-1.0" tag.
 * 
 * Green: the element fulfills all the above rules, it does not have any "@cts"
 * tags, and either it or one if its parent elements (class, package) has been
 * marked with the "@since Android-1.0" tag.
 * 
 * These colors propagate upwards in the hierarchy. Parent elements are assigned
 * colors as follows:
 * 
 * Red: At least on the children is red.
 * 
 * Yellow: None of the children are red and at least one of the children is
 * yellow.
 * 
 * Green: All of the children are green.
 * 
 * The ultimate goal, of course, is to get the summary for the complete API
 * green.
 */
public class SpecProgressDoclet {

    public static final int TYPE_FIELD = 0;

    public static final int TYPE_METHOD = 1;

    public static final int TYPE_CLASS = 2;

    public static final int TYPE_PACKAGE = 3;

    public static final int TYPE_ROOT = 4;

    public static final int VALUE_RED = 0;

    public static final int VALUE_YELLOW = 1;

    public static final int VALUE_GREEN = 2;

    public static final String[] COLORS = { "#ffa0a0", "#ffffa0", "#a0ffa0" };

    public static final String[] TYPES = { "Field", "Method", "Class",
                                           "Package", "All packages" };

    /**
     * Holds our basic output directory.
     */
    private File directory;

    /**
     * Holds a reference to the doc for java.lang.RuntimeException, so we can
     * compare against it later.
     */
    private ClassDoc runtimeException;

    /**
     * Holds a reference to the doc for java.lang.Error, so we can
     * compare against it later.
     */
    private ClassDoc error;

    /**
     * States whether to check type parameters on class level. 
     * To enable these checks use the option: '-Xgeneric' 
     */
    private static boolean checkTypeParameters;

    /**
     * Helper class for comparing element with each other, in oder to determine
     * an order. Uses lexicographic order of names.
     */
    private class DocComparator implements Comparator<Doc> {
        public int compare(Doc elem1, Doc elem2) {
            return elem1.name().compareTo(elem2.name());
        }

        public boolean equals(Doc elem) {
            return this == elem;
        }
    }

    /**
     * Class for collecting stats and propagating them upwards in the element
     * hierarchy.
     */
    class Stats {

        /**
         * Holds the element type.
         */
        int type;

        /**
         * Holds the name of the element.
         */
        String name;

        /**
         * Holds information that is sufficient for building a hyperlink.
         */
        String link;

        /**
         * Holds the total number of elements per type (package, class, etc.).
         */
        private int[] numbersPerType = new int[4];

        /**
         * Holds the total number of elements per status value (red, yellow,
         * green).
         */
        private int[] numbersPerValue = new int[3];

        /**
         * Holds the total number of "@cts" comments.
         */
        private int numberOfComments;

        /**
         * Creates a new Stats instance.
         */
        public Stats(int type, String name, String link) {
            this.type = type;
            this.name = name;
            this.link = link;
        }

        /**
         * Adds the contents of a single child element to this instance,
         * propagating values up in the hierachy
         */
        public void add(int type, int status, int comments) {
            numbersPerType[type]++;
            numbersPerValue[status]++;
            numberOfComments += comments;
        }

        /**
         * Adds the contents of a child Stats instance to this instance,
         * propagating values up in the hierachy
         */
        public void add(Stats stats) {
            for (int i = 0; i < numbersPerType.length; i++) {
                numbersPerType[i] += stats.numbersPerType[i];
            }

            for (int i = 0; i < numbersPerValue.length; i++) {
                numbersPerValue[i] += stats.numbersPerValue[i];
            }

            numberOfComments += stats.numberOfComments;
        }

        /**
         * Returns the link.
         */
        public String getLink() {
            return link;
        }

        /**
         * Returns the name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the number of elements per element type.
         */
        public int getNumbersPerType(int type) {
            return numbersPerType[type];
        }

        /**
         * Returns the number of elements per status value.
         */
        public int getNumbersPerValue(int type) {
            return numbersPerValue[type];
        }

        /**
         * Returns the number of comments.
         */
        public int getNumberOfComments() {
            return numberOfComments;
        }

        /**
         * Returns the type of the element to which this Stats instance belongs.
         */
        public int getType() {
            return type;
        }

        /**
         * Returns the accumulated status value.
         */
        public int getValue() {
            if (numbersPerValue[VALUE_RED] != 0) {
                return VALUE_RED;
            } else if (numbersPerValue[VALUE_YELLOW] != 0) {
                return VALUE_YELLOW;
            } else {
                return VALUE_GREEN;
            }
        }

    }

    /**
     * Holds our comparator instance for everything.
     */
    private DocComparator comparator = new DocComparator();

    /**
     * Creates a new instance of the SpecProgressDoclet for a given target
     * directory.
     */
    public SpecProgressDoclet(String directory) {
        this.directory = new File(directory);
    }

    /**
     * Opens a new output file and writes the usual HTML header. Directories
     * are created on demand.
     */
    private PrintWriter openFile(String name, String title) throws IOException {
        System.out.println("Writing file \"" + name + "\"...");
        
        File file = new File(directory, name);
        File parent = file.getParentFile();
        parent.mkdirs();

        OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        PrintWriter printer = new PrintWriter(stream);

        printer.println("<html>");
        printer.println("  <head>");
        printer.println("    <title>" + title + "</title>");
        printer.println("  <head>");
        printer.println("  <body>");
        printer.println("    <h1>" + title + "</h1>");

        return printer;
    }

    /**
     * Closes the given output file, writing the usual HTML footer before.
     */
    private void closeFile(PrintWriter printer) {
        printer.println("  </body>");
        printer.println("</html>");

        printer.flush();
        printer.close();
    }

    /**
     * Processes the whole list of classes that JavaDoc knows about.
     */
    private void process(RootDoc root) throws IOException {
        runtimeException = root.classNamed("java.lang.RuntimeException");
        error = root.classNamed("java.lang.Error");

        PrintWriter printer = openFile("index.html", "All packages");

        printer.println("Generated " + new Date().toString());
        
        Stats derived = new Stats(TYPE_ROOT, "All packages", null);

        printer.println("      <h2>Children</h2>");
        printer.println("      <table width=\"100%\">");
        printStatsHeader(printer);

        PackageDoc[] packages = root.specifiedPackages();
        Arrays.sort(packages, comparator);
        for (PackageDoc pack : packages) {
            if (pack.allClasses().length != 0 && !isHidden(pack)) {
                Stats subStats = processPackage(pack);
                printStats(printer, subStats, true);
                derived.add(subStats);
            }
        }

        printer.println("      </table>");

        printer.println("      <p>");

        printer.println("      <h2>Summary</h2>");
        printer.println("      <table width=\"100%\">");
        printStatsHeader(printer);
        printStats(printer, derived, false);
        printer.println("      </table>");

        closeFile(printer);
    }

    /**
     * Processes the details of a single package.
     */
    private Stats processPackage(PackageDoc pack) throws IOException {
        String file = getPackageDir(pack) + "/package.html";
        PrintWriter printer = openFile(file, "Package " + pack.name());

        Stats derived = new Stats(TYPE_PACKAGE, pack.name(), file);

        printer.println("      <h2>Elements</h2>");
        printer.println("      <table width=\"100%\">");

        printElementHeader(printer);
        processElement(printer, pack, TYPE_PACKAGE, derived);

        printer.println("      </table>");

        printer.println("      <p>");

        printer.println("      <h2>Children</h2>");
        printer.println("      <table width=\"100%\">");

        printStatsHeader(printer);

        ClassDoc[] classes = pack.allClasses();
        Arrays.sort(classes, comparator);
        for (ClassDoc clazz : classes) {
            if (!isHidden(clazz)) {
                Stats subStats = processClass(clazz);
                printStats(printer, subStats, true);
                derived.add(subStats);
            }
        }

        printer.println("      </table>");

        printer.println("      <h2>Summary</h2>");
        printer.println("      <table width=\"100%\">");
        printStatsHeader(printer);
        printStats(printer, derived, false);
        printer.println("      </table>");

        closeFile(printer);

        return derived;
    }

    /**
     * Processes the details of a single class.
     */
    private Stats processClass(ClassDoc clazz) throws IOException {
        String file = getPackageDir(clazz.containingPackage()) + "/" + clazz.name() + ".html";
        PrintWriter printer = openFile(file, "Class " + clazz.name());

        Stats derived = new Stats(TYPE_CLASS, clazz.name(), clazz.name() + ".html");

        printer.println("      <h2>Elements</h2>");
        printer.println("      <table width=\"100%\">");

        printElementHeader(printer);

        processElement(printer, clazz, TYPE_CLASS, derived);

        if(clazz.isEnum()){
            FieldDoc[] enums = clazz.enumConstants();
            Arrays.sort(enums, comparator);
            for(FieldDoc e : enums) {
                processElement(printer, e, TYPE_FIELD, derived);
            }
        }

        FieldDoc[] fields = clazz.fields();
        Arrays.sort(fields, comparator);
        for (FieldDoc field : fields) {
            processElement(printer, field, TYPE_FIELD, derived);
        }

        ConstructorDoc[] constructors = clazz.constructors();
        Arrays.sort(constructors, comparator);
        for (ConstructorDoc constructor : constructors) {
            if (constructor.position() != null) {
                String constPos = constructor.position().toString();
                String classPos = constructor.containingClass().position()
                        .toString();

                if (!constPos.equals(classPos)) {
                    processElement(printer, constructor, TYPE_METHOD, derived);
                }
            }
        }

        HashSet<MethodDoc> methodSet = new HashSet<MethodDoc>();
        
        
        ClassDoc superClass = clazz.superclass();
        MethodDoc[] methods = null;
        if (superClass != null && superClass.isPackagePrivate())
        {
            MethodDoc[] classMethods = clazz.methods();
            for (int i = 0; i < classMethods.length; i++) {
                methodSet.add(classMethods[i]);
            }


            while (superClass != null && superClass.isPackagePrivate()) {
                classMethods = superClass.methods();
                for (int i = 0; i < classMethods.length; i++) {
                    methodSet.add(classMethods[i]);
                }
                superClass = superClass.superclass();
            }

            methods = new MethodDoc[methodSet.size()];
            methodSet.toArray(methods);
        }
        else
        {
            methods = clazz.methods();
        }

        Arrays.sort(methods, comparator);
        for (MethodDoc method : methods) {
            if (!(clazz.isEnum() && ("values".equals(method.name()) ||
                                     "valueOf".equals(method.name())))) {
                processElement(printer, method, TYPE_METHOD, derived);
            }
        }

        printer.println("      </table>");

        printer.println("      <p>");

        printer.println("      <h2>Summary</h2>");
        printer.println("      <table width=\"100%\">");
        printStatsHeader(printer);
        printStats(printer, derived, false);
        printer.println("      </table>");

        closeFile(printer);

        return derived;
    }

    /**
     * Processes a single element.
     */
    private void processElement(PrintWriter printer, Doc doc, int type, Stats derived) {
        if (isHidden(doc)) {
            return;
        }

        List<String> errors = new ArrayList<String>();
        
        boolean documented = isValidComment(doc.commentText());
        boolean inherited = false;

        if(checkTypeParameters && (doc.isClass() || doc.isInterface())){
            boolean typeParamsOk = hasAllTypeParameterDocs((ClassDoc)doc, errors);
            documented = documented && typeParamsOk;
        }

        if (doc.isMethod()) {
            MethodDoc method = (MethodDoc) doc;
            
            if ("".equals(method.commentText().trim())) {
                inherited = method.overriddenMethod() != null || 
                            implementedMethod(method) != null;
                documented = inherited;
            }
        }

        if (!documented) {
            errors.add("Missing or insufficient doc.");
        }
        
        if (!inherited) {
            if (doc.isMethod() || doc.isConstructor()) {
                ExecutableMemberDoc executable = (ExecutableMemberDoc) doc;
                boolean paramsOk = hasAllParameterDocs(executable, errors);
                boolean exceptionsOk = hasAllExceptionDocs(executable, errors);

                documented = documented && paramsOk && exceptionsOk;
            }

            if (doc.isMethod()) {
                MethodDoc method = (MethodDoc) doc;
                boolean resultOk = hasReturnDoc(method, errors);
                documented = documented && resultOk;
            }
        }
        
        boolean reviewed = hasSinceTag(doc);
        Tag[] comments = doc.tags("cts");

        int status = getStatus(documented, reviewed || inherited, comments);

        printer.println("        <tr bgcolor=\"" + COLORS[status] + "\">");
        printer.println("          <td>" + TYPES[type] + "</td>");
        
        if (doc instanceof PackageDoc) {
            printer.println("          <td>" + doc.toString() + "</td>");
        } else {
            String s = doc.name();
            String t = doc.toString();
            
            int i = t.indexOf(s);
            
            if (i != -1) {
                t = t.substring(i);
            }
            
            printer.println("          <td>" + t + "</td>");
        }
        
        printer.println("          <td>" + getFirstSentence(doc) + "</td>");
        printer.println("          <td>" + (documented ? "Yes" : "No") + "</td>");
        printer.println("          <td>" + (reviewed ? "Yes" : "No") + "</td>");
        printer.println("          <td>");

        if (comments.length != 0 || errors.size() != 0) {
            printer.println("            </ul>");

            for (int i = 0; i < comments.length; i++) {
                printer.print("              <li>");
                printer.print(comments[i].text());
                printer.println("</li>");
            }
            
            for (int i = 0; i < errors.size(); i++) {
                printer.print("              <li>");
                printer.print(errors.get(i));
                printer.println("</li>");
            }

            printer.println("            </ul>");
        } else {
            printer.println("&nbsp;");
        }

        printer.println("          </td>");
        printer.println("        </tr>");

        derived.add(type, status, comments.length);
    }

    /**
     * Print the table header for an element table.
     */
    private void printElementHeader(PrintWriter printer) {
        printer.println("        <tr>");
        printer.println("          <td>Type</td>");
        printer.println("          <td>Name</td>");
        printer.println("          <td>First sentence</td>");
        printer.println("          <td>Doc'd</td>");
        printer.println("          <td>Rev'd</td>");
        printer.println("          <td>Comments</td>");
        printer.println("        </tr>");
    }

    /**
     * Print the table header for stats table table.
     */
    private void printStatsHeader(PrintWriter printer) {
        printer.println("        <tr>");
        printer.println("          <td>Type</td>");
        printer.println("          <td>Name</td>");
        printer.println("          <td>#Classes</td>");
        printer.println("          <td>#Fields</td>");
        printer.println("          <td>#Methods</td>");
        printer.println("          <td>#Red</td>");
        printer.println("          <td>#Yellow</td>");
        printer.println("          <td>#Green</td>");
        printer.println("          <td>#Comments</td>");
        printer.println("        </tr>");
    }

    /**
     * Prints a single row to a stats table.
     */
    private void printStats(PrintWriter printer, Stats info, boolean wantLink) {
        printer.println("        <tr bgcolor=\"" + COLORS[info.getValue()] + "\">");
        printer.println("          <td>" + TYPES[info.getType()] + "</td>");

        printer.print("          <td>");
        String link = info.getLink();
        if (wantLink && link != null) {
            printer.print("<a href=\"" + link + "\">" + info.getName() + "</a>");
        } else {
            printer.print(info.getName());
        }
        printer.println("</td>");

        printer.println("          <td>" + info.getNumbersPerType(TYPE_CLASS) + "</td>");
        printer.println("          <td>" + info.getNumbersPerType(TYPE_FIELD) + "</td>");
        printer.println("          <td>" + info.getNumbersPerType(TYPE_METHOD) + "</td>");
        printer.println("          <td>" + info.getNumbersPerValue(VALUE_RED) + "</td>");
        printer.println("          <td>" + info.getNumbersPerValue(VALUE_YELLOW) + "</td>");
        printer.println("          <td>" + info.getNumbersPerValue(VALUE_GREEN) + "</td>");
        printer.println("          <td>" + info.getNumberOfComments() + "</td>");
        printer.println("        </tr>");
    }

    /**
     * Returns the directory for a given package. Basically converts embedded
     * dots in the name into slashes. 
     */
    private File getPackageDir(PackageDoc pack) {
        if (pack == null || pack.name() == null || "".equals(pack.name())) {
            return new File(".");
        } else {
            return new File(pack.name().replace('.', '/'));
        }
    }

    /**
     * Checks whether the given comment is not null and not of length 0.
     */
    private boolean isValidComment(String comment) {
        return comment != null && comment.length() > 0;
    }

    /**
     * Checks whether the given interface or class has documentation for
     * all declared type parameters (no less, no more). 
     */
    private boolean hasAllTypeParameterDocs(ClassDoc doc, List<String> errors) {
        boolean result = true;
        
        TypeVariable[] params = doc.typeParameters();
        Set<String> paramsSet = new HashSet<String>();
        for (TypeVariable param : params) {
            paramsSet.add(param.typeName()); 
        }  

        ParamTag[] paramTags = doc.typeParamTags();
        Map<String, String> paramTagsMap = new HashMap<String, String>();
        for (ParamTag paramTag : paramTags) {
            if (!paramsSet.contains(paramTag.parameterName())) {
                errors.add("Unknown type parameter \"" + paramTag.parameterName() + "\"");
                result = false;
            }
            paramTagsMap.put(paramTag.parameterName(), paramTag.parameterComment());
        }

        for (TypeVariable param : params) {
            if (!isValidComment(paramTagsMap.get(param.typeName()))) {
                errors.add("Undocumented type parameter \"" + param.typeName() + "\"");
                result = false;
            }
        }

        return result;
    }

    /**
     * Checks whether the given executable member has documentation for
     * all declared parameters (no less, no more).  
     */
    private boolean hasAllParameterDocs(ExecutableMemberDoc doc, List<String> errors) {
        boolean result = true;
        
        Parameter params[] = doc.parameters();
        Set<String> paramsSet = new HashSet<String>();
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            paramsSet.add(param.name());
        }

        ParamTag[] paramTags = doc.paramTags();
        Map<String, String> paramTagsMap = new HashMap<String, String>();
        for (int i = 0; i < paramTags.length; i++) {
            ParamTag paramTag = paramTags[i];

            if (!paramsSet.contains(paramTag.parameterName())) {
                errors.add("Unknown parameter \"" + paramTag.parameterName() + "\"");
                result = false;
            }

            paramTagsMap.put(paramTag.parameterName(), paramTag.parameterComment());
        }

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];

            if (!isValidComment(paramTagsMap.get(param.name()))) {
                errors.add("Undocumented parameter \"" + param.name() + "\"");
                result = false;
            }
        }

        return result;
    }

    /**
     * Checks whether the given executable member has documentation for
     * all non-runtime exceptions. Runtime exceptions may or may not be
     * documented.   
     */
    private boolean hasAllExceptionDocs(ExecutableMemberDoc doc, List<String> errors) {
        boolean result = true;
        
        ClassDoc exceptions[] = doc.thrownExceptions();
        Set<ClassDoc> exceptionSet = new HashSet<ClassDoc>();
        for (int i = 0; i < exceptions.length; i++) {
            ClassDoc exception = exceptions[i];
            if (isRelevantException(exception)) {
                exceptionSet.add(exception);
            }
        }

        ThrowsTag[] throwsTags = doc.throwsTags();
        Map<ClassDoc, String> throwsTagsMap = new HashMap<ClassDoc, String>();
        for (int i = 0; i < throwsTags.length; i++) {
            ThrowsTag throwsTag = throwsTags[i];

            if (throwsTag.exception() == null) {
                errors.add("Unknown exception \"" + throwsTag.exceptionName() + "\"");
                result = false;
            } else if (isRelevantException(throwsTag.exception())) {

                ClassDoc exception = throwsTag.exception();
                while (exception != null && !exceptionSet.contains(exception)) {
                    exception = exception.superclass();
                }
                if (exception == null) {
                    errors.add("Unknown exception \"" + throwsTag.exceptionName() + "\"");
                    result = false;
                }
            }
            
            throwsTagsMap.put(throwsTag.exception(), throwsTag.exceptionComment());
        }

        for (int i = 0; i < exceptions.length; i++) {
            ClassDoc exception = exceptions[i];
            boolean found = false;
            
            for (int j = 0; j < throwsTags.length && !found; j++) {
                ThrowsTag throwsTag = throwsTags[j];
                
                ClassDoc candidate = throwsTag.exception();
                if (candidate != null) {
                    if (candidate.equals(exception) || candidate.subclassOf(exception)) {
                        if (isValidComment(throwsTag.exceptionComment())) {
                            found = true;
                        }
                    }
                }
            }
            
            if (!found) {
                errors.add("Undocumented exception \"" + exception.name() + "\"");
                result = false;
            }
        }

        return result;
    }

    /**
     * Checks whether an exception needs to be documented. Runtime exceptions
     * and errors don't necessarily need documentation (although it doesn't
     * hurt to have it).
     */
    private boolean isRelevantException(ClassDoc clazz) {
        return !(clazz.subclassOf(runtimeException) || clazz.subclassOf(error));
    }
    
    /**
     * Checks whether the given method has documentation for the return value.
     */
    private boolean hasReturnDoc(MethodDoc method, List<String> errors) {
        boolean result = true;
        
        if (!"void".equals(method.returnType().typeName())) {
            Tag[] returnTags = method.tags("return");

            if (returnTags.length == 0) {
                errors.add("Missing result.");
                result = false;
            }

            for (int i = 0; i < returnTags.length; i++) {
                Tag tag = returnTags[i];
                if (!isValidComment(tag.text())) {
                    errors.add("Insufficient result.");
                    result = false;
                }
            }
        } else {
            Tag[] returnTags = method.tags("return");
            if (returnTags.length != 0) {
                errors.add("Unknown result.");
                result = false;
            }
        }

        return result;
    }

    /**
     * Returns the first sentence for the given documentation element.
     */
    private String getFirstSentence(Doc doc) {
        StringBuilder builder = new StringBuilder();

        Tag[] tags = doc.firstSentenceTags();
        for (int i = 0; i < tags.length; i++) {
            Tag tag = tags[i];

            if ("Text".equals(tag.kind())) {
                builder.append(tag.text());
            } else {
                builder.append("{" + tag.toString() + "}");
            }
        }

        return builder.toString();
    }

    /**
     * Returns the interface method that a given method implements, or null if
     * the method does not implement any interface method.
     */
    private MethodDoc implementedMethod(MethodDoc doc) {
        ClassDoc clazz = doc.containingClass();
        MethodDoc myDoc = null;
        while(clazz != null && myDoc == null){
        ClassDoc[] interfaces = clazz.interfaces();
            myDoc = implementedMethod0(doc, interfaces);
            clazz = clazz.superclass();
        }
        return myDoc;
    }

    /**
     * Recursive helper method for finding out which interface method a given
     * method implements.
     */
    private MethodDoc implementedMethod0(MethodDoc doc, ClassDoc[] interfaces) {
        for (int i = 0; i < interfaces.length; i++) {
            ClassDoc classDoc = interfaces[i];

            MethodDoc[] methods = classDoc.methods();
            for (int j = 0; j < methods.length; j++) {
                MethodDoc methodDoc = methods[j];
                if (doc.overrides(methodDoc)) {
                    return methodDoc;
                }
            }
        }

        for (int i = 0; i < interfaces.length; i++) {
            MethodDoc myDoc = implementedMethod0(doc, interfaces[i].interfaces());
            if (myDoc != null) {
                return myDoc;
            }
        }

        return null;
    }

    /**
     * Checks whether the given documentation element has a "@since" tag for
     * Android.
     */
    private boolean hasSinceTag(Doc doc) {
        Tag[] tags = doc.tags("since");

        for (int i = 0; i < tags.length; i++) {
            if ("Android 1.0".equals(tags[i].text())) {
                return true;
            }
        }

        if (doc instanceof MemberDoc) {
            return hasSinceTag(((MemberDoc)doc).containingClass());
        }
        
        if (doc instanceof ClassDoc) {
            return hasSinceTag(((ClassDoc)doc).containingPackage());
        }
        
        return false;
    }

    /**
     * Checks whether the given documentation element has a "@hide" tag that
     * excludes it from the official API.
     */
    private boolean isHidden(Doc doc) {
        Tag[] tags = doc.tags("hide");

        return tags != null && tags.length != 0;
    }
    
    /**
     * Determines the status of an element based on the existence of
     * documentation, the review status, and any comments it might have.
     */
    private int getStatus(boolean documented, boolean reviewed, Tag[] comments) {
        if (!documented) {
            return VALUE_RED;
        } else if (reviewed && comments.length == 0) {
            return VALUE_GREEN;
        } else {
            return VALUE_YELLOW;
        }
    }

    /**
     * Called by JavaDoc to find our which command line arguments are supported
     * and how many parameters they take. Part of the JavaDoc API.
     */
    public static int optionLength(String option) {
        if ("-d".equals(option)) {
            return 2;
        } else if("-Xgeneric".equals(option)){
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Returns a particular command line argument for a given option.
     */
    private static String getOption(RootDoc root, String option, int index, String defValue) {
        String[][] allOptions = root.options();
        for (int i = 0; i < allOptions.length; i++) {
            if (allOptions[i][0].equals(option)) {
                return allOptions[i][index];
            }
        }

        return defValue;
    }
    
    /**
     * Returns whether the specified option is present.
     */
    private static boolean isOptionSet(RootDoc root, String option){
        String[][] allOptions = root.options();
        for (int i = 0; i < allOptions.length; i++) {
            if (allOptions[i][0].equals(option)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called by JavaDoc to find out which Java version we claim to support.
     * Part of the JavaDoc API.
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    /**
     * The main entry point called by JavaDoc after all required information has
     * been collected. Part of the JavaDoc API.
     */
    public static boolean start(RootDoc root) {
        try {
            String target = getOption(root, "-d", 1, ".");
            checkTypeParameters = isOptionSet(root, "-Xgeneric");

            SpecProgressDoclet doclet = new SpecProgressDoclet(target);
            doclet.process(root);

            File file = new File(target, "index.html");
            System.out.println("Please see complete report in " + 
                    file.getAbsolutePath());
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

}
