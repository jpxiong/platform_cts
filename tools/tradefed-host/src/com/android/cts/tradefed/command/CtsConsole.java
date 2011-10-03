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
import com.android.cts.tradefed.testtype.ITestCaseRepo;
import com.android.cts.tradefed.testtype.TestCaseRepo;
import com.android.tradefed.command.Console;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.RegexTrie;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

/**
 * Specialization of trade federation console that adds CTS commands to list plans and packages.
 */
public class CtsConsole extends Console {

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

        // find existing help for 'LIST_PATTERN' commands, and append these commands help
        String listHelp = commandHelp.get(LIST_PATTERN);
        if (listHelp == null) {
            // no help? Unexpected, but soldier on
            listHelp = new String();
        }
        String combinedHelp = String.format("%s" + LINE_SEPARATOR +
                "\tp[lans]  List all CTS test plans" + LINE_SEPARATOR +
                "\tpackages  List all CTS packages" + LINE_SEPARATOR, listHelp);
        commandHelp.put(LIST_PATTERN, combinedHelp);
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
        ITestCaseRepo testCaseRepo = new TestCaseRepo(ctsBuild.getTestCasesDir());
        for (String packageUri : testCaseRepo.getPackageNames()) {
            printLine(packageUri);
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
