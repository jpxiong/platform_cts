#!/bin/sh

# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# A lit XML parser which could get attribute value of the specified tag.
# parseXML(tag, attribute)
parseXML() {
    awk -v tag=${2} -v attribute=${3} \
    'BEGIN { RS=">" }
     { ind=split($0,field," ");
       if (substr(field[1],2)==tag) {
         for(ind2=2;ind2<=ind;ind2++) {
           split(field[ind2],token,"=");
           if (token[1]==attribute)
             { print token[2] }
     }}}' ${1}
}

# Generate signature checking description xml file.
generateSignatureCheckDescription() {
     echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > ${SIGNATURE_CHECK_PATH}
     echo "<TestPackage AndroidFramework=\"Android 1.0\" jarPath=\"\" "\
          "name=\"${SIGNATURE_TEST_NAME}\" runner=\".InstrumentationRunner\" "\
          "targetNameSpace=\"\" targetBinaryName=\"\" version=\"1.0\" signatureCheck=\"true\" "\
          "appPackageName=\"${SIGNATURE_TESTS}\">"  >> ${SIGNATURE_CHECK_PATH}
     echo "<TestSuite name=\"android\">"       >> ${SIGNATURE_CHECK_PATH}
     echo "<TestSuite name=\"tests\">"         >> ${SIGNATURE_CHECK_PATH}
     echo "<TestSuite name=\"sigtest\">"       >> ${SIGNATURE_CHECK_PATH}
     echo "<TestCase name=\"SignatureTest\">"  >> ${SIGNATURE_CHECK_PATH}
     echo "<Test name=\"signatureTest\">"    >> ${SIGNATURE_CHECK_PATH}
     echo "</Test>"        >> ${SIGNATURE_CHECK_PATH}
     echo "</TestCase>"    >> ${SIGNATURE_CHECK_PATH}
     echo "</TestSuite>"   >> ${SIGNATURE_CHECK_PATH}
     echo "</TestSuite>"   >> ${SIGNATURE_CHECK_PATH}
     echo "</TestSuite>"   >> ${SIGNATURE_CHECK_PATH}
     echo "</TestPackage>" >> ${SIGNATURE_CHECK_PATH}
}

generateReferenceAppDescription() {
     PACKAGE_NAME=${1}
     PACKAGE_PATH=${2}
     JAVA_PACKAGE=${3}
     CLASS_NAME=${4}
     METHOD_NAME=${5}
     APK_TO_TEST_NAME=${6}
     PACKAGE_TO_TEST=${7}
     # Convert the dotted package name into a list so we can loop though it.
     JAVA_PACKAGE_LIST=`echo $JAVA_PACKAGE | sed 's/\./ /g'`

     echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > ${PACKAGE_PATH}
     echo "<TestPackage AndroidFramework=\"Android 1.0\" jarPath=\"\" "\
          "name=\"${PACKAGE_NAME}\" runner=\"android.test.InstrumentationTestRunner\" "\
          "targetNameSpace=\"\" targetBinaryName=\"\" version=\"1.0\" "\
          "signatureCheck=\"false\" referenceAppTest=\"true\""\
          "apkToTestName=\"${APK_TO_TEST_NAME}\""\
          "packageToTest=\"${PACKAGE_TO_TEST}\""\
          "appPackageName=\"${JAVA_PACKAGE}\">"  >> ${PACKAGE_PATH}
     for pack_part in ${JAVA_PACKAGE_LIST}; do
       echo "<TestSuite name=\"${pack_part}\">"       >> ${PACKAGE_PATH}
     done
     echo "<TestCase name=\"${CLASS_NAME}\">"  >> ${PACKAGE_PATH}
     echo "<Test name=\"${METHOD_NAME}\">"    >> ${PACKAGE_PATH}
     echo "</Test>"        >> ${PACKAGE_PATH}
     echo "</TestCase>"    >> ${PACKAGE_PATH}
     for pack_part in ${JAVA_PACKAGE_LIST}; do
       echo "</TestSuite>"   >> ${PACKAGE_PATH}
     done
     echo "</TestPackage>" >> ${PACKAGE_PATH}
}

# Genrate the header of the test plan XML file.
genTestPlanHeader() {
    TEST_PLAN=${1}
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" > ${TEST_PLAN}
    echo "<TestPlan version=\"1.0\">" >> ${TEST_PLAN}
}

# add one entry into test plan XML file.
addTestPlanEntry() {
    TEST_PLAN=${1}
    APP_JAVA_PKG_NAME=${2}
    entry=$(grep "<Entry uri=\"${APP_JAVA_PKG_NAME}\"/>" ${TEST_PLAN})
    if [ "${entry}" == ""  ]; then
        echo "<Entry uri=\"${APP_JAVA_PKG_NAME}\"/>" >> ${TEST_PLAN}
    fi
}

