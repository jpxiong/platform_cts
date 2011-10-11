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

# These test cases will be analyzed by the CTS API coverage tools. 
CTS_COVERAGE_TEST_CASE_LIST := \
	CtsAccelerationTestCases \
	CtsAccelerationTestStubs \
	CtsAccessibilityServiceTestCases \
	CtsAccountManagerTestCases \
	CtsAdminTestCases \
	CtsAppTestCases \
	CtsBluetoothTestCases \
	CtsContentTestCases \
	CtsDatabaseTestCases \
	CtsDpiTestCases \
	CtsDpiTestCases2 \
	CtsDrmTestCases \
	CtsExampleTestCases \
	CtsGestureTestCases \
	CtsGraphicsTestCases \
	CtsHardwareTestCases \
	CtsJniTestCases \
	CtsLocationTestCases \
	CtsMediaTestCases \
	CtsNdefTestCases \
	CtsNetTestCases \
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
	CtsTestStubs \
	CtsTextTestCases \
	CtsThemeTestCases \
	CtsUtilTestCases \
	CtsViewTestCases \
	CtsWebkitTestCases \
	CtsWidgetTestCases

CTS_TEST_CASE_LIST := \
	TestDeviceSetup \
	CtsDelegatingAccessibilityService \
	CtsDeviceAdmin \
	SignatureTest \
	ApiDemos \
	ApiDemosReferenceTest \
	$(CTS_COVERAGE_TEST_CASE_LIST) \
	$(CTS_SECURITY_APPS_LIST)

# The following files will be placed in the tools directory of the CTS distribution
CTS_TOOLS_LIST :=
