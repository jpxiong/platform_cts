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
package com.android.cts.tradefed.command;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.result.ITestResultRepo;
import com.android.cts.tradefed.result.ITestSummary;
import com.android.cts.tradefed.result.PlanCreator;
import com.android.cts.tradefed.result.TestResultRepo;
import com.android.cts.tradefed.testtype.ITestPackageRepo;
import com.android.cts.tradefed.testtype.TestPackageRepo;
import com.android.tradefed.command.Console;
import com.android.tradefed.config.ArgsOptionParser;
import com.android.tradefed.config.ConfigurationException;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RegexTrie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Specialization of trade federation console that adds CTS commands to list plans and packages.
 */
public class CtsConsole extends Console {

    protected static final String ADD_PATTERN = "a(?:dd)?";

    private CtsBuildHelper mCtsBuild = null;


    CtsConsole() {
        super();
    }

    /**
     * Adds the 'list packages' and 'list plans' commands
     */
    @Override
    protected void setCustomCommands(RegexTrie<Runnable> trie, List<String> genericHelp,
            Map<String, String> commandHelp) {
        trie.put(new Runnable() {
            @Override
            public void run() {
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    listPlans(ctsBuild);
                }
            }
        }, LIST_PATTERN, "p(?:lans)?");
        trie.put(new Runnable() {
            @Override
            public void run() {
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    listPackages(ctsBuild);
                }
            }
        }, LIST_PATTERN, "packages");
        trie.put(new Runnable() {
            @Override
            public void run() {
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    listResults(ctsBuild);
                }
            }
        }, LIST_PATTERN, "r(?:esults)?");

        // find existing help for 'LIST_PATTERN' commands, and append these commands help
        String listHelp = commandHelp.get(LIST_PATTERN);
        if (listHelp == null) {
            // no help? Unexpected, but soldier on
            listHelp = new String();
        }
        String combinedHelp = listHelp +
                "\tp[lans]\t\tList all CTS test plans" + LINE_SEPARATOR +
                "\tpackages\tList all CTS packages" + LINE_SEPARATOR +
                "\tr[esults]\tList all CTS results" + LINE_SEPARATOR;
        commandHelp.put(LIST_PATTERN, combinedHelp);

        ArgRunnable<CaptureList> addDerivedCommand = new ArgRunnable<CaptureList>() {
            @Override
            public void run(CaptureList args) {
                // Skip 2 tokens to get past addPattern and "derivedplan"
                String[] flatArgs = new String[args.size() - 2];
                for (int i = 2; i < args.size(); i++) {
                    flatArgs[i - 2] = args.get(i).get(0);
                }
                CtsBuildHelper ctsBuild = getCtsBuild();
                if (ctsBuild != null) {
                    addDerivedPlan(ctsBuild, flatArgs);
                }
            }
        };
        trie.put(addDerivedCommand, ADD_PATTERN, "d(?:erivedplan?)", null);
        commandHelp.put(ADD_PATTERN, String.format(
                "%s help:" + LINE_SEPARATOR +
                "\tderivedplan      Add a derived plan" + LINE_SEPARATOR,
                ADD_PATTERN));


    }

    private void listPlans(CtsBuildHelper ctsBuild) {
        FilenameFilter xmlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
        try {
            for (File planFile : ctsBuild.getTestPlansDir().listFiles(xmlFilter)) {
                printLine(FileUtil.getBaseName(planFile.getName()));
            }
        }
        catch (FileNotFoundException e) {
            printLine("Could not find CTS plan folder");
        }
    }

    private void listPackages(CtsBuildHelper ctsBuild) {
        ITestPackageRepo testCaseRepo = new TestPackageRepo(ctsBuild.getTestCasesDir());
        for (String packageUri : testCaseRepo.getPackageNames()) {
            printLine(packageUri);
        }
    }

    private void listResults(CtsBuildHelper ctsBuild) {
        printLine("Session\t\tPass\tFail\tNot Executed\tStart time\t\tPlan name");
        ITestResultRepo testResultRepo = new TestResultRepo(ctsBuild.getResultsDir());
        for (ITestSummary result : testResultRepo.getSummaries()) {
            printLine(String.format("%d\t\t%d\t%d\t%d\t\t%s\t%s", result.getId(),
                    result.getNumPassed(), result.getNumFailed(),
                    result.getNumIncomplete(), result.getTimestamp(), result.getTestPlan()));
        }
    }

    private void addDerivedPlan(CtsBuildHelper ctsBuild, String[] flatArgs) {
        PlanCreator creator = new PlanCreator();
        try {
            ArgsOptionParser optionParser = new ArgsOptionParser(creator);
            optionParser.parse(Arrays.asList(flatArgs));
            creator.createAndSerializeDerivedPlan(ctsBuild);
        } catch (ConfigurationException e) {
            printLine("Error: " + e.getMessage());
            printLine(ArgsOptionParser.getOptionHelp(false, creator));
        }
    }

    private CtsBuildHelper getCtsBuild() {
        if (mCtsBuild == null) {
            String ctsInstallPath = System.getProperty("CTS_ROOT");
            if (ctsInstallPath != null) {
                mCtsBuild = new CtsBuildHelper(new File(ctsInstallPath));
                try {
                    mCtsBuild.validateStructure();
                } catch (FileNotFoundException e) {
                    printLine(String.format("Invalid cts install: %s", e.getMessage()));
                    mCtsBuild = null;
                }
            } else {
                printLine("Could not find CTS install location: CTS_ROOT env variable not set");
            }
        }
        return mCtsBuild;
    }

    public static void main(String[] args) throws InterruptedException {
        Console console = new CtsConsole();
        Console.startConsole(console, args);
    }
}
