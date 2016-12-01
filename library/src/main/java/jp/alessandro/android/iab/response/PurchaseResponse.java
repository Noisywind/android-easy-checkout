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

package jp.alessandro.android.iab.response;

import jp.alessandro.android.iab.BillingException;
import jp.alessandro.android.iab.Purchase;

/**
 * Created by Alessandro Yuichi Okimoto on 2016/11/22.
 */

public class PurchaseResponse {

    private final Purchase mPurchase;
    private final BillingException mException;
    private final boolean mIsSuccess;

    public PurchaseResponse(Purchase purchase, BillingException exception) {
        mPurchase = purchase;
        mException = exception;
        mIsSuccess = mException == null;
    }

    public Purchase getPurchase() {
        return mPurchase;
    }

    public BillingException getException() {
        return mException;
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }
}