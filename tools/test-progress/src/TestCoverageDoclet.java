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

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 */
public class TestCoverageDoclet {

    public static final int TYPE_FIELD = 0;
    public static final int TYPE_METHOD = 1;
    public static final int TYPE_CLASS = 2;
    public static final int TYPE_PACKAGE = 3;
    public static final int TYPE_ROOT = 4;
    public static final int VALUE_RED = 0;
    public static final int VALUE_YELLOW = 1;
    public static final int VALUE_GREEN = 2;
    public static final String[] COLORS = { "#ffa0a0", "#ffffa0", "#a0ffa0" };
    public static final String[] TYPES = { "Field", "Method", "Class", "Package", "All packages" };

    /**
     * Holds our basic output directory.
     */
    private File directory;

    private Map<ExecutableMemberDoc, AnnotationPointer> resolved =
            new HashMap<ExecutableMemberDoc, AnnotationPointer>(8192);

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

    private class MemberComparator implements Comparator<ExecutableMemberDoc> {
        public int compare(ExecutableMemberDoc mem1, ExecutableMemberDoc mem2) {
            return mem1.toString().compareTo(mem2.toString());
        }
    }

    class MyStats {
        private String name;
        private String link;
        private int elemCnt = 0;
        private int[] ryg = new int[3];
        private String extra;

        public MyStats(int type, String name, String link) {
            this.name = name;
            this.link = link;
        }

        public void add(MyStats subStats) {
           elemCnt++;
           for (int i = 0; i < ryg.length; i++) {
               ryg[i]+= subStats.ryg[i];
           }
        }

        public int getCountFor(int color) {
            return ryg[color];
        }

        public String getStat() {
            float coverage = (float)(ryg[1]+ryg[2]) / (float)(ryg[0]+ryg[1]+ryg[2]);
            return "red: "+ryg[0]+", yellow:"+ryg[1]+", green:"+ryg[2]+",coverage:"+coverage;
        }

        public void inc(int color) {
            ryg[color]++;
        }

        public String getLink() {
            return link;
        }

        public String getName() {
            return name;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }
    }

    /**
     * Holds our comparator instance for everything.
     */
    private DocComparator comparator = new DocComparator();
    private MemberComparator membercomparator = new MemberComparator();

    /**
     * Creates a new instance of the TestProgressDoclet for a given target
     * directory.
     */
    public TestCoverageDoclet(String directory) {
        this.directory = new File(directory);
    }

