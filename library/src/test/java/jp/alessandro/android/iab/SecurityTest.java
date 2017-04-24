/*
 * Copyright (C) 2016 Alessandro Yuichi Okimoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact email: alessandro@alessandro.jp
 */

package jp.alessandro.android.iab;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import jp.alessandro.android.iab.logger.DiscardLogger;
import jp.alessandro.android.iab.logger.Logger;
import jp.alessandro.android.iab.util.DataConverter;
import jp.alessandro.android.iab.util.DataSigner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by Alessandro Yuichi Okimoto on 2017/02/26.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, constants = BuildConfig.class)
public class SecurityTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final DataSigner mDataSigner = new DataSigner();

    private List<Security> mSecurities;

    @Before
    public void setUp() {
        mSecurities = new ArrayList<>();
        mSecurities.add(spy(new Security(false)));
        mSecurities.add(spy(new Security(true)));
    }

    @Test
    public void verifyPurchaseSuccess() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = DataConverter.TEST_PUBLIC_KEY_BASE_64;
        String signedData = DataConverter.TEST_JSON_RECEIPT;
        String signature = mDataSigner.sign(signedData, Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isTrue();
        }
    }

    @Test
    public void verifyPurchaseFailed() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = DataConverter.TEST_PUBLIC_KEY_BASE_64;
        String signedData = DataConverter.TEST_JSON_RECEIPT;
        String signedDifferentData = DataConverter.TEST_JSON_RECEIPT_AUTO_RENEWING_FALSE;
        String signature = mDataSigner.sign(signedDifferentData, Security.KEY_FACTORY_ALGORITHM, Security.SIGNATURE_ALGORITHM);

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseBase64PublicKeyEmpty() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "";
        String signedData = "signedData";
        String signature = "signature";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponseSignedDataEmpty() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "";
        String signature = "signature";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseSignedDataAndSignatureEmpty() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponseJsonBroken() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "\"{\"test\"}\"";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponseNotMatch() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.test\"}";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
        }
    }

    @Test
    public void verifyPurchaseStaticResponsePurchased() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.purchased\"}";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isEqualTo(s.isDebug());
        }
    }

    @Test
    public void verifyPurchaseStaticResponseCanceled() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.canceled\"}";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isEqualTo(s.isDebug());
        }
    }

    @Test
    public void verifyPurchaseStaticResponseRefunded() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.refunded\"}";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isEqualTo(s.isDebug());
        }
    }

    @Test
    public void verifyPurchaseStaticResponseItemUnavailable() {
        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "{\"productId\": \"android.test.item_unavailable\"}";
        String signature = "";

        for (Security s : mSecurities) {
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isEqualTo(s.isDebug());
        }
    }

    @Test
    public void generatePublicKeyNoSuchAlgorithmException()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {

        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "signature";

        for (Security s : mSecurities) {
            doThrow(new NoSuchAlgorithmException()).when(s).generatePublicKey(base64PublicKey);
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).generatePublicKey(base64PublicKey);
        }
    }

    @Test
    public void generatePublicKeyInvalidKeySpecException()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {

        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "signature";

        for (Security s : mSecurities) {
            doThrow(new InvalidKeySpecException()).when(s).generatePublicKey(base64PublicKey);
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).generatePublicKey(base64PublicKey);
        }
    }

    @Test
    public void generatePublicKeyIllegalArgumentException()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalArgumentException {

        Logger logger = new DiscardLogger();
        String base64PublicKey = "base64PublicKey";
        String signedData = "signedData";
        String signature = "signature";

        for (Security s : mSecurities) {
            doThrow(new IllegalArgumentException()).when(s).generatePublicKey(base64PublicKey);
            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).generatePublicKey(base64PublicKey);
        }
    }

    @Test
    public void verifyUnsupportedEncodingException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        for (Security s : mSecurities) {
            doReturn(publicKey).when(s).generatePublicKey(anyString());
            doThrow(new UnsupportedEncodingException()).when(s).verify(logger, publicKey, signedData, signature);

            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).verify(logger, publicKey, signedData, signature);
        }

    }

    @Test
    public void verifyNoSuchAlgorithmException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        for (Security s : mSecurities) {
            doReturn(publicKey).when(s).generatePublicKey(anyString());
            doThrow(new NoSuchAlgorithmException()).when(s).verify(logger, publicKey, signedData, signature);

            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).verify(logger, publicKey, signedData, signature);
        }
    }

    @Test
    public void verifyInvalidKeyException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        for (Security s : mSecurities) {
            doReturn(publicKey).when(s).generatePublicKey(anyString());
            doThrow(new InvalidKeyException()).when(s).verify(logger, publicKey, signedData, signature);

            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).verify(logger, publicKey, signedData, signature);
        }
    }

    @Test
    public void verifyInvalidKeySpecException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        for (Security s : mSecurities) {
            doReturn(publicKey).when(s).generatePublicKey(anyString());
            doThrow(new InvalidKeySpecException()).when(s).verify(logger, publicKey, signedData, signature);

            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).verify(logger, publicKey, signedData, signature);
        }
    }

    @Test
    public void verifySignatureException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        for (Security s : mSecurities) {
            doReturn(publicKey).when(s).generatePublicKey(anyString());
            doThrow(new SignatureException()).when(s).verify(logger, publicKey, signedData, signature);

            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).verify(logger, publicKey, signedData, signature);
        }
    }

    @Test
    public void verifyIllegalArgumentException() throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidKeySpecException,
            SignatureException,
            IllegalArgumentException {

        Logger logger = new DiscardLogger();
        PublicKey publicKey = mock(PublicKey.class);
        String signedData = "signedData";
        String signature = "signature";
        String base64PublicKey = "base64PublicKey";

        for (Security s : mSecurities) {
            doReturn(publicKey).when(s).generatePublicKey(anyString());
            doThrow(new IllegalArgumentException()).when(s).verify(logger, publicKey, signedData, signature);

            assertThat(s.verifyPurchase(logger, base64PublicKey, signedData, signature)).isFalse();
            verify(s).verify(logger, publicKey, signedData, signature);
        }
    }
}