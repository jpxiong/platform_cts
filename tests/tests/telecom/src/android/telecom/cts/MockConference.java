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
import android.telecom.Conference;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.util.Log;

/**
 * {@link Conference} subclass that immediately performs any state changes that are a result of
 * callbacks sent from Telecom.
 */
public class MockConference extends Conference {

    // todo: Dummy implementation for now.
    public MockConference(PhoneAccountHandle phoneAccount) {
        super(phoneAccount);
    }
}
