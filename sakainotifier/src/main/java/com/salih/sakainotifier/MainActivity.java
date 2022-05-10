package com.salih.sakainotifier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.salih.sakainotifier.utilities.Utility;
import com.salih.sakainotifier.worker.GetAlertsWorker;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

//todo: MAKE THE APP RUNTIME ERROR SAFE (if requests fail, string and some other operations may cause runtime error)

public class MainActivity extends AppCompatActivity {

    String username = "";
    String password = "";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

        //verifyStoragePermissions(this);

        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
        // if username and password has been saved already, go directly to the second activity
        if( sharedPreferences.contains("username") && sharedPreferences.contains("password")) {
            loadActivity();
        }

        setContentView(R.layout.activity_main);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void loadActivity() {

        setWork();

        OneTimeWorkRequest firstAlertRequest = new OneTimeWorkRequest.Builder(GetAlertsWorker.class).build();

        PeriodicWorkRequest alertsRequest =
                new PeriodicWorkRequest.Builder(GetAlertsWorker.class, 15, TimeUnit.MINUTES) // ,PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
                        //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // makes the app crash on startup
                        //.setInitialDelay(100, TimeUnit.MILLISECONDS)
                        .build();
        //WorkManager.getInstance(getApplicationContext()).beginUniqueWork("firstAlertRequest", ExistingWorkPolicy.REPLACE, firstAlertRequest);
        //WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("firstAlertRequest", ExistingWorkPolicy.REPLACE, firstAlertRequest);
        //WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("firstAlertRequestInitial", ExistingWorkPolicy.REPLACE, firstAlertRequest);
        //WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("alertRequest", ExistingPeriodicWorkPolicy.REPLACE, alertsRequest);

        //  load the second activity that contains the navigation bar and fragments
        Intent loggedInIntent = new Intent(MainActivity.this, NavigationBarActivity.class);
        startActivity(loggedInIntent);
        finish();
    }

    public void setWork() {
        WorkManager.getInstance(getApplicationContext()).cancelAllWork();

        PeriodicWorkRequest alertsRequest =
                new PeriodicWorkRequest.Builder(GetAlertsWorker.class, PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS) // ,PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
                        //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // makes the app crash on startup
                        //.setInitialDelay(500, TimeUnit.MILLISECONDS)
                        .build();

        OneTimeWorkRequest firstAlertRequest = new OneTimeWorkRequest.Builder(GetAlertsWorker.class).build();

        //WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("firstAlertRequest", ExistingWorkPolicy.REPLACE, firstAlertRequest);
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("alertRequest", ExistingPeriodicWorkPolicy.REPLACE, alertsRequest);
    }

    public void openInstagram(View v) {
        Uri uri = Uri.parse("https://www.instagram.com/salihasimov/");
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

        likeIng.setPackage("com.instagram.android");

        try {
            startActivity(likeIng);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://instagram.com/xxx")));
        }
    }

    public void loginClicked(View v) {
        TextInputEditText usernameField = findViewById(R.id.input_username);
        TextInputEditText passwordField = findViewById(R.id.input_password);
        username = usernameField.getText().toString();
        password = passwordField.getText().toString();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        postRequestCall(client);
    }

    public void postRequestCall(OkHttpClient client) {
        if(username.equals("") || password.equals("")) {
            Utility.showShortToast("Enter your information first.");
            return;
        }

        RequestBody sakaiBody = new FormBody.Builder()
                .add("eid", username + "@ogr.deu.edu.tr\n")
                .add("pw", password)
                .add("submit", "Giriş")
                .build();

        okhttp3.Request loginRequest = new okhttp3.Request.Builder()
                .url("https://online.deu.edu.tr/portal/xlogin")
                .post(sakaiBody)
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

                if(responseBodyString.contains("\"loggedIn\": true")) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Utility.showShortToast("Giriş Başarılı");
                    });

                    SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putString("username", username);
                    myEdit.putString("password", password);
                    myEdit.apply();

                    //setWork();

                    String begin = "title=\"Genel Bakış";
                    String endLink = "\" title=\"Profil";
                    String href = "href=\"";
                    int linkIndexStart = responseBodyString.indexOf(href, responseBodyString.indexOf(begin)) + href.length();
                    int linkIndexEnd = responseBodyString.indexOf(endLink, linkIndexStart);
                    String profileLink = responseBodyString.substring(linkIndexStart, linkIndexEnd);

                    getNameCall(client, profileLink);
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Utility.showShortToast("Olmadı. Bilgileri kontrol edip tekrar deneyin.");
                    });
                }
            }
        });
    }

    public void getNameCall(OkHttpClient client, String profileUrl) {
        okhttp3.Request getNameRequest = new okhttp3.Request.Builder()
                .url(profileUrl)
                .build();

        Call call = client.newCall(getNameRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();

                String searchText = "profileHeadingName";
                int startIndex = responseBodyString.indexOf(searchText) + searchText.length()+2;
                int endIndex = responseBodyString.indexOf("</span>", startIndex);

                if(endIndex < startIndex || startIndex < 0 || endIndex < 0) {
                    getNameCall(client, profileUrl);
                } else {
                    String name = responseBodyString.substring(startIndex, endIndex);

                    SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();

                    myEdit.putString("studentName", name);
                    myEdit.apply();

                    loadActivity();
                }
            }
        });
    }

}

