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
 */
package com.android.compatibility.common.deviceinfo;

/**
 * Example Objects for {@link DeviceInfoActivity} test package.
 */
public final class ExampleObjects {

    private static final int MAX_LENGTH = 1000;

    private static final String SAMPLE_DEVICE_INFO_JSON = "{\n" +
        "  \"foo\": {\n" +
        "    \"foo_boolean\": true,\n" +
        "    \"bar\": {\n" +
        "      \"bar_string\": [\n" +
        "        \"bar-string-1\",\n" +
        "        \"bar-string-2\",\n" +
        "        \"bar-string-3\"\n" +
        "      ],\n" +
        "      \"bar_boolean\": [\n" +
        "        true,\n" +
        "        false\n" +
        "      ],\n" +
        "      \"bar_double\": [\n" +
        "        1.7976931348623157E308,\n" +
        "        4.9E-324\n" +
        "      ],\n" +
        "      \"bar_int\": [\n" +
        "        2147483647,\n" +
        "        -2147483648\n" +
        "      ],\n" +
        "      \"bar_long\": [\n" +
        "        9223372036854775807,\n" +
        "        -9223372036854775808\n" +
        "      ]\n" +
        "    },\n" +
        "    \"foo_double\": 1.7976931348623157E308,\n" +
        "    \"foo_int\": 2147483647,\n" +
        "    \"foo_long\": 9223372036854775807,\n" +
        "    \"foo_string\": \"foo-string\",\n" +
        "    \"long_string\": \"%s\",\n" +
        "    \"long_int_array\": [\n%s" +
        "    ]\n" +
        "  }\n" +
        "}\n";

    public static String sampleDeviceInfoJson() {
        StringBuilder longStringSb = new StringBuilder();
        StringBuilder longArraySb = new StringBuilder();
        int lastNum = MAX_LENGTH - 1;
        for (int i = 0; i < MAX_LENGTH; i++) {
            longStringSb.append("a");
            longArraySb.append(String.format("      %d%s\n", i, ((i == lastNum)? "" : ",")));
        }
        return String.format(SAMPLE_DEVICE_INFO_JSON,
            longStringSb.toString(), longArraySb.toString());
    }
}