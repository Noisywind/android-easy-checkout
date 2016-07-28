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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.List;

import jp.alessandro.android.iab.handler.PurchaseHandler;
import jp.alessandro.android.iab.logger.Logger;

class PurchaseLauncher {

    private final String mItemType;
    private final String mSignatureBase64;
    private final int mApiVersion;
    private final String mPackageName;
    private final Logger mLogger;

    private static PurchaseLaunchState sState = new PurchaseLaunchState();

    PurchaseLauncher(BillingContext context, String itemType) {
        mItemType = itemType;
        mSignatureBase64 = context.getSignatureBase64();
        mApiVersion = context.getApiVersion();
        mPackageName = context.getContext().getPackageName();
        mLogger = context.getLogger();
    }

    public void launch(IInAppBillingService service, final Activity activity,
                       final List<String> oldItemIds,
                       final String itemId,
                       final String developerPayload,
                       final PurchaseHandler handler) throws BillingException {
        Bundle bundle = getBuyIntent(service, oldItemIds, itemId, developerPayload);
        PendingIntent intent = getPendingIntent(activity, bundle);
        startBuyIntent(activity, intent, handler);
    }

    private Bundle getBuyIntent(IInAppBillingService service, List<String> oldItemIds,
                                String itemId, String developerPayload) throws BillingException {
        try {
            // Purchase an item
            if (oldItemIds == null || oldItemIds.isEmpty()) {
                return service.getBuyIntent(mApiVersion,
                        mPackageName, itemId, mItemType, developerPayload);
            }
            // Upgrade/downgrade of subscriptions must be done on api version 5
            // See https://developer.android.com/google/play/billing/billing_reference.html#upgrade-getBuyIntentToReplaceSkus
            return service.getBuyIntentToReplaceSkus(BillingApi.VERSION_5.getValue(),
                    mPackageName, oldItemIds, itemId, mItemType, developerPayload);
        } catch (RemoteException e) {
            throw new BillingException(Constants.ERROR_REMOTE_EXCEPTION, e.getMessage());
        }
    }

    private PendingIntent getPendingIntent(Activity activity, Bundle bundle) throws BillingException {
        int response = getResponseCodeFromBundle(bundle);
        if (response != Constants.BILLING_RESPONSE_RESULT_OK) {
            throw new BillingException(response, Constants.ERROR_MSG_UNABLE_TO_BUY);
        }
        if (activity == null) {
            throw new BillingException(Constants.ERROR_LOST_CONTEXT,
                    Constants.ERROR_MSG_LOST_CONTEXT);
        }
        PendingIntent pendingIntent = bundle.getParcelable(Constants.RESPONSE_BUY_INTENT);
        if (pendingIntent == null) {
            throw new BillingException(Constants.ERROR_PENDING_INTENT,
                    Constants.ERROR_MSG_PENDING_INTENT);
        }
        return pendingIntent;
    }

    private void startBuyIntent(final Activity activity, final PendingIntent pendingIntent,
                                PurchaseHandler handler) throws BillingException {
        if (!sState.tryLock()) {
            throw new BillingException(Constants.ERROR_IS_ALREADY_LAUNCHING,
                    Constants.ERROR_MSG_IS_ALREADY_LAUNCHING);
        }
        sState.setHandler(handler);
        try {
            int requestCode = mItemType.equals(Constants.ITEM_TYPE_INAPP)
                    ? Constants.CONSUMABLE_REQUEST_CODE
                    : Constants.SUBS_REQUEST_CODE;

            activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    requestCode, new Intent(), 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            sState.tryUnlock();
            throw new BillingException(Constants.ERROR_SEND_INTENT_FAILED, e.getMessage());
        }
    }

    // ******************** CHECK BILLING ACTIVITY RESULT ******************** //

    public PurchaseLaunchState tryUnlock() {
        return sState.tryUnlock();
    }

    public Purchase handleResult(int resultCode, Intent data) throws BillingException {
        if (data == null) {
            throw new BillingException(Constants.ERROR_BAD_RESPONSE,
                    Constants.ERROR_MSG_RESULT_NULL_INTENT);
        }
        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(Constants.RESPONSE_INAPP_PURCHASE_DATA);
        String signature = data.getStringExtra(Constants.RESPONSE_INAPP_SIGNATURE);
        return checkResultCode(resultCode, responseCode, purchaseData, signature, data);
    }

