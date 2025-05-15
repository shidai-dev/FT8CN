package com.bg7yoz.ft8cn;

import static com.bg7yoz.ft8cn.lotwlook.PreferencesActivity.PREFERENCES_KEY;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bg7yoz.ft8cn.html.ImportTaskList;
import com.bg7yoz.ft8cn.lotwlook.lotw.QueryLoTW;
import com.bg7yoz.ft8cn.lotwlook.service.LotwAdifIntentService;
import com.bg7yoz.ft8cn.lotwlook.PreferencesActivity;
import com.bg7yoz.ft8cn.lotwlook.utils.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class LocalLogManagerActivity extends AppCompatActivity {
    private String TAG = "ylf";
    private Button btn_download;
    private Button btn_import;
    private String url = "https://lotw.arrl.org/lotwuser/lotwreport.adi?qso_query=1&login=BI1RRE&qso_qsl=yes&qso_qsldetail=yes&qso_mydetail=yes&qso_withown=yes&qso_qslsince=1900-01-01&qso_qsorxsince=1900-01-01";
    private ProgressDialog progressDialog = null;
    private long lastQslDateTime = 0L;
    public static final String PREFERENCES_KEY = "n1kdo.lotwlook.preferences";
    public static final int LOTW_ADIF_REQUEST_CODE = 0;
    private MainViewModel viewMode = null;
    public static final int LOTW_UPDATE_JOB_ID = 0x73;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.locallog_activity);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
        btn_download = (Button) findViewById(R.id.download);
        viewMode = MainViewModel.getInstance(this);
        btn_import = (Button) findViewById(R.id.import_local_log);
        btn_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(getApplicationContext(), "import", Toast.LENGTH_SHORT);
                toast.show();
                updateFromLoTW();
            }
        });
        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(getApplicationContext(), "download", Toast.LENGTH_SHORT);
                toast.show();
                Intent intent = new Intent(getApplicationContext(),PreferencesActivity.class);
                startActivity(intent);
            }
        });
    }
    private void updateFromLoTW(){
        if (checkIsOnline()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.accessingLoTW));
            progressDialog.setMessage(getString(R.string.pleaseWait));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();

            if (lastQslDateTime == 0) { // really don't want to search from the beginning of time
                lastQslDateTime = System.currentTimeMillis() - (30L * 86400000L); // 30 days.
            }

            PendingIntent pendingResult = createPendingResult(LOTW_ADIF_REQUEST_CODE, new Intent(), 0);
            Intent intent = new Intent(this, LotwAdifIntentService.class);
            intent.putExtra(LotwAdifIntentService.PENDING_RESULT, pendingResult);
            intent.putExtra(LotwAdifIntentService.QSL_SINCE_DATE, lastQslDateTime);
            intent.putExtra(LotwAdifIntentService.UPDATE_DATABASE, true);
            startService(intent);
        }
    }
    private boolean checkIsOnline() {
        if (!isConnected()) {
            Toast.makeText(this, R.string.network_unavailable, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }
        return false;
    }
    protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", Intent data)");
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        Resources res = getResources();

        if (requestCode == LOTW_ADIF_REQUEST_CODE) {
            switch (resultCode) {
                case LotwAdifIntentService.ERROR_CODE:
                    Log.w(TAG, "onActivityResult result is ERROR_CODE");
                    //Toast.makeText(this, R.string.cannot_connect_to_lotw, Toast.LENGTH_LONG).show();
                    String errorMessage = data.getStringExtra(LotwAdifIntentService.ERROR_MESSAGE);
                    Util.alert(this, errorMessage);
                    break;
                case LotwAdifIntentService.NO_CONNECTIVITY_CODE:
                    Log.w(TAG, "onActivityResult result is NO_CONNECTIVITY_CODE");
                    Toast.makeText(this, R.string.network_unavailable, Toast.LENGTH_LONG).show();
                    break;
                case LotwAdifIntentService.NO_CREDENTIALS_CODE:
                    Log.w(TAG, "onActivityResult result is NO_CREDENTIALS_CODE");
                    alertCredentials();
                    break;
                case LotwAdifIntentService.BAD_CREDENTIALS_CODE:
                    Log.w(TAG, "onActivityResult result is BAD_CREDENTIALS_CODE");
                    alertCredentials();
                    break;
                case LotwAdifIntentService.LOGIN_FAILURE_CODE:
                    Log.w(TAG, "onActivityResult result is LOGIN_FAILURE_CODE");
                    Util.alert(this, res.getString(R.string.loginFailed));
                    break;
                default:{
                    viewMode.getHttpServer().doImportLogFile(getFilesDir()+"/ft8cn"+"/"+"lotw.adi");
                    Toast.makeText(this,"import lotw success",Toast.LENGTH_SHORT).show();
                    break;
                }
            } // switch
        } // if requestCode == LOTW_ADIF_REQUEST_CODE
        super.onActivityResult(requestCode, resultCode, data);
    } // onActivityResult()
    private void alertCredentials() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.invalid_missing_credentials);
        adb.setMessage(R.string.you_need_to_set_credentials);
        adb.setPositiveButton(R.string.set_credentials, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getApplicationContext(), PreferencesActivity.class));
            }
        });

        adb.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed(); // get out of the app.
            }
        });
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.show();
    } // alertCredentials()
}
