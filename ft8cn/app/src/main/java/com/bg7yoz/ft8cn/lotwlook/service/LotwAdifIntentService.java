package com.bg7yoz.ft8cn.lotwlook.service;

import static com.bg7yoz.ft8cn.lotwlook.PreferencesActivity.PREFERENCES_KEY;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.lotwlook.PreferencesActivity;
import com.bg7yoz.ft8cn.lotwlook.lotw.QueryLoTW;
import com.bg7yoz.ft8cn.lotwlook.utils.Util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

public class LotwAdifIntentService extends IntentService {
    private static final String TAG = LotwAdifIntentService.class.getSimpleName();
    public static final String PENDING_RESULT = "pendingResult"; // object
    public static final String CALLSIGN = "callsign"; // string
    public static final String DETAIL = "detail"; // boolean
    public static final String QSL_SINCE_DATE = "sinceDate"; // long
    public static final String QSO_START_DATE = "startDate"; // long
    public static final String QSO_END_DATE = "endDate"; // long
    public static final String QSO_MODE = "mode"; // string
    public static final String QSO_BAND = "band"; // string
    public static final String QSO_DXCC = "dxcc"; // long
    public static final String UPDATE_DATABASE = "updateDatabase"; // boolean

    public static final String ADIF_RECORDS_LIST = "adifRecordsList";
    public static final String NEW_QSL_COUNT = "newQslCount";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String TRUNCATED_MESSAGE = "truncatedMessage";

    // response codes
    public static final int RESULT_CODE = 0;
    public static final int ERROR_CODE = 1;
    public static final int NO_CONNECTIVITY_CODE = 2;
    public static final int NO_CREDENTIALS_CODE = 3;
    public static final int BAD_CREDENTIALS_CODE = 4;
    public static final int LOGIN_FAILURE_CODE = 5;

    public LotwAdifIntentService() {
        super(TAG);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale")
    @Override
    protected final void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent()");
        assert intent != null;
        PendingIntent reply = intent.getParcelableExtra(PENDING_RESULT);

        if (reply == null) {
            // if reply is null, we cannot do a damned thing, so just bail here now.
            Log.wtf(TAG, "Could not get the pending intent value!");
            return;
        }

        try {
            if (!Util.isOnline(this)) {
                reply.send(NO_CONNECTIVITY_CODE);
                return;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);

            String username = sharedPreferences.getString(PreferencesActivity.USERNAME_KEY, "");
            String owncall = sharedPreferences.getString(PreferencesActivity.OWNCALL_KEY, "");
            String password = sharedPreferences.getString(PreferencesActivity.PASSWORD_KEY, "");

            if (username.length() == 0 || password.length() == 0) {
                // no credentials.
                reply.send(NO_CREDENTIALS_CODE);
                return;
            }

            int maxDatabaseEntries = Integer.valueOf(sharedPreferences.getString(PreferencesActivity.MAX_DATABASE_ENTRIES_KEY, "100"));

            String callsign = intent.getStringExtra(CALLSIGN);
            boolean detail = intent.getBooleanExtra(DETAIL, true);
            Date sinceDate = dateFromIntentExtra(intent, QSL_SINCE_DATE);
            Date qsoStartDate = dateFromIntentExtra(intent, QSO_START_DATE);
            Date qsoEndDate = dateFromIntentExtra(intent, QSO_END_DATE);
            String mode = intent.getStringExtra(QSO_MODE);
            String band = intent.getStringExtra(QSO_BAND);
            int dxcc = intent.getIntExtra(QSO_DXCC, 0);
            boolean updateDatabase = intent.getBooleanExtra(UPDATE_DATABASE, false);

            Intent result = new Intent();

            try {
                BufferedReader buffer = QueryLoTW.callLotwTo(username, password, owncall, callsign, detail, sinceDate, qsoStartDate,
                        qsoEndDate, mode, band, dxcc);
                Util.writeLogToLocal(getApplicationContext(),buffer);
                reply.send(this, RESULT_CODE, result);
            } catch (Exception e) {
                result.putExtra(ERROR_MESSAGE, e.getMessage());
                reply.send(this, ERROR_CODE, result);
            }
        } catch (PendingIntent.CanceledException ce) {
            Log.i(TAG, "canceledException", ce);
        }
    } // onHandleIntent()

    public static Date dateFromIntentExtra(Intent intent, String item) {
        long dateLong = intent.getLongExtra(item, 0L);
        if (dateLong != 0)
            return new Date(dateLong);
        else
            return null;
    }
} // class