# Process the inclusive list.
procInclusiveList() {
    TESTCASE_PATH=${1}
    TEST_PLAN=${2}
    INC_LIST=${3}

    for FILE_NAME in $(ls ${TESTCASE_PATH}/*.xml); do
        APP_JAVA_PKG_NAME=$(parseXML ${FILE_NAME} TestPackage appPackageName | sed 's/\"//g')
        for INC_NAME in ${INC_LIST} ; do
            if [ "${INC_NAME}" == "${APP_JAVA_PKG_NAME}" ]; then
               addTestPlanEntry ${TEST_PLAN} ${APP_JAVA_PKG_NAME}
            fi
        done
    done
}

# Process the exclusive list.
procExclusiveList() {
    TESTCASE_PATH=${1}
    TEST_PLAN=${2}
    EXC_LIST=${3}

    for FILE_NAME in $(ls ${TESTCASE_PATH}/*.xml); do
        APP_JAVA_PKG_NAME=$(parseXML ${FILE_NAME} TestPackage appPackageName | sed 's/\"//g')
        excluded=""
        for EXC_NAME in ${EXC_LIST} ; do
            if [ "${EXC_NAME}" == "${APP_JAVA_PKG_NAME}" ]; then
                excluded="true"
            fi
        done
        if [ "${excluded}" != "true" ]; then
            addTestPlanEntry ${TEST_PLAN} ${APP_JAVA_PKG_NAME}
        fi
    done
}

# Generate test plan with given information.
genTestPlan() {
    TESTCASE_PATH=${1}
    TEST_PLAN=${2}
    TYPE=${3}
    LIST=${4}

    genTestPlanHeader ${TEST_PLAN};
    if [ "${TYPE}" == "inclusive" ]; then
        procInclusiveList ${TESTCASE_PATH} ${TEST_PLAN} "${LIST}"
    else
        procExclusiveList ${TESTCASE_PATH} ${TEST_PLAN} "${LIST}"
    fi
    echo "</TestPlan>" >> ${TEST_PLAN}
}

# Generate all of the default test plans
generateAllTestPlans() {
    TESTCASE_PATH=${1}
    PLAN_PATH=${2}

    TEST_PLAN=${PLAN_PATH}/CTS.xml
    LIST=""
    TYPE="exclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"

    TEST_PLAN=${PLAN_PATH}/Android.xml
    LIST="${SIGNATURE_TESTS} ${ANDROID_CORE_TESTS} ${ANDROID_CORE_VM_TESTS}"
    TYPE="exclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"

    TEST_PLAN=${PLAN_PATH}/Java.xml
    LIST="${ANDROID_CORE_TESTS}"
    TYPE="inclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"

    TEST_PLAN=${PLAN_PATH}/VM.xml
    LIST="${ANDROID_CORE_VM_TESTS}"
    TYPE="inclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"

    TEST_PLAN=${PLAN_PATH}/Signature.xml
    LIST="${SIGNATURE_TESTS}"
    TYPE="inclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"

    TEST_PLAN=${PLAN_PATH}/RefApp.xml
    LIST="${REFERENCE_APP_TESTS}"
    TYPE="inclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"

    TEST_PLAN=${PLAN_PATH}/Performance.xml
    LIST="${PERFORMANCE_TESTS}"
    TYPE="inclusive"
    genTestPlan ${TESTCASE_PATH} ${TEST_PLAN} ${TYPE}  "${LIST}"
}

# Build the DescriptionGenerator as the Doclet
buildDescriptionGenerator() {
    OUT=${1}
    READLINK_OPT=""
    HOST=$(uname -s | tr 'A-Z' 'a-z')
    if [ ${HOST} != "darwin" ]; then
        READLINK_OPT="-m"
    fi;

    echo "Build "Description generator""
    JAVADOC_PATH=$(readlink ${READLINK_OPT} $(which javadoc))
    JAVADOC_LIBS=$(echo ${JAVADOC_PATH} | sed 's/bin\/javadoc/lib\//')

    javac -d ${OUT} -extdirs ${JAVADOC_LIBS} \
          ${CTS_ROOT}/utils/DescriptionGenerator.java
}

addControllerInfo() {
    CTSROOT=${1}
    OUT=${2}
    XMLNAME=${3}
    APP_PACKAGE_NAME=${4}

    MANIFEST="AndroidManifest.xml"
    MAKEFILE="Android.mk"
    TARGET_BINARY_MARK="LOCAL_INSTRUMENTATION_FOR"
    HOST_CONTROLLER_NAME="hostController.jar"

    APP_NAME_SPACE=$(parseXML ${CTSROOT}/${MANIFEST} manifest package | sed 's/\"//g')
    RUNNER_NAME=$(parseXML ${CTSROOT}/${MANIFEST} instrumentation android:name | sed 's/\"//g')
    TARGET_NAME_SPACE=$(parseXML ${CTSROOT}/${MANIFEST} instrumentation android:targetPackage | sed 's/\"//g')
    TARGET_BINARY_NAME=$(cat ${CTSROOT}/${MAKEFILE} | grep "LOCAL_INSTRUMENTATION_FOR :=" | cut -d"=" -f2 | sed 's/^[ \t]*//g;s/[ \t]*$//g')
    if [ -z "${RUNNER_NAME}" ] ; then
        echo "Can't find instrumentation in the manifest file."
        return 1;
    fi

    HOST_CONTROLLER_LOCATION=$(find ${ANDROID_OUT_ROOT} -type f -name ${HOST_CONTROLLER_NAME})
    if [ -f "${HOST_CONTROLLER_LOCATION}" ]; then
        cp -f ${HOST_CONTROLLER_LOCATION} ${CASE_REPOSITORY}/${HOST_CONTROLLER_NAME}
        XML_INFO="jarPath=\"${HOST_CONTROLLER_NAME}\" name=\"${XMLNAME}\" "\
"runner=\"${RUNNER_NAME}\" appNameSpace=\"${APP_NAME_SPACE}\" appPackageName=\"${APP_PACKAGE_NAME}\""
    else
        XML_INFO="jarPath=\"\" name=\"${XMLNAME}\" runner=\"${RUNNER_NAME}\" "\
"appNameSpace=\"${APP_NAME_SPACE}\" appPackageName=\"${APP_PACKAGE_NAME}\""
    fi

    if [ "${PACKAGE_NAME}" != "${TARGET_PACKAGE_NAME}" ]; then
        # A dependancy is required by this package
        XML_INFO="${XML_INFO} targetNameSpace=\"${TARGET_NAME_SPACE}\" targetBinaryName=\"${TARGET_BINARY_NAME}\""
    fi
    # Update "Package description" file
    sed 's/XML_INFO=\"\"/'"${XML_INFO}"'/' ${OUT}/description.xml \
    > ${CASE_REPOSITORY}/${XMLNAME}.xml
}

# Generate "Package description" file
generatePackageDescription() {
    SRC=${1}
    OUT=${2}

    # Build "Description generator" if not available
    if [ ! -f ${OUT}/DescriptionGenerator.class ]; then
        buildDescriptionGenerator ${OUT}
    fi
    SOURCES=$(find ${SRC} -type f -name "*.java")

    echo "Generating TestCases description file from ${SRC}..."
    javadoc -J-Xmx256m -quiet -doclet DescriptionGenerator -docletpath ${OUT} -sourcepath \
${TOP_DIR}/frameworks/base/awt/java:\
${TOP_DIR}/frameworks/base/core/java:\
${TOP_DIR}/frameworks/base/graphics/java:\
${TOP_DIR}/frameworks/base/location/java:\
${TOP_DIR}/frameworks/base/media/java:\
${TOP_DIR}/frameworks/base/opengl/java:\
${TOP_DIR}/frameworks/base/sax/java:\
${TOP_DIR}/frameworks/base/services/java:\
${TOP_DIR}/frameworks/base/telephony/java:\
${TOP_DIR}/frameworks/base/wifi/java:\
${TOP_DIR}/frameworks/base/test-runner:\
${TOP_DIR}/cts/tests/tests/app/src:\
${TOP_DIR}/cts/tests/tests/content/src:\
${TOP_DIR}/cts/tests/tests/database/src:\
${TOP_DIR}/cts/tests/tests/graphics/src:\
${TOP_DIR}/cts/tests/tests/hardware/src:\
${TOP_DIR}/cts/tests/tests/location/src:\
${TOP_DIR}/cts/tests/tests/media/src:\
${TOP_DIR}/cts/tests/tests/net/src:\
${TOP_DIR}/cts/tests/tests/os/src:\
${TOP_DIR}/cts/tests/tests/permission/src:\
${TOP_DIR}/cts/tests/tests/preference/src:\
${TOP_DIR}/cts/tests/tests/provider/src:\
${TOP_DIR}/cts/tests/tests/telephony/src:\
${TOP_DIR}/cts/tests/tests/text/src:\
${TOP_DIR}/cts/tests/tests/util/src:\
${TOP_DIR}/cts/tests/tests/view/src:\
${TOP_DIR}/cts/tests/tests/widget/src:\
${TOP_DIR}/cts/tests/tests/performance/src:\
${TOP_DIR}/cts/tests/tests/performance2/src:\
${TOP_DIR}/cts/tests/tests/performance3/src:\
${TOP_DIR}/cts/tests/tests/performance4/src:\
${TOP_DIR}/cts/tests/tests/performance5/src:\
${TOP_DIR}/dalvik/libcore/dalvik/src/main/java:\
${TOP_DIR}/dalvik/libcore/junit/src/main/java \
${SOURCES}  1>/dev/null 2>/dev/null
    mv description.xml ${OUT}/description.xml
}

if [ $# -ne 6 ]; then
    echo "Usage ${0} <source dir> <destination dir> <temp dir> <top dir> "\
    "<target common out root> == JAVA_LIBS=${5}/obj/JAVA_LIBRARIES <android out dir>"
    exit 1
fi

TESTCASES_SOURCE=${1}
OUT_DIR=${2}
TEMP_DIR=${3}
TOP_DIR=${4}
JAVA_LIBS=${5}/obj/JAVA_LIBRARIES
ANDROID_OUT_ROOT=${6}
CTS_ROOT=${TOP_DIR}/cts/tools
CASE_REPOSITORY=${OUT_DIR}/repository/testcases
PLAN_REPOSITORY=${OUT_DIR}/repository/plans

SIGNATURE_TEST_NAME="SignatureTest"
SIGNATURE_TESTS="android.tests.sigtest"
SIGNATURE_CHECK_PATH="${CASE_REPOSITORY}/${SIGNATURE_TEST_NAME}.xml"

ANDROID_CORE_TESTS="android.core.tests.annotation android.core.tests.archive android.core.tests.concurrent android.core.tests.crypto android.core.tests.dom android.core.tests.logging android.core.tests.luni.io android.core.tests.luni.lang android.core.tests.luni.net android.core.tests.luni.util android.core.tests.math android.core.tests.nio android.core.tests.nio_char android.core.tests.prefs android.core.tests.regex android.core.tests.security android.core.tests.sql android.core.tests.text android.core.tests.xml android.core.tests.xnet"
ANDROID_CORE_VM_TESTS="android.core.vm-tests"

#Creating Signature check description xml file, if not existed.
generateSignatureCheckDescription

REFERENCE_APP_TESTS="android.apidemos.cts"

API_DEMOS_REFERENCE_NAME="ApiDemosReferenceTest"
API_DEMOS_REFERENCE_PATH="${CASE_REPOSITORY}/${API_DEMOS_REFERENCE_NAME}.xml"

PERFORMANCE_TESTS="android.performance android.performance2 android.performance3 android.performance4 android.performance5"

generateReferenceAppDescription "ApiDemosReferenceTest"\
  ${API_DEMOS_REFERENCE_PATH}\
  "android.apidemos.cts"\
  "ApiDemosTest"\
  "testNumberOfItemsInListView"\
  "ApiDemos"\
  "com.example.android.apis"

# Every test case package ends with "cts"
for CASE_SOURCE in $(find ${TESTCASES_SOURCE} -type d | grep "cts$" | sed 's/\/\//\//'); do
    TARGET_PACKAGE_NAME=$(echo ${CASE_SOURCE} | sed 's/^.*src\///g' | sed 's/\/cts//g' | sed 's/\//./g' | sed 's/android\..*\..*//g')

    if [ x${TARGET_PACKAGE_NAME} != x ]; then
        # TODO: translate this script to python to reduce dependencies on external tools
        # darwin sed does not support \u in replacement pattern, use perl for now
        NAME=$(echo $TARGET_PACKAGE_NAME | sed 's/android\.//g' | perl -p -e 's/([a-z])([a-zA-Z0-9]*)/\u\1\2/g' | sed 's/^/Cts/g' | sed 's/$/TestCases/g')

        if [ x${NAME} != x ]; then
            # TODO: Currently hard coded as -f4, should find a better way to remove this hard code
            TESTCASE_DIR=$TESTCASES_SOURCE$(echo $CASE_SOURCE | cut -d"/" -f4)

            # Since case source java packages always end in cts, we need to
            # start the search for sources in the parent package, so that e.g.
            # the android.content.cts test package also includes the sources in
            # android.content.pm.cts.
            CASE_SOURCE_PARENT=$(echo ${CASE_SOURCE} | sed 's/\/cts$//g')
            generatePackageDescription ${CASE_SOURCE_PARENT} ${TEMP_DIR}
            addControllerInfo ${TESTCASE_DIR} ${TEMP_DIR} ${NAME} ${TARGET_PACKAGE_NAME}
            if [[ $? -ne 0 ]]; then
                exit 1
            fi
        fi
    fi
done

# Creating the default test plans
generateAllTestPlans ${CASE_REPOSITORY} ${PLAN_REPOSITORY}
