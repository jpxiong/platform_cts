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
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import com.android.cts.keystore.R;

import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.RSAKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.Provider.Service;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for algorithm-agnostic functionality of {@code Cipher} implementations backed by Android
 * Keystore.
 */
public class CipherTest extends AndroidTestCase {

    private static final String EXPECTED_PROVIDER_NAME = TestUtils.EXPECTED_CRYPTO_OP_PROVIDER_NAME;

    private static final String[] EXPECTED_ALGORITHMS = {
        "AES/ECB/NoPadding",
        "AES/ECB/PKCS7Padding",
        "AES/CBC/NoPadding",
        "AES/CBC/PKCS7Padding",
        "AES/CTR/NoPadding",
        "AES/GCM/NoPadding",
        "RSA/ECB/NoPadding",
        "RSA/ECB/PKCS1Padding",
        "RSA/ECB/OAEPPadding",
        "RSA/ECB/OAEPWithSHA-1AndMGF1Padding",
        "RSA/ECB/OAEPWithSHA-224AndMGF1Padding",
        "RSA/ECB/OAEPWithSHA-256AndMGF1Padding",
        "RSA/ECB/OAEPWithSHA-384AndMGF1Padding",
        "RSA/ECB/OAEPWithSHA-512AndMGF1Padding",
    };

    private static class KatVector {
        private final byte[] plaintext;
        private final byte[] ciphertext;
        private final AlgorithmParameterSpec params;

        private KatVector(String plaintextHex, String ciphertextHex) {
            this(plaintextHex, null, ciphertextHex);
        }

        private KatVector(String plaintextHex, AlgorithmParameterSpec params,
                String ciphertextHex) {
            this(HexEncoding.decode(plaintextHex), params, HexEncoding.decode(ciphertextHex));
        }

        private KatVector(byte[] plaintext, byte[] ciphertext) {
            this(plaintext, null, ciphertext);
        }

