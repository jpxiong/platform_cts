/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.security.cts;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A poor man's implementation of the readelf command. This program is
 * designed to parse ELF (Executable and Linkable Format) files.
 */
class ReadElf {
    /** The magic values for the ELF identification. */
    private static final byte[] ELF_IDENT = {
            (byte) 0x7F, (byte) 'E', (byte) 'L', (byte) 'F',
    };

    /** Size of the e_ident[] structure in the ELF header. */
    private static final int EI_NIDENT = 16;

    /** Offset from end of ident structure in half-word sizes. */
    private static final int OFFSET_TYPE = 0;

    /** Machine type. */
    private static final int OFFSET_MACHINE = 1;

    /** ELF version. */
    private static final int OFFSET_VERSION = 2;

    /**
     * The offset to which the system transfers control. e.g., the first thing
     * executed.
     */
    private static final int OFFSET_ENTRY = 4;

    /** Program header offset in bytes. */
    private static final int OFFSET_PHOFF = 6;

    /** Segment header offset in bytes. */
    private static final int OFFSET_SHOFF = 8;

    /** Processor-specific flags for binary. */
    private static final int OFFSET_FLAGS = 10;

    /** ELF header size in bytes. */
    private static final int OFFSET_EHSIZE = 12;

    /** All program headers entry size in bytes. */
    private static final int OFFSET_PHENTSIZE = 13;

    /** Number of program headers in ELF. */
    private static final int OFFSET_PHNUM = 14;

    /** All segment headers entry size in bytes. */
    private static final int OFFSET_SHENTSIZE = 15;

    /** Number of segment headers in ELF. */
    private static final int OFFSET_SHNUM = 16;

    /** The section header index that refers to string table. */
    private static final int OFFSET_SHTRNDX = 17;

    /** Program header offset for type of this program header. */
    private static final int PHOFF_TYPE = 0;

    /** Program header offset for absolute offset in file. */
    private static final int PHOFF_OFFSET = 2;

    /** Program header offset for virtual address. */
    private static final int PHOFF_VADDR = 4;

    /** Program header offset for physical address. */
    private static final int PHOFF_PADDR = 6;

    /** Program header offset for file size in bytes. */
    private static final int PHOFF_FILESZ = 8;

    /** Program header offset for memory size in bytes. */
    private static final int PHOFF_MEMSZ = 10;

    /** Program header offset for flags. */
    private static final int PHOFF_FLAGS = 12;

    /**
     * Program header offset for required alignment. 0 or 1 means no alignment
     * necessary.
     */
    private static final int PHOFF_ALIGN = 14;

    /** Index into string pool for segment name. */
    private static final int SHOFF_NAME = 0;

    /** Segment header type. */
    private static final int SHOFF_TYPE = 2;

    /** Data is presented in LSB format. */
    private static final int ELFDATA2LSB = 1;

    /** Date is presented in MSB format. */
    private static final int ELFDATA2MSB = 2;

    private static final int ELFCLASS32 = 1;

    private static final int ELFCLASS64 = 2;

    private static final long PT_LOAD = 1;

    private RandomAccessFile mFile = null;
    private final byte[] mBuffer = new byte[512];
    private int mClass;
    private int mEndian;
    private boolean mIsDynamic;
    private boolean mIsPIE;
    private int mType;
    private int mWordSize;
    private int mHalfWordSize;

    static ReadElf read(File file) throws IOException {
        return new ReadElf(file);
    }

    boolean isDynamic() {
        return mIsDynamic;
    }

    int getType() {
        return mType;
    }

    boolean isPIE() {
        return mIsPIE;
    }

    private ReadElf(File file) throws IOException {
        try {
            mFile = new RandomAccessFile(file, "r");

            readIdent();

            readHeader();
        } finally {
            if (mFile != null) {
                mFile.close();
            }
        }
    }

    private void readHeader() throws IOException {
        mType = readHalf(getHeaderOffset(OFFSET_TYPE));

        final long shOffset = readWord(getHeaderOffset(OFFSET_SHOFF));
        final int shNumber = readHalf(getHeaderOffset(OFFSET_SHNUM));
        final int shSize = readHalf(getHeaderOffset(OFFSET_SHENTSIZE));

        readSectionHeaders(shOffset, shNumber, shSize);

        final long phOffset = readWord(getHeaderOffset(OFFSET_PHOFF));
        final int phNumber = readHalf(getHeaderOffset(OFFSET_PHNUM));
        final int phSize = readHalf(getHeaderOffset(OFFSET_PHENTSIZE));

        readProgramHeaders(phOffset, phNumber, phSize);
    }

    private void readSectionHeaders(long shOffset, int shNumber, int shSize) throws IOException {
        for (int i = 0; i < shNumber; i++) {
            final long type = readWord(shOffset + i * shSize + mHalfWordSize * SHOFF_TYPE);
            if (type == 6) {
                mIsDynamic = true;
            }
        }
    }

    private void readProgramHeaders(long phOffset, int phNumber, int phSize) throws IOException {
        for (int i = 0; i < phNumber; i++) {
            final long baseOffset = phOffset + i * phSize;
            final long type = readWord(baseOffset);
            if (type == PT_LOAD) {
                final long virtAddress = readWord(baseOffset + mHalfWordSize * PHOFF_VADDR);
                if (virtAddress == 0) {
                    mIsPIE = true;
                }
            }
        }
    }

    private int getHeaderOffset(int halfWorldOffset) {
        return EI_NIDENT + halfWorldOffset * mHalfWordSize;
    }

    private int readHalf(long offset) throws IOException {
        mFile.seek(offset);
        mFile.readFully(mBuffer, 0, mWordSize);

        final int answer;
        if (mEndian == ELFDATA2LSB) {
            answer = mBuffer[1] << 8 | mBuffer[0];
        } else {
            answer = mBuffer[0] << 8 | mBuffer[1];
        }

        return answer;
    }

    private long readWord(long offset) throws IOException {
        mFile.seek(offset);
        mFile.readFully(mBuffer, 0, mWordSize);

        int answer = 0;
        if (mEndian == ELFDATA2LSB) {
            for (int i = mWordSize - 1; i >= 0; i--) {
                answer = (answer << 8) | (mBuffer[i] & 0xFF);
            }
        } else {
            final int N = mWordSize - 1;
            for (int i = 0; i <= N; i++) {
                answer = (answer << 8) | mBuffer[i];
            }
        }

        return answer;
    }

    private void readIdent() throws IOException {
        mFile.seek(0);
        mFile.readFully(mBuffer, 0, EI_NIDENT);

        if (mBuffer[0] != ELF_IDENT[0] || mBuffer[1] != ELF_IDENT[1] || mBuffer[2] != ELF_IDENT[2]
                || mBuffer[3] != ELF_IDENT[3]) {
            throw new IllegalArgumentException("Invalid ELF file");
        }

        mClass = mBuffer[4];
        if (mClass == ELFCLASS32) {
            mWordSize = 4;
            mHalfWordSize = 2;
        } else {
            throw new IOException("Invalid executable type " + mClass + ": not ELFCLASS32!");
        }

        mEndian = mBuffer[5];
    }
}