    private Purchase checkResultCode(int resultCode, int responseCode,
                                     String purchaseData, String signature,
                                     Intent data) throws BillingException {
        // Check the Billing response
        if ((resultCode == Activity.RESULT_OK)
                && (responseCode == Constants.BILLING_RESPONSE_RESULT_OK)) {
            return checkBillingResponse(purchaseData, signature, data);
        }
        // Something happened while trying to purchase the item
        switch (resultCode) {
            case Activity.RESULT_OK:
                throw new BillingException(responseCode, Constants.ERROR_MSG_RESULT_OK);

            case Activity.RESULT_CANCELED:
                throw new BillingException(responseCode, Constants.ERROR_MSG_RESULT_CANCELED);

            default:
                throw new BillingException(resultCode, Constants.ERROR_MSG_RESULT_UNKNOWN);
        }
    }

    private Purchase checkBillingResponse(String purchaseData, String signature
            , Intent data) throws BillingException {
        displayBillingResponse(purchaseData, signature, data);
        verifyPurchaseData(purchaseData, signature);
        return parsePurchaseData(purchaseData, signature);
    }

    private void verifyPurchaseData(String purchaseData, String signature) throws BillingException {
        if (purchaseData == null || signature == null) {
            throw new BillingException(Constants.ERROR_PURCHASE_DATA,
                    Constants.ERROR_MSG_NULL_PURCHASE_DATA);
        }
        if (!Security.verifyPurchase(purchaseData, mLogger, mSignatureBase64, purchaseData, signature)) {
            throw new BillingException(Constants.ERROR_VERIFICATION_FAILED,
                    Constants.ERROR_MSG_VERIFICATION_FAILED);
        }
    }

    private Purchase parsePurchaseData(String purchaseInfo, String signature) throws BillingException {
        try {
            return Purchase.parseJson(purchaseInfo, signature);
        } catch (JSONException e) {
            throw new BillingException(Constants.ERROR_BAD_RESPONSE,
                    Constants.ERROR_MSG_BAD_RESPONSE);
        }
    }

    private void displayBillingResponse(String purchaseData, String dataSignature, Intent data) {
        mLogger.i(Logger.TAG, "------------- BILLING RESPONSE start -------------");
        mLogger.i(Logger.TAG, "Successful resultCode from purchase activity.");
        mLogger.i(Logger.TAG, String.format("Purchase data: %s", purchaseData));
        mLogger.i(Logger.TAG, String.format("Data signature: %s", dataSignature));
        mLogger.i(Logger.TAG, String.format("Extras: %s",
                (data.getExtras() != null ? data.getExtras().toString() : "")));
        mLogger.i(Logger.TAG, "------------- BILLING RESPONSE end -------------");
    }

    /**
     * Workaround to bug where sometimes response codes come as Long instead of Integer
     */
    private int getResponseCodeFromIntent(Intent intent) throws BillingException {
        Object obj = intent.getExtras().get(Constants.RESPONSE_CODE);
        if (obj == null) {
            mLogger.e(Logger.TAG,
                    "Intent with no response code, assuming there is no problem (known issue).");
            return Constants.BILLING_RESPONSE_RESULT_OK;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).intValue();
        }
        if (obj instanceof Long) {
            return (int) ((Long) obj).longValue();
        }
        mLogger.e(Logger.TAG, "Unexpected type for intent response code.");
        throw new BillingException(Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_UNEXPECTED_INTENT_RESPONSE);
    }

    /**
     * Workaround to bug where sometimes response codes come as Long instead of Integer
     */
    private int getResponseCodeFromBundle(Bundle bundle) throws BillingException {
        Object obj = bundle.get(Constants.RESPONSE_CODE);
        if (obj == null) {
            mLogger.e(Logger.TAG,
                    "Bundle with null response code, assuming there is no problem (known issue).");
            return Constants.BILLING_RESPONSE_RESULT_OK;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).intValue();
        }
        if (obj instanceof Long) {
            return (int) ((Long) obj).longValue();
        }
        mLogger.e(Logger.TAG, "Unexpected type for bundle response.");
        throw new BillingException(Constants.ERROR_UNEXPECTED_TYPE,
                Constants.ERROR_MSG_UNEXPECTED_BUNDLE_RESPONSE);
    }
}