        private KatVector(byte[] plaintext, AlgorithmParameterSpec params, byte[] ciphertext) {
            this.plaintext = plaintext;
            this.ciphertext = ciphertext;
            this.params = params;
        }
    }
    private static final Map<String, KatVector> KAT_VECTORS =
            new TreeMap<String, KatVector>(String.CASE_INSENSITIVE_ORDER);
    static {
        // From RI
        KAT_VECTORS.put("AES/ECB/NoPadding", new KatVector(
                "0383911bb1519d58e6656f3fd35639c502dbeb2196cea937fca272666cb4a80b",
                "6574c5065283b89e0c930019e4655d8516b98170db6516cd83e589bd9c5e5adc"));
        KAT_VECTORS.put("AES/ECB/PKCS7Padding", new KatVector(
                "1ad3d73a3cfa66dac78a51a95c2cb2125ea701e6e9ecbca2415b436f0258e2ba7439b67545",
                "920f873f2f9e91bac4c9c948d66496a21b8b2606850490dac7abecae83317488ee550b9973ac5cd142"
                + "f387d7d2a12752"));
        KAT_VECTORS.put("AES/CBC/NoPadding", new KatVector(
                "1dffe21c8f18276c3a39ed0c53ab257b84efcedab60095c4cadd131143058cf7",
                new IvParameterSpec(HexEncoding.decode("10b3eea6cc8a7d6f48337e9b6987d28c")),
                "47ab115bfadca91eaebec73ab942a06f3121fdd5aa55d223bd2cbcc3855e1ef8"));
        KAT_VECTORS.put("AES/CBC/PKCS7Padding", new KatVector(
                "9d49fb970b23bfe742ae7c45a773ada9faad84708c8858a06e4a192e0a90e2f6083548e0bf3f67",
                new IvParameterSpec(HexEncoding.decode("ecd87bf9c49f37dcd2294e309192289a")),
                "aeb64f48ec18a086eda7ee080948651a50b6f582ab54aac5454c9ab0a4de5b4a4abac526a4307011d1"
                + "2881f1849c32ae"));
        KAT_VECTORS.put("AES/CTR/NoPadding", new KatVector(
                "b4e786cab9df48d2fce0c7872651314db1318d1f31a1b10a2c334d2555b4117668",
                new IvParameterSpec(HexEncoding.decode("94d9f7a6d16f58018819b668020b68cc")),
                "022e74572a70be57a0b65b2fb5bc9b803ce48973b6163f528bbe1fd001e29d330a"));
        KAT_VECTORS.put("AES/GCM/NoPadding", new KatVector(
                "03889a6ca811e3fd7e78467e3dae587d2110e80e98edbc9dfe17afba238c4c493186",
                new GCMParameterSpec(128, HexEncoding.decode("f67aaf97cdec65b12188315e")),
                "159eb1ffc86589b38f18097c32db646c7de3525b603876c3ae671bc2ca52a5395a374b377a915c9ed1"
                + "a349abf9fc54c9ca81"));
        KAT_VECTORS.put("RSA/ECB/NoPadding", new KatVector(
                "50c499d558c38fd48ea76832887db2abc76e4e153a98fd4323ccb8006d34f11724a5692fb101b0eb96"
                + "060eb9d15222",
                "349b1d5061e98d0ab3f2327680bbc0cbb1b8ef8ee26148d7c67cf535223e3f78d822d369592ede29b1"
                + "654aab25e6ae5e098318e55c13dc405f5ba27e5cc69ced32778592a51e6293a03f95e14ed17099fb"
                + "0ac585e41297b87c3432953df0d98be7e505dc7de7bfe9d9ec750f475afeba4cc2dd78838c0d4399"
                + "d8de02b07f00b292dc3d32d2a2f98ea5a5dac1a0fec4d01e5c3aea8c56eeff264896fb6cf2144401"
                + "278c6663417bc00aafbb9eb97c056573cdec88d6ac6fd6c333d131337b16031da229029e3b6fe6f8"
                + "ee427f2e90041e9636d67cddac75845914ce4be56092eed7188fe7e2bb33769efdeed86a7acbe15d"
                + "debf92d9fbaaddede206acfa650697"));
        KAT_VECTORS.put("RSA/ECB/PKCS1Padding", new KatVector(
                "aed8cd94f35b2a54cdd3ed771482bd87e256b995408558fb82e5d475d1ee54711472f899ad6cbb6847"
                + "99e52ff1d57cbc39f4",
                "64148dee294dd3ea31d2b595ea661318cf90c89f71393cf6559087d6e8993e73eb1e6b5f4d3cfde3cb"
                + "267938c5eca522b95a2df02df9c703dbe3103c157af0d2ed5b70da51cb4caa49061319420d0ea433"
                + "f24b727530c162226bc806b7f39079cd494a5c8a242737413d27063f9fb74aadd20f521211316719"
                + "c628fd4351d0608928949b6f59f351d9ccec4c596514335010834fcabd53a2cbb2642e0f83c4f89c"
                + "199ee2c68ace9182cf484d99e86b0b2213c1cc113d24891958e5a0774b7486abae1475e46a939a94"
                + "5d6491b98ad7979fd6e752b47e43e960557a0c0589d7d0444b011d75c9f5b143da6e1dcf7b678a2e"
                + "f82fbe37a74df3e20fb1a9dbfd5978"));
        KAT_VECTORS.put("RSA/ECB/OAEPPadding", new KatVector(
                "c219f4e3e37eae2315f0fa4ebc4b46ef0c6befbb43a51ceda07435fc88a9",
                "7a9bcfd0d02b6434025bbf5ba09c2dad118a4a3bca7cced8b404bc0fc2f17ddee13de82c8324294bf2"
                + "60ad6e5171c2c3728a0c0fab20dd60e4e56cfef3e66239439ed2eddcc83ac8eeaedfd970e9966de3"
                + "94ad1df0df503a0a640a49e10885b3a4115c3e94e893fff87bf9a5808350f957d6bc556ca6b08f81"
                + "bf697704a3eb3db774797f883af0dcdc9bd9196d7595bab5e87d3187eb45b5771abe4e4dc70c25fa"
                + "b9e3cddb6ae453a1d8e517d000779472e1376e5848b1654a51a9e90be4a4a6d0f6b8723c6e93c471"
                + "313ea94f24504ca377b502057331355965a7e0b9c3b1d1fbd24ab5a4167f721d1ddac4d3c094d5c9"
                + "0d2e277e9b5617cbf2770186323e89"));
        KAT_VECTORS.put("RSA/ECB/OAEPWithSHA-1AndMGF1Padding", new KatVector(
                "bb2854620bb0e361d1384703dda12acee1fefc22024bcfc40a86390d5342c693aab8c7ed6517d8da86"
                + "04492c9d",
                "77033c578f24ef0ed93bfe6dc6f7c3f9f0505e7562f67ce987a269cabaa8a3ae7dd5e567a8b37db42d"
                + "a79aa86ea2e189af5b9560b39407ff86f2785cdaf660fc7c93649bc24a818de564cb0d03e7681fa8"
                + "f3cd42b3bfc58c49d3f049e0c98b07aff95876f05ddc45ebaa7127a198f27ae0cfd161c5598ac795"
                + "8ed386d98b13d45730e6dc16313fe012af27d7be0e45215040bbfb07f2d35e34291fe4335a68175a"
                + "46be99a15c1ccf673659157e1f52105de5a0a6f8c9d946740216eefe2a01a37b0ab144a44ff0d800"
                + "be713b5b44acf4fcb1a60d5db977af4d77fa77bdb8594032b2f5bbdd49346b08e0e98ab1051b462e"
                + "160c1bff62b927cd26c936948b723a"));
        KAT_VECTORS.put("RSA/ECB/OAEPWithSHA-224AndMGF1Padding", new KatVector(
                "1bae19434be6599d1987b1ed866dd6b684dcd908bd98d797250be545eafea46d05ebdf9018",
                "0f18b4a1153c6f8821e18a4275e4b570d540c8ad86bfc99146e5475238a43ecbe63bc81368cd64b9a2"
                + "ab3ccd586e6afaad054c9d7bdc986adf022ec86335d110c53ebd5f2f2bd49d48d6da9541312c9b1b"
                + "cc299ca4f59475869e4ec2253c91b137eae274a245fc9ee6262f74754bbda55d8bd25bfa4c1698f3"
                + "a22d2d8d7fc6e9fbb56d828e61912b3085d82cceaeb1d2da425871575e7ba31a3d47b1b7d7df0bda"
                + "81d62c75a9887bbc528fc6bb51db09884bb513b4cc94ca4a5fe0b370ca548dcdf60eebbf61e7efe7"
                + "630fc47256d6d617fc1c2c774405f385650898abea03502cfbdcb53579fd18d896490e67aecdb7c7"
                + "b7b950dc7ddba5c64188494c1a177b"));
        KAT_VECTORS.put("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", new KatVector(
                "332c2f2fc066fb29ec0928a52b5111ce6965546ce73927340c42d33b56b6ba547b77ac361ac0d13316"
                + "345ca953840023d892fa4ff1aa32cc66d5aa88b79867",
                "942c0ba1c67a34a7e116d9281b1df5084c66bc1458faf1b26d4f0f63a57307a9addcd3e5d2f3320071"
                + "5a3d95ae84fb40a8dfe4cb0a28873fd5883ff8ee6efbfe38c460c755577b34fcf05bb2077afec7b2"
                + "203799022be6a0903915e01e94abc51efe9c5548eb86bbbb4fd7f3bfc7b86f388128b6df1e6ce651"
                + "230c6bc18bbf55b029f1e31da880c27d947ff97519df66a57ead6db791c4978f1d62edec0d89bb16"
                + "83d237213f3f24271ddb8c4b50a82527954f0e49ae44d3acd8ddd3a57cfbfa456dd40675d5d75542"
                + "31c6b79c7fb3500b1631be1d100e67d85ce423845fdc7c7f45e346a8ba573f5d11de9009069468dd"
                + "8d517ad4adb1509dd5173ee1862d74"));
        KAT_VECTORS.put("RSA/ECB/OAEPWithSHA-384AndMGF1Padding", new KatVector(
                "f51f158cbad4dbab38403b839c724f09a480c49be29c0e72615539dbe57ec86143f31f19392f419b5b"
                + "e4ba9e3c6f1e870d307a7cf1a9e2",
                "944243f35f534e7a273e94986b6835a4f5cdc5bc4efb9970d4760986599a02f652a848fcae333ff25a"
                + "64108c9b900aaf002688398ad9fc17c73be52726306af9c13540df9d1765336b6f09ba4cb8a54d72"
                + "5a4e45854bfa3802cfb110a6d7f7054e6072440ec00da62828cb75fe2566ec5be79eb8a3d1fbe2c2"
                + "4439c107e5018e445e201ad80725755543c00dec50bb464c6ca897600eb3cda51fcef8161ac13d75"
                + "a3eb30d385a1e718a61ae1b5d47aadb966fc007becc84db397d0b3cd983121872f9975995153e869"
                + "9e24554a3c5e885f0ed8cd03e916da5ed541f1598da9bd6209447301d00f086153da353deff9d045"
                + "8976ff7570410f0bdcfb3f56b782f5"));
        KAT_VECTORS.put("RSA/ECB/OAEPWithSHA-512AndMGF1Padding", new KatVector(
                "d45f6ccc7e663957f234c237c1f09bf7791f6f5c1b9ef4fefb16e55ded0d96112e590f1bb08a60f85c"
                + "2d0d2533f1d69792dfd8d647d880b18f87cfe32488c73613a3d535da7d776d90d9a4ba6a0311f456"
                + "8511da49107c",
                "5a037df3e5d6f3f703541e2db2aef7c69985e513bdff67c8ade6a09f50e27267bfb444f6c69b40a77a"
                + "9136a27b29876af9d2bf4e7099863445d35b188d31f376b89fbd196059667ca657e10b9454c2b25f"
                + "046fc9f7b42506e382e6b6fd99409cf97e865e65f8dce5d14a06b8aa8833c4bc72c8764467758f2d"
                + "7960243161dce4ca8231e91bfcd3c933a80bc703ceab976224c876b1f550f91a6c2a0332d4377bd8"
                + "dfe4b1283ab114e517b7b9e4a6e0bf166d5b506e7a3b7328078e12cb23b1d938760767dc9b3c3eb0"
                + "848ddda101792aca9273ad414314c13fc511ffa0358a8f4c5f38edded3a2dc111fa62c80e6032c32"
                + "ae04aeac7729f16a6310f1f6785c27"));
    }

