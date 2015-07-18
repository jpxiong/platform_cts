/*
 * Copyright 2015 The Android Open Source Project
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

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import junit.framework.TestCase;

import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for algorithm-agnostic functionality of MAC implementations backed by Android Keystore.
 */
public class MacTest extends TestCase {

    private static final String EXPECTED_PROVIDER_NAME = TestUtils.EXPECTED_CRYPTO_OP_PROVIDER_NAME;

    private static final String[] EXPECTED_ALGORITHMS = {
        "HmacSHA1",
        "HmacSHA224",
        "HmacSHA256",
        "HmacSHA384",
        "HmacSHA512",
    };

    private static final byte[] KAT_KEY = HexEncoding.decode(
            "227b212bebd775493929ef626729a587d3f81b8e18a3ed482d403910e184479b448cfa79b62bd90595efdd"
            + "15f87bd7b2d2dac480c61e969ba90a7b8ceadd3284");

    private static final byte[] SHORT_MSG_KAT_MESSAGE = HexEncoding.decode("a16037e3c901c9a1ab");

    private static final Map<String, byte[]> SHORT_MSG_KAT_MACS =
            new TreeMap<String, byte[]>(String.CASE_INSENSITIVE_ORDER);
    static {
        // From RI
        SHORT_MSG_KAT_MACS.put("HmacSHA1", HexEncoding.decode(
                "47d5677267f0efe1f7416bf504b210765674ef50"));
        SHORT_MSG_KAT_MACS.put("HmacSHA224", HexEncoding.decode(
                "03a8bbcd05e7166bff5b0b2368709a0c61c0b9d94f1b4d65c0e04948"));
        SHORT_MSG_KAT_MACS.put("HmacSHA256", HexEncoding.decode(
                "17feed3de0b2d53b69b228c2d9d26e9d57b314c50d36662596777f49445df729"));
        SHORT_MSG_KAT_MACS.put("HmacSHA384", HexEncoding.decode(
                "26e034c6696d28722ffc446ff0994f835e616cc704517d283a29648aee1eca5569c792ada8a176cdc7"
                + "813a87437e4ea0"));
        SHORT_MSG_KAT_MACS.put("HmacSHA512", HexEncoding.decode(
                "15cff189d754d6612bd18d157c8e59ac2ecd9a4b97b2ef343b7778130f7741795d5d2dc2b7addb9a36"
                + "7677ad57833b42bfa0733f49b57afd6bc32cddc0dcebec"));
    }

    private static final byte[] LONG_MSG_KAT_SEED = SHORT_MSG_KAT_MESSAGE;
    private static final int LONG_MSG_KAT_SIZE_BYTES = 3 * 1024 * 1024 + 149;

    private static final Map<String, byte[]> LONG_MSG_KAT_MACS =
            new TreeMap<String, byte[]>(String.CASE_INSENSITIVE_ORDER);
    static {
        // From RI
        LONG_MSG_KAT_MACS.put("HmacSHA1", HexEncoding.decode(
                "2a89d12da79f541512db9c35c0a1e76750e01d48"));
        LONG_MSG_KAT_MACS.put("HmacSHA224", HexEncoding.decode(
                "5fef55c822f9b931c1b4ad7142e0a74ceaddf03f0a6533155cc06871"));
        LONG_MSG_KAT_MACS.put("HmacSHA256", HexEncoding.decode(
                "0bc25f22b8993d003a95a88c6cfa1c5a7b067a8aae1064ef897712418569bfe9"));
        LONG_MSG_KAT_MACS.put("HmacSHA384", HexEncoding.decode(
                "595a616295123966126102c06d69f8bb06c11090490186243420c2c4692877d75752b220c1b0447320"
                + "959e28345523fc"));
        LONG_MSG_KAT_MACS.put("HmacSHA512", HexEncoding.decode(
                "aa97d594d799164d56e6652578f7884d1198bb2663641ad7903e3c0bda4c136e9f94ca0d16c3504302"
                + "2944224e538e88a5410adb38eaa5169b3125738990e6d0"));
    }


    private static final long DAY_IN_MILLIS = TestUtils.DAY_IN_MILLIS;

