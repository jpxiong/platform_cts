/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.os.cts;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.MessageQueue.OnFileDescriptorEventListener;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.SystemClock;
import android.os.MessageQueue.IdleHandler;
import android.test.AndroidTestCase;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageQueueTest extends AndroidTestCase {

    private static final long TIMEOUT = 1000;

    public void testAddIdleHandler() throws InterruptedException {
        TestLooperThread looperThread = new TestLooperThread(Test.ADD_IDLE_HANDLER);
        looperThread.start();

        try {
            if (!looperThread.hasIdleHandlerBeenCalled()) {
                fail("IdleHandler#queueIdle was NOT called: " + looperThread.getTestProgress());
            }
        } finally {
            assertTrue("The looper should have been running.", looperThread.quit());
        }
    }

    public void testRemoveIdleHandler() throws InterruptedException {
        TestLooperThread looperThread = new TestLooperThread(Test.REMOVE_IDLE_HANDLER);
        looperThread.start();

        try {
            if (looperThread.hasIdleHandlerBeenCalled()) {
                fail("IdleHandler#queueIdle was called: " + looperThread.getTestProgress());
            }
        } finally {
            assertTrue("The looper should have been running.", looperThread.quit());
        }
    }

    private enum Test {ADD_IDLE_HANDLER, REMOVE_IDLE_HANDLER};

    /**
     * {@link HandlerThread} that adds or removes an idle handler depending on the {@link Test}
     * given. It uses a {@link CountDownLatch} with an initial count of 2. The first count down
     * occurs right before the looper's run thread had started running. The final count down
     * occurs when the idle handler was executed. Tests can call {@link #hasIdleHandlerBeenCalled()}
     * to see if the countdown reached to 0 or not.
     */
    private static class TestLooperThread extends HandlerThread {

        private final Test mTestMode;

        private final CountDownLatch mIdleLatch = new CountDownLatch(2);

        TestLooperThread(Test testMode) {
            super("TestLooperThread");
            mTestMode = testMode;
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();

            IdleHandler idleHandler = new IdleHandler() {
                public boolean queueIdle() {
                    mIdleLatch.countDown();
                    return false;
                }
            };

            if (mTestMode == Test.ADD_IDLE_HANDLER) {
                Looper.myQueue().addIdleHandler(idleHandler);
            } else {
                Looper.myQueue().addIdleHandler(idleHandler);
                Looper.myQueue().removeIdleHandler(idleHandler);
            }
        }

        @Override
        public void run() {
            mIdleLatch.countDown();
            super.run();
        }

        public boolean hasIdleHandlerBeenCalled() throws InterruptedException {
            return mIdleLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        public long getTestProgress() {
            return mIdleLatch.getCount();
        }
    }

    public void testIsIdle() throws Exception {
        HandlerThread thread = new HandlerThread("testIsIdle");
        thread.start();
        try {
            // Queue should initially be idle.
            assertTrue(thread.getLooper().getQueue().isIdle());

            // Post two messages.  Block in the first one leaving the second one pending.
            final CountDownLatch latch1 = new CountDownLatch(1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Wait for latch1 released before returning.
                    try {
                        latch1.await(TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) { }
                }
            });
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Release latch2 when finished.
                    latch2.countDown();
                }
            });

            // The first message is blocked so the second should still be in the queue.
            // At this point the queue will not be idle because there is a pending message.
            assertFalse(thread.getLooper().getQueue().isIdle());

            // Let the first message complete and wait for the second to leave the queue.
            // At this point the queue will be idle because it is empty.
            latch1.countDown();
            latch2.await(TIMEOUT, TimeUnit.MILLISECONDS);
            assertTrue(thread.getLooper().getQueue().isIdle());
        } finally {
            thread.quitSafely();
        }
    }

    /**
     * Use MessageQueue, send message by order
     */
    public void testMessageOrder() throws Exception {

        OrderTestHelper tester = new OrderTestHelper() {
            public void init() {
                super.init();
                long now = SystemClock.uptimeMillis() + 200;
                mLastMessage = 4;

                mHandler.sendMessageAtTime(mHandler.obtainMessage(2), now + 1);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(3), now + 2);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(4), now + 2);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(0), now + 0);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(1), now + 0);
            }

        };
        tester.doTest(1000, 50);
    }

    /**
     * Use MessageQueue, send message at front of queue.
     */
    public void testAtFrontOfQueue() throws Exception {

        OrderTestHelper tester = new OrderTestHelper() {

            public void init() {
                super.init();
                long now = SystemClock.uptimeMillis() + 200;
                mLastMessage = 3;
                mHandler.sendMessageAtTime(mHandler.obtainMessage(3), now);
                mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(2));
                mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(0));
            }

            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(1));
                }
            }
        };

        tester.doTest(1000, 50);
    }

    public void testRegisterFileDescriptorCallbackThrowsWhenFdIsNull() {
        MessageQueue queue = Looper.getMainLooper().getQueue();
        try {
            queue.addOnFileDescriptorEventListener(null, 0,
                    new OnFileDescriptorEventListener() {
                @Override
                public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                    return 0;
                }
            });
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testRegisterFileDescriptorCallbackThrowsWhenCallbackIsNull() throws Exception {
        MessageQueue queue = Looper.getMainLooper().getQueue();
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        try (ParcelFileDescriptor reader = pipe[0];
                ParcelFileDescriptor writer = pipe[1]) {
            try {
                queue.addOnFileDescriptorEventListener(reader.getFileDescriptor(), 0, null);
                fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException ex) {
                // expected
            }
        }
    }

    public void testUnregisterFileDescriptorCallbackThrowsWhenFdIsNull() throws Exception {
        MessageQueue queue = Looper.getMainLooper().getQueue();
        try {
            queue.removeOnFileDescriptorEventListener(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testUnregisterFileDescriptorCallbackDoesNothingWhenFdNotRegistered()
            throws Exception {
        MessageQueue queue = Looper.getMainLooper().getQueue();
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        try (ParcelFileDescriptor reader = pipe[0];
                ParcelFileDescriptor writer = pipe[1]) {
            queue.removeOnFileDescriptorEventListener(reader.getFileDescriptor());
        }
    }

    public void testFileDescriptorCallbacks() throws Throwable {
        // Prepare a special looper that we can catch exceptions from.
        AssertableHandlerThread thread = new AssertableHandlerThread();
        thread.start();
        try {
            final CountDownLatch writerSawError = new CountDownLatch(1);
            final CountDownLatch readerDone = new CountDownLatch(1);
            final MessageQueue queue = thread.getLooper().getQueue();
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (final FileInputStream reader = new AutoCloseInputStream(pipe[0]);
                    final FileOutputStream writer = new AutoCloseOutputStream(pipe[1])) {
                final int size = 256 * 1024;

                // Prepare to write a lot of data to the pipe asynchronously.
                // We don't actually care about the content (assume pipes work correctly)
                // so we just write lots of zeros.
                OnFileDescriptorEventListener writerCallback = new OnFileDescriptorEventListener() {
                    private byte[] mBuffer = new byte[4096];
                    private int mRemaining = size;
                    private boolean mDone;

                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        assertEquals(pipe[1].getFileDescriptor(), fd);
                        if (!mDone) {
                            // When an error happens because the reader closed its end,
                            // signal the test, and remove the callback.
                            if ((events & OnFileDescriptorEventListener.EVENT_ERROR) != 0) {
                                writerSawError.countDown();
                                mDone = true;
                                return 0;
                            }

                            // Write all output until an error is observed.
                            if ((events & OnFileDescriptorEventListener.EVENT_OUTPUT) != 0) {
                                int count = Math.min(mBuffer.length, mRemaining);
                                try {
                                    writer.write(mBuffer, 0, count);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                mRemaining -= count;
                                return mRemaining != 0 ? EVENT_OUTPUT : EVENT_ERROR;
                            }
                        }

                        // Should never see anything else.
                        fail("Saw unexpected events: " + events + ", mDone=" + mDone);
                        return 0;
                    }
                };

                // Prepare to read all of that data.
                OnFileDescriptorEventListener readerCallback = new OnFileDescriptorEventListener() {
                    private byte[] mBuffer = new byte[4096];
                    private int mRemaining = size;
                    private boolean mDone;

                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        assertEquals(pipe[0].getFileDescriptor(), fd);
                        if (!mDone) {
                            // Errors should not happen.
                            if ((events & OnFileDescriptorEventListener.EVENT_ERROR) != 0) {
                                fail("Saw unexpected error.");
                                return 0;
                            }

                            // Read until everything is read, signal the test,
                            // and remove the callback.
                            if ((events & OnFileDescriptorEventListener.EVENT_INPUT) != 0) {
                                try {
                                    int count = reader.read(mBuffer, 0, mBuffer.length);
                                    mRemaining -= count;
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                if (mRemaining != 0) {
                                    return EVENT_INPUT;
                                }
                                readerDone.countDown();
                                mDone = true;
                                return 0;
                            }
                        }

                        // Should never see anything else.
                        fail("Saw unexpected events: " + events + ", mDone=" + mDone);
                        return 0;
                    }
                };

                // Register the callbacks.
                queue.addOnFileDescriptorEventListener(reader.getFD(),
                        OnFileDescriptorEventListener.EVENT_INPUT, readerCallback);
                queue.addOnFileDescriptorEventListener(writer.getFD(),
                        OnFileDescriptorEventListener.EVENT_OUTPUT, writerCallback);

                // Wait for the reader to see all of the data that the writer
                // is prepared to send.
                readerDone.await(TIMEOUT, TimeUnit.MILLISECONDS);

                // At this point the reader's callback should be unregistered.
                // Close the reader's file descriptor (pretend it crashed or something).
                reader.close();

                // Because the reader is gone, the writer should observe an error (EPIPE).
                // Wait for this to happen.
                writerSawError.await(TIMEOUT, TimeUnit.MILLISECONDS);

                // The reader and writer should already be unregistered.
                // Try to unregistered them again to ensure nothing bad happens.
                queue.removeOnFileDescriptorEventListener(reader.getFD());
                queue.removeOnFileDescriptorEventListener(writer.getFD());
            }
        } finally {
            thread.quitAndRethrow();
        }
    }

    /**
     * Since file descriptor numbers may be reused, there are some interesting
     * edge cases around closing file descriptors within the callback and adding
     * new ones with the same number.
     *
     * Register a file descriptor, close it from within the callback before
     * returning, return.  Then create a new file descriptor (with the same number),
     * register it.  Ensure that we start getting events for the new file descriptor.
     *
     * This test exercises special logic in Looper.cpp for EPOLL_CTL_DEL handling EBADF.
     */
    public void testPathologicalFileDescriptorReuseCallbacks1() throws Throwable {
        // Prepare a special looper that we can catch exceptions from.
        AssertableHandlerThread thread = new AssertableHandlerThread();
        thread.start();
        try {
            final MessageQueue queue = thread.getLooper().getQueue();
            final Handler handler = new Handler(thread.getLooper());

            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final int oldReaderFd = pipe[0].getFd();
            try (final FileInputStream reader = new AutoCloseInputStream(pipe[0]);
                    final FileOutputStream writer = new AutoCloseOutputStream(pipe[1])) {
                // Register the callback.
                final boolean[] awoke = new boolean[1];
                queue.addOnFileDescriptorEventListener(reader.getFD(),
                        OnFileDescriptorEventListener.EVENT_ERROR, new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        awoke[0] = true;

                        // Close the file descriptor before we return.
                        try {
                            reader.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }

                        // Return 0 to unregister the callback.
                        return 0;
                    }
                });

                // Close the writer to wake up the callback (due to hangup).
                writer.close();

                // Wait for the looper to catch up and run the callback.
                syncWait(handler);
                assertTrue(awoke[0]);
            }

            // At this point, the reader and writer are both closed.
            // If we're lucky, we can create a new pipe with the same file
            // descriptor numbers as before.
            final ParcelFileDescriptor[] pipe2 = ParcelFileDescriptor.createPipe();
            assertEquals("Expected new pipe to be created with same fd number as "
                    + "previous pipe we just closed for the purpose of this test.",
                    oldReaderFd, pipe2[0].getFd());
            try (final FileInputStream reader2 = new AutoCloseInputStream(pipe2[0]);
                    final FileOutputStream writer2 = new AutoCloseOutputStream(pipe2[1])) {
                // Register the callback.
                final boolean[] awoke = new boolean[1];
                queue.addOnFileDescriptorEventListener(reader2.getFD(),
                        OnFileDescriptorEventListener.EVENT_INPUT, new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        awoke[0] = true;

                        // Return 0 to unregister the callback.
                        return 0;
                    }
                });

                // Close the writer to wake up the callback (due to hangup).
                writer2.close();

                // Wait for the looper to catch up and run the callback.
                syncWait(handler);
                assertTrue(awoke[0]);
            }
        } finally {
            thread.quitAndRethrow();
        }
    }

    /**
     * Since file descriptor numbers may be reused, there are some interesting
     * edge cases around closing file descriptors within the callback and adding
     * new ones with the same number.
     *
     * Register a file descriptor, close it from within the callback before
     * returning, create a new file descriptor (with the same number) and return.
     * Then register the same file descriptor.  Ensure that we start getting events for
     * the new file descriptor.
     *
     * This test exercises special logic in Looper.cpp for EPOLL_CTL_DEL handling ENOENT.
     */
    public void testPathologicalFileDescriptorReuseCallbacks2() throws Throwable {
        // Prepare a special looper that we can catch exceptions from.
        AssertableHandlerThread thread = new AssertableHandlerThread();
        thread.start();
        try {
            final MessageQueue queue = thread.getLooper().getQueue();
            final Handler handler = new Handler(thread.getLooper());

            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final int oldReaderFd = pipe[0].getFd();
            try (final FileInputStream reader = new AutoCloseInputStream(pipe[0]);
                    final FileOutputStream writer = new AutoCloseOutputStream(pipe[1])) {
                // Register the callback.
                final boolean[] awoke = new boolean[1];
                queue.addOnFileDescriptorEventListener(reader.getFD(),
                        OnFileDescriptorEventListener.EVENT_ERROR, new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        awoke[0] = true;

                        try {
                            // Close the file descriptor before we return.
                            reader.close();

                            // At this point, the reader and writer are both closed.
                            // Assuming no one else has created a file descriptor in the meantime,
                            // when we recreate the pipe we will get the same number as before.
                            final ParcelFileDescriptor[] pipe2 = ParcelFileDescriptor.createPipe();
                            assertEquals("Expected new pipe to be created with same fd number as "
                                    + "previous pipe we just closed for the purpose of this test.",
                                    oldReaderFd, pipe2[0].getFd());
                            pipe[0] = pipe2[0];
                            pipe[1] = pipe2[1];
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }

                        // Return 0 to unregister the callback.
                        return 0;
                    }
                });

                // Close the writer to wake up the callback (due to hangup).
                writer.close();

                // Wait for the looper to catch up and run the callback.
                syncWait(handler);
                assertTrue(awoke[0]);
            }

            // Now we have a new pipe, make sure we can register it successfully.
            try (final FileInputStream reader2 = new AutoCloseInputStream(pipe[0]);
                    final FileOutputStream writer2 = new AutoCloseOutputStream(pipe[1])) {
                // Register the callback.
                final boolean[] awoke = new boolean[1];
                queue.addOnFileDescriptorEventListener(reader2.getFD(),
                        OnFileDescriptorEventListener.EVENT_INPUT, new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        awoke[0] = true;

                        // Return 0 to unregister the callback.
                        return 0;
                    }
                });

                // Close the writer to wake up the callback (due to hangup).
                writer2.close();

                // Wait for the looper to catch up and run the callback.
                syncWait(handler);
                assertTrue(awoke[0]);
            }
        } finally {
            thread.quitAndRethrow();
        }
    }

    /**
     * Since file descriptor numbers may be reused, there are some interesting
     * edge cases around closing file descriptors within the callback and adding
     * new ones with the same number.
     *
     * Register a file descriptor, close it from within the callback before
     * returning, create a new file descriptor (with the same number),
     * register it, and return.  Ensure that we start getting events for the
     * new file descriptor.
     *
     * This test exercises special logic in Looper.cpp for EPOLL_CTL_MOD handling
     * ENOENT and fallback to EPOLL_CTL_ADD as well as sequence number checks when removing
     * the fd after the callback returns.
     */
    public void testPathologicalFileDescriptorReuseCallbacks3() throws Throwable {
        // Prepare a special looper that we can catch exceptions from.
        AssertableHandlerThread thread = new AssertableHandlerThread();
        thread.start();
        try {
            final MessageQueue queue = thread.getLooper().getQueue();
            final Handler handler = new Handler(thread.getLooper());

            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final boolean[] awoke2 = new boolean[1];
            final int oldReaderFd = pipe[0].getFd();
            try (final FileInputStream reader = new AutoCloseInputStream(pipe[0]);
                    final FileOutputStream writer = new AutoCloseOutputStream(pipe[1])) {
                // Register the callback.
                final boolean[] awoke = new boolean[1];
                queue.addOnFileDescriptorEventListener(reader.getFD(),
                        OnFileDescriptorEventListener.EVENT_ERROR, new OnFileDescriptorEventListener() {
                    @Override
                    public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                        awoke[0] = true;

                        final ParcelFileDescriptor[] pipe2;
                        try {
                            // Close the file descriptor before we return.
                            reader.close();

                            // At this point, the reader and writer are both closed.
                            // Assuming no one else has created a file descriptor in the meantime,
                            // when we recreate the pipe we will get the same number as before.
                            pipe2 = ParcelFileDescriptor.createPipe();
                            assertEquals("Expected new pipe to be created with same fd number as "
                                    + "previous pipe we just closed for the purpose of this test.",
                                    oldReaderFd, pipe2[0].getFd());
                            pipe[0] = pipe2[0];
                            pipe[1] = pipe2[1];
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }

                        // Now we have a new pipe, make sure we can register it successfully.
                        queue.addOnFileDescriptorEventListener(pipe[0].getFileDescriptor(),
                                OnFileDescriptorEventListener.EVENT_INPUT,
                                new OnFileDescriptorEventListener() {
                            @Override
                            public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                                awoke2[0] = true;

                                // Return 0 to unregister the callback.
                                return 0;
                            }
                        });

                        // Return 0 to unregister the callback.
                        return 0;
                    }
                });

                // Close the writer to wake up the callback (due to hangup).
                writer.close();

                // Wait for the looper to catch up and run the callback.
                syncWait(handler);
                assertTrue(awoke[0]);
            }

            // Close the second writer to wake up the second callback (due to hangup).
            pipe[1].close();

            // Wait for the looper to catch up and run the callback.
            syncWait(handler);
            assertTrue(awoke2[0]);

            // Close the second reader now that we're done with the test.
            pipe[0].close();
        } finally {
            thread.quitAndRethrow();
        }
    }

    /**
     * Since file descriptor numbers may be reused, there are some interesting
     * edge cases around closing file descriptors within the callback and adding
     * new ones with the same number.
     *
     * Register a file descriptor, make a duplicate of it, close it from within the
     * callback before returning, return.  Look for signs that the Looper is spinning
     * and never getting a chance to block.
     *
     * This test exercises special logic in Looper.cpp for rebuilding the epoll set
     * in case it contains a file descriptor which has been closed and cannot be removed.
     */
    public void testPathologicalFileDescriptorReuseCallbacks4() throws Throwable {
        // Prepare a special looper that we can catch exceptions from.
        ParcelFileDescriptor dup = null;
        AssertableHandlerThread thread = new AssertableHandlerThread();
        thread.start();
        try {
            try {
                final MessageQueue queue = thread.getLooper().getQueue();
                final Handler handler = new Handler(thread.getLooper());

                final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                dup = pipe[0].dup();
                try (final FileInputStream reader = new AutoCloseInputStream(pipe[0]);
                        final FileOutputStream writer = new AutoCloseOutputStream(pipe[1])) {
                    // Register the callback.
                    final boolean[] awoke = new boolean[1];
                    queue.addOnFileDescriptorEventListener(reader.getFD(),
                            OnFileDescriptorEventListener.EVENT_ERROR, new OnFileDescriptorEventListener() {
                        @Override
                        public int onFileDescriptorEvents(FileDescriptor fd, int events) {
                            awoke[0] = true;

                            // Close the file descriptor before we return.
                            try {
                                reader.close();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }

                            // Return 0 to unregister the callback.
                            return 0;
                        }
                    });

                    // Close the writer to wake up the callback (due to hangup).
                    writer.close();

                    // Wait for the looper to catch up and run the callback.
                    syncWait(handler);
                    assertTrue(awoke[0]);
                }

                // Wait a little bit before we stop the thread.
                Thread.sleep(2000);
            } finally {
                // Check for how long the thread was running.
                // If the Looper behaved correctly, then it should have blocked for most of
                // the duration of the test (including that sleep above) since not much else
                // was happening.  If we failed to actually rebuild the epoll set then the
                // Looper may have been spinning continuously due to an FD that was never
                // properly removed from the epoll set so the thread runtime will be very high.
                long runtime = thread.quitAndRethrow();
                assertFalse("Looper thread spent most of its time spinning instead of blocked.",
                        runtime > 1000);
            }
        } finally {
            // Close the duplicate now that we are done with it.
            if (dup != null) {
                dup.close();
            }
        }
    }

    public void testSyncBarriers() throws Exception {
        OrderTestHelper tester = new OrderTestHelper() {
            private int mBarrierToken1;
            private int mBarrierToken2;

            public void init() {
                super.init();
                mLastMessage = 10;
                mHandler.sendEmptyMessage(0);
                mBarrierToken1 = Looper.myQueue().postSyncBarrier();
                mHandler.sendEmptyMessage(5);
                sendAsyncMessage(1);
                sendAsyncMessage(2);
                sendAsyncMessage(3);
                mHandler.sendEmptyMessage(6);
            }

            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 3) {
                    mHandler.sendEmptyMessage(7);
                    mBarrierToken2 = Looper.myQueue().postSyncBarrier();
                    sendAsyncMessage(4);
                    sendAsyncMessage(8);
                } else if (msg.what == 4) {
                    Looper.myQueue().removeSyncBarrier(mBarrierToken1);
                    sendAsyncMessage(9);
                    mHandler.sendEmptyMessage(10);
                } else if (msg.what == 8) {
                    Looper.myQueue().removeSyncBarrier(mBarrierToken2);
                }
            }

            private void sendAsyncMessage(int what) {
                Message msg = mHandler.obtainMessage(what);
                msg.setAsynchronous(true);
                mHandler.sendMessage(msg);
            }
        };

        tester.doTest(1000, 50);
    }

    public void testReleaseSyncBarrierThrowsIfTokenNotValid() throws Exception {
        MessageQueue queue = Looper.getMainLooper().getQueue();

        // Invalid token
        try {
            queue.removeSyncBarrier(-1);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Token already removed.
        int barrierToken = queue.postSyncBarrier();
        queue.removeSyncBarrier(barrierToken);
        try {
            queue.removeSyncBarrier(barrierToken);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    private void syncWait(Handler handler) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Helper class used to test sending message to message queue.
     */
    private class OrderTestHelper {
        Handler mHandler;
        int mLastMessage;
        int mCount;
        private boolean mSuccess;
        private RuntimeException mFailure;
        private boolean mDone;
        private Looper mLooper;

        public void init() {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    OrderTestHelper.this.handleMessage(msg);
                }
            };
        }

        public void handleMessage(Message msg) {
            if (mCount <= mLastMessage) {
                if (msg.what != mCount) {
                    failure(new RuntimeException("Expected message #" + mCount + ", received #"
                            + msg.what));
                } else if (mCount == mLastMessage) {
                    success();
                }

                mCount++;
            } else {
                failure(new RuntimeException("Message received after done, #" + msg.what));
            }
        }

        public void doTest(long timeout, long interval) throws InterruptedException {
            (new LooperThread()).start();

            synchronized (this) {
                long now = System.currentTimeMillis();
                long endTime = now + timeout;
                while (!mDone && now < endTime) {
                    wait(interval);
                    now = System.currentTimeMillis();
                }
            }

            mLooper.quit();

            if (!mDone) {
                throw new RuntimeException("test timed out");
            }
            if (!mSuccess) {
                throw mFailure;
            }
        }

        class LooperThread extends HandlerThread {

            public LooperThread() {
                super("MessengerLooperThread");
            }

            public void onLooperPrepared() {
                init();
                mLooper = getLooper();
            }

            @Override
            public void run() {
                super.run();
                synchronized (OrderTestHelper.this) {
                    mDone = true;
                    if (!mSuccess && mFailure == null) {
                        mFailure = new RuntimeException("no failure exception set");
                    }
                    OrderTestHelper.this.notifyAll();
                }
            }
        }

        public void success() {
            synchronized (this) {
                mSuccess = true;
                quit();
            }
        }

        public void failure(RuntimeException failure) {
            synchronized (this) {
                mSuccess = false;
                mFailure = failure;
                quit();
            }
        }

        private void quit() {
            synchronized (this) {
                mDone = true;
                notifyAll();
            }
        }
    }

    /**
     * A HandlerThread that propagates exceptions out of the event loop
     * instead of crashing the process.
     */
    private class AssertableHandlerThread extends HandlerThread {
        private Throwable mThrowable;
        private long mRuntime;

        public AssertableHandlerThread() {
            super("AssertableHandlerThread");
        }

        @Override
        public void run() {
            final long startTime = SystemClock.currentThreadTimeMillis();
            try {
                super.run();
            } catch (Throwable t) {
                mThrowable = t;
            } finally {
                mRuntime = SystemClock.currentThreadTimeMillis() - startTime;
            }
        }

        public long quitAndRethrow() throws Throwable {
            quitSafely();
            join(TIMEOUT);
            if (mThrowable != null) {
                throw mThrowable;
            }
            return mRuntime;
        }
    }
}