    private static final long DAY_IN_MILLIS = TestUtils.DAY_IN_MILLIS;

    private static final byte[] AES_KAT_KEY_BYTES =
            HexEncoding.decode("7d9f11a0da111e9d8bdd14f04648ed91");

    private static final KeyProtection.Builder GOOD_IMPORT_PARAMS_BUILDER =
            new KeyProtection.Builder(
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB,
                            KeyProperties.BLOCK_MODE_CBC,
                            KeyProperties.BLOCK_MODE_CTR,
                            KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setDigests(KeyProperties.DIGEST_NONE)
                    .setRandomizedEncryptionRequired(false);

    public void testAlgorithmList() {
        // Assert that Android Keystore Provider exposes exactly the expected Cipher
        // transformations. We don't care whether the transformations are exposed via aliases, as
        // long as canonical names of transformation are accepted.
        // If the Provider exposes extraneous algorithms, it'll be caught because it'll have to
        // expose at least one Service for such an algorithm, and this Service's algorithm will
        // not be in the expected set.

        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        Set<Service> services = provider.getServices();
        Set<String> actualAlgsLowerCase = new HashSet<String>();
        Set<String> expectedAlgsLowerCase = new HashSet<String>(
                Arrays.asList(TestUtils.toLowerCase(EXPECTED_ALGORITHMS)));
        for (Service service : services) {
            if ("Cipher".equalsIgnoreCase(service.getType())) {
                String algLowerCase = service.getAlgorithm().toLowerCase(Locale.US);
                actualAlgsLowerCase.add(algLowerCase);
            }
        }

        TestUtils.assertContentsInAnyOrder(actualAlgsLowerCase,
                expectedAlgsLowerCase.toArray(new String[0]));
    }

    public void testAndroidKeyStoreKeysHandledByAndroidKeyStoreProviderWhenDecrypting()
            throws Exception {
        Collection<SecretKey> secretKeys = importDefaultKatSecretKeys();
        Collection<KeyPair> keyPairs = importDefaultKatKeyPairs();

        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                // Decryption may need additional parameters. Initializing a Cipher for encryption
                // forces it to generate any such parameters.
                Cipher cipher = Cipher.getInstance(algorithm, provider);
                cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(algorithm, secretKeys, keyPairs));
                AlgorithmParameters params = cipher.getParameters();

                // Test DECRYPT_MODE
                Key key = getDecryptionKey(algorithm, secretKeys, keyPairs);
                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key, params);
                assertSame(provider, cipher.getProvider());

