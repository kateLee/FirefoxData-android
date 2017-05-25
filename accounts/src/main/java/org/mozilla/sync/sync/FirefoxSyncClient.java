/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import org.mozilla.sync.FirefoxSyncException;

import java.util.List;

/**
 * An interface which allows a caller to retrieve data associated with a Sync account.
 *
 * Retrieve an instance through {@link org.mozilla.sync.login.FirefoxSyncLoginManager}, which
 * can be obtained from the primary {@link org.mozilla.sync.FirefoxSync} entry point.
 */
public interface FirefoxSyncClient {

    // --- BOOKMARKS --- //
    /**
     * Retrieves all bookmarks associated with this Sync account.
     *
     * Inside the results container will be the root folder at the top of the bookmarks hierarchy.
     * This is a virtual folder: in Firefox, this folder's name, description, etc. are never seen
     * but its internal data can be seen by clicking "Show All Bookmarks". A list of bookmarks the
     * user has saved can be accessed with {@link BookmarkFolder#getBookmarks()} &
     * {@link BookmarkFolder#getSubfolders()}.
     *
     * This method is blocking and can time out.
     *
     * @return a container with the requested sync data; never null.
     * @throws FirefoxSyncGetCollectionException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread SyncCollectionResult<BookmarkFolder> getAllBookmarks() throws FirefoxSyncGetCollectionException; // TODO: maybe throw typed exceptions instead of FailureReason.

    /**
     * Retrieves a limited number of bookmarks associated with this Sync account.
     *
     * Inside the results container will be the root folder at the top of the bookmarks hierarchy.
     * This is a virtual folder: in Firefox, this folder's name, description, etc. are never seen
     * but its internal data can be seen by clicking "Show All Bookmarks". A list of bookmarks the
     * user has saved can be accessed with {@link BookmarkFolder#getBookmarks()} &
     * {@link BookmarkFolder#getSubfolders()}.
     *
     * This method is blocking and can time out.
     *
     * @param itemLimit The maximum number of bookmarks to retrieve.
     * @return a container with the requested sync data; never null.
     * @throws FirefoxSyncGetCollectionException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread SyncCollectionResult<BookmarkFolder> getBookmarksWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    // --- HISTORY --- //
    /**
     * Retrieves all the history entries a user has created from visiting pages. The
     * results will be returned in most-recently visited to least-recently visited order.
     *
     * This method is blocking and can time out.
     *
     * @return a container with the requested sync data; never null.
     * @throws FirefoxSyncGetCollectionException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread SyncCollectionResult<List<HistoryRecord>> getAllHistory() throws FirefoxSyncGetCollectionException;

    /**
     * Retrieves a limited number of history entries a user has created from visiting pages. The
     * results will be returned in most-recently visited to least-recently visited order, with the
     * least-recently visited being omitted if the specified item limit is reached.
     *
     * This method is blocking and can time out.
     *
     * @param itemLimit The maximum number of history items to retrieve.
     * @return a container with the requested sync data; never null.
     * @throws FirefoxSyncGetCollectionException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread SyncCollectionResult<List<HistoryRecord>> getHistoryWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    // --- PASSWORDS --- //
    /**
     * Retrieves all the passwords the user has saved.
     *
     * This method is blocking and can time out.
     *
     * @return a container with the requested sync data; never null.
     * @throws FirefoxSyncGetCollectionException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread SyncCollectionResult<List<PasswordRecord>> getAllPasswords() throws FirefoxSyncGetCollectionException;

    /**
     * Retrieves a limited number of passwords the user has saved.
     *
     * This method is blocking and can time out.
     *
     * @param itemLimit The maximum number of passwords to retrieve.
     * @return a container with the requested sync data; never null.
     * @throws FirefoxSyncGetCollectionException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread SyncCollectionResult<List<PasswordRecord>> getPasswordsWithLimit(int itemLimit) throws FirefoxSyncGetCollectionException;

    /**
     * Gets the email associated with this Sync Client. It is intended to be used in the UI to
     * notify a user which account they have logged in.
     *
     * This value can change and should never be used to uniquely identify a user.
     *
     * @return the email address associated with this account; this will never be null.
     * @throws FirefoxSyncException if there was a failure retrieving the email address.
     */
    @NonNull String getEmail() throws FirefoxSyncException;
}
