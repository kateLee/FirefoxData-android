package org.mozilla.accountsexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.sync.FirefoxSync;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException;
import org.mozilla.sync.login.FirefoxSyncLoginManager;
import org.mozilla.sync.login.FirefoxSyncLoginException;
import org.mozilla.sync.sync.BookmarkFolder;
import org.mozilla.sync.sync.BookmarkRecord;
import org.mozilla.sync.sync.HistoryRecord;
import org.mozilla.sync.sync.PasswordRecord;

import java.util.List;

public class AccountsExampleActivity extends AppCompatActivity {

    private static final String LOGTAG = "AccountsExampleActivity";

    private static final String KEY_HAS_USER_CANCELLED = "fx-sync-has-user-cancelled";

    private FirefoxSyncLoginManager loginManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If we have an account, we want to refresh our data on start-up. If we don't have
        // an account, we'll prompt the user if they've manually cancelled a login request.
        loginManager = FirefoxSync.getLoginManager(this);
        final SharedPreferences sharedPrefs = getSharedPreferences("fx-sync", 0);
        final FirefoxSyncLoginManager.LoginCallback loginCallback = new LoginManagerCallback(sharedPrefs);
        if (loginManager.isSignedIn()) {
            loginManager.loadStoredSyncAccount(loginCallback);
        } else if (!sharedPrefs.getBoolean(KEY_HAS_USER_CANCELLED, false)){
            loginManager.promptLogin(this, "AccountsExample", loginCallback);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginManager.onActivityResult(requestCode, resultCode, data);
    }

    private static class LoginManagerCallback implements FirefoxSyncLoginManager.LoginCallback {
        private final SharedPreferences sharedPrefs;

        private LoginManagerCallback(final SharedPreferences sharedPrefs) {this.sharedPrefs = sharedPrefs;}

        @Override
        public void onSuccess(final FirefoxSyncClient syncClient) {
            Log.d(LOGTAG, "onSuccess: load stored account.");
            getSyncAndLog(syncClient);
        }

        // TODO: implement these failure cases later.
        @Override
        public void onFailure(final FirefoxSyncLoginException e) {
            Log.d(LOGTAG, "onFailure: load stored account", e);
            // Oh well, we'll try again next run.
        }

        @Override
        public void onUserCancel() { // not called for loadStoredSyncAccount.
            Log.d(LOGTAG, "onUserCancel: load stored account");
            sharedPrefs.edit().putBoolean(KEY_HAS_USER_CANCELLED, true).apply();
            // Tell user they can log in from the settings.
        }

        private void getSyncAndLog(final FirefoxSyncClient syncClient) {
            final List<HistoryRecord> receivedHistory;
            final List<PasswordRecord> receivedPasswords;
            final BookmarkFolder rootBookmark;
            try {
                receivedHistory = syncClient.getAllHistory().getResult();
                receivedPasswords = syncClient.getAllPasswords().getResult();
                rootBookmark = syncClient.getAllBookmarks().getResult();
            } catch (final FirefoxSyncGetCollectionException e) {
                // We could switch on e.getFailureReason() if we wanted to do more specific handling, but
                // ultimately, failure means we should try again later.
                Log.w(LOGTAG, "testSync: failure to receive! " + e.getFailureReason(), e);
                return;
            }

            for (final HistoryRecord record : receivedHistory) {
                Log.d(LOGTAG, record.getTitle() + ": " + record.getURI());
            }
            for (final PasswordRecord record : receivedPasswords) {
                Log.d(LOGTAG, record.getUsername() + ": " + record.getPassword());
            }
            Log.d(LOGTAG, rootBookmark.getTitle());
            for (final BookmarkRecord record : rootBookmark.getBookmarks()) {
                Log.d(LOGTAG, "root child: " + record.getTitle() + ": " + record.getURI());
            }
            for (final BookmarkFolder folder : rootBookmark.getSubfolders()) {
                Log.d(LOGTAG, "root subfolder: " + folder.getTitle());
            }
        }
    }
}
