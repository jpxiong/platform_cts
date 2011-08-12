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

package android.theme.cts;

/**
 * This interface exists for the sole purpose of separating the
 * {@link ActivityInstrumentationTestCase2} that runs the activity
 * screenshot tests from the actual code. In order to work properly
 * with multiple activities, the {@link ActivityInstrumentationTestCase2}
 * must {@link ActivityInstrumentationTestCase2#tearDown} and
 * {@link ActivityInstrumentationTestCase2#setUp} itself after
 * each test. Since those methods are protected, this interface
 * serves as a way to implement that functionality while allowing
 * two separate {@link ActivityInstrumentationTestCase2}s to exist,
 * one for the regular activity screenshot tests and the other one for
 * the while sharing code between both for most of the test framework.
 */
public interface TestReset {
    public void reset();
}
