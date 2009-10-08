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

import org.apache.harmony.luni.util.Base64;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Simple http test server for testing webkit client functionality.
 */
public class CtsTestServer {
    private static final String TAG = "CtsTestServer";
    private static final int SERVER_PORT = 4444;
    private static final int SSL_SERVER_PORT = 4445;

    public static final String FAVICON_PATH = "/favicon.ico";
    public static final String ASSET_PREFIX = "/assets/";
    public static final String FAVICON_ASSET_PATH = ASSET_PREFIX + "favicon.png";
    public static final String REDIRECT_PREFIX = "/redirect";
    public static final String DELAY_PREFIX = "/delayed";
    public static final String BINARY_PREFIX = "/binary";
    public static final int DELAY_MILLIS = 2000;

    private ServerThread mServerThread;
    private String mServerUri;
    private AssetManager mAssets;
    private Context mContext;
    private MimeTypeMap mMap;
    private String mLastQuery;

    /**
     * Create and start a local HTTP server instance.
     * @param context The application context to use for fetching assets.
     * @throws IOException
     */
    public CtsTestServer(Context context) throws Exception {
        this(context, false);
    }

    /**
     * Create and start a local HTTP server instance.
     * @param context The application context to use for fetching assets.
     * @param ssl True if the server should be using secure sockets.
     * @throws Exception
     */
    public CtsTestServer(Context context, boolean ssl) throws Exception {
        mContext = context;
        mAssets = mContext.getAssets();
        if (ssl) {
            mServerUri = "https://localhost:" + SSL_SERVER_PORT;
        } else {
            mServerUri = "http://localhost:" + SERVER_PORT;
        }
        mMap = MimeTypeMap.getSingleton();
        mServerThread = new ServerThread(this, ssl);
        mServerThread.start();
    }

    /**
     * Terminate the http server.
     */
    public void shutdown() {
        mServerThread.shutdown();
    }

