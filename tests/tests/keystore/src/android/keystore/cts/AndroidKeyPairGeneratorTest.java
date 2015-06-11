/*
 * Copyright 2013 The Android Open Source Project
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

package android.keystore.cts;

import android.security.KeyPairGeneratorSpec;
import android.test.AndroidTestCase;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.security.spec.RSAKeyGenParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.security.auth.x500.X500Principal;

import libcore.java.security.TestKeyStore;
import libcore.javax.net.ssl.TestKeyManager;
import libcore.javax.net.ssl.TestSSLContext;

public class AndroidKeyPairGeneratorTest extends AndroidTestCase {
    private KeyPairGenerator mGenerator;

    private KeyStore mKeyStore;

    private static final String TEST_ALIAS_1 = "test1";

    private static final String TEST_ALIAS_2 = "test2";

    private static final X500Principal TEST_DN_1 = new X500Principal("CN=test1");

    private static final X500Principal TEST_DN_2 = new X500Principal("CN=test2");

    private static final BigInteger TEST_SERIAL_1 = BigInteger.ONE;

    private static final BigInteger TEST_SERIAL_2 = BigInteger.valueOf(2L);

    private static final long NOW_MILLIS = System.currentTimeMillis();

    /* We have to round this off because X509v3 doesn't store milliseconds. */
    private static final Date NOW = new Date(NOW_MILLIS - (NOW_MILLIS % 1000L));

    @SuppressWarnings("deprecation")
    private static final Date NOW_PLUS_10_YEARS = new Date(NOW.getYear() + 10, 0, 1);

    @Override
    protected void setUp() throws Exception {
        mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        mKeyStore.load(null, null);

        mGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
    }

    public void testKeyPairGenerator_Initialize_Params_Unencrypted_Success() throws Exception {
        mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setSubject(TEST_DN_1)
                .setSerialNumber(TEST_SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build());
    }

    public void testKeyPairGenerator_Initialize_KeySize_Unencrypted_Failure() throws Exception {
        try {
            mGenerator.initialize(1024);
            fail("KeyPairGenerator should not support setting the key size");
        } catch (IllegalArgumentException success) {
        }
    }

    public void testKeyPairGenerator_Initialize_KeySizeAndSecureRandom_Unencrypted_Failure()
            throws Exception {
        try {
            mGenerator.initialize(1024, new SecureRandom());
            fail("KeyPairGenerator should not support setting the key size");
        } catch (IllegalArgumentException success) {
        }
    }

    public void testKeyPairGenerator_Initialize_ParamsAndSecureRandom_Unencrypted_Failure()
            throws Exception {
        mGenerator.initialize(
                new KeyPairGeneratorSpec.Builder(getContext())
                        .setAlias(TEST_ALIAS_1)
                        .setSubject(TEST_DN_1)
                        .setSerialNumber(TEST_SERIAL_1)
                        .setStartDate(NOW)
                        .setEndDate(NOW_PLUS_10_YEARS)
                        .build(),
                new SecureRandom());
    }

    public void testKeyPairGenerator_GenerateKeyPair_EC_Unencrypted_Success() throws Exception {
        mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setKeyType("EC")
                .setSubject(TEST_DN_1)
                .setSerialNumber(TEST_SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build());

        final KeyPair pair = mGenerator.generateKeyPair();
        assertNotNull("The KeyPair returned should not be null", pair);

        assertKeyPairCorrect(pair, TEST_ALIAS_1, "EC", 256, null, TEST_DN_1, TEST_SERIAL_1, NOW,
                NOW_PLUS_10_YEARS);
    }

    public void testKeyPairGenerator_GenerateKeyPair_EC_P521_Unencrypted_Success() throws Exception {
        mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setKeyType("EC")
                .setKeySize(521)
                .setSubject(TEST_DN_1)
                .setSerialNumber(TEST_SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build());

        final KeyPair pair = mGenerator.generateKeyPair();
        assertNotNull("The KeyPair returned should not be null", pair);

        assertKeyPairCorrect(pair, TEST_ALIAS_1, "EC", 521, null, TEST_DN_1, TEST_SERIAL_1, NOW,
                NOW_PLUS_10_YEARS);
    }

    public void testKeyPairGenerator_GenerateKeyPair_RSA_Unencrypted_Success() throws Exception {
        mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setSubject(TEST_DN_1)
                .setSerialNumber(TEST_SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build());

        final KeyPair pair = mGenerator.generateKeyPair();
        assertNotNull("The KeyPair returned should not be null", pair);

        assertKeyPairCorrect(pair, TEST_ALIAS_1, "RSA", 2048, null, TEST_DN_1, TEST_SERIAL_1, NOW,
                NOW_PLUS_10_YEARS);
    }

    public void testKeyPairGenerator_GenerateKeyPair_Replaced_Unencrypted_Success()
            throws Exception {
        // Generate the first key
        {
            mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1).setSubject(TEST_DN_1).setSerialNumber(TEST_SERIAL_1)
                    .setStartDate(NOW).setEndDate(NOW_PLUS_10_YEARS).build());
            final KeyPair pair1 = mGenerator.generateKeyPair();
            assertNotNull("The KeyPair returned should not be null", pair1);
            assertKeyPairCorrect(pair1, TEST_ALIAS_1, "RSA", 2048, null, TEST_DN_1, TEST_SERIAL_1,
                    NOW, NOW_PLUS_10_YEARS);
        }

        // Replace the original key
        {
            mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1).setSubject(TEST_DN_2).setSerialNumber(TEST_SERIAL_2)
                    .setStartDate(NOW).setEndDate(NOW_PLUS_10_YEARS).build());
            final KeyPair pair2 = mGenerator.generateKeyPair();
            assertNotNull("The KeyPair returned should not be null", pair2);
            assertKeyPairCorrect(pair2, TEST_ALIAS_1, "RSA", 2048, null, TEST_DN_2, TEST_SERIAL_2,
                    NOW, NOW_PLUS_10_YEARS);
        }
    }

    public void testKeyPairGenerator_GenerateKeyPair_No_Collision_Unencrypted_Success()
            throws Exception {
        // Generate the first key
        mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setSubject(TEST_DN_1)
                .setSerialNumber(TEST_SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build());
        final KeyPair pair1 = mGenerator.generateKeyPair();
        assertNotNull("The KeyPair returned should not be null", pair1);
        assertKeyPairCorrect(pair1, TEST_ALIAS_1, "RSA", 2048, null, TEST_DN_1, TEST_SERIAL_1, NOW,
                NOW_PLUS_10_YEARS);

        // Generate the second key
        mGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_2)
                .setSubject(TEST_DN_2)
                .setSerialNumber(TEST_SERIAL_2)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build());
        final KeyPair pair2 = mGenerator.generateKeyPair();
        assertNotNull("The KeyPair returned should not be null", pair2);
        assertKeyPairCorrect(pair2, TEST_ALIAS_2, "RSA", 2048, null, TEST_DN_2, TEST_SERIAL_2, NOW,
                NOW_PLUS_10_YEARS);

        // Check the first key again
        assertKeyPairCorrect(pair1, TEST_ALIAS_1, "RSA", 2048, null, TEST_DN_1, TEST_SERIAL_1, NOW,
                NOW_PLUS_10_YEARS);
    }

    private void assertKeyPairCorrect(KeyPair pair, String alias, String keyType, int keySize,
            AlgorithmParameterSpec spec, X500Principal dn, BigInteger serial, Date start, Date end)
            throws Exception {
        final PublicKey pubKey = pair.getPublic();
        assertNotNull("The PublicKey for the KeyPair should be not null", pubKey);
        assertEquals(keyType, pubKey.getAlgorithm());
        assertEquals("Public keys should be in X.509 format", "X.509", pubKey.getFormat());
        assertNotNull("Public keys should be encodable", pubKey.getEncoded());

        if ("EC".equalsIgnoreCase(keyType)) {
            assertEquals("Curve should be what was specified during initialization", keySize,
                    ((ECPublicKey) pubKey).getParams().getCurve().getField().getFieldSize());
        } else if ("RSA".equalsIgnoreCase(keyType)) {
            RSAPublicKey rsaPubKey = (RSAPublicKey) pubKey;
            assertEquals("Modulus size should be what is specified during initialization",
                    (keySize + 7) & ~7, (rsaPubKey.getModulus().bitLength() + 7) & ~7);
            if (spec != null) {
                RSAKeyGenParameterSpec params = (RSAKeyGenParameterSpec) spec;
                assertEquals((keySize + 7) & ~7, (params.getKeysize() + 7) & ~7);
                assertEquals(params.getPublicExponent(), rsaPubKey.getPublicExponent());
            }
        } else {
            fail("Unsupported key algorithm: " + keyType);
        }

        final PrivateKey privKey = pair.getPrivate();
        assertNotNull("The PrivateKey for the KeyPair should be not null", privKey);
        assertEquals(keyType, privKey.getAlgorithm());
        assertNull("getFormat() should return null", privKey.getFormat());
        assertNull("getEncoded() should return null", privKey.getEncoded());

        if ("EC".equalsIgnoreCase(keyType)) {
            assertTrue("EC private key must be instanceof ECKey: " + privKey.getClass().getName(),
                    privKey instanceof ECKey);
            assertECParameterSpecEqualsIgnoreSeed(
                    "Private and public key must have the same EC parameters",
                    ((ECKey) pubKey).getParams(), ((ECKey) privKey).getParams());
        } else if ("RSA".equalsIgnoreCase(keyType)) {
            assertTrue("RSA private key must be instance of RSAKey: "
                    + privKey.getClass().getName(),
                    privKey instanceof RSAKey);
            assertEquals("Private and public key must have the same RSA modulus",
                    ((RSAKey) pubKey).getModulus(), ((RSAKey) privKey).getModulus());
        }

        KeyStore.Entry entry = mKeyStore.getEntry(alias, null);
        assertNotNull("Entry should exist", entry);

        assertTrue("Entry should be a PrivateKeyEntry", entry instanceof KeyStore.PrivateKeyEntry);
        KeyStore.PrivateKeyEntry privEntry = (KeyStore.PrivateKeyEntry) entry;

        Certificate userCert = privEntry.getCertificate();
        assertTrue("Certificate should be in X.509 format", userCert instanceof X509Certificate);

        final X509Certificate x509userCert = (X509Certificate) userCert;

        assertEquals(
                "Public key used to sign certificate should have the same algorithm as in KeyPair",
                pubKey.getAlgorithm(), x509userCert.getPublicKey().getAlgorithm());

        assertEquals("PublicKey used to sign certificate should match one returned in KeyPair",
                pubKey, x509userCert.getPublicKey());

        assertEquals("The Subject DN should be the one passed into the params", dn,
                x509userCert.getSubjectDN());

        assertEquals("The Issuer DN should be the same as the Subject DN", dn,
                x509userCert.getIssuerDN());

        assertEquals("The Serial should be the one passed into the params", serial,
                x509userCert.getSerialNumber());

        assertDateEquals("The notBefore date should be the one passed into the params", start,
                x509userCert.getNotBefore());

        assertDateEquals("The notAfter date should be the one passed into the params", end,
                x509userCert.getNotAfter());

        // Assert that the cert's signature verifies using the public key from generated KeyPair
        x509userCert.verify(pubKey);
        // Assert that the cert's signature verifies using the public key from the cert itself.
        x509userCert.verify(x509userCert.getPublicKey());

        Certificate[] chain = privEntry.getCertificateChain();
        assertEquals("A list of CA certificates should not exist for the generated entry", 1,
                chain.length);

        assertUsableInSSLConnection(privKey, x509userCert);

        assertEquals("Retrieved key and generated key should be equal", privKey,
                privEntry.getPrivateKey());
    }

    private static void assertUsableInSSLConnection(PrivateKey privKey,
            X509Certificate x509userCert) throws Exception {
        // Set up both client and server to use the same private key + cert, and to trust that cert
        // when it's presented by peer. This exercises the use of the private key both in client
        // and server scenarios.
        TestKeyStore serverKeyStore = TestKeyStore.getServer();
        serverKeyStore.keyStore.setCertificateEntry("client-selfSigned", x509userCert);
        SSLContext serverContext = TestSSLContext.createSSLContext("TLS",
                new KeyManager[] {
                    TestKeyManager.wrap(new MyKeyManager(privKey, x509userCert))
                },
                TestKeyStore.createTrustManagers(serverKeyStore.keyStore));
        SSLContext clientContext = serverContext;

        if ("EC".equalsIgnoreCase(privKey.getAlgorithm())) {
            // As opposed to RSA (see below) EC keys are used in the same way in all cipher suites.
            // Assert that the key works with the default list of cipher suites.
            assertSSLConnectionWithClientAuth(
                clientContext, serverContext, null, x509userCert, x509userCert);
        } else if ("RSA".equalsIgnoreCase(privKey.getAlgorithm())) {
            // RSA keys are used differently between Forward Secure and non-Forward Secure cipher
            // suites. For example, RSA key exchange requires the server to decrypt using its RSA
            // private key, whereas ECDHE_RSA key exchange requires the server to sign usnig its
            // RSA private key. We thus assert that the key works with Forward Secure cipher suites
            // and that it works with non-Forward Secure cipher suites.
            List<String> fsCipherSuites = new ArrayList<String>();
            List<String> nonFsCipherSuites = new ArrayList<String>();
            for (String cipherSuite : clientContext.getDefaultSSLParameters().getCipherSuites()) {
                if (cipherSuite.contains("_ECDHE_RSA_") || cipherSuite.contains("_DHE_RSA_")) {
                    fsCipherSuites.add(cipherSuite);
                } else if (cipherSuite.contains("_RSA_WITH_")) {
                    nonFsCipherSuites.add(cipherSuite);
                }
            }
            assertFalse("No FS RSA cipher suites enabled by default", fsCipherSuites.isEmpty());
            assertFalse("No non-FS RSA cipher suites enabled", nonFsCipherSuites.isEmpty());

            // Assert that the key works with RSA Forward Secure cipher suites.
            assertSSLConnectionWithClientAuth(
                    clientContext, serverContext, fsCipherSuites.toArray(new String[0]),
                    x509userCert, x509userCert);
            // Assert that the key works with RSA non-Forward Secure cipher suites.
            assertSSLConnectionWithClientAuth(
                    clientContext, serverContext, nonFsCipherSuites.toArray(new String[0]),
                    x509userCert, x509userCert);
        } else {
            fail("Unsupported key algorithm: " + privKey.getAlgorithm());
        }
    }

    private static void assertSSLConnectionWithClientAuth(
            SSLContext clientContext, SSLContext serverContext, String[] enabledCipherSuites,
            X509Certificate expectedClientCert, X509Certificate expectedServerCert)
            throws Exception {
        SSLServerSocket serverSocket = (SSLServerSocket) serverContext.getServerSocketFactory()
                .createServerSocket(0);
        InetAddress host = InetAddress.getLocalHost();
        int port = serverSocket.getLocalPort();
        SSLSocket client = (SSLSocket) clientContext.getSocketFactory().createSocket(host, port);

        final SSLSocket server = (SSLSocket) serverSocket.accept();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Certificate[]> future = executor.submit(new Callable<Certificate[]>() {
            @Override
            public Certificate[] call() throws Exception {
                server.setNeedClientAuth(true);
                server.setWantClientAuth(true);
                server.startHandshake();
                return server.getSession().getPeerCertificates();
            }
        });
        executor.shutdown();
        if (enabledCipherSuites != null) {
            client.setEnabledCipherSuites(enabledCipherSuites);
        }
        client.startHandshake();
        Certificate[] usedServerCerts = client.getSession().getPeerCertificates();
        Certificate[] usedClientCerts = future.get();
        client.close();
        server.close();

        assertNotNull(usedServerCerts);
        assertEquals(1, usedServerCerts.length);
        assertEquals(expectedServerCert, usedServerCerts[0]);

        assertNotNull(usedClientCerts);
        assertEquals(1, usedClientCerts.length);
        assertEquals(expectedClientCert, usedClientCerts[0]);
    }

    private static class MyKeyManager extends X509ExtendedKeyManager {
        private final PrivateKey key;
        private final X509Certificate[] chain;

        public MyKeyManager(PrivateKey key, X509Certificate cert) {
            this.key = key;
            this.chain = new X509Certificate[] { cert };
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return "fake";
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return "fake";
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return chain;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[] { "fake" };
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[] { "fake" };
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return key;
        }
    }

    private static void assertDateEquals(String message, Date date1, Date date2) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

        String result1 = formatter.format(date1);
        String result2 = formatter.format(date2);

        assertEquals(message, result1, result2);
    }

    private static void assertECParameterSpecEqualsIgnoreSeed(String message,
            ECParameterSpec expected, ECParameterSpec actual) {
        EllipticCurve expectedCurve = expected.getCurve();
        EllipticCurve actualCurve = actual.getCurve();
        assertEquals(message + ": curve field", expectedCurve.getField(), actualCurve.getField());
        assertEquals(message + ": curve A", expectedCurve.getA(), actualCurve.getA());
        assertEquals(message + ": curve B", expectedCurve.getB(), actualCurve.getB());
        assertEquals(message + ": order mismatch", expected.getOrder(), actual.getOrder());
        assertEquals(message + ": generator mismatch",
                expected.getGenerator(), actual.getGenerator());
        assertEquals(message + ": cofactor mismatch", expected.getCofactor(), actual.getCofactor());
    }
}
