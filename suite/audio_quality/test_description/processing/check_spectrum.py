#!/usr/bin/python

# Copyright (C) 2012 The Android Open Source Project
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

from consts import *
import numpy as np
import scipy as sp
import scipy.fftpack as fft
import matplotlib.pyplot as plt
import sys
sys.path.append(sys.path[0])
import calc_delay

# check if Transfer Function of DUT / Host signal
#  lies in the given error boundary
# input: host record
#        device record,
#        sampling rate
#        low frequency in Hz,
#        high frequency in Hz,
#        allowed error in negative side for pass in %,
#        allowed error ih positive side for pass
# output: min value in negative side, normalized to 1.0
#         max value in positive side
#         calculated TF in magnitude (DUT / Host)

def do_check_spectrum(hostData, DUTData, samplingRate, fLow, fHigh, margainLow, margainHigh):
    # reduce FFT resolution to have averaging effects
    N = 512 if (len(hostData) > 512) else len(hostData)
    iLow = N * fLow / samplingRate
    if iLow > (N / 2 - 1):
        iLow = (N / 2 - 1)
    iHigh = N * fHigh / samplingRate
    if iHigh > (N / 2):
        iHigh = N / 2
    print fLow, iLow, fHigh, iHigh, samplingRate
    hostFFT = abs(fft.fft(hostData, n = N))[iLow:iHigh]
    dutFFT = abs(fft.fft(DUTData, n = N))[iLow:iHigh]
    TF = dutFFT / hostFFT
    TFmean = sum(TF) / len(TF)
    TF = TF / TFmean # TF normalized to 1
    positiveMax = abs(max(TF))
    negativeMin = abs(min(TF))
    passFail = True if (positiveMax < (margainHigh / 100.0 + 1.0)) and\
        ((1.0 - negativeMin) < margainLow / 100.0) else False
    TFResult = np.zeros(len(TF), dtype=np.int16)
    for i in range(len(TF)):
        TFResult[i] = TF[i] * 256 # make fixed point
    #freq = np.linspace(0.0, fHigh, num=iHigh, endpoint=False)
    #plt.plot(freq, abs(fft.fft(hostData, n = N))[:iHigh], freq, abs(fft.fft(DUTData, n = N))[:iHigh])
    #plt.show()
    print "positiveMax", positiveMax, "negativeMin", negativeMin
    return (passFail, negativeMin, positiveMax, TFResult)

def check_spectrum(inputData, inputTypes):
    output = []
    outputData = []
    outputTypes = []
    # basic sanity check
    inputError = False
    if (inputTypes[0] != TYPE_MONO):
        inputError = True
    if (inputTypes[1] != TYPE_MONO):
        inputError = True
    if (inputTypes[2] != TYPE_I64):
        inputError = True
    if (inputTypes[3] != TYPE_I64):
        inputError = True
    if (inputTypes[4] != TYPE_I64):
        inputError = True
    if (inputTypes[5] != TYPE_DOUBLE):
        inputError = True
    if (inputTypes[6] != TYPE_DOUBLE):
        inputError = True
    if inputError:
        output.append(RESULT_ERROR)
        output.append(outputData)
        output.append(outputTypes)
        return output
    hostData = inputData[0]
    dutData = inputData[1]
    samplingRate = inputData[2]
    fLow = inputData[3]
    fHigh = inputData[4]
    margainLow = inputData[5]
    margainHigh = inputData[6]
    delay = calc_delay.calc_delay(hostData, dutData)
    N = len(dutData)
    print "delay ", delay, "deviceRecording samples ", N
    (passFail, minError, maxError, TF) = do_check_spectrum(hostData[delay:delay+N], dutData,\
        samplingRate, fLow, fHigh, margainLow, margainHigh)

    if passFail:
        output.append(RESULT_PASS)
    else:
        output.append(RESULT_OK)
    outputData.append(minError)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(maxError)
    outputTypes.append(TYPE_DOUBLE)
    outputData.append(TF)
    outputTypes.append(TYPE_MONO)
    output.append(outputData)
    output.append(outputTypes)
    return output

# test code
if __name__=="__main__":
    sys.path.append(sys.path[0])
    mod = __import__("gen_random")
    peakAmpl = 10000
    durationInMSec = 1000
    samplingRate = 44100
    fLow = 500
    fHigh = 15000
    data = getattr(mod, "do_gen_random")(peakAmpl, durationInMSec, samplingRate, fLow, fHigh,\
                                         stereo=False)
    print len(data)
    (passFail, minVal, maxVal, TF) = do_check_spectrum(data, data, samplingRate, fLow, fHigh,\
                                                           1.0, 1.0)
    plt.plot(TF)
    plt.show()
