#!/usr/bin/python

from numpy import *

# functions used for generating input matrixes.
def bandGen(a, kl, ku):
    if kl > 0:
        for i in range(1 + kl, a.shape[0]):
            for j in range(0, i - kl):
                a[i, j] = 0
    if ku > 0:
        for i in range(0, a.shape[0]):
            for j in range(ku + 1 + i, a.shape[1]):
                a[i, j] = 0
    return;

def triagGen(a, uplo):
    if uplo == 'u': #upper = 1, lower = 2
        for i in range(1, a.shape[0]):
            for j in range(0, i):
                a[i, j] = 0
    elif uplo == 'l':
        for i in range(0, a.shape[0]-1):
            for j in range(i+1, a.shape[1]):
                a[i, j] = 0
    return;

def symm(a):
    for i in range(1, a.shape[0]):
        for j in range(0, i):
            a[i, j] = a[j, i];
    return;

def herm(a):
    for i in range(0, a.shape[0]):
        a[i,i] = complex(a[i,i].real, 0);
    for i in range(1, a.shape[0]):
        for j in range(0, i):
            a[i, j] = complex(a[j, i].real, -a[j, i].imag);
    return;

def sMatGen(m, n):
    a = mat(random.randint(1, 10, size=(m, n)).astype('f4'))
    return a;

def dMatGen(m, n):
    a = mat(random.randint(1, 10, size=(m, n)).astype('f8'))
    return a;

def cMatGen(m, n):
    a_real = mat(random.randint(1, 10, size=(m, n)).astype('f4'))
    a_img = mat(random.randint(1, 10, size=(m, n)).astype('f4'))
    a = a_real + 1j * a_img
    return a;

def zMatGen(m, n):
    a_real = mat(random.randint(1, 10, size=(m, n)).astype('f8'))
    a_img = mat(random.randint(1, 10, size=(m, n)).astype('f8'))
    a = a_real + 1j * a_img
    return a;


