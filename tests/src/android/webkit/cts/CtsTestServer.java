/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.webkit.cts;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

/**
 * Simple http test server for testing webkit client functionality.
 */
public class CtsTestServer {
    private static final String TAG = "CtsTestServer";
    private static final int SERVER_PORT = 4444;
    private static final String SERVER_URI = "http://localhost:" + SERVER_PORT;

    public static final String FAVICON_PATH = "/favicon.ico";
    public static final String ASSET_PREFIX = "/assets/";
    public static final String FAVICON_ASSET_PATH = ASSET_PREFIX + "favicon.png";
    public static final String REDIRECT_PREFIX = "/redirect";

    private ServerThread mServerThread;
    private AssetManager mAssets;
    private MimeTypeMap mMap;

    /**
     * Create and start a local http server instance.
     * @param context The application context to use for fetching assets.
     * @throws IOException
     */
    public CtsTestServer(Context context) throws IOException {
        mAssets = context.getAssets();
        mMap = MimeTypeMap.getSingleton();
        mServerThread = new ServerThread(this);
        mServerThread.start();
    }

    /**
     * Terminate the http server.
     */
    public void shutdown() {
        mServerThread.shutdown();
    }

    /**
     * Return a http URI that points to the server root.
     */
    public String getBaseUri() {
        return SERVER_URI;
    }

    /**
     * Return the absolute URL that refers to the given asset.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getAssetUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }

    /**
     * Return an absolute URL that indirectly refers to the given asset.
     * When a client fetches this URL, the server will respond with a temporary redirect (302)
     * referring to the absolute URL of the given asset.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getRedirectingAssetUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(REDIRECT_PREFIX);
        sb.append(ASSET_PREFIX);
        sb.append(path);
        return sb.toString();
    }

    /**
     * Generate a response to the given request.
     */
    private HttpResponse getResponse(HttpRequest request) {
        RequestLine requestLine = request.getRequestLine();
        HttpResponse response = null;
        if (requestLine.getMethod().equals(HttpGet.METHOD_NAME)) {
            Log.i(TAG, "GET: " + requestLine.getUri());
            String uri = requestLine.getUri();
            String path = URI.create(uri).getPath();
            if (path.equals(FAVICON_PATH)) {
                path = FAVICON_ASSET_PATH;
            }
            if (path.startsWith(ASSET_PREFIX)) {
                path = path.substring(ASSET_PREFIX.length());
                // request for an asset file
                try {
                    InputStream in = mAssets.open(path);
                    response = createResponse(HttpStatus.SC_OK);
                    InputStreamEntity entity = new InputStreamEntity(in, in.available());
                    String mimeType =
                        mMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
                    if (mimeType == null) {
                        mimeType = "text/html";
                    }
                    entity.setContentType(mimeType);
                    response.setEntity(entity);
                } catch (IOException e) {
                    response = null;
                    // fall through, return 404 at the end
                }
            } else if (path.startsWith(REDIRECT_PREFIX)) {
                response = createResponse(HttpStatus.SC_MOVED_TEMPORARILY);
                String location = getBaseUri() + path.substring(REDIRECT_PREFIX.length());
                Log.i(TAG, "Redirecting to: " + location);
                response.addHeader("Location", location);
                String content = "<html><head><title>moved</title></head><body><a href=\"" +
                    location + "\">here</a></body></html>";
                try {
                    StringEntity entity = new StringEntity(content);
                    entity.setContentType("text/html");
                    response.setEntity(entity);
                } catch (UnsupportedEncodingException e) {
                    Log.w(TAG, e);
                }
            }
        }
        if (response == null) {
            response = createResponse(HttpStatus.SC_NOT_FOUND);
        }
        return response;
    }

    /**
     * Create an empty response with the given status.
     */
    private HttpResponse createResponse(int status) {
        return new BasicHttpResponse(HttpVersion.HTTP_1_0, status, null);
    }

    private static class ServerThread extends Thread {
        private CtsTestServer mServer;
        private ServerSocket mSocket;
        private boolean isCancelled;

        public ServerThread(CtsTestServer server) throws IOException {
            mServer = server;
            mSocket = new ServerSocket(SERVER_PORT);
        }

        public void run() {
            HttpParams params = new BasicHttpParams();
            params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
            while(!isCancelled) {
                try {
                    Socket socket = mSocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, params);
                    HttpRequest request = conn.receiveRequestHeader();
                    HttpResponse response = mServer.getResponse(request);
                    conn.sendResponseHeader(response);
                    conn.sendResponseEntity(response);
                    conn.close();
                } catch (IOException e) {
                    // fall through
                } catch (HttpException h) {
                    // fall through
                }
            }
            try {
                mSocket.close();
            } catch (IOException ignored) {
                // safe to ignore
            }
        }

        public void shutdown() {
            isCancelled = true;
            try {
                mSocket.close();
            } catch (IOException ignored) {
                // safe to ignore
            }
        }
    }
}
