package com.salih.sakainotifier.alarm_receiver;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.legacy.content.WakefulBroadcastReceiver;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.salih.sakainotifier.MainActivity;
import com.salih.sakainotifier.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        /*if (pendingAlarmIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingAlarmIntent);
        }*/

        /*
        Intent i = new Intent(context, AlarmReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);

        Intent fullScreenIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "newid")
                .setSmallIcon(R.drawable.diamond_icon)
                .setContentTitle("Alarm triggered")
                .setContentText("from AlarmReceiver")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(pendingIntent);
                */
        // ^^for notificaiton

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, 15);
        calendar.set(Calendar.SECOND, 0);

        /*
        NotificationCompat.Builder alarmSetBuilder = new NotificationCompat.Builder(context, "newid")
                .setSmallIcon(R.drawable.diamond_icon)
                .setContentTitle("Alarm Set")
                .setContentText("from AlarmReceiver for " + calendar.getTime())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(123, builder.build());
        notificationManager.notify(123, alarmSetBuilder.build());
         */

        // notificationId is a unique int for each notification that you must define

        // todo: where the things really happen:
        postRequest(context);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingAlarmIntent),pendingAlarmIntent);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingAlarmIntent);
        else
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingAlarmIntent);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("newid", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotif(Context context, int id, String fromWhom, String title, String siteTitle, String url, boolean flag) {
        Log.d("notif", "tried to show notif");
        createNotificationChannel(context);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis (System.currentTimeMillis());

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent fullScreenIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "newid")
                .setGroup("test")
                .setSmallIcon(R.drawable.ic_deunotif)
                .setContentTitle(siteTitle)
                .setContentText(title)
                .setSubText(fromWhom)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);
                // Set the intent that will fire when the user taps the notification
                //.setContentIntent(pendingIntent);

        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(context, "newid")
                .setGroup("test")
                .setGroupSummary(true)
                .setStyle(new NotificationCompat.InboxStyle())
                .setSmallIcon(R.drawable.ic_deunotif)
                .setContentTitle("Fired at " + calendar.getTime())
                .setContentText("0 new announcements");
        // Set the intent that will fire when the user taps the notification
        //.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if(flag) {
            notificationManager.notify(id, builder2.build());
        }

        else {
            notificationManager.notify(id, builder.build());
        }

            // notificationId is a unique int for each notification that you must define

    }

    public void getRequest(Context context) {
        String urlAnnouncements = "https://online.deu.edu.tr/direct/portal/bullhornAlerts.json";

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest stringGetRequest = new StringRequest(Request.Method.GET, urlAnnouncements, //urlTest
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String savedAlerts = "";
                        File file = new File(context.getFilesDir(),"alerts.txt");
                        if(file.exists()) { // NOT the first time running
                            Log.d("run", "not the first tim running");
                            savedAlerts = readFromFile(context).trim();

                            try {
                                JSONObject jsonResponseObject = new JSONObject(response);
                                JSONArray alertsJsonArray = jsonResponseObject.getJSONArray("alerts");
                                String alertsJson = alertsJsonArray.toString();

                                JSONArray savedAlertsJsonArray = new JSONArray(savedAlerts);

                                if( alertsJson.toLowerCase().trim().equals(savedAlerts.toLowerCase().trim()) ) {
                                    // now new announcement, json's are equal, program ran once before
                                    Log.d("compare", "Json's are equal, not the first time app is running. No need for comparison");
                                } else {
                                    // new announcement
                                    Log.d("compare", "Json's are NOT equal, there might be new announcement");

                                    // find which one of the new alerts doesn't match with the old records
                                    for (int i = 0; i < alertsJsonArray.length(); i++) {
                                        boolean flag = false;
                                        for (int j = 0; j < savedAlertsJsonArray.length(); j++) {
                                            if(alertsJsonArray.get(i).toString().trim().equals(savedAlertsJsonArray.get(j).toString().trim())) {
                                                flag = true;
                                            }
                                        }
                                        // alertsJsonArray.get(i) could not be found in savedAlerts, means it is new
                                        if(!flag) {
                                            //Todo: send notif for alertsJsonArray.get(i), maybe put it in a new list or smth idk
                                            JSONObject obj = (JSONObject) alertsJsonArray.get(i);
                                            String fromWhom = obj.getString("fromDisplayName");
                                            String siteTitle = obj.getString("siteTitle");
                                            String title = obj.getString("title");
                                            String url = obj.getString("url");
                                            Log.d("beautified", fromWhom + " - " + siteTitle + " - " + title);
                                            showNotif(context, i, fromWhom, title, siteTitle, url, false);
                                        }
                                    }
                                }
                                // todo: make it show only when there is new announcement
                                showNotif(context, 5, "fromWhom", "title", "siteTitle", "url", true);

                                // write the freshly got alerts no matter the relationship between new and old alerts
                                //todo: activate
                                //writeToFile(alertsJsonArray.toString(), context);

                                WakeLocker.release();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else { // first time running
                            Log.d("run", "first tim running");
                            try {
                                JSONObject jsonResponseObject = new JSONObject(response);
                                JSONArray alertsJsonArray = jsonResponseObject.getJSONArray("alerts");
                                String alertsJson = alertsJsonArray.toString();

                                writeToFile(alertsJson, context);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error","That didn't work!");
                Log.d("error", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.d("StatusCode", String.valueOf(response.statusCode));
                return super.parseNetworkResponse(response);
            }
        };

        queue.add(stringGetRequest);
    }

    public void postRequest(Context context) {
        String urlSakai = "https://online.deu.edu.tr/portal/xlogin";

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.start();

        StringRequest sakaiRequest = new StringRequest(Request.Method.POST, urlSakai, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("VOLLEY", "Volley response: " + response);
                getRequest(context); // get announcements
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VOLLEY", error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("eid", "2018280059@ogr.deu.edu.tr\n");
                params.put("pw", "Kasa2man");
                params.put("submit", "Giriş");

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("content-Type","application/x-www-form-urlencoded");
                params.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                params.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        requestQueue.add(sakaiRequest);
    }

    private void writeToFile(String data, @NonNull Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("alerts.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(@NonNull Context context) {
        String ret = "";
        try {
            InputStream inputStream = context.openFileInput("alerts.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return "";
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
        return ret;
    }
}