    /**
     * Opens a new output file and writes the usual HTML header. Directories
     * are created on demand.
     */
    private PrintWriter openFile(String name, String title) throws IOException {
        File file = new File(directory, name);
        File parent = file.getParentFile();
        parent.mkdirs();

        PrintWriter printer = new PrintWriter(new FileOutputStream(file));

        printer.println("<html>");
        printer.println("  <head>");
        printer.println("    <title>" + title + "</title>");
        printer.println("<style type=\"text/css\">\n"+
                "body { }\n"+
                "table {border-width: 0px; border: solid; border-collapse: collapse;}\n"+
                "table tr td { vertical-align:top; padding:3px; border: 1px solid black;}\n"+
                "</style>");
        printer.println("  </head>");
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

    private class TablePrinter {
        private PrintWriter pr;

        public TablePrinter(PrintWriter pr) {
            this.pr = pr;
        }

        public void printRow(int color, String... columns) {
            String colo = COLORS[color];
            pr.print("<tr style=\"background-color:"+colo+"\">");
            for (String col : columns) {
                pr.print("<td>"+col+"</td>");
            }
            pr.print("</tr>");
        }

        public void printRow(String... columns) {
            printRow(1, columns);
        }

        public void printPlain(String val) {
            pr.print(val);
        }

    }

    /**
     * Processes the whole list of classes that JavaDoc knows about.
     */
    private void process(RootDoc root) throws IOException {

        // 1. traverse all test-classes (those extending JUnit's TestCase)
        // and collect the annotation info. Print which test classes
        // need annotating
        PrintWriter pr = openFile("test-annotation.html", "test class annotation coverage");
        TablePrinter printer = new TablePrinter(pr);
        printer.printPlain("<table>");
        printer.printRow("className", "annotated methods", "total methods", "percentage");

        ClassDoc[] classes = root.classes();
        Arrays.sort(classes, new Comparator<ClassDoc>() {
            public int compare(ClassDoc c1, ClassDoc c2) {
                return c1.toString().compareTo(c2.toString());
            }});
        for (ClassDoc classDoc : classes) {
            if (extendsJUnitTestCase(classDoc)) {
                processTestClass(classDoc, printer);
            }
        }
        printer.printPlain("</table>");
        closeFile(pr);
        //dumpInfo();

        // 2. traverse all "normal" (non-junit) source files, for each method
        // get its status and propagate it up the tree
        MyStats stats = new MyStats(TYPE_ROOT, "All", "aaa.html");
        PrintWriter aprinter = openFile("index.html", "All packages");
        aprinter.println("Generated " + new Date().toString());
        aprinter.println("<br/><a href=\"test-annotation.html\">annotation progress of test classes</a><br/>");
        aprinter.println("<br/><a href=\"hidden-doc.html\">hidden classes and methods</a><br/>");
        aprinter.println("<br/><a href=\"interfaces.html\">interfaces</a><br/>");
        aprinter.println("<h2>Packages</h2>");
        aprinter.println("<table>");

        PrintWriter hiddenDocPr = openFile("hidden-doc.html", "hidden classes and methods list");
        TablePrinter hiddenDocPrinter = new TablePrinter(hiddenDocPr);
        hiddenDocPrinter.printPlain("<table>");
        hiddenDocPrinter.printRow("Package Name", "Class Name", "Method Name");

        PrintWriter interfacePr = openFile("interfaces.html", "interface list");
        TablePrinter interfacePrinter = new TablePrinter(interfacePr);
        interfacePrinter.printPlain("<table>");
        interfacePrinter.printRow("packageName", "className");

        PackageDoc[] packages = root.specifiedPackages();
        Arrays.sort(packages, comparator);
        for (PackageDoc pack : packages) {
            if (pack.allClasses().length != 0) {

                if (pack.name().endsWith(".cts")) {
                    // Skip the cts test packages
//                    System.out.println(">>>>>>>>>>>Skip package: " + pack.name());
                } else {
                    MyStats subStat = processPackage(pack, hiddenDocPrinter, interfacePrinter);
                    
                    System.out.println("package " + pack.name() + " has " + subStat.getCountFor(0) + " red.");
                    printStats(aprinter, subStat, true);
                    stats.add(subStat);
                }
            }
        }
        

        System.out.println("Total has " + stats.getCountFor(0) + " red.");

        interfacePrinter.printPlain("</table>");
        closeFile(interfacePr);

        hiddenDocPrinter.printPlain("</table>");
        closeFile(hiddenDocPr);

        aprinter.println("</table>");
        aprinter.println("<h2>Summary</h2>");
        aprinter.println("<table>");
        printStats(aprinter, stats, false);
        aprinter.println("</table>");

        closeFile(aprinter);
    }

    /*private void processTargetClass(ClassDoc classDoc) {
        System.out.println("class:"+classDoc);
        // show all public/protected constructors
        for (ExecutableMemberDoc constr : classDoc.constructors()) {
            if (constr.isPublic() || constr.isProtected()) {
                processTargetMC(constr);
            }
        }
        // show all public/protected methods
        for (ExecutableMemberDoc method : classDoc.methods()) {
            if (method.isPublic() || method.isProtected()) {
                processTargetMC(method);
            }
        }
    }*/

    /*private void dumpInfo() {
        for (Map.Entry<ExecutableMemberDoc, AnnotationPointer> entry : resolved.entrySet()) {
            ExecutableMemberDoc mdoc = entry.getKey();
            AnnotationPointer ap = entry.getValue();
            System.out.println("----- entry -----------------------");
            System.out.println("target:"+mdoc.toString());
            System.out.println("=");
            for (MethodDoc meth : ap.testMethods) {
                System.out.println("test method:"+meth);
            }
        }
    }*/

    private void processTestClass(ClassDoc classDoc, TablePrinter printer) {
        // System.out.println("Processing >>> " + classDoc);
        // collects all testinfo-annotation info of this class
        ClassDoc targetClass = null;
        // get the class annotation which names the default test target class
        AnnotationDesc[] cAnnots = classDoc.annotations();
        for (AnnotationDesc cAnnot : cAnnots) {

            AnnotationTypeDoc atype = cAnnot.annotationType();
            if (atype.toString().equals("dalvik.annotation.TestTargetClass")) {
                // single member annot with one child 'value'
                ElementValuePair[] cpairs = cAnnot.elementValues();
                ElementValuePair evp = cpairs[0];
                AnnotationValue av = evp.value();
                Object obj = av.value();

                // value must be a class doc
                if (obj instanceof ClassDoc) {
                    targetClass = (ClassDoc) obj;
                } else if (obj instanceof ParameterizedType) {
                    targetClass = ((ParameterizedType)obj).asClassDoc();
                }
                else throw new RuntimeException("annotation elem value is of type "+obj.getClass().getName());
            }
        }

        // now visit all methods (junit test methods - therefore we need not visit the constructors
        AnnotStat ast = new AnnotStat();

        //System.out.println("checking:"+classDoc.qualifiedName());

        MethodDoc[] methods = classDoc.methods();
        String note = "";
        if (targetClass == null) {
            note += "<br/>targetClass annotation missing!<br/>";
        }

        for (MethodDoc methodDoc : methods) {
            // ignore if it is not a junit test method
            if (!methodDoc.name().startsWith("test")) continue;
            if (classDoc.qualifiedName().equals("tests.api.java.io.BufferedInputStreamTest")) {
                //System.out.println("method: "+methodDoc.toString());
            }

            if (targetClass == null) {
                // if the targetClass is missing, count all methods as non-annotated
                ast.incMethodCnt(false);
            } else {
                String error = processTestMethod(methodDoc, ast, targetClass);
                if (error != null) {
                    note+="<br/><b>E:</b> "+error;
                }
            }
        }

        int man = ast.cntMethodWithAnnot;
        int mto = ast.cntAllMethods;
        float perc = mto==0? 100f : ((float)man)/mto * 100f;

        printer.printRow(man==mto && note.equals("")? 2:0, classDoc.qualifiedName(), ""+ast.cntMethodWithAnnot, ""+ast.cntAllMethods,
                ""+perc+ note);

    }

    private class AnnotStat {
        int cntMethodWithAnnot = 0;
        int cntAllMethods = 0;
        /**
         * @param correctAnnot
         */
        public void incMethodCnt(boolean correctAnnot) {
            cntAllMethods++;
            if (correctAnnot) {
                cntMethodWithAnnot++;
            }
        }
    }

    // points from one targetMethod to 0..n testMethods which test the target method
    private class AnnotationPointer {
        AnnotationPointer(ExecutableMemberDoc targetMethod) {
            this.targetMethod = targetMethod;
        }

        final ExecutableMemberDoc targetMethod;
        List<MethodDoc> testMethods = new ArrayList<MethodDoc>();

        public void addTestMethod(MethodDoc testMethod) {
            if (testMethods.contains(testMethod)) {
                System.out.println("warn: testMethod refers more than once to the targetMethod, testMethod="+testMethod);
            } else {
                testMethods.add(testMethod);
            }
        }
    }

    private String processTestMethod(MethodDoc methodDoc, AnnotStat ast, ClassDoc targetClass) {
        //System.out.println("processing method: " + methodDoc);
        // get all per-method-annotation
        boolean correctAnnot = false;
        AnnotationDesc[] annots = methodDoc.annotations();
        for (AnnotationDesc annot : annots) {
            if (annot.annotationType().toString().equals("dalvik.annotation.TestInfo")) {
                ElementValuePair[] pairs = annot.elementValues();
                for (ElementValuePair kv : pairs) {
                    if (kv.element().qualifiedName().equals("dalvik.annotation.TestInfo.targets")) {
                        // targets is an [] type
                        AnnotationValue[] targets = (AnnotationValue[]) kv.value().value();
                        for (AnnotationValue tval : targets) {
                            // the test targets must be annotations themselves
                            AnnotationDesc targetAnnot = (AnnotationDesc) tval.value();
                            ExecutableMemberDoc targetMethod = getTargetMethod(targetAnnot, targetClass);
                            if (targetMethod != null) {
                                AnnotationPointer tar = getAnnotationPointer(targetMethod, true);
                                tar.addTestMethod(methodDoc);
                                correctAnnot = true;
                            } else {
                                ast.incMethodCnt(false);
                                return "error: could not resolve targetMethod for class "+targetClass+", annotation was:"+targetAnnot+", testMethod = "+methodDoc.toString();
                            }
                        }
                    }
                }
            } // else some other annotation
        }
        ast.incMethodCnt(correctAnnot);
        return null;
    }

    private AnnotationPointer getAnnotationPointer(ExecutableMemberDoc targetMethod, boolean create) {
        AnnotationPointer ap = resolved.get(targetMethod);
        if (create && ap == null) {
            ap = new AnnotationPointer(targetMethod);
            resolved.put(targetMethod, ap);
        }
        return ap;
    }

    private ExecutableMemberDoc getTargetMethod(AnnotationDesc targetAnnot,
            ClassDoc targetClass) {
        // targetAnnot like @android.annotation.TestTarget(methodName="group", methodArgs=int.class)
        ElementValuePair[] pairs = targetAnnot.elementValues();
        String methodName = null;
        String args = "";
        for (ElementValuePair kval : pairs) {
            if (kval.element().name().equals("methodName")) {
                methodName = (String) kval.value().value();
            } else if (kval.element().name().equals("methodArgs")) {
                AnnotationValue[] vals = (AnnotationValue[]) kval.value().value();
                for (int i = 0; i < vals.length; i++) {
                    AnnotationValue arg = vals[i];
                    String argV;
                    if (arg.value() instanceof ClassDoc) {
                       ClassDoc cd = (ClassDoc)arg.value();
                       argV = cd.qualifiedName();
                    } else { // primitive type or array type
                        // is there a nicer way to do this?
                        argV = arg.toString();
                    }
                    // strip .class out of args since signature does not contain those
                    if (argV.endsWith(".class")) {
                        argV = argV.substring(0, argV.length()-6);
                    }
                    args+= (i>0? ",":"") + argV;
                }
            }
        }
        // both methodName and methodArgs != null because of Annotation definition

        String refSig = methodName+"("+args+")";
        //System.out.println("Check " + refSig);
        // find the matching method in the target class
        // check all methods
        for (ExecutableMemberDoc mdoc : targetClass.methods()) {
            if (equalsSignature(mdoc, refSig)) {
                return mdoc;
            }
        }
        // check constructors, too
        for (ExecutableMemberDoc mdoc : targetClass.constructors()) {
            if (equalsSignature(mdoc, refSig)) {
                return mdoc;
            }
        }
        return null;
    }

    private boolean equalsSignature(ExecutableMemberDoc mdoc, String refSignature) {
        Parameter[] params = mdoc.parameters();
        String targs = "";
        for (int i = 0; i < params.length; i++) {
            Parameter parameter = params[i];
            // check for generic type types
            Type ptype = parameter.type();
            TypeVariable typeVar = ptype.asTypeVariable();
            String ptname;
            if (typeVar != null) {
                ptname = "java.lang.Object"; // the default fallback
                Type[] bounds = typeVar.bounds();
                if (bounds.length > 0) {
                    ClassDoc typeClass = bounds[0].asClassDoc();
                    ptname = typeClass.qualifiedName();
                }
            } else {
                // regular var
                //ptname = parameter.type().qualifiedTypeName();
                ptname = parameter.type().toString();

                //System.out.println("quali:"+ptname);
                //ptname = parameter.typeName();
                // omit type signature
                ptname = ptname.replaceAll("<.*>","");
            }
            targs+= (i>0? ",":"") + ptname;
        }
        String testSig = mdoc.name()+"("+targs+")";

        //return testSig.equals(refSignature);
        if (testSig.equals(refSignature)) {
            //System.out.println("found: Sig:"+testSig);
            return true;
        } else {
            //System.out.println("no match: ref = "+refSignature+", test = "+testSig);
            return false;
        }
    }

    private boolean extendsJUnitTestCase(ClassDoc classDoc) {
        //junit.framework.TestCase.java
        ClassDoc curClass = classDoc;
        while ((curClass = curClass.superclass()) != null) {
            if (curClass.toString().equals("junit.framework.TestCase")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Processes the details of a single package.
     * @param hiddenDocPrinter
     * @param excludedClassPrinter
     * @param interfacePrinter
     */
    private MyStats processPackage(PackageDoc pack, TablePrinter hiddenDocPrinter,
            TablePrinter interfacePrinter) throws IOException {
        String file = getPackageDir(pack) + "/package.html";
        PrintWriter printer = openFile(file, "Package " + pack.name());

        MyStats stats = new MyStats(TYPE_PACKAGE, pack.name(), file);
        printer.println("<table>");

        ClassDoc[] classes = pack.allClasses();
        Arrays.sort(classes, comparator);
        for (ClassDoc clazz : classes) {
            if (extendsJUnitTestCase(clazz)) {
                printer.println("<tr><td>ignored(junit):"+clazz.name()+"</td></tr>");
            } else if (isHiddenClass(clazz)) {
                hiddenDocPrinter.printRow(pack.name(), clazz.name(), "*");
            } else if (clazz.isInterface()) {
                interfacePrinter.printRow(pack.name(), clazz.name());
            } else {
                MyStats subStats = processClass(clazz, hiddenDocPrinter);
                printStats(printer, subStats, true);
                stats.add(subStats);
            }
        }
        printer.println("</table>");
        closeFile(printer);
        return stats;
    }

    private boolean isHiddenClass(ClassDoc clazz) {
        if (clazz == null) {
            return false;
        }

        if (isHiddenDoc(clazz)) {
            return true;
        }

        // If outter class is hidden, this class should be hidden as well
        return isHiddenClass(clazz.containingClass());
    }

    private boolean isHiddenDoc(Doc doc) {
        // Since currently we have two kinds of annotations to mark a class as hide:
        //  1. @hide
        //  2. {@hide}
        // So we should consider both conditions.
        for (Tag t : doc.tags()) {
            if (t.name().equals("@hide")) {
                return true;
            }
        }

        for (Tag t : doc.inlineTags()) {
            if (t.name().equals("@hide")) {
                return true;
            }
        }

        return false;
    }
    
    private MyStats processClass(ClassDoc clazz, TablePrinter hiddenDocPrinter) throws IOException {
        //System.out.println("Process source class: " + clazz);
        String file = getPackageDir(clazz.containingPackage()) + "/" + clazz.name() + ".html";
        PrintWriter printer = openFile(file, "Class " + clazz.name());

        String packageName = clazz.containingPackage().name();
        String className = clazz.name();
        
        MyStats stats = new MyStats(TYPE_CLASS, className, className+".html");
        printer.println("<table><tr><td>name</td><td>tested by</td></tr>");
        ConstructorDoc[] constructors = clazz.constructors();
        Arrays.sort(constructors, comparator);
        for (ConstructorDoc constructor : constructors) {
            //System.out.println("constructor: " + constructor);
            if (isHiddenDoc(constructor)) {
                hiddenDocPrinter.printRow(packageName, className, constructor.name());
            } else if (!isGeneratedConstructor(constructor)) {
                MyStats subStat = processElement(constructor);
                printStats(printer, subStat, false);
                stats.add(subStat);
            }
        }

        MethodDoc[] methods = clazz.methods();
        Arrays.sort(methods, comparator);
        for (MethodDoc method : methods) {
            //System.out.println("method: " + method);
            if ("finalize".equals(method.name())) {
                // Skip finalize method
            } else if (isHiddenDoc(method)) {
                hiddenDocPrinter.printRow(packageName, className, method.name());
            } else if (method.isAbstract()) {
                // Skip abstract method
            } else {
                MyStats subStat = processElement(method);
                printStats(printer, subStat, false);
                stats.add(subStat);
            }
        }

        printer.println("</table>");
        closeFile(printer);
        return stats;
    }

    /**
     * Determines whether a constructor has been automatically generated and is
     * thus not present in the original source. The only way to find out seems
     * to compare the source position against the one of the class. If they're
     * equal, the constructor does not exist. It's a bit hacky, but it works.
     */
    private boolean isGeneratedConstructor(ConstructorDoc doc) {
        SourcePosition constPos = doc.position();
        SourcePosition classPos = doc.containingClass().position();

        return ("" + constPos).equals("" + classPos);
    }

    /**
     * Processes a single method/constructor.
     */
    private MyStats processElement(ExecutableMemberDoc method) {
        //int color = getColor(doc)
        //derived.add(subStats)
        AnnotationPointer ap = getAnnotationPointer(method, false);
        MyStats stats = new MyStats(TYPE_METHOD, "<b>"+method.name() + "</b> "+method.signature(), null);
        int refCnt = 0;
        if (ap != null) {
            refCnt = ap.testMethods.size();
            String by = "";
            List<MethodDoc> testM = ap.testMethods;
            Collections.sort(testM, membercomparator);
            for (MethodDoc teme : testM) {
                by+= "<br/>"+teme.toString();
            }
            stats.setExtra(by);
        } // else this class has no single test that targets one of its method

        if (refCnt == 0) {
            stats.inc(VALUE_RED);
        } else if (refCnt == 1) {
            stats.inc(VALUE_YELLOW);
        } else {
            stats.inc(VALUE_GREEN);
        }
        return stats;
    }

    /**
     * Prints a single row to a stats table.
     */
    private void printStats(PrintWriter printer, MyStats info, boolean wantLink) {
        int red = info.getCountFor(VALUE_RED);
        int yellow = info.getCountFor(VALUE_YELLOW);

        printer.println("<tr>");
      
        // rule for coloring:
        // if red > 0 -> red
        // if yellow > 0 -> yellow
        // else green
        int color;
        if (red > 0) {
            color = VALUE_RED;
        } else if (yellow > 0) {
            color = VALUE_YELLOW;
        } else {
            color = VALUE_GREEN;
        }

        printer.println("<td bgcolor=\""+COLORS[color]+"\">");
        String link = info.getLink();
        if (wantLink && link != null) {
            printer.print("<a href=\"" + link + "\">" + info.getName() + "</a>");
        } else {
            printer.print(info.getName());
        }
        printer.println(" ("+info.getStat()+") </td>");
        if (info.getExtra()!=null) {
            printer.println("<td>"+info.getExtra()+"</td>");
        }
        printer.println("</tr>");
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
     * Called by JavaDoc to find our which command line arguments are supported
     * and how many parameters they take. Part of the JavaDoc API.
     */
    public static int optionLength(String option) {
        if ("-d".equals(option)) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * Called by JavaDoc to query a specific command line argument. Part of the
     * JavaDoc API.
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
            TestCoverageDoclet doclet = new TestCoverageDoclet(target);
            doclet.process(root);

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

}
