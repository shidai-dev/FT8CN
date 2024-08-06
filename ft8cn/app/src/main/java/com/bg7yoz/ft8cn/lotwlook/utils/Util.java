package com.bg7yoz.ft8cn.lotwlook.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.bg7yoz.ft8cn.MainActivity;
import com.bg7yoz.ft8cn.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Util {
    private static final String TAG = Util.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL_ID = "LOTWLOOK_NTF_CHN_ID";

    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) {
            return false;
        }
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static void alert(Activity activity, CharSequence message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
        adb.setMessage(message);
        adb.setNeutralButton("OK", null);
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.create().show();
    }
    static TextView textViewForTable(final Context context, CharSequence s, int displayMode, int color) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.NO_GRAVITY);
        textView.setPadding(2, 2, 2, 2);

        displayMode = displayMode & 0x07;
        //textView.setBackgroundResource(bkgResList[displayMode]);

        textView.setText(s);
        textView.setTextColor(color);
        return textView;
    }

    public static void createNotification(Context context, CharSequence title, CharSequence message) {
        Log.d(TAG, "createNotification(\"" + title + "\", \"" + message + "\")");
        // see https://developer.android.com/develop/ui/views/notifications/build-notification

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ft8cn_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getResources().getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription("LoTWLook new QSL");
            notificationManager.createNotificationChannel(channel);
        }
        int notificationId = 0; // ID for this app.  there is only one.
        notificationManager.notify(notificationId, builder.build());
    } // createNotification()

    public static boolean writeLogToLocal(Context context, BufferedReader buffer) {
        String path = context.getFilesDir().getPath() + "/ft8cn";
        try {
            File dir = new File(path);
            dir.mkdir();
            String file_name = "lotw.adi";
            File saveFile = new File(path + "/" + file_name);
            String line;
            if (saveFile.exists()) {
                saveFile.delete();
                saveFile.createNewFile();
            } else {
                saveFile.createNewFile();
            }
            if (saveFile.canWrite()) {
                FileWriter fileWriter = new FileWriter(saveFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                while ((line = buffer.readLine()) != null) {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }
                bufferedWriter.flush();
                bufferedWriter.close();
                buffer.close();
            }
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
            return false;
        }
        return true;
    }
}
