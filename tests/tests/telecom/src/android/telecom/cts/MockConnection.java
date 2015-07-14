/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.telecom.cts;

import static android.telecom.CallAudioState.*;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.VideoProfile;
import android.util.Log;

/**
 * {@link Connection} subclass that immediately performs any state changes that are a result of
 * callbacks sent from Telecom.
 */
public class MockConnection extends Connection {

    private CallAudioState mCallAudioState =
            new CallAudioState(false, CallAudioState.ROUTE_EARPIECE, ROUTE_EARPIECE | ROUTE_SPEAKER);
    private int mState = STATE_NEW;
    public int videoState = VideoProfile.STATE_AUDIO_ONLY;
    private String mDtmfString = "";
    private MockVideoProvider mMockVideoProvider;

    @Override
    public void onAnswer() {
        super.onAnswer();
    }

    @Override
    public void onAnswer(int videoState) {
        super.onAnswer(videoState);
        this.videoState = videoState;
        setActive();
    }

    @Override
    public void onReject() {
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
    }

    @Override
    public void onHold() {
        super.onHold();
        setOnHold();
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
        setActive();
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        destroy();
    }

    @Override
    public void onAbort() {
        super.onAbort();
    }

    @Override
    public void onPlayDtmfTone(char c) {
        super.onPlayDtmfTone(c);
        mDtmfString += c;
    }

    @Override
    public void onStopDtmfTone() {
        super.onStopDtmfTone();
        mDtmfString += ".";
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        super.onCallAudioStateChanged(state);
        mCallAudioState = state;
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);
        mState = state;
    }

    public int getCurrentState()  {
        return mState;
    }

    public CallAudioState getCurrentCallAudioState() {
        return mCallAudioState;
    }

    public String getDtmfString() {
        return mDtmfString;
    }

    /**
     * Creates a mock video provider for this connection.
     */
    public void createMockVideoProvider() {
        final MockVideoProvider mockVideoProvider = new MockVideoProvider(this);
        mMockVideoProvider = mockVideoProvider;
        setVideoProvider(mockVideoProvider);
    }

    public void sendMockVideoQuality(int videoQuality) {
        if (mMockVideoProvider == null) {
            return;
        }
        mMockVideoProvider.sendMockVideoQuality(videoQuality);
    }

    public void sendMockCallSessionEvent(int event) {
        if (mMockVideoProvider == null) {
            return;
        }
        mMockVideoProvider.sendMockCallSessionEvent(event);
    }

    public void sendMockPeerWidth(int width) {
        if (mMockVideoProvider == null) {
            return;
        }
        mMockVideoProvider.sendMockPeerWidth(width);
    }

    public void sendMockSessionModifyRequest(VideoProfile request) {
        if (mMockVideoProvider == null) {
            return;
        }
        mMockVideoProvider.sendMockSessionModifyRequest(request);
    }

    public MockVideoProvider getMockVideoProvider() {
        return mMockVideoProvider;
    }
}
