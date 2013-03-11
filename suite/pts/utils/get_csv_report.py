#!/usr/bin/env python
#
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import re
import subprocess
import sys
from xml.dom import Node
from xml.dom import minidom

def getChildrenWithTag(parent, tagName):
  children = []
  for child in  parent.childNodes:
    if (child.nodeType == Node.ELEMENT_NODE) and (child.tagName == tagName):
      #print "parent " + parent.getAttribute("name") + " " + tagName +\
      #  " " + child.getAttribute("name")
      children.append(child)
  return children

def getText(tag):
  return str(tag.firstChild.nodeValue)

class TestCase(object):
  def __init__(self, name, summary, details, result):
    self.name = name
    self.summary = summary
    self.details = details
    self.result = result

  def getName(self):
    return self.name

  def getSummary(self):
    return self.summary

  def getDetails(self):
    return self.details

  def getResult(self):
    return self.result

def parseSuite(suite, parentName):
  if parentName != "":
    parentName += '.'
  cases = {}
  childSuites = getChildrenWithTag(suite, "TestSuite")
  for child in childSuites:
    cases.update(parseSuite(child, parentName + child.getAttribute("name")))
  childTestCases = getChildrenWithTag(suite, "TestCase")
  for child in childTestCases:
    className = parentName + child.getAttribute("name")
    for test in getChildrenWithTag(child, "Test"):
      methodName = test.getAttribute("name")
      # do not include this
      if methodName == "testAndroidTestCaseSetupProperly":
        continue
      caseName = str(className + "#" + methodName)
      result = str(test.getAttribute("result"))
      summary = {}
      details = {}
      if result == "pass":
        sts = getChildrenWithTag(test, "Summary")
        dts = getChildrenWithTag(test, "Details")
        if len(sts) == len(dts) == 1:
          summary[sts[0].getAttribute("message")] = getText(sts[0])
          for d in getChildrenWithTag(dts[0], "ValueArray"):
            values = []
            for c in getChildrenWithTag(d, "Value"):
              values.append(getText(c))
            details[d.getAttribute("message")] = values
        else:
          result = "no results"
      testCase = TestCase(caseName, summary, details, result)
      cases[caseName] = testCase
  return cases


class Result(object):
  def __init__(self, reportXml):
    self.results = {}
    self.infoKeys = []
    self.infoValues = []
    doc = minidom.parse(reportXml)
    testResult = doc.getElementsByTagName("TestResult")[0]
    buildInfo = testResult.getElementsByTagName("BuildInfo")[0]
    buildId = buildInfo.getAttribute("buildID")
    deviceId = buildInfo.getAttribute("deviceID")
    deviceName = buildInfo.getAttribute("build_device")
    boardName = buildInfo.getAttribute("build_board")
    partitions = buildInfo.getAttribute("partitions")
    m = re.search(r'.*;/data\s+([\w\.]+)\s+([\w\.]+)\s+([\w\.]+)\s+([\w\.]+);', partitions)
    dataPartitionSize = m.group(1)
    self.addKV("device", deviceName)
    self.addKV("board", boardName)
    self.addKV("serial", deviceId)
    self.addKV("build", buildId)
    self.addKV("data size", dataPartitionSize)
    packages = getChildrenWithTag(testResult, "TestPackage")
    for package in packages:
      casesFromChild = parseSuite(package, "")
      self.results.update(casesFromChild)
    #print self.results.keys()

  def addKV(self, key, value):
    self.infoKeys.append(key)
    self.infoValues.append(value)

  def getResults(self):
    return self.results

  def getKeys(self):
    return self.infoKeys

  def getValues(self):
    return self.infoValues

def executeWithResult(command):
  p = subprocess.Popen(command.split(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
  out, err = p.communicate()
  return out

def parseReports(path):
  deviceResults = []
  xmls = executeWithResult("find " + path + " -name testResult.xml -print")
  print "xml files found :"
  print xmls
  for xml in xmls.splitlines():
    result = Result(xml)
    deviceResults.append(result)
  reportInfo = {}
  keys = deviceResults[0].getKeys()
  noDevices = len(deviceResults)
  for i in xrange(len(keys)):
    values = []
    for j in xrange(noDevices):
      values.append(str(deviceResults[j].getValues()[i]))
    reportInfo[keys[i]] = values
  #print reportInfo

  tests = []
  for deviceResult in deviceResults:
    for key in deviceResult.getResults().keys():
      if not key in tests:
        tests.append(key)
  tests.sort()
  #print tests

  reportTests = {}
  for i in xrange(len(tests)):
    test = tests[i]
    reportTests[test] = []
    for j in xrange(noDevices):
      values = {}
      if deviceResults[j].getResults().has_key(test):
        result = deviceResults[j].getResults()[test]
        values["result"] = result.getResult()
        values["summary"] = result.getSummary()
        values["details"] = result.getDetails()
        values["device"] = deviceResults[j].getValues()[0]
        reportTests[test].append(values)

  #print reportTests
  return (reportInfo, reportTests)

def main(argv):
  if len(argv) < 3:
    print "get_csv_report.py pts_report_dir output_file"
    sys.exit(1)
  reportPath = os.path.abspath(argv[1])
  outputCsv = os.path.abspath(argv[2])

  (reportInfo, reportTests) = parseReports(reportPath)

  with open(outputCsv, 'w') as f:
    for key in reportInfo:
      f.write(key)
      for value in reportInfo[key]:
        f.write(',')
        f.write(value)
      f.write('\n')
    for test in reportTests:
      for report in reportTests[test]:
        if report.has_key('result'):
          result = report['result'] 
          f.write(test)
          f.write(',')
          f.write(result)
          for key in report['summary']:
            f.write(',')
            f.write(report['summary'][key])
          for key in report['details']:
            for value in report['details'][key]:
              f.write(',')
              f.write(value)
          f.write('\n')

if __name__ == '__main__':
  main(sys.argv)
