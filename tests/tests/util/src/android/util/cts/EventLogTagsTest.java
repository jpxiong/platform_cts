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

package android.util.cts;

import java.io.BufferedReader;
import java.io.FileReader;

import android.test.AndroidTestCase;
import android.util.EventLogTags;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

/**
 * Test class android.util.EventLogTags.
 */
@TestTargetClass(EventLogTags.class)
public class EventLogTagsTest extends AndroidTestCase {

    private final static String TAGS_FILE = "/etc/event-log-tags";

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "EventLogTags",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "EventLogTags",
            args = {BufferedReader.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "get",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "get",
            args = {String.class}
        )
    })
    public void testEventLogTagsOp() throws Exception {
        EventLogTags tags = new EventLogTags();
        assertEquals(42, tags.get("answer").mTag);
        assertEquals(314, tags.get("pi").mTag);
        assertEquals(2718, tags.get("e").mTag);
        assertEquals("answer", tags.get(42).mName);
        assertEquals("pi", tags.get(314).mName);
        assertEquals("e", tags.get(2718).mName);

        tags = new EventLogTags(new BufferedReader(new FileReader(TAGS_FILE), 256));
        assertEquals(42, tags.get("answer").mTag);
        assertEquals(314, tags.get("pi").mTag);
        assertEquals(2718, tags.get("e").mTag);
        assertEquals("answer", tags.get(42).mName);
        assertEquals("pi", tags.get(314).mName);
        assertEquals("e", tags.get(2718).mName);
    }
}
