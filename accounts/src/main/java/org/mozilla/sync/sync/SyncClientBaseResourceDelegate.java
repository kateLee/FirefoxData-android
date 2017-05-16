/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.sync.impl.FirefoxAccountShared;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException.FailureReason;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.ResourceDelegate;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.util.FileUtil;
import org.mozilla.util.IOUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for requests made by {@see org.mozilla.accounts.sync.FirefoxAccountSyncClient}:
 * provides basic configuration and simplifies the error/response handling.
 */
abstract class SyncClientBaseResourceDelegate<T> implements ResourceDelegate {
    protected static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private static final int connectionTimeoutInMillis = 1000 * 30; // Wait 30s for a connection to open.
    private static final int socketTimeoutInMillis = 1000 * 2 * 60; // Wait 2 minutes for data.

    /** The sync config associated with the request. */
    protected final FirefoxAccountSyncConfig syncConfig;
    protected final IOUtil.OnAsyncCallComplete<SyncCollectionResult<T>> onComplete;

    SyncClientBaseResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<SyncCollectionResult<T>> onComplete) {
        this.syncConfig = syncConfig;
        this.onComplete = onComplete;
    }

    public abstract void handleResponse(final HttpResponse response, final String responseBody);

    @Override
    public final void handleHttpResponse(final HttpResponse response) {
        final String responseBody;
        try {
            responseBody = FileUtil.readStringFromInputStreamAndCloseStream(response.getEntity().getContent(), 4096);
        } catch (final IOException e) {
            onComplete.onException(new FirefoxSyncGetCollectionException(e, FailureReason.SERVER_RESPONSE_UNEXPECTED));
            return;
        }
        handleResponse(response, responseBody);
    }

    private void handleException(final Throwable cause) {
        onComplete.onException(new FirefoxSyncGetCollectionException(cause, FailureReason.NETWORK_ERROR));
    }

    @Override public String getUserAgent() { return null; } // TODO: decide if necessary.

    @Override public void handleHttpProtocolException(final ClientProtocolException e) { handleException(e); }
    @Override public void handleHttpIOException(final IOException e) { handleException(e); }
    @Override public void handleTransportException(final GeneralSecurityException e) {
        // An error occurred in the request preparation - I wonder if there's a more useful FailureReason.
        handleException(e);
    }

    @Override public int connectionTimeout() { return connectionTimeoutInMillis; }
    @Override public int socketTimeout() { return socketTimeoutInMillis; }
    @Override public AuthHeaderProvider getAuthHeaderProvider() {
        try {
            return FirefoxSyncRequestUtils.getAuthHeaderProvider(syncConfig.token);
        } catch (final UnsupportedEncodingException | URISyntaxException e) {
            // Since we don't have the auth header, we can expect this request to fail on unauthorized. However,
            // we can't cancel the request here so we return null to go through with it anyway, and we handle it
            // when the request fails.
            Log.e(LOGTAG, "getAuthHeaderProvider: unable to get auth header."); // Don't log e to avoid leaking user data.
            return null;
        }
    }

    @Override public void addHeaders(HttpRequestBase request, DefaultHttpClient client) { }

    /** Convenience function to turn a request's response body into a list of records of the parametrized type. */
    protected static <R> List<R> responseBodyToRawRecords(final FirefoxAccountSyncConfig syncConfig, final String responseBody,
            final String collectionName, final RecordFactory recordFactory) throws NoCollectionKeysSetException, JSONException {
        final KeyBundle keyBundle = syncConfig.collectionKeys.keyBundleForCollection(collectionName);
        final JSONArray recordArray = new JSONArray(responseBody);

        final ArrayList<R> receivedRecords = new ArrayList<>(recordArray.length());
        for (int i = 0; i < recordArray.length(); ++i) {
            try {
                final JSONObject jsonRecord = recordArray.getJSONObject(i);
                final R record = getAndDecryptRecord(recordFactory, keyBundle, jsonRecord);
                receivedRecords.add(record);
            } catch (final IOException | JSONException | NonObjectJSONException | CryptoException e) {
                Log.w(LOGTAG, "Unable to decrypt record"); // Let's not log exception to avoid leaking user data.
            }
        }
        return receivedRecords;
    }

    private static <R> R getAndDecryptRecord(final RecordFactory recordFactory, final KeyBundle keyBundle,
            final JSONObject json) throws NonObjectJSONException, IOException, CryptoException, JSONException {
        final Record recordToWrap = new HistoryRecord(json.getString("id")); // Not the most correct but this can be any record since we just init id.
        final CryptoRecord cryptoRecord = new CryptoRecord(recordToWrap);
        cryptoRecord.payload = new ExtendedJSONObject(json.getString("payload"));
        cryptoRecord.setKeyBundle(keyBundle);
        cryptoRecord.decrypt();
        return (R) recordFactory.createRecord(cryptoRecord); // TODO: rm cast. To save time, I didn't generify RecordFactory.
    }
}
