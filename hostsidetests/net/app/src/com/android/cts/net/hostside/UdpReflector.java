/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.cts.net.hostside;

import android.system.Os;
import android.system.ErrnoException;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

public class UdpReflector extends Thread {

    private static int IPV4_HEADER_LENGTH = 20;
    private static int IPV6_HEADER_LENGTH = 40;
    private static int UDP_HEADER_LENGTH = 8;

    private static int IPV4_PROTO_OFFSET = 9;
    private static int IPV6_PROTO_OFFSET = 6;
    private static int IPPROTO_UDP = 17;

    private static int IPV4_ADDR_OFFSET = 12;
    private static int IPV6_ADDR_OFFSET = 8;
    private static int IPV4_ADDR_LENGTH = 4;
    private static int IPV6_ADDR_LENGTH = 16;

    private static String TAG = "UdpReflector";

    private FileDescriptor mFd;
    private byte[] mBuf;

    public UdpReflector(FileDescriptor fd, int mtu) {
        super("UdpReflector");
        mFd = fd;
        mBuf = new byte[mtu];
    }

    private static void swapBytes(byte[] buf, int pos1, int pos2, int len) {
        for (int i = 0; i < len; i++) {
            byte b = buf[pos1 + i];
            buf[pos1 + i] = buf[pos2 + i];
            buf[pos2 + i] = b;
        }
    }

    /** Reads one packet from our mFd, and possibly writes the packet back. */
    private void processPacket() {
        int len;
        try {
            len = Os.read(mFd, mBuf, 0, mBuf.length);
        } catch (ErrnoException|IOException e) {
            Log.e(TAG, "Error reading packet: " + e.getMessage());
            return;
        }

        int version = mBuf[0] >> 4;
        int addressOffset, protoOffset, headerLength, addressLength;
        if (version == 4) {
            headerLength = IPV4_HEADER_LENGTH;
            protoOffset = IPV4_PROTO_OFFSET;
            addressOffset = IPV4_ADDR_OFFSET;
            addressLength = IPV4_ADDR_LENGTH;
        } else if (version == 6) {
            headerLength = IPV6_HEADER_LENGTH;
            protoOffset = IPV6_PROTO_OFFSET;
            addressOffset = IPV6_ADDR_OFFSET;
            addressLength = IPV6_ADDR_LENGTH;
        } else {
            return;
        }

        if (len < headerLength + UDP_HEADER_LENGTH || mBuf[protoOffset] != IPPROTO_UDP) {
            return;
        }

        // Swap src and dst IP addresses.
        swapBytes(mBuf, addressOffset, addressOffset + addressLength, addressLength);

        // Swap dst and src ports.
        int portOffset = headerLength;
        swapBytes(mBuf, portOffset, portOffset + 2, 2);

        // Send the packet back. We don't need to recalculate the checksum because we didn't change
        // the packet bytes, we only moved them around.
        try {
            len = Os.write(mFd, mBuf, 0, len);
        } catch (ErrnoException|IOException e) {
            Log.e(TAG, "Error writing packet: " + e.getMessage());
        }
    }

    public void run() {
        Log.i(TAG, "UdpReflector starting fd=" + mFd + " valid=" + mFd.valid());
        while (!interrupted() && mFd.valid()) {
            processPacket();
        }
        Log.i(TAG, "UdpReflector exiting fd=" + mFd + " valid=" + mFd.valid());
    }
}