    /**
     * Return the URI that points to the server root.
     */
    public String getBaseUri() {
        return mServerUri;
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
     * Return an artificially delayed absolute URL that refers to the given asset. This can be
     * used to emulate a slow HTTP server or connection.
     * @param path The path of the asset. See {@link AssetManager#open(String)}
     */
    public String getDelayedAssetUrl(String path) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(DELAY_PREFIX);
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

    public String getBinaryUrl(String mimeType, int contentLength) {
        StringBuilder sb = new StringBuilder(getBaseUri());
        sb.append(BINARY_PREFIX);
        sb.append("?type=");
        sb.append(mimeType);
        sb.append("&length=");
        sb.append(contentLength);
        return sb.toString();
    }

    public String getLastRequestUrl() {
        return mLastQuery;
    }

    /**
     * Generate a response to the given request.
     */
    private HttpResponse getResponse(HttpRequest request) {
        RequestLine requestLine = request.getRequestLine();
        HttpResponse response = null;
        if (requestLine.getMethod().equals(HttpGet.METHOD_NAME)) {
            Log.i(TAG, "GET: " + requestLine.getUri());
            String uriString = requestLine.getUri();
            mLastQuery = uriString;
            URI uri = URI.create(uriString);
            String path = uri.getPath();
            if (path.equals(FAVICON_PATH)) {
                path = FAVICON_ASSET_PATH;
            }
            if (path.startsWith(DELAY_PREFIX)) {
                try {
                    Thread.sleep(DELAY_MILLIS);
                } catch (InterruptedException ignored) {
                    // ignore
                }
                path = path.substring(DELAY_PREFIX.length());
            }
            if (path.startsWith(BINARY_PREFIX)) {
                List <NameValuePair> args = URLEncodedUtils.parse(uri, "UTF-8");
                int length = 0;
                String mimeType = null;
                try {
                    for (NameValuePair pair : args) {
                        String name = pair.getName();
                        if (name.equals("type")) {
                            mimeType = pair.getValue();
                        } else if (name.equals("length")) {
                            length = Integer.parseInt(pair.getValue());
                        }
                    }
                    if (length > 0 && mimeType != null) {
                        ByteArrayEntity entity = new ByteArrayEntity(new byte[length]);
                        entity.setContentType(mimeType);
                        response = createResponse(HttpStatus.SC_OK);
                        response.setEntity(entity);
                        response.addHeader("Content-Disposition", "attachment; filename=test.bin");
                    } else {
                        // fall through, return 404 at the end
                    }
                } catch (Exception e) {
                    // fall through, return 404 at the end
                    Log.w(TAG, e);
                }
            } else if (path.startsWith(ASSET_PREFIX)) {
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
        StatusLine sl = response.getStatusLine();
        Log.i(TAG, sl.getStatusCode() + "(" + sl.getReasonPhrase() + ")");
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
        private boolean mIsSsl;
        private boolean mIsCancelled;
        private SSLContext mSslContext;

        /**
         * Defines the keystore contents for the server, BKS version. Holds just a
         * single self-generated key. The subject name is "Test Server".
         */
        private static final String SERVER_KEYS_BKS =
            "AAAAAQAAABQDkebzoP1XwqyWKRCJEpn/t8dqIQAABDkEAAVteWtleQAAARpYl20nAAAAAQAFWC41" +
            "MDkAAAJNMIICSTCCAbKgAwIBAgIESEfU1jANBgkqhkiG9w0BAQUFADBpMQswCQYDVQQGEwJVUzET" +
            "MBEGA1UECBMKQ2FsaWZvcm5pYTEMMAoGA1UEBxMDTVRWMQ8wDQYDVQQKEwZHb29nbGUxEDAOBgNV" +
            "BAsTB0FuZHJvaWQxFDASBgNVBAMTC1Rlc3QgU2VydmVyMB4XDTA4MDYwNTExNTgxNFoXDTA4MDkw" +
            "MzExNTgxNFowaTELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExDDAKBgNVBAcTA01U" +
            "VjEPMA0GA1UEChMGR29vZ2xlMRAwDgYDVQQLEwdBbmRyb2lkMRQwEgYDVQQDEwtUZXN0IFNlcnZl" +
            "cjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA0LIdKaIr9/vsTq8BZlA3R+NFWRaH4lGsTAQy" +
            "DPMF9ZqEDOaL6DJuu0colSBBBQ85hQTPa9m9nyJoN3pEi1hgamqOvQIWcXBk+SOpUGRZZFXwniJV" +
            "zDKU5nE9MYgn2B9AoiH3CSuMz6HRqgVaqtppIe1jhukMc/kHVJvlKRNy9XMCAwEAATANBgkqhkiG" +
            "9w0BAQUFAAOBgQC7yBmJ9O/eWDGtSH9BH0R3dh2NdST3W9hNZ8hIa8U8klhNHbUCSSktZmZkvbPU" +
            "hse5LI3dh6RyNDuqDrbYwcqzKbFJaq/jX9kCoeb3vgbQElMRX8D2ID1vRjxwlALFISrtaN4VpWzV" +
            "yeoHPW4xldeZmoVtjn8zXNzQhLuBqX2MmAAAAqwAAAAUvkUScfw9yCSmALruURNmtBai7kQAAAZx" +
            "4Jmijxs/l8EBaleaUru6EOPioWkUAEVWCxjM/TxbGHOi2VMsQWqRr/DZ3wsDmtQgw3QTrUK666sR" +
            "MBnbqdnyCyvM1J2V1xxLXPUeRBmR2CXorYGF9Dye7NkgVdfA+9g9L/0Au6Ugn+2Cj5leoIgkgApN" +
            "vuEcZegFlNOUPVEs3SlBgUF1BY6OBM0UBHTPwGGxFBBcetcuMRbUnu65vyDG0pslT59qpaR0TMVs" +
            "P+tcheEzhyjbfM32/vwhnL9dBEgM8qMt0sqF6itNOQU/F4WGkK2Cm2v4CYEyKYw325fEhzTXosck" +
            "MhbqmcyLab8EPceWF3dweoUT76+jEZx8lV2dapR+CmczQI43tV9btsd1xiBbBHAKvymm9Ep9bPzM" +
            "J0MQi+OtURL9Lxke/70/MRueqbPeUlOaGvANTmXQD2OnW7PISwJ9lpeLfTG0LcqkoqkbtLKQLYHI" +
            "rQfV5j0j+wmvmpMxzjN3uvNajLa4zQ8l0Eok9SFaRr2RL0gN8Q2JegfOL4pUiHPsh64WWya2NB7f" +
            "V+1s65eA5ospXYsShRjo046QhGTmymwXXzdzuxu8IlnTEont6P4+J+GsWk6cldGbl20hctuUKzyx" +
            "OptjEPOKejV60iDCYGmHbCWAzQ8h5MILV82IclzNViZmzAapeeCnexhpXhWTs+xDEYSKEiG/camt" +
            "bhmZc3BcyVJrW23PktSfpBQ6D8ZxoMfF0L7V2GQMaUg+3r7ucrx82kpqotjv0xHghNIm95aBr1Qw" +
            "1gaEjsC/0wGmmBDg1dTDH+F1p9TInzr3EFuYD0YiQ7YlAHq3cPuyGoLXJ5dXYuSBfhDXJSeddUkl" +
            "k1ufZyOOcskeInQge7jzaRfmKg3U94r+spMEvb0AzDQVOKvjjo1ivxMSgFRZaDb/4qw=";

        private String PASSWORD = "android";

        /**
         * Loads a keystore from a base64-encoded String. Returns the KeyManager[]
         * for the result.
         */
        private KeyManager[] getKeyManagers() throws Exception {
            byte[] bytes = Base64.decode(SERVER_KEYS_BKS.getBytes());
            InputStream inputStream = new ByteArrayInputStream(bytes);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(inputStream, PASSWORD.toCharArray());
            inputStream.close();

            String algorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
            keyManagerFactory.init(keyStore, PASSWORD.toCharArray());

            return keyManagerFactory.getKeyManagers();
        }


        public ServerThread(CtsTestServer server, boolean ssl) throws Exception {
            mServer = server;
            mIsSsl = ssl;
            if (mIsSsl) {
                mSslContext = SSLContext.getInstance("TLS");
                mSslContext.init(getKeyManagers(), null, null);
                mSocket = mSslContext.getServerSocketFactory().createServerSocket(SSL_SERVER_PORT);
            } else {
                mSocket = new ServerSocket(SERVER_PORT);
            }
        }

        public void run() {
            HttpParams params = new BasicHttpParams();
            params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
            while(!mIsCancelled) {
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
                    // normal during shutdown, ignore
                } catch (HttpException h) {
                    Log.w(TAG, h);
                }
            }
            try {
                mSocket.close();
            } catch (IOException ignored) {
                // safe to ignore
            }
        }

        public void shutdown() {
            mIsCancelled = true;
            try {
                mSocket.close();
            } catch (IOException ignored) {
                // safe to ignore
            }
        }
    }
}
