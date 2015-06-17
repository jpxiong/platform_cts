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

def zero(a):
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            a[i, j] = 0;
    return;

def sMatGen(m, n, scale):
    a = mat(random.randint(1, 10, size=(m, n)).astype('f4')/scale)
    return a;

def dMatGen(m, n, scale):
    a = mat(random.randint(1, 10, size=(m, n)).astype('f8')/scale)
    return a;

def cMatGen(m, n, scale):
    a_real = mat(random.randint(1, 10, size=(m, n)).astype('f4')/scale)
    a_img = mat(random.randint(1, 10, size=(m, n)).astype('f4')/scale)
    a = a_real + 1j * a_img
    return a;

def zMatGen(m, n, scale):
    a_real = mat(random.randint(1, 10, size=(m, n)).astype('f8')/scale)
    a_img = mat(random.randint(1, 10, size=(m, n)).astype('f8')/scale)
    a = a_real + 1j * a_img
    return a;

def matrixCreateScale(dt, m, n, scale):
    if dt == 's':
        return sMatGen(m, n, scale);
    elif dt == 'd':
        return dMatGen(m, n, scale);
    elif dt == 'c':
        return cMatGen(m, n, scale);
    else:
        return zMatGen(m, n, scale);
    return;

def matrixCreate(dt, m, n):
    return matrixCreateScale(dt, m, n, 10);

