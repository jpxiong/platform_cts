/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.annotation.cts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying what device profiles are supported by the test. The device's profile
 * must match one of the profiles specified by the annotation. Not having the annotation means the
 * test should be executed for all devices.
 * <p>
 * Examples:
 * <pre>
 * @SupportedProfiles(Profile.HANDHELD)
 * @SupportedProfiles({Profile.HANDHELD, Profile.STB})
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SupportedProfiles {
    Profile[] value();
}
