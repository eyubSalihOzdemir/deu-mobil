package com.salih.sakainotifier.worker;

import static android.content.Context.MODE_PRIVATE;

import com.salih.sakainotifier.utilities.Utility;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.salih.sakainotifier.alarm_receiver.AlarmReceiver;
import com.salih.sakainotifier.MainActivity;
import com.salih.sakainotifier.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

//TODO: alerts will be converted to predefined objects and will be handled accordingly

public class GetAlertsWorker extends Worker {

    RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

    boolean flag = false;
    String username = "";
    String password = "";

    public GetAlertsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context myContext = getApplicationContext();

        SharedPreferences sharedPreferences = myContext.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String user = sharedPreferences.getString("username","username");
        String pass = sharedPreferences.getString("password", "password");
        username = user;
        password = pass;



        setForegroundAsync(createForegroundInfo());

        postRequest(myContext);

        // OKHTTP -----------------------------------------------
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        final OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        RequestBody loginRequestBody = new FormBody.Builder()
                .add("eid", username + "@ogr.deu.edu.tr")
                .add("pw", password)
                .add("submit", "Giriş")
                .build();
        okhttpLoginCall(client, loginRequestBody);

        return Result.success();
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {

        Context context = getApplicationContext();

        //todo: check if this is necessary, the call is being made already in doWork()
        postRequest(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel("1", "channelName", NotificationManager.IMPORTANCE_NONE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }

        Notification notification = new NotificationCompat.Builder(context, "1")
                .setContentTitle("Sakai bildirimleri kontrol ediliyor...")
                .setTicker("ticker")
                .setContentText("Duyurular kontrol ediliyor...")
                .setSmallIcon(R.drawable.ic_deunotif)
                .setOngoing(true)
                .build();

        return new ForegroundInfo(1,notification);
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

    public void showNotifText(Context context, String text) {
        createNotificationChannel(context);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis (System.currentTimeMillis());

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Intent fullScreenIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "newid")
                .setGroup("test")
                .setSmallIcon(R.drawable.ic_deunotif)
                .setContentTitle(text)
                .setContentText("Run time: " + calendar.getTimeInMillis())
                .setSubText("hi")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(1, builder.build());

    }

    public void showNotif(Context context, int id, String fromWhom, String title, String siteTitle, @Nullable String url) {
        createNotificationChannel(context);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis (System.currentTimeMillis());

        Uri uri = Uri.parse(url);
        Intent openPos = new Intent(Intent.ACTION_VIEW, uri);
        PendingIntent pendingIntentPos = PendingIntent.getActivity(context, 0, openPos, 0);

        // Set the intent that will fire when the user taps the notification
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

        if(url == null) {
            builder.setContentIntent(pendingIntent);
        } else {
            builder.setContentIntent(pendingIntentPos);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(id, builder.build());
    }

    public void postRequest(Context context) {
        boolean remindWeeklyFood;
        SharedPreferences sharedPreferences = context.getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        remindWeeklyFood = sharedPreferences.getBoolean("remindWeeklyFood", false);

        // yemekhane haftalık reminder
        java.util.Date date = new java.util.Date();
        if(date.getDay() == 5 && date.getHours() >= 10 && remindWeeklyFood) {
            //showNotif(context, i, fromWhom, title, siteTitle, url);
            //todo: create new function for open deu.pos intent
            showNotif(context, 99, "", "14:30'a kadar vaktin var", "Haftalık yemek yüklemeyi unutma", "https://pos.deu.edu.tr/");
            myEdit.putBoolean("remindWeeklyFood", false);
        }

        if(date.getDay() < 5) {
            myEdit.putBoolean("remindWeeklyFood", true);
        }

        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

        flag = false;
        //showNotifText(context, "in post()");
        String urlSakai = "https://online.deu.edu.tr/portal/xlogin";

        //RequestQueue requestQueue = Volley.newRequestQueue(context);
        //requestQueue.start();

        //RequestFuture<StringRequest> futureRequest = RequestFuture.newFuture();

        StringRequest sakaiRequest = new StringRequest(Request.Method.POST, urlSakai, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("VOLLEY", "Volley response: " + response);
                //showNotifText(context, "in get()");
                getRequest(context); // get announcements
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                postRequest(context);
                Log.d("error_worker_post", "error");
                Log.e("error_worker_post", error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("eid", username + "@ogr.deu.edu.tr\n"); //2019504111
                params.put("pw", password); //Musa201323
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

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.d("headers", String.valueOf(response.headers));
                //(context, String.valueOf(response.headers));
                return super.parseNetworkResponse(response);
            }
        };

        sakaiRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(sakaiRequest);

        /*try {
            Log.d("future", "trying future get");
            StringRequest response = futureRequest.get(30, TimeUnit.SECONDS); // this will block
        } catch (InterruptedException e) {
            // exception handling
            Log.d("error", "interruptedException");
        } catch (ExecutionException | TimeoutException e) {
            // exception handling
            Log.d("error", "time out");
        }*/
    }

    public void getRequest(Context context) {
        //showNotifText(context, "in getDeeper()");
        String urlAnnouncements = "https://online.deu.edu.tr/direct/portal/bullhornAlerts.json";

        //RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest stringGetRequest = new StringRequest(Request.Method.GET, urlAnnouncements, //urlTest
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //showNotifText(context, "in getResponse()");
                        String savedAlerts = "";
                        File file = new File(context.getFilesDir(),"alerts.txt");
                        if(file.exists()) { // NOT the first time running
                            Log.d("RUN", "not the first tim running");
                            savedAlerts = Utility.readFromFile(context).trim();

                            try {
                                JSONObject jsonResponseObject = new JSONObject(response);
                                JSONArray alertsJsonArray = jsonResponseObject.getJSONArray("alerts");
                                String alertsJson = alertsJsonArray.toString();

                                JSONArray savedAlertsJsonArray = new JSONArray(savedAlerts);

                                if( alertsJson.toLowerCase().trim().equals(savedAlerts.toLowerCase().trim()) ) {
                                    //showNotifText(context, "in json's are equal()");
                                    // now new announcement, json's are equal, program ran once before
                                    Log.d("compare", "Json's are equal, not the first time app is running. No need for comparison");
                                } else {
                                    //showNotifText(context, "in json's are NOT equal()");
                                    // new announcement
                                    Log.d("compare", "Json's are NOT equal, there might be new announcement");

                                    // find which one of the new alerts doesn't match with the old records
                                    for (int i = 0; i < alertsJsonArray.length(); i++) {
                                        boolean flag = false;
                                        for (int j = 0; j < savedAlertsJsonArray.length(); j++) {
                                            //todo: there might be a problem with ".trim()", remove it and test that
                                            if(alertsJsonArray.get(i).toString().trim().equals(savedAlertsJsonArray.get(j).toString().trim())) {
                                                flag = true;
                                            }
                                        }
                                        // alertsJsonArray.get(i) could not be found in savedAlerts, means it is a new announcement
                                        if(!flag) {
                                            JSONObject obj = (JSONObject) alertsJsonArray.get(i);
                                            String fromWhom = obj.getString("fromDisplayName");
                                            String siteTitle = obj.getString("siteTitle");
                                            String title = obj.getString("title");
                                            String url = obj.getString("url");
                                            Log.d("beautified", fromWhom + " - " + siteTitle + " - " + title);
                                            showNotif(context, i, fromWhom, title, siteTitle, null);
                                        }
                                    }
                                }

                                // write the freshly got alerts no matter the relationship between new and old alerts
                                Utility.writeToFile(alertsJsonArray.toString(), context);

                                flag = true;

                            } catch (JSONException e) {
                                flag = true;
                                e.printStackTrace();
                            }
                        } else { // first time running
                            //showNotifText(context, "first time running, send new message to test");
                            Log.d("RUN", "first tim running");
                            try {
                                JSONObject jsonResponseObject = new JSONObject(response);
                                JSONArray alertsJsonArray = jsonResponseObject.getJSONArray("alerts");
                                String alertsJson = alertsJsonArray.toString();

                                Utility.writeToFile(alertsJson, context);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            //first time running, start the app again to update the alerts fragment to show saved alerts
                            /*Intent restartIntent = new Intent(getApplicationContext(), MainActivity.class);
                            restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(restartIntent);
                             */
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //showNotifText(context, error.toString());
                //getRequest(context);
                Log.d("error_worker_post", "That didn't work!");
                Log.d("error_worker_post", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("accept", "application/json, text/javascript, */*; q=0.01\n");
                params.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
                // maybe try sending current eopch time in millis as payload
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.d("StatusCode", String.valueOf(response.statusCode));
                //showNotifText(context, String.valueOf(response.headers));
                return super.parseNetworkResponse(response);
            }
        };

        stringGetRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringGetRequest);
    }

    public void okhttpLoginCall(OkHttpClient client, okhttp3.RequestBody requestBody) {
        okhttp3.Request loginRequest = new okhttp3.Request.Builder()
                .url("https://online.deu.edu.tr/portal/xlogin")
                .post(requestBody)
                .build();

        Call call = client.newCall(loginRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();

                JSONObject jsonResponseObject = null;
                try {
                    jsonResponseObject = new JSONObject(responseBodyString);
                    //JSONArray jsonArray = jsonResponseObject.getJSONArray("employees");

                    JSONArray jsonArray = jsonResponseObject.getJSONArray("topnav");

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
    }

}
