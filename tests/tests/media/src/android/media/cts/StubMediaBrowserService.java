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

package android.media.cts;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.service.media.MediaBrowserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of (@link android.service.media.MediaBrowserService}.
 */
public class StubMediaBrowserService extends MediaBrowserService {
    static final String MEDIA_ID_ROOT = "test_media_id_root";
    static final String EXTRAS_KEY = "test_extras_key";
    static final String EXTRAS_VALUE = "test_extras_value";
    static final String[] MEDIA_ID_CHILDREN = new String[] {
        "test_media_id_children_0", "test_media_id_children_1",
        "test_media_id_children_2", "test_media_id_children_3"
    };

    /* package private */ static MediaSession sSession;
    private Bundle mExtras;

    @Override
    public void onCreate() {
        super.onCreate();
        sSession = new MediaSession(this, "MediaBrowserStubService");
        setSessionToken(sSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        mExtras = new Bundle();
        mExtras.putString(EXTRAS_KEY, EXTRAS_VALUE);
        return new BrowserRoot(MEDIA_ID_ROOT, mExtras);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result) {
        List<MediaItem> mediaItems = new ArrayList<>();
        if (MEDIA_ID_ROOT.equals(parentMediaId)) {
            for (String id : MEDIA_ID_CHILDREN) {
                mediaItems.add(new MediaItem(new MediaDescription.Builder()
                        .setMediaId(id).build(), MediaItem.FLAG_BROWSABLE));
            }
        }
        result.sendResult(mediaItems);
    }
}