def sDataWriter(a, name, fo):
    fo.write("    static float[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(a[i,j]) + "f,");
        fo.write("\n");
        for k in range(0, len(name) + 23):
            fo.write(" ");
    fo.write(" };\n")
    return;

def dDataWriter(a, name, fo):
    fo.write("    static double[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(a[i,j]) + ",");
        fo.write("\n");
        for k in range(0, len(name) + 24):
            fo.write(" ");
    fo.write(" };\n")
    return;

def cDataWriter(a, name, fo):
    fo.write("    static float[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(real(a[i,j])) + "f,");
            fo.write(" " + str(imag(a[i,j])) + "f,");
        fo.write("\n");
        for k in range(0, len(name) + 23):
            fo.write(" ");
    fo.write(" };\n")
    return;

def zDataWriter(a, name, fo):
    fo.write("    static double[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(real(a[i,j])) + ",");
            fo.write(" " + str(imag(a[i,j])) + ",");
        fo.write("\n");
        for k in range(0, len(name) + 24):
            fo.write(" ");
    fo.write(" };\n")
    return;


def matrixCreate(dt, m, n):
    if dt == 's':
        return sMatGen(m, n);
    elif dt == 'd':
        return dMatGen(m, n);
    elif dt == 'c':
        return cMatGen(m, n);
    else:
        return zMatGen(m, n);
    return;

def dataWriter(dt, a, name, fo):
    if dt == 's':
        sDataWriter(a, name, fo);
    elif dt == 'd':
        dDataWriter(a, name, fo);
    elif dt == 'c':
        cDataWriter(a, name, fo);
    else:
        zDataWriter(a, name, fo);
    return;


#L3 Functions
def L3_xGEMM(fo, alpha, beta, m, n, k):
    dataType = ['s', 'd', 'c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, m, k);
        b = matrixCreate(dt, k, n);
        c = matrixCreate(dt, m, n);
        dataWriter(dt, a, "L3_" + dt + "GEMM_A_mk", fo);
        dataWriter(dt, b, "L3_" + dt + "GEMM_B_kn", fo);
        dataWriter(dt, c, "L3_" + dt + "GEMM_C_mn", fo);

        d = alpha * a * b + beta * c;
        dataWriter(dt, d, "L3_" + dt + "GEMM_o_NN", fo);

        a = matrixCreate(dt, k, m);
        b = matrixCreate(dt, n, k);
        dataWriter(dt, a, "L3_" + dt + "GEMM_A_km", fo);
        dataWriter(dt, b, "L3_" + dt + "GEMM_B_nk", fo);

        d = alpha * a.T * b.T + beta * c;
        dataWriter(dt, d, "L3_" + dt + "GEMM_o_TT", fo);
        d = alpha * a.H * b.H + beta * c;
        dataWriter(dt, d, "L3_" + dt + "GEMM_o_HH", fo);
    return

def L3_xSYMM(fo, alpha, beta, m, n):
    dataType = ['s', 'd', 'c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, m, m);
        symm(a);
        dataWriter(dt, a, "L3_" + dt + "SYMM_A_mm", fo);

        b = matrixCreate(dt, m, n);
        c = matrixCreate(dt, m, n);
        dataWriter(dt, b, "L3_" + dt + "SYMM_B_mn", fo);
        dataWriter(dt, c, "L3_" + dt + "SYMM_C_mn", fo);

        d = alpha * a * b + beta * c;
        dataWriter(dt, d, "L3_" + dt + "SYMM_o_L", fo);

        a = matrixCreate(dt, n, n);
        symm(a);
        dataWriter(dt, a, "L3_" + dt + "SYMM_A_nn", fo);
        d = alpha * b * a + beta * c;
        dataWriter(dt, d, "L3_" + dt + "SYMM_o_R", fo);
    return

def L3_xHEMM(fo, alpha, beta, m, n):
    dataType = ['c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, m, m);
        herm(a);
        dataWriter(dt, a, "L3_" + dt + "HEMM_A_mm", fo);

        b = matrixCreate(dt, m, n);
        c = matrixCreate(dt, m, n);
        dataWriter(dt, b, "L3_" + dt + "HEMM_B_mn", fo);
        dataWriter(dt, c, "L3_" + dt + "HEMM_C_mn", fo);

        d = alpha * a * b + beta * c;
        dataWriter(dt, d, "L3_" + dt + "HEMM_o_L", fo);

        a = matrixCreate(dt, n, n);
        herm(a);
        dataWriter(dt, a, "L3_" + dt + "HEMM_A_nn", fo);
        d = alpha * b * a + beta * c;
        dataWriter(dt, d, "L3_" + dt + "HEMM_o_R", fo);
    return

def L3_xSYRK(fo, alpha, beta, n, k):
    dataType = ['s', 'd', 'c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, n, k);
        dataWriter(dt, a, "L3_" + dt + "SYRK_A_nk", fo);
        c = matrixCreate(dt, n, n);
        symm(c);
        dataWriter(dt, c, "L3_" + dt + "SYRK_C_nn", fo);
        d = alpha * a * a.T + beta * c;
        dataWriter(dt, d, "L3_" + dt + "SYRK_o_N", fo);

        a = matrixCreate(dt, k, n);
        dataWriter(dt, a, "L3_" + dt + "SYRK_A_kn", fo);
        d = alpha * a.T * a + beta * c;
        dataWriter(dt, d, "L3_" + dt + "SYRK_o_T", fo);
    return

def L3_xHERK(fo, alpha, beta, n, k):
    dataType = ['c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, n, k);
        dataWriter(dt, a, "L3_" + dt + "HERK_A_nk", fo);
        c = matrixCreate(dt, n, n);
        herm(c);
        dataWriter(dt, c, "L3_" + dt + "HERK_C_nn", fo);
        d = alpha * a * a.H + beta * c;
        dataWriter(dt, d, "L3_" + dt + "HERK_o_N", fo);

        a = matrixCreate(dt, k, n);
        dataWriter(dt, a, "L3_" + dt + "HERK_A_kn", fo);
        d = alpha * a.H * a + beta * c;
        dataWriter(dt, d, "L3_" + dt + "HERK_o_H", fo);
    return

def L3_xSYR2K(fo, alpha, beta, n, k):
    dataType = ['s', 'd', 'c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, n, k);
        b = matrixCreate(dt, n, k);
        dataWriter(dt, a, "L3_" + dt + "SYR2K_A_nk", fo);
        dataWriter(dt, b, "L3_" + dt + "SYR2K_B_nk", fo);
        c = matrixCreate(dt, n, n);
        symm(c);
        dataWriter(dt, c, "L3_" + dt + "SYR2K_C_nn", fo);
        d = alpha * (a * b.T + b * a.T) + beta * c;
        dataWriter(dt, d, "L3_" + dt + "SYR2K_o_N", fo);

        a = matrixCreate(dt, k, n);
        b = matrixCreate(dt, k, n);
        dataWriter(dt, a, "L3_" + dt + "SYR2K_A_kn", fo);
        dataWriter(dt, b, "L3_" + dt + "SYR2K_B_kn", fo);
        d = alpha * (a.T * b + b.T * a) + beta * c;
        dataWriter(dt, d, "L3_" + dt + "SYR2K_o_T", fo);
    return

def L3_xHER2K(fo, alpha, beta, n, k):
    dataType = ['c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, n, k);
        b = matrixCreate(dt, n, k);
        dataWriter(dt, a, "L3_" + dt + "HER2K_A_nk", fo);
        dataWriter(dt, b, "L3_" + dt + "HER2K_B_nk", fo);
        c = matrixCreate(dt, n, n);
        herm(c);
        dataWriter(dt, c, "L3_" + dt + "HER2K_C_nn", fo);
        d = alpha * (a * b.H + b * a.H) + beta * c;
        dataWriter(dt, d, "L3_" + dt + "HER2K_o_N", fo);

        a = matrixCreate(dt, k, n);
        b = matrixCreate(dt, k, n);
        dataWriter(dt, a, "L3_" + dt + "HER2K_A_kn", fo);
        dataWriter(dt, b, "L3_" + dt + "HER2K_B_kn", fo);
        d = alpha * (a.H * b + b.H * a) + beta * c;
        dataWriter(dt, d, "L3_" + dt + "HER2K_o_H", fo);
    return


def L3_xTRMM(fo, alpha, m, n):
    dataType = ['s', 'd', 'c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, m, m);
        triagGen(a, 'u');
        dataWriter(dt, a, "L3_" + dt + "TRMM_A_mm", fo);
        b = matrixCreate(dt, m, n);
        dataWriter(dt, b, "L3_" + dt + "TRMM_B_mn", fo);
        d = alpha * a * b;
        dataWriter(dt, d, "L3_" + dt + "TRMM_o_LUN", fo);

        a = matrixCreate(dt, n, n);
        triagGen(a, 'l');
        dataWriter(dt, a, "L3_" + dt + "TRMM_A_nn", fo);
        d = alpha * b * a.T;
        dataWriter(dt, d, "L3_" + dt + "TRMM_o_RLT", fo);
    return

def L3_xTRSM(fo, alpha, m, n):
    dataType = ['s', 'd', 'c', 'z'];

    for dt in dataType:
        a = matrixCreate(dt, m, m);
        triagGen(a, 'u');
        dataWriter(dt, a, "L3_" + dt + "TRSM_A_mm", fo);
        b = matrixCreate(dt, m, n);
        dataWriter(dt, b, "L3_" + dt + "TRSM_B_mn", fo);
        d = alpha * (a.I * b);
        dataWriter(dt, d, "L3_" + dt + "TRSM_o_LUN", fo);

        a = matrixCreate(dt, n, n);
        triagGen(a, 'l');
        dataWriter(dt, a, "L3_" + dt + "TRSM_A_nn", fo);
        d = alpha * (b * a.I.T);
        dataWriter(dt, d, "L3_" + dt + "TRSM_o_RLT", fo);
    return


def testBLASL3(fo):
    m = random.randint(10, 20);
    n = random.randint(10, 20);
    k = random.randint(10, 20);
    alpha = 1.0;
    beta = 1.0;

    fo.write("    static int dM = " + str(m) + ';\n');
    fo.write("    static int dN = " + str(n) + ';\n');
    fo.write("    static int dK = " + str(k) + ';\n');
    fo.write('\n');
    fo.write("    static double ALPHA = " + str(alpha) + ';\n');
    fo.write("    static double BETA = " + str(beta) + ';\n');
    fo.write('\n');

    L3_xGEMM(fo, alpha, beta, m, n, k);
    L3_xSYMM(fo, alpha, beta, m, n);
    L3_xHEMM(fo, alpha, beta, m, n);
    L3_xSYRK(fo, alpha, beta, n, k);
    L3_xHERK(fo, alpha, beta, n, k);
    L3_xSYR2K(fo, alpha, beta, n, k);
    L3_xHER2K(fo, alpha, beta, n, k);
    L3_xTRMM(fo, alpha, m, n);
    L3_xTRSM(fo, alpha, m, n);

    return;

def javaDataGen():
    fo = open("BLASData.java", "w+")
    fo.write("/*\n");
    fo.write(" * Copyright (C) 2015 The Android Open Source Project\n");
    fo.write(" *\n");
    fo.write(" * Licensed under the Apache License, Version 2.0 (the \"License\");\n");
    fo.write(" * you may not use this file except in compliance with the License.\n");
    fo.write(" * You may obtain a copy of the License at\n");
    fo.write(" *\n");
    fo.write(" *      http://www.apache.org/licenses/LICENSE-2.0\n");
    fo.write(" *\n");
    fo.write(" * Unless required by applicable law or agreed to in writing, software\n");
    fo.write(" * distributed under the License is distributed on an \"AS IS\" BASIS,\n");
    fo.write(" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
    fo.write(" * See the License for the specific language governing permissions and\n");
    fo.write(" * limitations under the License.\n");
    fo.write(" */\n");
    fo.write("\n");
    fo.write("/* Don't edit this file!  It is auto-generated by blas_gen.py. */\n");

    fo.write("\n");
    fo.write("package android.renderscript.cts;\n");
    fo.write("\n");

    fo.write("public class BLASData {\n");
    #data body
    testBLASL3(fo);
    fo.write("}\n");
    fo.close()
    return;

javaDataGen();

