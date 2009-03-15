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
          "name=\"${SIGNATURE_CHECK_NAME}\" runner=\".InstrumentationRunner\" "\
          "targetNameSpace=\"\" targetBinaryName=\"\" version=\"1.0\" signatureCheck=\"true\" "\
          "appPackageName=\"android.tests.sigtest\">"  >> ${SIGNATURE_CHECK_PATH}
     echo "<TestSuite name=\"android\">"       >> ${SIGNATURE_CHECK_PATH}
     echo "<TestSuite name=\"tests\">"         >> ${SIGNATURE_CHECK_PATH}
     echo "<TestSuite name=\"sigtest\">"       >> ${SIGNATURE_CHECK_PATH}
     echo "<TestCase name=\"SignatureTest\">"  >> ${SIGNATURE_CHECK_PATH}
     echo "<Test method=\"signatureTest\">"    >> ${SIGNATURE_CHECK_PATH}
     echo "</Test>"        >> ${SIGNATURE_CHECK_PATH}
     echo "</TestCase>"    >> ${SIGNATURE_CHECK_PATH}
     echo "</TestSuite>"   >> ${SIGNATURE_CHECK_PATH}
     echo "</TestSuite>"   >> ${SIGNATURE_CHECK_PATH}
     echo "</TestSuite>"   >> ${SIGNATURE_CHECK_PATH}
     echo "</TestPackage>" >> ${SIGNATURE_CHECK_PATH}
}

# Generate the default test plan.
generateTestPlan() {
    TEST_PLAN=${2}/CTS.xml
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" > ${TEST_PLAN}
    echo "<TestPlan version=\"1.0\">" >> ${TEST_PLAN}

    # Let the signature test to be the 1st entry of the plan.
    if [ -f ${SIGNATURE_CHECK_PATH}  ]; then
        URI=${SIGNATURE_CHECK_NAME}
        echo "<Entry uri=\"${URI}\"/>" >> ${TEST_PLAN}
    fi
    
    for FILE_NAME in $(ls ${1}/*.xml); do
        URI=$(basename ${FILE_NAME} .xml)
        if [ "${URI}" != "${SIGNATURE_CHECK_NAME}" ]; then
            APP_JAVA_PKG_NAME=$(parseXML ${FILE_NAME} TestPackage appPackageName | sed 's/\"//g')
            echo "<Entry uri=\"${APP_JAVA_PKG_NAME}\"/>" >> ${TEST_PLAN}
        fi
    done
    echo "</TestPlan>" >> ${TEST_PLAN}
}

SIGNATURE_CHECK_NAME="SignatureTest"
SIGNATURE_CHECK_PATH=${CTS_OUT_DIR}/repository/testcases/${SIGNATURE_CHECK_NAME}".xml"

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

SIGNATURE_CHECK_NAME="SignatureTest"
SIGNATURE_CHECK_PATH="${CASE_REPOSITORY}/${SIGNATURE_CHECK_NAME}.xml"

CORETESTS_NAME="android.core.tests"
CORETESTS_PATH="${CASE_REPOSITORY}/${CORETESTS_NAME}.xml"

#Creating Signature check description xml file, if not existed.
generateSignatureCheckDescription

# Every test case package ends with "cts"
for CASE_SOURCE in $(find ${TESTCASES_SOURCE} -type d | grep "cts$" | sed 's/\/\//\//'); do
    TARGET_PACKAGE_NAME=$(echo ${CASE_SOURCE} | sed 's/^.*src\///g' | sed 's/\/cts//g' | sed 's/\//./g' | sed 's/android\..*\..*//g')

    # TODO: Currently we have ignored the sub-packages, should add support for it later
    if [ x${TARGET_PACKAGE_NAME} != x ]; then
        # TODO: translate this script to python to reduce dependencies on external tools
        # darwin sed does not support \u in replacement pattern, use perl for now
        NAME=$(echo $TARGET_PACKAGE_NAME | sed 's/android.//g' | perl -p -e 's/([a-z])([a-zA-Z0-9]*)/\u\1\2/g' | sed 's/^/Cts/g' | sed 's/$/TestCases/g')

        if [ x${NAME} != x ]; then
            # TODO: Currently use hardcode -f4, should find a better way to remove this harde code
            TESTCASE_DIR=$TESTCASES_SOURCE$(echo $CASE_SOURCE | cut -d"/" -f4)

            generatePackageDescription ${CASE_SOURCE} ${TEMP_DIR}
            addControllerInfo ${TESTCASE_DIR} ${TEMP_DIR} ${NAME} ${TARGET_PACKAGE_NAME}
            if [[ $? -ne 0 ]]; then
                exit 1
            fi
        fi
    fi
done

# Creating "Test plan" file
generateTestPlan ${CASE_REPOSITORY} ${PLAN_REPOSITORY}