                // Test UNWRAP_MODE
                cipher = Cipher.getInstance(algorithm);
                if (params != null) {
                    cipher.init(Cipher.UNWRAP_MODE, key, params);
                } else {
                    cipher.init(Cipher.UNWRAP_MODE, key);
                }
                assertSame(provider, cipher.getProvider());
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testAndroidKeyStorePublicKeysAcceptedByHighestPriorityProviderWhenEncrypting()
            throws Exception {
        Collection<KeyPair> keyPairs = importDefaultKatKeyPairs();

        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            if (isSymmetric(algorithm)) {
                continue;
            }
            try {
                Key key = getEncryptionKey(algorithm, Collections.<SecretKey>emptyList(), keyPairs);

                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key);

                cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.WRAP_MODE, key);
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testCiphertextGeneratedByAndroidKeyStoreDecryptsByAndroidKeyStore()
            throws Exception {
        Collection<SecretKey> secretKeys = importDefaultKatSecretKeys();
        Collection<KeyPair> keyPairs = importDefaultKatKeyPairs();
        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                Key encryptionKey = getEncryptionKey(algorithm, secretKeys, keyPairs);
                Cipher cipher = Cipher.getInstance(algorithm, provider);
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
                AlgorithmParameters params = cipher.getParameters();
                byte[] expectedPlaintext = "Very secret message goes here...".getBytes("UTF-8");
                byte[] ciphertext = cipher.doFinal(expectedPlaintext);
                if ("RSA/ECB/NoPadding".equalsIgnoreCase(algorithm)) {
                    // RSA decryption without padding left-pads resulting plaintext with NUL bytes
                    // to the length of RSA modulus.
                    int modulusLengthBytes =
                            (((RSAKey) encryptionKey).getModulus().bitLength() + 7) / 8;
                    expectedPlaintext = TestUtils.leftPadWithZeroBytes(
                            expectedPlaintext, modulusLengthBytes);
                }

                cipher = Cipher.getInstance(algorithm, provider);
                Key decryptionKey = getDecryptionKey(algorithm, secretKeys, keyPairs);
                cipher.init(Cipher.DECRYPT_MODE, decryptionKey, params);
                byte[] actualPlaintext = cipher.doFinal(ciphertext);
                MoreAsserts.assertEquals(expectedPlaintext, actualPlaintext);
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testCiphertextGeneratedByHighestPriorityProviderDecryptsByAndroidKeyStore()
            throws Exception {
        Collection<SecretKey> secretKeys = getDefaultKatSecretKeys();
        Collection<SecretKey> keystoreSecretKeys = importDefaultKatSecretKeys();
        Collection<KeyPair> keyPairs = getDefaultKatKeyPairs();
        Collection<KeyPair> keystoreKeyPairs = importDefaultKatKeyPairs();
        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                Key encryptionKey = getEncryptionKey(algorithm, secretKeys, keyPairs);
                Cipher cipher;
                try {
                    cipher = Cipher.getInstance(algorithm);
                    cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
                } catch (InvalidKeyException e) {
                    // No providers support encrypting using this algorithm and key.
                    continue;
                }
                if (provider == cipher.getProvider()) {
                    // This is covered by another test.
                    continue;
                }
                AlgorithmParameters params = cipher.getParameters();

                // TODO: Remove this workaround for Bug 22405492 once the issue is fixed. The issue
                // is that Bouncy Castle incorrectly defaults the MGF1 digest to the digest
                // specified in the transformation. RI and Android Keystore keep the MGF1 digest
                // defaulted at SHA-1.
                if ((params != null) && ("OAEP".equalsIgnoreCase(params.getAlgorithm()))) {
                    OAEPParameterSpec spec = params.getParameterSpec(OAEPParameterSpec.class);
                    if (!"SHA-1".equalsIgnoreCase(
                            ((MGF1ParameterSpec) spec.getMGFParameters()).getDigestAlgorithm())) {
                        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new OAEPParameterSpec(
                                spec.getDigestAlgorithm(),
                                "MGF1",
                                MGF1ParameterSpec.SHA1,
                                PSource.PSpecified.DEFAULT));
                        params = cipher.getParameters();
                    }
                }

                byte[] expectedPlaintext = "Very secret message goes here...".getBytes("UTF-8");
                byte[] ciphertext = cipher.doFinal(expectedPlaintext);
                if ("RSA/ECB/NoPadding".equalsIgnoreCase(algorithm)) {
                    // RSA decryption without padding left-pads resulting plaintext with NUL bytes
                    // to the length of RSA modulus.
                    int modulusLengthBytes =
                            (((RSAKey) encryptionKey).getModulus().bitLength() + 7) / 8;
                    expectedPlaintext = TestUtils.leftPadWithZeroBytes(
                            expectedPlaintext, modulusLengthBytes);
                }

                // TODO: Remove this workaround for Bug 22319986 once the issue is fixed. The issue
                // is that Conscrypt and Bouncy Castle's AES/GCM/NoPadding implementations return
                // AlgorithmParameters of algorithm "AES" from which it's impossible to obtain a
                // GCMParameterSpec. They should be returning AlgorithmParameters of algorithm
                // "GCM".
                if (("AES/GCM/NoPadding".equalsIgnoreCase(algorithm))
                        && (!"GCM".equalsIgnoreCase(params.getAlgorithm()))) {
                    params = AlgorithmParameters.getInstance("GCM");
                    params.init(new GCMParameterSpec(128, cipher.getIV()));
                }

                cipher = Cipher.getInstance(algorithm, provider);
                Key decryptionKey = getDecryptionKey(
                        algorithm, keystoreSecretKeys, keystoreKeyPairs);
                cipher.init(Cipher.DECRYPT_MODE, decryptionKey, params);
                byte[] actualPlaintext = cipher.doFinal(ciphertext);
                MoreAsserts.assertEquals(expectedPlaintext, actualPlaintext);
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testCiphertextGeneratedByAndroidKeyStoreDecryptsByHighestPriorityProvider()
            throws Exception {
        Collection<SecretKey> secretKeys = getDefaultKatSecretKeys();
        Collection<SecretKey> keystoreSecretKeys = importDefaultKatSecretKeys();
        Collection<KeyPair> keyPairs = getDefaultKatKeyPairs();
        Collection<KeyPair> keystoreKeyPairs = importDefaultKatKeyPairs();
        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                Key encryptionKey =
                        getEncryptionKey(algorithm, keystoreSecretKeys, keystoreKeyPairs);
                Cipher cipher = Cipher.getInstance(algorithm, provider);
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
                AlgorithmParameters params = cipher.getParameters();

                byte[] expectedPlaintext = "Very secret message goes here...".getBytes("UTF-8");
                byte[] ciphertext = cipher.doFinal(expectedPlaintext);
                if ("RSA/ECB/NoPadding".equalsIgnoreCase(algorithm)) {
                    // RSA decryption without padding left-pads resulting plaintext with NUL bytes
                    // to the length of RSA modulus.
                    int modulusLengthBytes =
                            (((RSAKey) encryptionKey).getModulus().bitLength() + 7) / 8;
                    expectedPlaintext = TestUtils.leftPadWithZeroBytes(
                            expectedPlaintext, modulusLengthBytes);
                }

                Key decryptionKey = getDecryptionKey(algorithm, secretKeys, keyPairs);
                try {
                    cipher = Cipher.getInstance(algorithm);
                    if (params != null) {
                        cipher.init(Cipher.DECRYPT_MODE, decryptionKey, params);
                    } else {
                        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
                    }
                } catch (InvalidKeyException e) {
                    // No providers support decrypting using this algorithm and key.
                    continue;
                }
                if (provider == cipher.getProvider()) {
                    // This is covered by another test.
                    continue;
                }
                byte[] actualPlaintext = cipher.doFinal(ciphertext);
                MoreAsserts.assertEquals(expectedPlaintext, actualPlaintext);
            } catch (Throwable e) {
                throw new RuntimeException(algorithm + " failed", e);
            }
        }
    }

    public void testKat() throws Exception {
        Collection<SecretKey> secretKeys = importDefaultKatSecretKeys();
        Collection<KeyPair> keyPairs = importDefaultKatKeyPairs();

        Provider provider = Security.getProvider(EXPECTED_PROVIDER_NAME);
        assertNotNull(provider);
        for (String algorithm : EXPECTED_ALGORITHMS) {
            try {
                Key key = getDecryptionKey(algorithm, secretKeys, keyPairs);
                KatVector testVector = KAT_VECTORS.get(algorithm);
                assertNotNull(testVector);
                Cipher cipher = Cipher.getInstance(algorithm, provider);
                cipher.init(Cipher.DECRYPT_MODE, key, testVector.params);
                byte[] actualPlaintext = cipher.doFinal(testVector.ciphertext);
                byte[] expectedPlaintext = testVector.plaintext;
                if ("RSA/ECB/NoPadding".equalsIgnoreCase(algorithm)) {
                    // RSA decryption without padding left-pads resulting plaintext with NUL bytes
                    // to the length of RSA modulus.
                    int modulusLengthBytes =
                            (((RSAKey) key).getModulus().bitLength() + 7) / 8;
                    expectedPlaintext = TestUtils.leftPadWithZeroBytes(
                            expectedPlaintext, modulusLengthBytes);
                }
                MoreAsserts.assertEquals(expectedPlaintext, actualPlaintext);
                if (!isRandomizedEncryption(algorithm)) {
                    // Deterministic encryption: ciphertext depends only on plaintext and input
                    // parameters. Assert that encrypting the plaintext results in the same
                    // ciphertext as in the test vector.
                    key = getEncryptionKey(algorithm, secretKeys, keyPairs);
                    cipher = Cipher.getInstance(algorithm, provider);
                    cipher.init(Cipher.ENCRYPT_MODE, key, testVector.params);
                    byte[] actualCiphertext = cipher.doFinal(testVector.plaintext);
                    MoreAsserts.assertEquals(testVector.ciphertext, actualCiphertext);
                }
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + algorithm, e);
            }
        }
    }

    private static boolean isRandomizedEncryption(String transformation) {
        String transformationUpperCase = transformation.toUpperCase(Locale.US);
        return (transformationUpperCase.endsWith("/PKCS1PADDING"))
                || (transformationUpperCase.contains("OAEP"));
    }

    public void testInitDecryptFailsWhenNotAuthorizedToDecrypt() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good, KeyProperties.PURPOSE_ENCRYPT).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptSymmetricFailsWhenNotAuthorizedToEncrypt() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }

            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good, KeyProperties.PURPOSE_DECRYPT).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptAsymmetricIgnoresAuthorizedPurposes() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (isSymmetric(transformation)) {
                continue;
            }

            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good, 0).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitDecryptFailsWhenBlockModeNotAuthorized() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            String transformationUpperCase = transformation.toUpperCase(Locale.US);
            if (transformationUpperCase.startsWith("RSA/")) {
                // Block modes do not apply
                continue;
            }
            String authorizedBlockMode =
                    (transformationUpperCase.contains("/CBC/")) ? "CTR" : "CBC";
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good).setBlockModes(authorizedBlockMode).build());
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Failed for " + transformation + " when authorized only for "
                                + authorizedBlockMode,
                        e);
            }
        }
    }

    public void testInitEncryptSymmetricFailsWhenBlockModeNotAuthorized() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }

            String transformationUpperCase = transformation.toUpperCase(Locale.US);
            if (transformationUpperCase.startsWith("RSA/")) {
                // Block modes do not apply
                continue;
            }
            String authorizedBlockMode =
                    (transformationUpperCase.contains("/CBC/")) ? "CTR" : "CBC";
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good).setBlockModes(authorizedBlockMode).build());
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Failed for " + transformation + " when authorized only for "
                                + authorizedBlockMode,
                        e);
            }
        }
    }

    public void testInitEncryptAsymmetricIgnoresAuthorizedBlockModes() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (isSymmetric(transformation)) {
                continue;
            }

            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good).setBlockModes().build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitDecryptFailsWhenDigestNotAuthorized() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            String transformationUpperCase = transformation.toUpperCase(Locale.US);
            if ((transformationUpperCase.endsWith("/NOPADDING"))
                    || (transformationUpperCase.endsWith("/PKCS1PADDING"))
                    || (transformationUpperCase.endsWith("/PKCS7PADDING"))) {
                // Digest not used
                continue;
            }
            String authorizedDigest =
                    (transformationUpperCase.contains("SHA-256")) ? "SHA-512" : "SHA-256";
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good).setDigests(authorizedDigest).build());
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Failed for " + transformation + " when authorized only for "
                                + authorizedDigest,
                        e);
            }
        }
    }

    public void testInitEncryptSymmetricFailsWhenDigestNotAuthorized() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }
            String transformationUpperCase = transformation.toUpperCase(Locale.US);
            if ((transformationUpperCase.endsWith("/NOPADDING"))
                    || (transformationUpperCase.endsWith("/PKCS1PADDING"))
                    || (transformationUpperCase.endsWith("/PKCS7PADDING"))) {
                // Digest not used
                continue;
            }
            String authorizedDigest =
                    (transformationUpperCase.contains("SHA-256")) ? "SHA-512" : "SHA-256";
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good).setDigests(authorizedDigest).build());
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Failed for " + transformation + " when authorized only for "
                                + authorizedDigest,
                        e);
            }
        }
    }

    public void testInitEncryptAsymmetricIgnoresAuthorizedDigests() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good).setDigests().build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitDecryptFailsWhenPaddingSchemeNotAuthorized() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            String transformationUpperCase = transformation.toUpperCase(Locale.US);
            String authorizedPaddingScheme;
            if (transformationUpperCase.startsWith("RSA/")) {
                authorizedPaddingScheme = transformationUpperCase.contains("PKCS1PADDING")
                        ? "OAEPPadding" : "PKCS1Padding";
            } else {
                authorizedPaddingScheme = transformationUpperCase.contains("PKCS1PADDING")
                        ? "NoPadding" : "PKCS1Padding";
            }
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good)
                                .setEncryptionPaddings(authorizedPaddingScheme)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Failed for " + transformation + " when authorized only for "
                                + authorizedPaddingScheme,
                        e);
            }
        }
    }

    public void testInitEncryptSymmetricFailsWhenPaddingSchemeNotAuthorized() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }
            String transformationUpperCase = transformation.toUpperCase(Locale.US);
            String authorizedPaddingScheme;
            if (transformationUpperCase.startsWith("RSA/")) {
                authorizedPaddingScheme = transformationUpperCase.contains("PKCS1PADDING")
                        ? "OAEPPadding" : "PKCS1Padding";
            } else {
                authorizedPaddingScheme = transformationUpperCase.contains("PKCS1PADDING")
                        ? "NoPadding" : "PKCS1Padding";
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good)
                                .setEncryptionPaddings(authorizedPaddingScheme)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Failed for " + transformation + " when authorized only for "
                                + authorizedPaddingScheme,
                        e);
            }
        }
    }

    public void testInitEncryptAsymmetricIgnoresAuthorizedPaddingSchemes() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good)
                                .setEncryptionPaddings()
                                .setSignaturePaddings()
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitDecryptFailsWhenKeyNotYetValid() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badStartDate = new Date(System.currentTimeMillis() + DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good).setKeyValidityStart(badStartDate).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptSymmetricFailsWhenKeyNotYetValid() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badStartDate = new Date(System.currentTimeMillis() + DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good).setKeyValidityStart(badStartDate).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptAsymmetricIgnoresThatKeyNotYetValid() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badStartDate = new Date(System.currentTimeMillis() + DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good).setKeyValidityStart(badStartDate).build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitDecryptFailsWhenKeyNoLongerValidForConsumption() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForConsumptionEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitDecryptIgnoresThatKeyNoLongerValidForOrigination() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            try {
                assertInitDecryptSucceeds(transformation, good.build());
                assertInitDecryptSucceeds(transformation,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForOriginationEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptSymmetricFailsWhenKeyNoLongerValidForOrigination() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptThrowsInvalidKeyException(transformation,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForOriginationEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptSymmetricIgnoresThatKeyNoLongerValidForConsumption()
            throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (!isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForConsumptionEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    public void testInitEncryptAsymmetricIgnoresThatKeyNoLongerValid() throws Exception {
        KeyProtection.Builder good = GOOD_IMPORT_PARAMS_BUILDER;
        Date badEndDate = new Date(System.currentTimeMillis() - DAY_IN_MILLIS);
        for (String transformation : EXPECTED_ALGORITHMS) {
            if (isSymmetric(transformation)) {
                continue;
            }
            try {
                assertInitEncryptSucceeds(transformation, good.build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForOriginationEnd(badEndDate)
                                .build());
                assertInitEncryptSucceeds(transformation,
                        TestUtils.buildUpon(good)
                                .setKeyValidityForConsumptionEnd(badEndDate)
                                .build());
            } catch (Throwable e) {
                throw new RuntimeException("Failed for " + transformation, e);
            }
        }
    }

    private AlgorithmParameterSpec getWorkingDecryptionParameterSpec(String transformation) {
        String transformationUpperCase = transformation.toUpperCase();
        if (transformationUpperCase.startsWith("RSA/")) {
            return null;
        } else if (transformationUpperCase.startsWith("AES/")) {
            if (transformationUpperCase.startsWith("AES/ECB")) {
                return null;
            } else if ((transformationUpperCase.startsWith("AES/CBC"))
                    || (transformationUpperCase.startsWith("AES/CTR"))) {
                return new IvParameterSpec(
                        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});
            } else if (transformationUpperCase.startsWith("AES/GCM")) {
                return new GCMParameterSpec(
                        128,
                        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
            }
        }

        throw new IllegalArgumentException("Unsupported transformation: " + transformation);
    }

    private void assertInitDecryptSucceeds(String transformation, KeyProtection importParams)
            throws Exception {
        Cipher cipher = Cipher.getInstance(transformation, EXPECTED_PROVIDER_NAME);
        Key key = importDefaultKatDecryptionKey(transformation, importParams);
        AlgorithmParameterSpec params = getWorkingDecryptionParameterSpec(transformation);
        cipher.init(Cipher.DECRYPT_MODE, key, params);
    }

    private void assertInitDecryptThrowsInvalidKeyException(
            String transformation, KeyProtection importParams) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation, EXPECTED_PROVIDER_NAME);
        Key key = importDefaultKatDecryptionKey(transformation, importParams);
        AlgorithmParameterSpec params = getWorkingDecryptionParameterSpec(transformation);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            fail("InvalidKeyException should have been thrown");
        } catch (InvalidKeyException expected) {}
    }

    private void assertInitEncryptSucceeds(String transformation, KeyProtection importParams)
            throws Exception {
        Cipher cipher = Cipher.getInstance(transformation, EXPECTED_PROVIDER_NAME);
        Key key = importDefaultKatEncryptionKey(transformation, importParams);
        cipher.init(Cipher.ENCRYPT_MODE, key);
    }

    private void assertInitEncryptThrowsInvalidKeyException(
            String transformation, KeyProtection importParams) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation, EXPECTED_PROVIDER_NAME);
        Key key = importDefaultKatEncryptionKey(transformation, importParams);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            fail("InvalidKeyException should have been thrown");
        } catch (InvalidKeyException expected) {}
    }

    private Key importDefaultKatEncryptionKey(String transformation,
            KeyProtection importParams) throws Exception {
        String transformationUpperCase = transformation.toUpperCase();
        if (transformationUpperCase.startsWith("RSA/")) {
            return TestUtils.importIntoAndroidKeyStore("testRsa",
                    TestUtils.getRawResPrivateKey(getContext(), R.raw.rsa_key2_pkcs8),
                    TestUtils.getRawResX509Certificate(getContext(), R.raw.rsa_key2_cert),
                    importParams).getPublic();
        } else if (transformationUpperCase.startsWith("AES/")) {
            return TestUtils.importIntoAndroidKeyStore("testAes",
                    new SecretKeySpec(AES_KAT_KEY_BYTES, "AES"),
                    importParams);
        } else {
            throw new IllegalArgumentException("Unsupported transformation: " + transformation);
        }
    }

    private Key importDefaultKatDecryptionKey(String transformation,
            KeyProtection importParams) throws Exception {
        String transformationUpperCase = transformation.toUpperCase();
        if (transformationUpperCase.startsWith("RSA/")) {
            return TestUtils.importIntoAndroidKeyStore("testRsa",
                    TestUtils.getRawResPrivateKey(getContext(), R.raw.rsa_key2_pkcs8),
                    TestUtils.getRawResX509Certificate(getContext(), R.raw.rsa_key2_cert),
                    importParams).getPrivate();
        } else if (transformationUpperCase.startsWith("AES/")) {
            return TestUtils.importIntoAndroidKeyStore("testAes",
                    new SecretKeySpec(AES_KAT_KEY_BYTES, "AES"),
                    importParams);
        } else {
            throw new IllegalArgumentException("Unsupported transformation: " + transformation);
        }
    }

    private static Key getEncryptionKey(String transformation,
            Iterable<SecretKey> secretKeys,
            Iterable<KeyPair> keyPairs) {
        String transformationUpperCase = transformation.toUpperCase();
        if (transformationUpperCase.startsWith("RSA/")) {
            return TestUtils.getKeyPairForKeyAlgorithm("RSA", keyPairs).getPublic();
        } else if (transformationUpperCase.startsWith("AES/")) {
            return TestUtils.getKeyForKeyAlgorithm("AES", secretKeys);
        } else {
            throw new IllegalArgumentException("Unsupported transformation: " + transformation);
        }
    }

    private static Key getDecryptionKey(String transformation,
            Iterable<SecretKey> secretKeys,
            Iterable<KeyPair> keyPairs) {
        String transformationUpperCase = transformation.toUpperCase();
        if (transformationUpperCase.startsWith("RSA/")) {
            return TestUtils.getKeyPairForKeyAlgorithm("RSA", keyPairs).getPrivate();
        } else if (transformationUpperCase.startsWith("AES/")) {
            return TestUtils.getKeyForKeyAlgorithm("AES", secretKeys);
        } else {
            throw new IllegalArgumentException("Unsupported transformation: " + transformation);
        }
    }

    private Collection<KeyPair> getDefaultKatKeyPairs() throws Exception {
        return Arrays.asList(
                new KeyPair(
                        TestUtils.getRawResX509Certificate(
                                getContext(), R.raw.rsa_key2_cert).getPublicKey(),
                        TestUtils.getRawResPrivateKey(getContext(), R.raw.rsa_key2_pkcs8)));
    }

    private Collection<KeyPair> importDefaultKatKeyPairs() throws Exception {
        return Arrays.asList(
                TestUtils.importIntoAndroidKeyStore(
                        "testRsa",
                        TestUtils.getRawResPrivateKey(getContext(), R.raw.rsa_key2_pkcs8),
                        TestUtils.getRawResX509Certificate(getContext(), R.raw.rsa_key2_cert),
                        new KeyProtection.Builder(
                                KeyProperties.PURPOSE_DECRYPT)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false) // due to PADDING_NONE
                                .setDigests(KeyProperties.DIGEST_NONE) // due to OAEP
                                .build()));
    }

    private Collection<SecretKey> getDefaultKatSecretKeys() throws Exception {
        return Arrays.asList((SecretKey) new SecretKeySpec(AES_KAT_KEY_BYTES, "AES"));
    }

    private Collection<SecretKey> importDefaultKatSecretKeys() throws Exception {
        return Arrays.asList(
                TestUtils.importIntoAndroidKeyStore("testAes",
                        new SecretKeySpec(AES_KAT_KEY_BYTES, "AES"),
                        new KeyProtection.Builder(
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_ECB,
                                        KeyProperties.BLOCK_MODE_CBC,
                                        KeyProperties.BLOCK_MODE_CTR,
                                        KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false) // due to ECB
                                .build()));
    }

    private static boolean isSymmetric(String transformation) {
        return transformation.toUpperCase(Locale.US).startsWith("AES/");
    }
}
