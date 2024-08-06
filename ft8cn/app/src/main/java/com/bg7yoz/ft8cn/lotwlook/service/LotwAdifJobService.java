package com.bg7yoz.ft8cn.lotwlook.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.BufferedReader;
import java.util.Date;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.lotwlook.PreferencesActivity;
import com.bg7yoz.ft8cn.lotwlook.lotw.QueryLoTW;
import com.bg7yoz.ft8cn.lotwlook.utils.Util;

public class LotwAdifJobService extends JobService {
    private static final String TAG = LotwAdifJobService.class.getSimpleName();

    @Override
    public final boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob()");
        Context context = getApplicationContext();
        if (Util.isOnline(context)) {
            LoTWQueryThread queryThread = new LoTWQueryThread(this, jobParameters);
            Log.d(TAG, "starting queryThread");
            queryThread.start();
        } else {
            jobFinished(jobParameters, false); // otherwise this is called in the queryThread.
        }
        return true;
    }

    private static class LoTWQueryThread extends Thread {
        private final JobParameters jobParameters;
        private final LotwAdifJobService service;

        private LoTWQueryThread(LotwAdifJobService service, JobParameters jobParameters) {
            this.service = service;
            this.jobParameters = jobParameters;
        }

        public final void run() {
            Log.d(TAG, "calling checkLoTW()");
            service.checkLoTW();
            Log.d(TAG, "returned from checkLoTW()");
            service.jobFinished(jobParameters, false);
        }
    }

    @Override
    public final boolean onStopJob(JobParameters params) {
        return true;
    }

    private void checkLoTW() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        getSharedPreferences(PreferencesActivity.PREFERENCES_KEY, MODE_PRIVATE);

        String username = sharedPreferences.getString(PreferencesActivity.USERNAME_KEY, "");
        String owncall = sharedPreferences.getString(PreferencesActivity.OWNCALL_KEY, "");
        String password = sharedPreferences.getString(PreferencesActivity.PASSWORD_KEY, "");

        if (username.length() == 0 || password.length() == 0) {
            // no credentials.
            Log.i(TAG, "Missing credentials!");
            Util.createNotification(this, "Update problem...", "LoTW credentials missing");
            return;
        }

        long lastQslSeen = sharedPreferences.getLong(PreferencesActivity.LAST_SEEN_QSL_KEY, 0L);
        int maxDatabaseEntries = Integer
                .valueOf(sharedPreferences.getString(PreferencesActivity.MAX_DATABASE_ENTRIES_KEY, "100"));

        long lastQslDateLong = sharedPreferences.getLong(PreferencesActivity.LAST_QSL_DATE_KEY, 0L);

        if (lastQslDateLong == 0) { // really don't want to search from the beginning of time
            lastQslDateLong = System.currentTimeMillis() - (30L * 86400000L); // 30 days.
        }

        Date lastQslDate = (lastQslDateLong == 0) ? null : new Date(lastQslDateLong);
        BufferedReader buffer =QueryLoTW.callLotwTo(username, password, owncall, null, true, lastQslDate, null, null,
                    null, null, 0);
        Util.writeLogToLocal(getApplicationContext(),buffer);
        MainViewModel.getInstance(this);
    }

}
