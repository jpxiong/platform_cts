# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

CTS_SECURITY_APPS_LIST := \
	CtsAppAccessData \
	CtsAppWithData \
	CtsInstrumentationAppDiffCert \
	CtsPermissionDeclareApp \
	CtsSharedUidInstall \
	CtsSharedUidInstallDiffCert \
	CtsSimpleAppInstall \
	CtsSimpleAppInstallDiffCert \
	CtsTargetInstrumentationApp \
	CtsUsePermissionDiffCert

# Any APKs that need to be copied to the CTS distribution's testcases
# directory but do not require an associated test package XML.
CTS_TEST_CASE_LIST := \
	TestDeviceSetup \
	CtsAccelerationTestStubs \
	CtsDelegatingAccessibilityService \
	CtsDeviceAdmin \
	CtsTestStubs \
	SignatureTest \
	ApiDemos \
	ApiDemosReferenceTest \
	$(CTS_SECURITY_APPS_LIST)

# Test packages that require an associated test package XML.
CTS_TEST_PACKAGES := \
	CtsAccelerationTestCases \
	CtsAccessibilityServiceTestCases \
	CtsAccountManagerTestCases \
	CtsAdminTestCases \
	CtsAnimationTestCases \
	CtsAppTestCases \
	CtsBluetoothTestCases \
	CtsContentTestCases \
	CtsDatabaseTestCases \
	CtsDpiTestCases \
	CtsDpiTestCases2 \
	CtsDrmTestCases \
	CtsEffectTestCases \
	CtsExampleTestCases \
	CtsGestureTestCases \
	CtsGraphicsTestCases \
	CtsHardwareTestCases \
	CtsHoloTestCases \
	CtsJniTestCases \
	CtsLocationTestCases \
	CtsMediaTestCases \
	CtsNdefTestCases \
	CtsNetTestCases \
	CtsOpenGlPerfTestCases \
	CtsOsTestCases \
	CtsPermissionTestCases \
	CtsPermission2TestCases \
	CtsPreferenceTestCases \
	CtsProviderTestCases \
	CtsRenderscriptTestCases \
	CtsSaxTestCases \
	CtsSecurityTestCases \
	CtsSpeechTestCases \
	CtsTelephonyTestCases \
	CtsTextTestCases \
	CtsUtilTestCases \
	CtsViewTestCases \
	CtsWebkitTestCases \
	CtsWidgetTestCases

# All APKs that need to be scanned by the coverage utilities.
CTS_COVERAGE_TEST_CASE_LIST := \
	$(CTS_TEST_CASE_LIST) \
	$(CTS_TEST_PACKAGES)

# Host side only tests
CTS_HOST_LIBRARIES := \
    CtsAppSecurityTests

# Native test executables that need to have associated test XMLs.
CTS_NATIVE_EXES := \
	NativeMediaTest_SL \
	NativeMediaTest_XA

# All the files that will end up under the repository/testcases
# directory of the final CTS distribution.
CTS_TEST_CASES := $(call cts-get-lib-paths,$(CTS_HOST_LIBRARIES)) \
		$(call cts-get-package-paths,$(CTS_TEST_PACKAGES)) \
		$(call cts-get-native-paths,$(CTS_NATIVE_EXES))

# All the XMLs that will end up under the repository/testcases
# and that need to be created before making the final CTS distribution.
CTS_TEST_XMLS := $(call cts-get-test-xmls,$(CTS_HOST_LIBRARIES)) \
		$(call cts-get-test-xmls,$(CTS_TEST_PACKAGES)) \
		$(call cts-get-test-xmls,$(CTS_NATIVE_EXES))

# The following files will be placed in the tools directory of the CTS distribution
CTS_TOOLS_LIST :=
