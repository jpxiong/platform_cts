/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include <stdio.h>

#include <utils/String8.h>

#include <UniquePtr.h>

#include "Log.h"
#include "Report.h"
#include "task/TaskGeneric.h"
#include "task/ModelBuilder.h"

// For flushing report and log before exiting
class CleanupStatics {
public:

    CleanupStatics() {};
    ~CleanupStatics() {
        Log::Finalize();
        Report::Finalize();
    }
};

int main(int argc, char *argv[])
{
    if (argc < 2) {
        fprintf(stderr, "%s [-llog_level] test_xml\n", argv[0]);
        return 1;
    }
    int logLevel = 3;
    int argCurrent = 1;
    if (strncmp(argv[argCurrent], "-l", 2) == 0) {
        logLevel = atoi(argv[argCurrent] + 2);
        argCurrent++;
    }
    if (argCurrent == argc) {
        fprintf(stderr, "wrong arguments");
        return 1;
    }
    android::String8 xmlFile(argv[argCurrent]);

    android::String8 dirName;
    if (!FileUtil::prepare(dirName)) {
        fprintf(stderr, "cannot prepare report dir");
        return 1;
    }

    UniquePtr<CleanupStatics> staticStuffs(new CleanupStatics());
    if (Log::Instance(dirName.string()) == NULL) {
        fprintf(stderr, "cannot create Log");
        return 1;
    }
    Log::Instance()->setLogLevel((Log::LogLevel)logLevel);
    // Log can be used from here
    if (Report::Instance(dirName.string()) == NULL) {

        LOGE("cannot create log");
        return 1;
    }

    ModelBuilder modelBuilder;
    UniquePtr<TaskGeneric> topTask(modelBuilder.parseTestDescriptionXml(xmlFile));
    if (topTask.get() == NULL) {
        LOGE("Parsing of %x failed", xmlFile.string());
        return 1;
    }
    topTask->run();
    return 0;
}

