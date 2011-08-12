Copyright (C) 2011 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


README for the Theme Tests

Located in themetestscripts.sh are a series of scripts that allow
one to generate the "masters" for the theme tests. The tests require
known-good versions (masters) of widgets, etc to compare against.
The scripts will allow you to generate these masters for mdpi, hdpi
and xhdpi devices.

To properly generate these masters, a device of the needed density is
required. Ie, an mdpi device for mdpi masters, etc.

Additionally, to deal with large form-factors, mdpi will also do tests
in a larger size. A large form-factor device is required.
