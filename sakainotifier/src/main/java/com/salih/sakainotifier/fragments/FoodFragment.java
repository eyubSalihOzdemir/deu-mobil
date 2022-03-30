package com.salih.sakainotifier.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.salih.sakainotifier.utilities.DownloadImageTask;
import com.salih.sakainotifier.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class FoodFragment extends Fragment {

    String key = "";
    String sessID= "";

    public FoodFragment() {
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
        return inflater.inflate(R.layout.fragment_food, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        getKeyRequest();
        //getKeyCall(client);
        getMenuCall(client);

        Button balanceButton = getView().findViewById(R.id.balanceButton);
        balanceButton.setOnClickListener(view1 -> {
            Uri uri = Uri.parse("https://pos.deu.edu.tr/");
            Intent likePos = new Intent(Intent.ACTION_VIEW, uri);

            try {
                startActivity(likePos);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://pos.deu.edu.tr/")));
            }
        });
    }

    public void getKeyRequest() {
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String urlPos = "https://pos.deu.edu.tr/";

        StringRequest stringGetRequest = new StringRequest(Request.Method.GET, urlPos,
                response -> {
                    String search = "kullanici_girisanahtar\" value=\"";
                    String searchEnd = "\">";
                    int beginIndex = response.indexOf(search) + search.length();
                    int endIndex = response.indexOf(searchEnd, beginIndex);
                    key = response.substring(beginIndex, endIndex);

                    postRequest(key, sessID);
                }, error -> {
            Log.d("error_food_key","That didn't work!");
            Log.d("error_food_key", error.toString());
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<>();
                params.put("accept", "text/html");
                params.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                //Log.d("networkresponse", String.valueOf(response.headers));
                // Set-Cookie=PHPSESSID=vg79435rn7sctddru3motdkbc1;
                sessID = response.headers.get("Set-Cookie");
                return super.parseNetworkResponse(response);
            }
        };

        stringGetRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringGetRequest);
    }

    public void postRequest(String key, String sessID) {
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        String urlPos = "https://pos.deu.edu.tr/page/login.php";

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String user = sharedPreferences.getString("username","username");
        String pass = sharedPreferences.getString("password", "password");

        StringRequest sakaiRequest = new StringRequest(Request.Method.POST, urlPos, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.d("posResponse", response);
                getBalanceRequest();
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

                params.put("kullanici_posta", user); //2019504111
                params.put("kullanici_parola", pass); //Musa201323
                params.put("kullanici_tip", "O");
                params.put("kullanici_girisanahtar", key);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                params.put("Accept", "text/html");
                params.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
                params.put("Cookie", sessID);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        sakaiRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(sakaiRequest);
    }

    public void getBalanceRequest() {
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String urlPos = "https://pos.deu.edu.tr/page/index.php?p=report";

        StringRequest stringGetRequest = new StringRequest(Request.Method.GET, urlPos,
                response -> {
                    String searchBegin = "Bakiyeniz: ";
                    String searchEnd = "<i class";
                    int beginIndex = response.indexOf(searchBegin) + searchBegin.length();
                    int endIndex = response.indexOf(searchEnd, beginIndex);
                    if(beginIndex <= 7 || endIndex <= 0) {
                        TextView balanceText = getView().findViewById(R.id.balanceText);
                        balanceText.setText("Bakiye: " + "error_rsp");
                    } else {
                        String balance = response.substring(beginIndex, endIndex);
                        Log.d("BALANCE", balance);
                        TextView balanceText = getView().findViewById(R.id.balanceText);
                        balanceText.setText("Bakiye: " + balance);
                    }
                }, error -> {
            getBalanceRequest();
            Log.d("error_food_getrequest","That didn't work!");
            TextView balanceText = getView().findViewById(R.id.balanceText);
            balanceText.setText("Bakiye: " + "error_er");
            error.printStackTrace();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Log.d("HERADER", sessID);
                Map<String,String> params = new HashMap<>();
                params.put("accept", "text/html");
                params.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:96.0) Gecko/20100101 Firefox/96.0");
                params.put("Cookie", sessID);
                params.put("p", "report");
                params.put("Referer", "https://pos.deu.edu.tr/page/index.php");
                params.put("Sec-Fetch-Site","same-origin");

                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.d("networkresponse", String.valueOf(response.headers));
                // Set-Cookie=PHPSESSID=vg79435rn7sctddru3motdkbc1;
                //sessID = response.headers.get("Set-Cookie");

                return super.parseNetworkResponse(response);
            }
        };

        stringGetRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringGetRequest);
    }

    // OKHTTP ------------------------------------------------------------------------------------vv------------------------------------------
    public void getMenuCall(OkHttpClient client) {
        okhttp3.Request getMenuRequest = new okhttp3.Request.Builder()
                .url("https://sks.deu.edu.tr/yemek-menusu/")
                .build();

        Call call = client.newCall(getMenuRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Document doc = Jsoup.parse(responseBodyString);

                Elements info = doc.select("div#tm_lunch_menu_widget-2");
                String test = info.first().html();
                test = test.replaceAll("<br>", "")
                        .replaceAll("<strong>", "-")
                        .replaceAll("</strong>", "_")
                        .replaceAll("<h2>", "")
                        .replaceAll("</h2>", "");
                String[] split = test.split("-");

                for (int i = 0; i < split.length; i++) {
                    Log.d("tag" + i, split[i]);
                }

                LinearLayout parentLayout = getView().findViewById(R.id.parentLayout);

                for (int i = 1; i < split.length; i++) {
                    String[] splitDeep = split[i].split("_");
                    View cardView = LayoutInflater.from(getContext()).inflate(R.layout.food_list_card_view, null);
                    TextView foodWeekday = cardView.findViewById(R.id.foodWeekday);
                    TextView dailyFood = cardView.findViewById(R.id.dailyMenu);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        foodWeekday.setText(splitDeep[0]);
                        dailyFood.setText(splitDeep[1].replaceAll("[\n\r]", ""));
                        parentLayout.addView(cardView);
                    });
                }

                Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
                String imagePath = images.get(2).attr("src");
                ImageView dailyFoodImageView = getView().findViewById(R.id.imageView);

                new DownloadImageTask((ImageView) dailyFoodImageView).execute(imagePath);

                new Handler(Looper.getMainLooper()).post(() -> {
                    View view = new View(getContext());
                    view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 25));
                    parentLayout.addView(view);
                });

            }
        });
    }

    public void getKeyCall(OkHttpClient client) {
        okhttp3.Request getKeyRequest = new okhttp3.Request.Builder()
                .url("https://pos.deu.edu.tr/")
                .build();

        Call call = client.newCall(getKeyRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();

                String search = "kullanici_girisanahtar\" value=\"";
                String searchEnd = "\">";
                int beginIndex = responseBodyString.indexOf(search) + search.length();
                int endIndex = responseBodyString.indexOf(searchEnd, beginIndex);
                key = responseBodyString.substring(beginIndex, endIndex);

                SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                String user = sharedPreferences.getString("username","username");
                String pass = sharedPreferences.getString("password", "password");

                RequestBody requestBody = new FormBody.Builder()
                        .add("kullanici_posta", user)
                        .add("kullanici_parola", pass)
                        .add("kullanici_tip", "0")
                        .add("kullanici_girisanahtar", key)
                        .build();

                postCall(client, requestBody);
            }
        });
    }

    public void postCall(OkHttpClient client, RequestBody requestBody) {
        okhttp3.Request postCallRequest = new okhttp3.Request.Builder()
                .url("https://pos.deu.edu.tr/page/login.php")
                .post(requestBody)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36")
                .addHeader("Accept","text/html")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Origin", "https://pos.deu.edu.tr")
                .addHeader("Referer", "https://pos.deu.edu.tr/")
                .addHeader("Host", "pos.deu.edu.tr")
                .build();

        Call call = client.newCall(postCallRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Log.d("balance_response", responseBodyString);

                getBalanceCall(client);
            }
        });
    }

    public void getBalanceCall(OkHttpClient client) {
        okhttp3.Request getBalanceRequest = new okhttp3.Request.Builder()
                .url("https://pos.deu.edu.tr/page/index.php?p=report")
                .build();

        Call call = client.newCall(getBalanceRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    TextView balanceText = getView().findViewById(R.id.balanceText);
                    balanceText.setText("Bakiye: " + "error_er");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();

                String searchBegin = "Bakiyeniz: ";
                String searchEnd = "<i class";
                int beginIndex = responseBodyString.indexOf(searchBegin) + searchBegin.length();
                int endIndex = responseBodyString.indexOf(searchEnd, beginIndex);
                if(beginIndex <= 7 || endIndex <= 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        TextView balanceText = getView().findViewById(R.id.balanceText);
                        balanceText.setText("Bakiye: " + "error_rsp");
                    });
                } else {
                    String balance = responseBodyString.substring(beginIndex, endIndex);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        TextView balanceText = getView().findViewById(R.id.balanceText);
                        balanceText.setText("Bakiye: " + balance);
                    });
                }
            }
        });
    }
    // ------------------------------------------------------------------------------------------------------------------------------
}

