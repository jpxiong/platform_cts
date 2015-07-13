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

package android.keystore.cts;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.Signature;

public class ECDSASignatureTest extends TestCase {

    public void testNONEwithECDSATruncatesInputToFieldSize() throws Exception {
        assertNONEwithECDSATruncatesInputToFieldSize(224);
        assertNONEwithECDSATruncatesInputToFieldSize(256);
        assertNONEwithECDSATruncatesInputToFieldSize(384);
        assertNONEwithECDSATruncatesInputToFieldSize(521);
    }

    private void assertNONEwithECDSATruncatesInputToFieldSize(int keySizeBits) throws Exception {
        byte[] message = new byte[(keySizeBits * 3) / 8];
        for (int i = 0; i < message.length; i++) {
            message[i] = (byte) (i + 1);
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
        generator.initialize(new KeyGenParameterSpec.Builder(
                "test1",
                KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_NONE)
                .setKeySize(keySizeBits)
                .build());
        KeyPair keyPair = generator.generateKeyPair();

        Signature signature = Signature.getInstance("NONEwithECDSA");
        signature.initSign(keyPair.getPrivate());
        assertSame(Security.getProvider(SignatureTest.EXPECTED_PROVIDER_NAME),
                signature.getProvider());
        signature.update(message);
        byte[] sigBytes = signature.sign();

        signature = Signature.getInstance(signature.getAlgorithm(), signature.getProvider());
        signature.initVerify(keyPair.getPublic());

        // Verify the full-length message
        signature.update(message);
        assertTrue(signature.verify(sigBytes));

        // Verify the message truncated to field size
        signature.update(message, 0, (keySizeBits + 7) / 8);
        assertTrue(signature.verify(sigBytes));

        // Verify message truncated to one byte shorter than field size -- this should fail
        signature.update(message, 0, (keySizeBits / 8) - 1);
        assertFalse(signature.verify(sigBytes));
    }

    public void testNONEwithECDSASupportsMessagesShorterThanFieldSize() throws Exception {
        assertNONEwithECDSASupportsMessagesShorterThanFieldSize(224);
        assertNONEwithECDSASupportsMessagesShorterThanFieldSize(256);
        assertNONEwithECDSASupportsMessagesShorterThanFieldSize(384);
        assertNONEwithECDSASupportsMessagesShorterThanFieldSize(521);
    }

    private void assertNONEwithECDSASupportsMessagesShorterThanFieldSize(
            int keySizeBits) throws Exception {
        byte[] message = new byte[(keySizeBits * 3 / 4) / 8];
        for (int i = 0; i < message.length; i++) {
            message[i] = (byte) (i + 1);
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
        generator.initialize(new KeyGenParameterSpec.Builder(
                "test1",
                KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_NONE)
                .setKeySize(keySizeBits)
                .build());
        KeyPair keyPair = generator.generateKeyPair();

        Signature signature = Signature.getInstance("NONEwithECDSA");
        signature.initSign(keyPair.getPrivate());
        assertSame(Security.getProvider(SignatureTest.EXPECTED_PROVIDER_NAME),
                signature.getProvider());
        signature.update(message);
        byte[] sigBytes = signature.sign();

        signature = Signature.getInstance(signature.getAlgorithm(), signature.getProvider());
        signature.initVerify(keyPair.getPublic());

        // Verify the message
        signature.update(message);
        assertTrue(signature.verify(sigBytes));

        // Assert that the message is left-padded with zero bits
        byte[] fullLengthMessage = TestUtils.leftPadWithZeroBytes(message, keySizeBits / 8);
        signature.update(fullLengthMessage);
        assertTrue(signature.verify(sigBytes));
    }
}