def sDataWriter(a, name, skip, fo):
    fo.write("    static float[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(a[i,j]) + "f,");
            for hh in range(0, skip):
                fo.write(" 0.0f,");
        fo.write("\n");
        for k in range(0, len(name) + 23):
            fo.write(" ");
    fo.write(" };\n")
    return;

def dDataWriter(a, name, skip, fo):
    fo.write("    static double[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(a[i,j]) + ",");
            for hh in range(0, skip):
                fo.write(" 0,");
        fo.write("\n");
        for k in range(0, len(name) + 24):
            fo.write(" ");
    fo.write(" };\n")
    return;

def cDataWriter(a, name, skip, fo):
    fo.write("    static float[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(real(a[i,j])) + "f,");
            fo.write(" " + str(imag(a[i,j])) + "f,");
            for hh in range(0, skip):
                fo.write(" 0.0f,");
                fo.write(" 0.0f,");
        fo.write("\n");
        for k in range(0, len(name) + 23):
            fo.write(" ");
    fo.write(" };\n")
    return;

def zDataWriter(a, name, skip, fo):
    fo.write("    static double[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(0, a.shape[1]):
            fo.write(" " + str(real(a[i,j])) + ",");
            fo.write(" " + str(imag(a[i,j])) + ",");
            for hh in range(0, skip):
                fo.write(" 0,");
                fo.write(" 0,");
        fo.write("\n");
        for k in range(0, len(name) + 24):
            fo.write(" ");
    fo.write(" };\n")
    return;

def dataWriterInc(dt, a, name, skip, fo):
    if dt == 's':
        sDataWriter(a, name, skip, fo);
    elif dt == 'd':
        dDataWriter(a, name, skip, fo);
    elif dt == 'c':
        cDataWriter(a, name, skip, fo);
    else:
        zDataWriter(a, name, skip, fo);
    return;

def dataWriter(dt, a, name, fo):
    dataWriterInc(dt, a, name, 0, fo);
    return;

def sApWriter(a, name, fo):
    fo.write("    static float[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(i, a.shape[1]):
            fo.write(" " + str(a[i,j]) + "f,");
        fo.write("\n");
        for k in range(0, len(name) + 23):
            fo.write(" ");
    fo.write(" };\n")
    return;

def dApWriter(a, name, fo):
    fo.write("    static double[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(i, a.shape[1]):
            fo.write(" " + str(a[i,j]) + ",");
        fo.write("\n");
        for k in range(0, len(name) + 24):
            fo.write(" ");
    fo.write(" };\n")
    return;

def cApWriter(a, name, fo):
    fo.write("    static float[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(i, a.shape[1]):
            fo.write(" " + str(real(a[i,j])) + "f,");
            fo.write(" " + str(imag(a[i,j])) + "f,");
        fo.write("\n");
        for k in range(0, len(name) + 23):
            fo.write(" ");
    fo.write(" };\n")
    return;

def zApWriter(a, name, fo):
    fo.write("    static double[] " + name + " = {");
    for i in range(0, a.shape[0]):
        for j in range(i, a.shape[1]):
            fo.write(" " + str(real(a[i,j])) + ",");
            fo.write(" " + str(imag(a[i,j])) + ",");
        fo.write("\n");
        for k in range(0, len(name) + 24):
            fo.write(" ");
    fo.write(" };\n")
    return;

def apWriter(dt, a, name, fo):
    if dt == 's':
        sApWriter(a, name, fo);
    elif dt == 'd':
        dApWriter(a, name, fo);
    elif dt == 'c':
        cApWriter(a, name, fo);
    else:
        zApWriter(a, name, fo);
    return;

def sGBandWriter(a, kl, ku, name, fo):
    m = a.shape[0];
    n = a.shape[1];
    b = sMatGen(m, kl + ku + 1, 1);
    zero(b);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            b[i, j-i+kl] = a[i, j]
    sDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            a[i, j] = b[i, j-i+kl]
    return;

def dGBandWriter(a, kl, ku, name, fo):
    m = a.shape[0];
    n = a.shape[1];
    b = dMatGen(m, kl + ku + 1, 1);
    zero(b);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            b[i, j-i+kl] = a[i, j]
    dDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            a[i, j] = b[i, j-i+kl]
    return;

def cGBandWriter(a, kl, ku, name, fo):
    m = a.shape[0];
    n = a.shape[1];
    b = cMatGen(m, kl + ku + 1, 1);
    zero(b);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            b[i, j-i+kl] = a[i, j]
    cDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            a[i, j] = b[i, j-i+kl]
    return;

def zGBandWriter(a, kl, ku, name, fo):
    m = a.shape[0];
    n = a.shape[1];
    b = zMatGen(m, kl + ku + 1, 1);
    zero(b);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            b[i, j-i+kl] = a[i, j]
    zDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, m):
        for j in range(max(0, i-kl), min(i+ku+1, n)):
            a[i, j] = b[i, j-i+kl]
    return;

def gBandWriter(dt, a, kl, ku, name, fo):
    if dt == 's':
        sGBandWriter(a, kl, ku, name, fo);
    elif dt == 'd':
        dGBandWriter(a, kl, ku, name, fo);
    elif dt == 'c':
        cGBandWriter(a, kl, ku, name, fo);
    else:
        zGBandWriter(a, kl, ku, name, fo);
    return;

def sBandWriter(a, k, name, fo):
    n = a.shape[1];
    b = sMatGen(n, k+1, 1);
    zero(b);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            b[i, j-i] = a[i, j]
    sDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            a[i, j] = b[i, j-i]
    return;

def dBandWriter(a, k, name, fo):
    n = a.shape[1];
    b = dMatGen(n, k+1, 1);
    zero(b);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            b[i, j-i] = a[i, j]
    dDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            a[i, j] = b[i, j-i]
    return;

def cBandWriter(a, k, name, fo):
    n = a.shape[1];
    b = cMatGen(n, k+1, 1);
    zero(b);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            b[i, j-i] = a[i, j]
    cDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            a[i, j] = b[i, j-i]
    return;

def zBandWriter(a, k, name, fo):
    n = a.shape[1];
    b = zMatGen(n, k+1, 1);
    zero(b);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            b[i, j-i] = a[i, j]
    zDataWriter(b, name, 0, fo);
    zero(a);
    for i in range(0, n):
        for j in range(i, min(i+k+1, n)):
            a[i, j] = b[i, j-i]
    return;

def bandWriter(dt, a, k, name, fo):
    if dt == 's':
        sBandWriter(a, k, name, fo);
    elif dt == 'd':
        dBandWriter(a, k, name, fo);
    elif dt == 'c':
        cBandWriter(a, k, name, fo);
    else:
        zBandWriter(a, k, name, fo);
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
        a = matrixCreateScale(dt, m, m, 1);
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

#L2 Functions
def L2_xGEMV(fo, alpha, beta, m, n):
    dataType = ['s', 'd', 'c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, m, n);
        dataWriter(dt, a, "L2_" + dt + "GEMV_A_mn", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "GEMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "GEMV_x_n2", 1, fo);

        y = matrixCreate(dt, m, 1);
        dataWriter(dt, y, "L2_" + dt + "GEMV_y_m1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "GEMV_y_m2", 1, fo);

        d = alpha * a * x + beta * y;
        dataWriter(dt, d, "L2_" + dt + "GEMV_o_N", fo);
        dataWriterInc(dt, d, "L2_" + dt + "GEMV_o_N2", 1, fo);

        d = alpha * a.T * y + beta * x;
        dataWriter(dt, d, "L2_" + dt + "GEMV_o_T", fo);

        d = alpha * a.H * y + beta * x;
        dataWriter(dt, d, "L2_" + dt + "GEMV_o_H", fo);
    return

def L2_xGBMV(fo, alpha, beta, m, n, kl, ku):
    dataType = ['s', 'd', 'c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, m, n);
        gBandWriter(dt, a, kl, ku, "L2_" + dt + "GBMV_A_mn", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "GBMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "GBMV_x_n2", 1, fo);

        y = matrixCreate(dt, m, 1);
        dataWriter(dt, y, "L2_" + dt + "GBMV_y_m1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "GBMV_y_m2", 1, fo);

        d = alpha * a * x + beta * y;
        dataWriter(dt, d, "L2_" + dt + "GBMV_o_N", fo);
        dataWriterInc(dt, d, "L2_" + dt + "GBMV_o_N2", 1, fo);

        d = alpha * a.T * y + beta * x;
        dataWriter(dt, d, "L2_" + dt + "GBMV_o_T", fo);

        d = alpha * a.H * y + beta * x;
        dataWriter(dt, d, "L2_" + dt + "GBMV_o_H", fo);
    return

def L2_xHEMV(fo, alpha, beta, n):
    dataType = ['c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        herm(a);
        dataWriter(dt, a, "L2_" + dt + "HEMV_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "HEMV_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "HEMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "HEMV_x_n2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "HEMV_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "HEMV_y_n2", 1, fo);

        d = alpha * a * x + beta * y;
        dataWriter(dt, d, "L2_" + dt + "HEMV_o_N", fo);
        dataWriterInc(dt, d, "L2_" + dt + "HEMV_o_N2", 1, fo);
    return

def L2_xHBMV(fo, alpha, beta, n, k):
    dataType = ['c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        herm(a);
        bandWriter(dt, a, k, "L2_" + dt + "HBMV_A_nn", fo);
        herm(a);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "HBMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "HBMV_x_n2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "HBMV_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "HBMV_y_n2", 1, fo);

        d = alpha * a * x + beta * y;
        dataWriter(dt, d, "L2_" + dt + "HBMV_o_N", fo);
        dataWriterInc(dt, d, "L2_" + dt + "HBMV_o_N2", 1, fo);
    return

def L2_xHPMV(fo, alpha, beta, n):
    dataType = ['c', 'z'];
    # the same as HEMV, just A is in compact shape.
    return

def L2_xSYMV(fo, alpha, beta, n):
    dataType = ['s', 'd'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        symm(a);
        dataWriter(dt, a, "L2_" + dt + "SYMV_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "SYMV_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "SYMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "SYMV_x_n2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "SYMV_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "SYMV_y_n2", 1, fo);

        d = alpha * a * x + beta * y;
        dataWriter(dt, d, "L2_" + dt + "SYMV_o_N", fo);
        dataWriterInc(dt, d, "L2_" + dt + "SYMV_o_N2", 1, fo);
    return

def L2_xSBMV(fo, alpha, beta, n, k):
    dataType = ['s', 'd'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        symm(a);
        bandWriter(dt, a, k, "L2_" + dt + "SBMV_A_nn", fo);
        symm(a);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "SBMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "SBMV_x_n2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "SBMV_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "SBMV_y_n2", 1, fo);

        d = alpha * a * x + beta * y;
        dataWriter(dt, d, "L2_" + dt + "SBMV_o_N", fo);
        dataWriterInc(dt, d, "L2_" + dt + "SBMV_o_N2", 1, fo);
    return

def L2_xSPMV(fo, alpha, beta, n):
    dataType = ['s', 'd'];
    # the same as SYMV, just A is in compact shape.
    return

def L2_xTRMV(fo, n):
    dataType = ['s', 'd', 'c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        triagGen(a, 'u');
        dataWriter(dt, a, "L2_" + dt + "TRMV_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "TRMV_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "TRMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "TRMV_x_n2", 1, fo);

        d = a * x;
        dataWriter(dt, d, "L2_" + dt + "TRMV_o_UN", fo);
        dataWriterInc(dt, d, "L2_" + dt + "TRMV_o_UN2", 1, fo);

        d = a.T * x;
        dataWriter(dt, d, "L2_" + dt + "TRMV_o_UT", fo);

        d = a.H * x;
        dataWriter(dt, d, "L2_" + dt + "TRMV_o_UH", fo);
    return

def L2_xTBMV(fo, n, k):
    dataType = ['s', 'd', 'c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        bandWriter(dt, a, k, "L2_" + dt + "TBMV_A_nn", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "TBMV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "TBMV_x_n2", 1, fo);

        d = a * x;
        dataWriter(dt, d, "L2_" + dt + "TBMV_o_UN", fo);
        dataWriterInc(dt, d, "L2_" + dt + "TBMV_o_UN2", 1, fo);

        d = a.T * x;
        dataWriter(dt, d, "L2_" + dt + "TBMV_o_UT", fo);

        d = a.H * x;
        dataWriter(dt, d, "L2_" + dt + "TBMV_o_UH", fo);
    return

def L2_xTPMV(fo, n):
    dataType = ['s', 'd', 'c', 'z'];
    # the same as TRMV, just A is in compact shape.
    return

def L2_xTRSV(fo, n):
    dataType = ['s', 'd', 'c', 'z'];
    for dt in dataType:
        a = matrixCreateScale(dt, n, n, 0.25);
        triagGen(a, 'u');
        dataWriter(dt, a, "L2_" + dt + "TRSV_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "TRSV_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "TRSV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "TRSV_x_n2", 1, fo);

        d = a.I * x;
        dataWriter(dt, d, "L2_" + dt + "TRSV_o_UN", fo);
        dataWriterInc(dt, d, "L2_" + dt + "TRSV_o_UN2", 1, fo);

        d = a.I.T * x;
        dataWriter(dt, d, "L2_" + dt + "TRSV_o_UT", fo);

        d = a.I.H * x;
        dataWriter(dt, d, "L2_" + dt + "TRSV_o_UH", fo);
    return

def L2_xTBSV(fo, n, k):
    dataType = ['s', 'd', 'c', 'z'];
    for dt in dataType:
        a = matrixCreateScale(dt, n, n, 0.25);
        bandWriter(dt, a, k, "L2_" + dt + "TBSV_A_nn", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "TBSV_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "TBSV_x_n2", 1, fo);

        d = a.I * x;
        dataWriter(dt, d, "L2_" + dt + "TBSV_o_UN", fo);
        dataWriterInc(dt, d, "L2_" + dt + "TBSV_o_UN2", 1, fo);

        d = a.I.T * x;
        dataWriter(dt, d, "L2_" + dt + "TBSV_o_UT", fo);

        d = a.I.H * x;
        dataWriter(dt, d, "L2_" + dt + "TBSV_o_UH", fo);
    return

def L2_xTPSV(fo, n):
    dataType = ['s', 'd', 'c', 'z'];
    # the same as TRSV, just A is in compact shape.
    return

def L2_xGER(fo, alpha, m, n):
    dataType = ['s', 'd'];
    for dt in dataType:
        a = matrixCreate(dt, m, n);
        dataWriter(dt, a, "L2_" + dt + "GER_A_mn", fo);

        x = matrixCreate(dt, m, 1);
        dataWriter(dt, x, "L2_" + dt + "GER_x_m1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "GER_x_m2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "GER_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "GER_y_n2", 1, fo);

        d = alpha * x * y.T + a;
        dataWriter(dt, d, "L2_" + dt + "GER_o_N", fo);
    return

def L2_xGERU(fo, alpha, m, n):
    dataType = ['c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, m, n);
        dataWriter(dt, a, "L2_" + dt + "GERU_A_mn", fo);

        x = matrixCreate(dt, m, 1);
        dataWriter(dt, x, "L2_" + dt + "GERU_x_m1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "GERU_x_m2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "GERU_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "GERU_y_n2", 1, fo);

        d = alpha * x * y.T + a;
        dataWriter(dt, d, "L2_" + dt + "GERU_o_N", fo);
    return

def L2_xGERC(fo, alpha, m, n):
    dataType = ['c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, m, n);
        dataWriter(dt, a, "L2_" + dt + "GERC_A_mn", fo);

        x = matrixCreate(dt, m, 1);
        dataWriter(dt, x, "L2_" + dt + "GERC_x_m1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "GERC_x_m2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "GERC_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "GERC_y_n2", 1, fo);

        d = alpha * x * y.H + a;
        dataWriter(dt, d, "L2_" + dt + "GERC_o_N", fo);
    return

def L2_xHER(fo, alpha, n):
    dataType = ['c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        herm(a);
        dataWriter(dt, a, "L2_" + dt + "HER_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "HER_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "HER_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "HER_x_n2", 1, fo);

        d = alpha * x * x.H + a;
        dataWriter(dt, d, "L2_" + dt + "HER_o_N", fo);
        apWriter(dt, d, "L2_" + dt + "HER_o_N_pu", fo);
    return

def L2_xHPR(fo, alpha, n):
    dataType = ['c', 'z'];
    # the same as HER, just A is in compact shape.
    return

def L2_xHER2(fo, alpha, n):
    dataType = ['c', 'z'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        herm(a);
        dataWriter(dt, a, "L2_" + dt + "HER2_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "HER2_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "HER2_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "HER2_x_n2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "HER2_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "HER2_y_n2", 1, fo);

        d = alpha * x * y.H + y * (alpha * x.H) + a;
        dataWriter(dt, d, "L2_" + dt + "HER2_o_N", fo);
        apWriter(dt, d, "L2_" + dt + "HER2_o_N_pu", fo);
    return

def L2_xHPR2(fo, alpha, n):
    dataType = ['c', 'z'];
    # the same as HER2, just A is in compact shape.
    return

def L2_xSYR(fo, alpha, n):
    dataType = ['s', 'd'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        dataWriter(dt, a, "L2_" + dt + "SYR_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "SYR_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "SYR_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "SYR_x_n2", 1, fo);

        d = alpha * x * x.T + a;
        dataWriter(dt, d, "L2_" + dt + "SYR_o_N", fo);
        apWriter(dt, d, "L2_" + dt + "SYR_o_N_pu", fo);
    return

def L2_xSPR(fo, alpha, n):
    dataType = ['s', 'd'];
    # the same as SPR, just A is in compact shape.
    return

def L2_xSYR2(fo, alpha, n):
    dataType = ['s', 'd'];
    for dt in dataType:
        a = matrixCreate(dt, n, n);
        dataWriter(dt, a, "L2_" + dt + "SYR2_A_nn", fo);
        apWriter(dt, a, "L2_" + dt + "SYR2_A_nn_pu", fo);

        x = matrixCreate(dt, n, 1);
        dataWriter(dt, x, "L2_" + dt + "SYR2_x_n1", fo);
        dataWriterInc(dt, x, "L2_" + dt + "SYR2_x_n2", 1, fo);

        y = matrixCreate(dt, n, 1);
        dataWriter(dt, y, "L2_" + dt + "SYR2_y_n1", fo);
        dataWriterInc(dt, y, "L2_" + dt + "SYR2_y_n2", 1, fo);

        d = alpha * x * y.T + y * (alpha * x.T) + a;
        dataWriter(dt, d, "L2_" + dt + "SYR2_o_N", fo);
        apWriter(dt, d, "L2_" + dt + "SYR2_o_N_pu", fo);
    return

def L2_xSPR2(fo, alpha, n):
    dataType = ['s', 'd'];
    # the same as SPR2, just A is in compact shape.
    return


def testBLASL2L3(fo):
    m = random.randint(15, 25);
    n = random.randint(15, 25);
    k = random.randint(15, 25);
    kl = random.randint(1, 5);
    ku = random.randint(1, 5);

    alpha = 1.0;
    beta = 1.0;

    fo.write("    static int dM = " + str(m) + ';\n');
    fo.write("    static int dN = " + str(n) + ';\n');
    fo.write("    static int dK = " + str(k) + ';\n');
    fo.write('\n');
    fo.write("    static int KL = " + str(kl) + ';\n');
    fo.write("    static int KU = " + str(ku) + ';\n');
    fo.write('\n');
    fo.write("    static double ALPHA = " + str(alpha) + ';\n');
    fo.write("    static double BETA = " + str(beta) + ';\n');
    fo.write('\n');


    L2_xGEMV(fo, alpha, beta, m, n);
    L2_xGBMV(fo, alpha, beta, m, n, kl, ku);
    L2_xHEMV(fo, alpha, beta, n);
    L2_xHBMV(fo, alpha, beta, n, kl);
    L2_xHPMV(fo, alpha, beta, n);
    L2_xSYMV(fo, alpha, beta, n);
    L2_xSBMV(fo, alpha, beta, n, kl);
    L2_xSPMV(fo, alpha, beta, n);
    L2_xTRMV(fo, n);
    L2_xTBMV(fo, n, kl);
    L2_xTPMV(fo, n);
    L2_xTRSV(fo, n);
    L2_xTBSV(fo, n, kl);
    L2_xTPSV(fo, n);
    L2_xGER(fo, alpha, m, n);
    L2_xGERU(fo, alpha, m, n);
    L2_xGERC(fo, alpha, m, n);
    L2_xHER(fo, alpha, n);
    L2_xHPR(fo, alpha, n);
    L2_xHER2(fo, alpha, n);
    L2_xHPR2(fo, alpha, n);
    L2_xSYR(fo, alpha, n);
    L2_xSPR(fo, alpha, n);
    L2_xSYR2(fo, alpha, n);
    L2_xSPR2(fo, alpha, n);

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
    testBLASL2L3(fo);
    fo.write("}\n");
    fo.close()
    return;

javaDataGen();

