package com.salih.sakainotifier.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.salih.sakainotifier.utilities.DownloadImageTask;
import com.salih.sakainotifier.R;
import com.salih.sakainotifier.utilities.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.sql.ClientInfoStatus;
import java.util.Calendar;
import java.util.Date;

import io.reactivex.internal.observers.ForEachWhileObserver;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class ProfileFragment extends Fragment {

    private final int CAPACITY = 525;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .addInterceptor(logging)
                .build();

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", MODE_PRIVATE);
        boolean showDialog = sharedPreferences.getBoolean("showDialog", false);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
        builder1.setMessage("Bildirimleri zamanında alabilmek için batarya kısıtlamalarını kaldırmayı ve otomatik başlatma izni vermeyi unutmayın!");
        builder1.setCancelable(true);

        builder1.setTitle("Uyarı");
        builder1.setPositiveButton(
                "Anladım",
                (dialog, id) -> dialog.cancel(
                        // do nothing
                ));
        builder1.setNeutralButton(
                "Bir daha gösterme",
                (dialog, id) -> {
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putBoolean("showDialog", true);
                    myEdit.apply();

                    dialog.cancel();
                });

        AlertDialog alert1 = builder1.create();

        if(!showDialog)
            alert1.show();

        CardView libraryCardview = getView().findViewById(R.id.library_cardview);
        CardView weatherCardview = getView().findViewById(R.id.weather_cardview);
        libraryCardview.setOnClickListener(view1 -> {
            Utility.showShortToast("Merkez Kütüphane Doluluğu");

            String username = sharedPreferences.getString("username","noUsername");
            String password = sharedPreferences.getString("password", "noPassword");

            // --------------------------------------------------------------------
            CookieManager cookieManager2 = new CookieManager();
            cookieManager2.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            OkHttpClient client2 = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager2))
                    .build();

            RequestBody loginRequestBody = new FormBody.Builder()
                    .add("eid", username + "@ogr.deu.edu.tr")
                    .add("pw", password)
                    .add("submit", "Giriş")
                    .build();
            okhttpLoginCall(client2, loginRequestBody);
            // --------------------------------------------------------------------
        });
        weatherCardview.setOnClickListener(view1 -> {
            Utility.showShortToast("Tınaztepe Kampüsü Hava Durumu");
        });

        MaterialToolbar myAppBar = getView().findViewById(R.id.topAppBar);
        myAppBar.setOnMenuItemClickListener((Toolbar.OnMenuItemClickListener) item -> {
            switch (item.getItemId()) {
                case R.id.information_item:
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
                    builder2.setMessage("Bildirimleri zamanında alabilmek için batarya kısıtlamalarını kaldırmayı ve otomatik başlatma izni vermeyi unutmayın!");
                    builder2.setCancelable(true);

                    builder2.setTitle("Uyarı");
                    builder2.setPositiveButton(
                            "Anladım",
                            (dialog, id) -> dialog.cancel(
                                    // do nothing
                            ));
                    AlertDialog alert2 = builder2.create();
                    alert2.show();
                    break;
                case R.id.contact_item:
                    Utility.contact();
                    break;
                case R.id.logout_item:
                    Utility.logout();
                    break;
                case R.id.transcript_download:
                    Utility.downloadTranscript();
                    break;
                case R.id.show_syllable_item:
                    // nothing for now
                    break;
                case R.id.settings_item:
                    // nothing for now
                    break;
            }
            return true;
        });

        TextView studentName = getView().findViewById(R.id.title_name);
        String name = sharedPreferences.getString("studentName", "defaultName").toLowerCase();
        String[] strArray = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap + " ");
        }
        studentName.setText(builder.toString());

        TextView currentUse = getView().findViewById(R.id.currentUse);
        TextView percentage = getView().findViewById(R.id.percentage);

        okhttpWeatherCall(client);
        okhttpGetRequest(client, currentUse, percentage);
        okhttpGetApodRequest(client);
    }

    public void okhttpWeatherCall(OkHttpClient client) {
        okhttp3.Request weatherRequest = new okhttp3.Request.Builder()
                .url("https://api.weatherapi.com/v1/forecast.json?key=3c660f89e2c94dea8f6174224211711&q=38.37,27.20&days=1&lang=tr&aqi=no&alerts=no")
                .build();

        Call call = client.newCall(weatherRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();

                if(response.isSuccessful()) {
                    Log.d("WEATHER_CALL", "on response success");

                    try {
                        JSONObject jsonObject = new JSONObject(responseBodyString);

                        JSONObject currentJson = jsonObject.getJSONObject("current");
                        JSONObject conditionJson = currentJson.getJSONObject("condition");
                        String condition = conditionJson.getString("text");
                        String iconUrl = conditionJson.getString("icon");
                        String temp = currentJson.getString("temp_c");
                        String lastUpdated = currentJson.getString("last_updated");
                        String wind = currentJson.getString("wind_kph");

                        /*
                        try {
                            condition = new String(condition.getBytes("ISO-8859-1"), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        condition = Html.fromHtml(condition).toString();
                         */
                        final String conditionFinal = condition;

                        lastUpdated = lastUpdated.substring(lastUpdated.length()-5);

                        JSONObject forecastJson = jsonObject.getJSONObject("forecast")
                                .getJSONArray("forecastday")
                                .getJSONObject(0)
                                .getJSONArray("hour")
                                .getJSONObject(12);

                        JSONObject conditionForecastJson = forecastJson.getJSONObject("condition");
                        String conditionForecast = conditionForecastJson.getString("text");
                        String iconUrlForecast = conditionForecastJson.getString("icon");
                        String tempForecast = forecastJson.getString("temp_c");
                        String windForecast = forecastJson.getString("wind_kph");

                        /*try {
                            conditionForecast = new String(conditionForecast.getBytes("ISO-8859-1"), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        conditionForecast = Html.fromHtml(conditionForecast).toString();
                         */
                        final String conditionForecastFinal = conditionForecast;

                        CardView weatherCardView = getView().findViewById(R.id.weather_cardview);
                        TextView mainTextView = weatherCardView.findViewById(R.id.weather_text);
                        TextView  tempTextView = weatherCardView.findViewById(R.id.temperature_text_view);
                        ImageView iconImageView = weatherCardView.findViewById(R.id.weather_icon_image_view);
                        TextView lastUpdatedTextView = weatherCardView.findViewById(R.id.weather_subtext);
                        TextView windTextView = weatherCardView.findViewById(R.id.weather_wind);

                        Date currentTime = Calendar.getInstance().getTime();
                        //currentTime.setHours(currentTime.getHours() + 3);

                        Calendar showTimeCalendar = Calendar.getInstance();
                        showTimeCalendar.setTime(currentTime);

                        Date time1 = Calendar.getInstance().getTime();
                        time1.setHours(18);
                        time1.setMinutes(0);

                        Date time2 = Calendar.getInstance().getTime();
                        time2.setHours(23);
                        time2.setMinutes(59);

                        if(currentTime.after(time1) && currentTime.before(time2)) {
                            // show tomorrows weather information
                            Log.d("TIME BETWEEN", currentTime.toString());
                            Log.d("TIME1", time1.toString());
                            Log.d("TIME2", time2.toString());
                            new Handler(Looper.getMainLooper()).post(() -> {
                                mainTextView.setText(conditionForecastFinal);
                                tempTextView.setText(String.format("%s°C", tempForecast));
                                lastUpdatedTextView.setText("Sonraki günün hava durumu");
                                windTextView.setText(String.format("Rüzgar: %s kph", windForecast));
                            });

                            iconUrlForecast = String.format("https:%s", iconUrlForecast);
                            final String iconUrlForecastFinal = iconUrlForecast;

                            new Handler(Looper.getMainLooper()).post(() -> {
                                new DownloadImageTask((ImageView) iconImageView).execute(iconUrlForecastFinal);
                            });
                        } else {
                            Log.d("TIME", currentTime.toString());
                            Log.d("TIME1", time1.toString());
                            Log.d("TIME2", time2.toString());
                            // show current weather information
                            Log.d("HERE", "HERE");
                            new Handler(Looper.getMainLooper()).post(() -> {
                                mainTextView.setText(conditionFinal);
                                tempTextView.setText(String.format("%s°C", temp));
                                lastUpdatedTextView.setText("Güncel hava durumu");
                                windTextView.setText(String.format("Rüzgar: %s kph", wind));
                            });
                            iconUrl = String.format("https:%s", iconUrl);
                            final String iconUrlFinal = iconUrl;

                            new Handler(Looper.getMainLooper()).post(() -> {
                                new DownloadImageTask((ImageView) iconImageView).execute(iconUrlFinal);
                            });
                        }

                    }catch (JSONException err){
                        okhttpWeatherCall(client);
                        Log.d("WEATHER_CALL", "try-catch error");
                    }
                } else {
                    Log.d("WEATHER_CALL", "on response failed");
                }
            }
        });
    }

    public void okhttpGetRequest(OkHttpClient client, TextView currentUse, TextView percentage) {
        okhttp3.Request getRequest = new okhttp3.Request.Builder()
                .url("https://akillikart.deu.edu.tr/kutuphane/")
                .build();

        Call call = client.newCall(getRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {

                String responseBodyString = response.body().string();

                String searchStart = "Şuanki Kullanım Doluluğu : ";
                String searchEnd = "<br>";
                int beginIndex = responseBodyString.indexOf(searchStart) + searchStart.length();
                int endIndex = responseBodyString.indexOf(searchEnd, beginIndex);
                String count = responseBodyString.substring(beginIndex, endIndex);
                float rate;
                if(Float.parseFloat(count) == 0)
                    rate = 0;
                else
                    rate = Float.parseFloat(count) / CAPACITY * 100;

                //CAPACITY
                new Handler(Looper.getMainLooper()).post(() -> {
                    currentUse.setText(String.format("Şu anki kullanım: %s", count));
                    percentage.setText(new StringBuilder().append("%").append("\n").append((int)rate).toString());
                });
            }
        });
    }

    public void okhttpGetApodRequest(OkHttpClient client) {
        String siteUrl = "https://apod.nasa.gov/apod/astropix.html";

        okhttp3.Request getApodRequest = new okhttp3.Request.Builder()
                .url("https://api.nasa.gov/planetary/apod?api_key=BWRB0A8k75HKx59QEIGK96duH1sv4o1a4KfM5dmW")
                .build();

        Call call = client.newCall(getApodRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();

                try {
                    JSONObject jsonObject = new JSONObject(responseBodyString);

                    Log.d("test_apod", jsonObject.toString());

                    String mediaType = jsonObject.getString("media_type");
                    CardView apodCardview = getView().findViewById(R.id.apod_cardview);
                    ImageView apodImageview = getView().findViewById(R.id.apod_imageview);

                    if(mediaType.equals("image")) {
                        String path = jsonObject.getString("url");

                        new Handler(Looper.getMainLooper()).post(() -> {
                            new DownloadImageTask((ImageView) apodImageview).execute(path);

                            apodCardview.setOnClickListener(view -> {
                                new AlertDialog.Builder(getContext())
                                        .setMessage("Detaylı açıklama için apod.nasa.gov'a gidilsin mi?")

                                        .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl));
                                                startActivity(browserIntent);
                                            }
                                        })
                                        .setNegativeButton("Vazgeç", null)
                                        .show();
                        });


                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            apodImageview.setImageResource(R.drawable.apod_video_bg);
                        });

                        apodCardview.setOnClickListener(view -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl));
                            startActivity(browserIntent);
                        });
                    }
                } catch (JSONException e) {
                    okhttpGetApodRequest(client);
                    e.printStackTrace();
                }
            }
        });
    }

    public void okhttpLoginCall(OkHttpClient client, okhttp3.RequestBody requestBody) {
        final String pvtMsg = "/privateMsg/pvtMsg";
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

                Document doc = Jsoup.parse(responseBodyString);
                Elements elements = doc.getElementsByAttributeValue("id", "topnav").get(0).children();

                // remove "anasayfa" from top navigation list
                elements.remove(0);

                for(Element x : elements) {
                    Log.d("each_element_text", x.text());
                    Element children = x.child(1);
                    String url = children.attr("href");

                    okhttpMessagesCall(client, url);
                }
            }
        });
    }

    public void okhttpMessagesCall(OkHttpClient client, String url) {
        final String pvtMsg = "/privateMsg/pvtMsgHpView";
        okhttp3.Request messagesRequest = new okhttp3.Request.Builder()
                .url(url)
                .build();

        Call call = client.newCall(messagesRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body().string();

                Document doc = Jsoup.parse(responseBodyString);
                Elements path = doc.select("#toolMenu > ul");
                Elements table = path.get(0).children();
                for (Element menuItem :
                        table) {
                    if(menuItem.text().equals("Mesajlar")) {
                        Elements messages = menuItem.getElementsByAttributeValueContaining("title", "Mesajlar");
                        String messagesLink = messages.attr("href");
                        messagesLink = messagesLink + pvtMsg;
                        Log.d("messages_mesajlar", messagesLink);

                        okhttpGetLastMessageCall(client, messagesLink);
                    }
                }
            }
        });
    }

    public void okhttpGetLastMessageCall(OkHttpClient client, String url) {
        okhttp3.Request lastMessageRequest = new okhttp3.Request.Builder()
                .url(url)
                .build();

        Call call = client.newCall(lastMessageRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // nothing.
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body().string();

                Document doc = Jsoup.parse(responseBodyString);

                String newUrl = url.substring(0, url.length()-6);

                Log.d("date_test", responseBodyString);
                // msgForum:j_id_jsp_1819031275_29:0:privateForums:0:j_id_jsp_1819031275_35
                // msgForum: msgForum
                // msgForum:mainOrHp: pvtMsgHpView

                // javax.faces.ViewState
                // pvtMsgTopicId

                Elements javax = doc.getElementsByAttributeValue("name", "javax.faces.ViewState");
                //Elements pvtMsgTopicID = doc.getElementsByAttribute("pvtMsgTopicId");
                //Element pvtMsgTopicID = doc.getElementById("pvtMsgTopicId");
                Log.d("is_it_there", javax.toString());
                //Log.d("is_this_there", pvtMsgTopicID.toString());
                
                //okhttpGetMessages(client, newUrl);
            }
        });
    }

    public void okhttpGetMessages(OkHttpClient client, String url) {
        okhttp3.Request lastMessageRequest = new okhttp3.Request.Builder()
                .url(url)
                .build();

        Call call = client.newCall(lastMessageRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // nothing.
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body().string();

                Document doc = Jsoup.parse(responseBodyString);

            }
        });
    }
}