    public void testAlgorithmList() {
        // Assert that Android Keystore Provider exposes exactly the expected MAC algorithms. We
        // don't care whether the algorithms are exposed via aliases, as long as the canonical names
        // of algorithms are accepted.
        // If the Provider exposes extraneous algorithms, it'll be caught because it'll have to
        // expose at least one Service for such an algorithm, and this Service's algorithm will
        // not be in the expected set.

        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        Set<Service> services = provider.getServices();
        Set<String> actualAlgsLowerCase = new HashSet<String>();
        Set<String> expectedAlgsLowerCase = new HashSet<String>(
                Arrays.asList(TestUtils.toLowerCase(EXPECTED_ALGORITHMS)));
        for (Service service : services) {
            if ("Mac".equalsIgnoreCase(service.getType())) {
                String algLowerCase = service.getAlgorithm().toLowerCase(Locale.US);
                actualAlgsLowerCase.add(algLowerCase);
            }
        }

        TestUtils.assertContentsInAnyOrder(actualAlgsLowerCase,
                expectedAlgsLowerCase.toArray(new String[0]));
    }

    public void testAndroidKeyStoreKeysHandledByAndroidKeyStoreProvider() throws Exception {
        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                SecretKey key = importDefaultKatKey(algorithm);

                // Generate a MAC
                Mac mac = Mac.getInstance(algorithm);
                mac.init(key);
                assertSame(provider, mac.getProvider());
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testMacGeneratedByAndroidKeyStoreVerifiesByAndroidKeyStore() throws Exception {
        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                SecretKey key = importDefaultKatKey(algorithm);

                // Generate a MAC
                Mac mac = Mac.getInstance(algorithm, provider);
                mac.init(key);
                byte[] message = "This is a test".getBytes("UTF-8");
                byte[] macBytes = mac.doFinal(message);

                assertMacVerifiesOneShot(algorithm, provider, key, message, macBytes);
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testMacGeneratedByAndroidKeyStoreVerifiesByHighestPriorityProvider()
            throws Exception {
        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                SecretKey key = getDefaultKatKey(algorithm);
                SecretKey keystoreKey = importDefaultKatKey(algorithm);

                // Generate a MAC
                Mac mac = Mac.getInstance(algorithm, provider);
                mac.init(keystoreKey);
                byte[] message = "This is a test".getBytes("UTF-8");
                byte[] macBytes = mac.doFinal(message);

                assertMacVerifiesOneShot(algorithm, key, message, macBytes);
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testMacGeneratedByHighestPriorityProviderVerifiesByAndroidKeyStore()
            throws Exception {
        Provider keystoreProvider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(keystoreProvider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            Provider signingProvider = null;
            try {
                SecretKey key = getDefaultKatKey(algorithm);
                SecretKey keystoreKey = importDefaultKatKey(algorithm);

                // Generate a MAC
                Mac mac = Mac.getInstance(algorithm);
                mac.init(key);
                signingProvider = mac.getProvider();
                byte[] message = "This is a test".getBytes("UTF-8");
                byte[] macBytes = mac.doFinal(message);

                assertMacVerifiesOneShot(
                        algorithm, keystoreProvider, keystoreKey, message, macBytes);
            } catch (Throwable e) {
                throw new RuntimeException(
                        algorithm + " failed, signing provider: " + signingProvider, e);
            }
        }
    }

    public void testSmallMsgKat() throws Exception {
        byte[] message = SHORT_MSG_KAT_MESSAGE;

        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                SecretKey key = importDefaultKatKey(algorithm);

                byte[] goodMacBytes = SHORT_MSG_KAT_MACS.get(algorithm);
                assertNotNull(goodMacBytes);
                assertMacVerifiesOneShot(algorithm, key, message, goodMacBytes);
                assertMacVerifiesFedOneByteAtATime(algorithm, key, message, goodMacBytes);
                assertMacVerifiesFedUsingFixedSizeChunks(algorithm, key, message, goodMacBytes, 3);

                byte[] messageWithBitFlip = message.clone();
                messageWithBitFlip[messageWithBitFlip.length / 2] ^= 1;
                assertMacDoesNotVerifyOneShot(algorithm, key, messageWithBitFlip, goodMacBytes);

                byte[] goodMacWithBitFlip = goodMacBytes.clone();
                goodMacWithBitFlip[goodMacWithBitFlip.length / 2] ^= 1;
                assertMacDoesNotVerifyOneShot(algorithm, key, message, goodMacWithBitFlip);
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    public void testLargeMsgKat() throws Exception {
        byte[] message = TestUtils.generateLargeKatMsg(LONG_MSG_KAT_SEED, LONG_MSG_KAT_SIZE_BYTES);

        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                SecretKey key = importDefaultKatKey(algorithm);

                byte[] goodMacBytes = LONG_MSG_KAT_MACS.get(algorithm);
                assertNotNull(goodMacBytes);
                assertMacVerifiesOneShot(algorithm,  key, message, goodMacBytes);
                assertMacVerifiesFedUsingFixedSizeChunks(
                        algorithm, key, message, goodMacBytes, 20389);
                assertMacVerifiesFedUsingFixedSizeChunks(
                        algorithm, key, message, goodMacBytes, 393571);
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    public void testInitFailsWhenNotAuthorizedToSign() throws Exception {
        int badPurposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                | KeyProperties.PURPOSE_VERIFY;

        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                KeyProtection.Builder good = getWorkingImportParams(algorithm);
                assertInitSucceeds(algorithm, good.build());
                assertInitThrowsInvalidKeyException(algorithm,
                        TestUtils.buildUpon(good, badPurposes).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    public void testInitFailsWhenDigestNotAuthorized() throws Exception {
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                KeyProtection.Builder good = getWorkingImportParams(algorithm);
                assertInitSucceeds(algorithm, good.build());

                String badKeyAlgorithm = ("HmacSHA256".equalsIgnoreCase(algorithm))
                        ? "HmacSHA384" : "HmacSHA256";
                assertInitThrowsInvalidKeyException(algorithm, badKeyAlgorithm, good.build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    public void testInitFailsWhenKeyNotYetValid() throws Exception {
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                KeyProtection.Builder good = getWorkingImportParams(algorithm)
                        .setKeyValidityStart(new Date(System.currentTimeMillis() - DAY_IN_MILLIS));
                assertInitSucceeds(algorithm, good.build());

                Date badStartDate = new Date(System.currentTimeMillis() + DAY_IN_MILLIS);
                assertInitThrowsInvalidKeyException(algorithm,
                        TestUtils.buildUpon(good).setKeyValidityStart(badStartDate).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    public void testInitFailsWhenKeyNoLongerValidForOrigination() throws Exception {
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                KeyProtection.Builder good = getWorkingImportParams(algorithm)
                        .setKeyValidityForOriginationEnd(
                                new Date(System.currentTimeMillis() + DAY_IN_MILLIS));
                assertInitSucceeds(algorithm, good.build());

                Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
                assertInitThrowsInvalidKeyException(algorithm,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForOriginationEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    public void testInitIgnoresThatKeyNoLongerValidForConsumption() throws Exception {
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                KeyProtection.Builder good = getWorkingImportParams(algorithm)
                        .setKeyValidityForConsumptionEnd(
                                new Date(System.currentTimeMillis() + DAY_IN_MILLIS));
                assertInitSucceeds(algorithm, good.build());

                Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
                assertInitSucceeds(algorithm,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForConsumptionEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    private void assertMacVerifiesOneShot(
            String algorithm,
            SecretKey key,
            byte[] message,
            byte[] mac) throws Exception {
        assertMacVerifiesOneShot(algorithm, null, key, message, mac);
    }

    private void assertMacVerifiesOneShot(
            String algorithm,
            Provider provider,
            SecretKey key,
            byte[] message,
            byte[] mac) throws Exception {
        Mac m = (provider != null)
                ? Mac.getInstance(algorithm, provider) : Mac.getInstance(algorithm);
        m.init(key);
        byte[] mac2 = m.doFinal(message);
        if (!Arrays.equals(mac, mac2)) {
            fail("MAC did not verify. algorithm: " + algorithm
                    + ", provider: " + m.getProvider().getName()
                    + ", MAC (" + mac.length + " bytes): " + HexEncoding.encode(mac)
                    + ", actual MAC (" + mac2.length + " bytes): " + HexEncoding.encode(mac2));
        }
    }

    private void assertMacDoesNotVerifyOneShot(
            String algorithm,
            SecretKey key,
            byte[] message,
            byte[] mac) throws Exception {
        Mac m = Mac.getInstance(algorithm);
        m.init(key);
        byte[] mac2 = m.doFinal(message);
        if (Arrays.equals(mac, mac2)) {
            fail("MAC verifies unexpectedly. algorithm: " + algorithm
                    + ", provider: " + m.getProvider().getName()
                    + ", MAC (" + mac.length + " bytes): " + HexEncoding.encode(mac));
        }
    }

    private void assertMacVerifiesFedOneByteAtATime(
            String algorithm,
            SecretKey key,
            byte[] message,
            byte[] mac) throws Exception {
        Mac m = Mac.getInstance(algorithm);
        m.init(key);
        for (int i = 0; i < message.length; i++) {
            m.update(message[i]);
        }
        byte[] mac2 = m.doFinal();
        if (!Arrays.equals(mac, mac2)) {
            fail("MAC did not verify. algorithm: " + algorithm
                    + ", provider: " + m.getProvider().getName()
                    + ", MAC (" + mac.length + " bytes): " + HexEncoding.encode(mac)
                    + ", actual MAC (" + mac2.length + " bytes): " + HexEncoding.encode(mac2));
        }
    }

    private void assertMacVerifiesFedUsingFixedSizeChunks(
            String algorithm,
            SecretKey key,
            byte[] message,
            byte[] mac,
            int chunkSizeBytes) throws Exception {
        Mac m = Mac.getInstance(algorithm);
        m.init(key);
        int messageRemaining = message.length;
        int messageOffset = 0;
        while (messageRemaining > 0) {
            int actualChunkSizeBytes =  Math.min(chunkSizeBytes, messageRemaining);
            m.update(message, messageOffset, actualChunkSizeBytes);
            messageOffset += actualChunkSizeBytes;
            messageRemaining -= actualChunkSizeBytes;
        }
        byte[] mac2 = m.doFinal();
        if (!Arrays.equals(mac, mac2)) {
            fail("MAC did not verify. algorithm: " + algorithm
                    + ", provider: " + m.getProvider().getName()
                    + ", MAC (" + mac.length + " bytes): " + HexEncoding.encode(mac)
                    + ", actual MAC (" + mac2.length + " bytes): " + HexEncoding.encode(mac2));
        }
    }

    private void assertInitSucceeds(String algorithm, KeyProtection keyProtection)
            throws Exception {
        assertInitSucceeds(algorithm, algorithm, keyProtection);
    }

    private void assertInitSucceeds(
            String macAlgorithm, String keyAlgorithm, KeyProtection keyProtection)
                    throws Exception {
        SecretKey key = importDefaultKatKey(keyAlgorithm, keyProtection);
        Mac mac = Mac.getInstance(macAlgorithm);
        mac.init(key);
    }

    private void assertInitThrowsInvalidKeyException(String algorithm, KeyProtection keyProtection)
                    throws Exception {
        assertInitThrowsInvalidKeyException(algorithm, algorithm, keyProtection);
    }

    private void assertInitThrowsInvalidKeyException(
            String macAlgorithm, String keyAlgorithm, KeyProtection keyProtection)
                    throws Exception {
        SecretKey key = importDefaultKatKey(keyAlgorithm, keyProtection);
        Mac mac = Mac.getInstance(macAlgorithm);
        try {
            mac.init(key);
            fail("InvalidKeyException should have been thrown. MAC algorithm: " + macAlgorithm
                    + ", key algorithm: " + keyAlgorithm);
        } catch (InvalidKeyException expected) {}
    }

    private SecretKey getDefaultKatKey(String keyAlgorithm) {
        return new SecretKeySpec(KAT_KEY, keyAlgorithm);
    }

    private SecretKey importDefaultKatKey(String keyAlgorithm) throws Exception {
        return importDefaultKatKey(
                keyAlgorithm,
                new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN).build());
    }

    private SecretKey importDefaultKatKey(
            String keyAlgorithm, KeyProtection keyProtection) throws Exception {
        return TestUtils.importIntoAndroidKeyStore(
                "test1",
                getDefaultKatKey(keyAlgorithm),
                keyProtection).getKeystoreBackedSecretKey();
    }

    private static KeyProtection.Builder getWorkingImportParams(
            @SuppressWarnings("unused") String algorithm) {
        return new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN);
    }
}
