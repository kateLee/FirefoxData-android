/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.util.DeviceUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * A group of util functions for the Sync servers.
 */
public class FirefoxSyncRequestUtils {
    private FirefoxSyncRequestUtils() {}

    /** Returns the user agent for requests from the library - expects {@link DeviceUtils} to be init. */
    static String getUserAgent(final String applicationName) {
        final String formFactor = DeviceUtils.isTablet() ? "Tablet" : "Mobile";
        final String osVersion = Build.VERSION.RELEASE;

        // Format is Mobile-<OS>-Sync/(<form factor>; <OS> <OS-version>) (<Application-name>)
        return String.format("Mobile-Android-Sync/(%s; Android %s) (%s)", formFactor, osVersion, applicationName);
    }

    // The URI methods wrap String methods so we can avoid allocating too many unnecessary objects when composing the methods.
    public static URI getServerURI(@NonNull final TokenServerToken token) throws URISyntaxException {
        return new URI(getServerURIString(token));
    }

    /**
     * Gets the URI associated with the given collection & id. Equivalent to
     * {@link org.mozilla.gecko.sync.GlobalSession#wboURI(java.lang.String, java.lang.String)}.
     */
    public static URI getCollectionURI(final TokenServerToken token, @NonNull final String collection,
            @Nullable final String id, @Nullable final Map<String, String> getArgs) throws URISyntaxException {
        return new URI(getCollectionURIString(token, collection, id, getArgs));
    }

    private static String getServerURIString(@NonNull final TokenServerToken token) {
        return token.endpoint;
    }

    private static String getServerStorageURIString(@NonNull final TokenServerToken token) {
        return getServerURIString(token) + "/storage";
    }

    private static String getCollectionURIString(final TokenServerToken token, @NonNull final String collection,
            @Nullable final String id, @Nullable final Map<String, String> getArgs) throws URISyntaxException {
        final StringBuilder stringBuilder = new StringBuilder(getServerStorageURIString(token))
                .append('/')
                .append(collection);

        if (id != null) {
            stringBuilder
                    .append('/')
                    .append(id);
        }

        if (getArgs != null && getArgs.size() > 0) {
            stringBuilder.append('?');
            for (final Map.Entry<String, String> entry : getArgs.entrySet()) {
                stringBuilder
                        .append(entry.getKey())
                        .append('=')
                        .append(entry.getValue())
                        .append('&');
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1); // rm extra '&'.
        }
        return stringBuilder.toString();
    }

    public static HawkAuthHeaderProvider getAuthHeaderProvider(final TokenServerToken token) throws UnsupportedEncodingException, URISyntaxException {
        // We expect Sync to upload large sets of records. Calculating the
        // payload verification hash for these record sets could be expensive,
        // so we explicitly do not send payload verification hashes to the
        // Sync storage endpoint.
        final boolean includePayloadVerificationHash = false;

        // We compute skew over time using SkewHandler. This yields an unchanging
        // skew adjustment that the HawkAuthHeaderProvider uses to adjust its
        // timestamps. Eventually we might want this to adapt within the scope of a
        // global session.
        final URI storageServerURI = FirefoxSyncRequestUtils.getServerURI(token);
        final String storageHostname = storageServerURI.getHost();
        final SkewHandler storageServerSkewHandler = SkewHandler.getSkewHandlerForHostname(storageHostname);
        final long storageServerSkew = storageServerSkewHandler.getSkewInSeconds();

        return new HawkAuthHeaderProvider(token.id, token.key.getBytes("UTF-8"), includePayloadVerificationHash,
                storageServerSkew);
    }
}
