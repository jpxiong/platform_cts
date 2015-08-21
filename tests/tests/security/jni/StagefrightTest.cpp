/*
 * Copyright (C) 2015 The Android Open Source Project
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
 *
 *
 * This code was provided to AOSP by Zimperium Inc and was
 * written by:
 *
 * Simone "evilsocket" Margaritelli
 * Joshua "jduck" Drake
 */

#define LOG_TAG "StagefrightWrapperLib"

#include <android/log.h>
#include <StagefrightMetadataRetriever.h>

#define METADATA_KEY_MIMETYPE 12

extern "C"
{
    void StagefrightExtractMetadataWrapper(int fd);
}

void StagefrightExtractMetadataWrapper(int fd)
{
    android::StagefrightMetadataRetriever smr;
    smr.setDataSource(fd, 0, 0x7ffffffffffffffL);
    smr.extractMetadata(METADATA_KEY_MIMETYPE);
}
