/*
 *  Copyright (C) 2016 Alessandro Yuichi Okimoto
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  Contact email: alessandro@alessandro.jp
 */

package jp.alessandro.android.iab;

import android.content.Context;

import jp.alessandro.android.iab.logger.DiscardLogger;
import jp.alessandro.android.iab.logger.Logger;

public class BillingContext {

    private final Context mContext;
    private final String mPublicKeyBase64;
    private final BillingApi mApiVersion;
    private final Logger mLogger;

    /**
     * Context that contains all information to execute the library
     *
     * @param context         application context
     * @param publicKeyBase64 rsa public key generated by Google Play Developer Console
     * @param apiVersion      google api version (The library supports version 3 & 5)
     */
    public BillingContext(Context context,
                          String publicKeyBase64,
                          BillingApi apiVersion) {
        this(context, publicKeyBase64, apiVersion, new DiscardLogger());
    }

    /**
     * Context that contains all information to execute the library
     *
     * @param context         application context
     * @param publicKeyBase64 rsa public key generated by Google Play Developer Console
     * @param apiVersion      google api version (The library supports version 3 & 5)
     * @param logger          interface to print the library's log
     */
    public BillingContext(Context context,
                          String publicKeyBase64,
                          BillingApi apiVersion,
                          Logger logger) {
        mContext = context;
        mPublicKeyBase64 = publicKeyBase64;
        mApiVersion = apiVersion;
        mLogger = logger;
    }

    Context getContext() {
        return mContext;
    }

    String getPublicKeyBase64() {
        return mPublicKeyBase64;
    }

    int getApiVersion() {
        return mApiVersion.getValue();
    }

    Logger getLogger() {
        return mLogger;
    }
}