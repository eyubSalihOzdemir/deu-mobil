package com.salih.sakainotifier.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.salih.sakainotifier.R;
import com.salih.sakainotifier.utilities.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class GradesFragment extends Fragment {

    private String m_Text = "";

    public GradesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String pass = sharedPreferences.getString("password", "");
        String name = sharedPreferences.getString("studentName", "");
        String studentID = sharedPreferences.getString("username", "");
        String debisPass = sharedPreferences.getString("debisPass", pass);
        name = Utility.formatNameForDebis(name);

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();

        RequestBody loginBody = new FormBody.Builder()
                .add("username", name)
                .add("password", debisPass)
                .add("emailHost", "ogr.deu.edu.tr")
                .add("tamam", "Gönder")
                .build();

        loginCall(client, loginBody, null);
    }

    public void loginCall(OkHttpClient client, okhttp3.RequestBody requestBody, @Nullable String debisPass) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String pass = sharedPreferences.getString("password", "");
        String name = sharedPreferences.getString("studentName", "");
        String studentID = sharedPreferences.getString("username", "");
        name = Utility.formatNameForDebis(name);

        okhttp3.Request loginRequest = new okhttp3.Request.Builder()
                .url("https://debis.deu.edu.tr/debis.php")
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
                Log.d("loginCall_rsp_size", String.valueOf(responseBodyString.length()));
                Log.d("grades_response", responseBodyString);

                if(responseBodyString.contains("isim.soyisim@ogr.deu.edu.tr")) {
                    /*
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("DEBIS Şifreniz:");

                    final EditText input = new EditText(getContext());
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            builder.setView(input);
                        }
                    });
                    //builder.setView(input);

                    builder.setPositiveButton("Devam et", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_Text = input.getText().toString();
                            SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                            String name = sharedPreferences.getString("studentName", "");
                            name = formatNameForDebis(name);

                            RequestBody loginBodyRetry = new FormBody.Builder()
                                    .add("username", name)
                                    .add("password", m_Text)
                                    .add("emailHost", "ogr.deu.edu.tr")
                                    .add("tamam", "Gönder")
                                    .build();

                            loginCall(client, loginBodyRetry, m_Text);
                        }
                    });
                    builder.setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                     */

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("DEBIS Şifreniz:");

                            final EditText input = new EditText(getContext());
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            builder.setView(input);

                            builder.setPositiveButton("Devam et", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    m_Text = input.getText().toString();
                                    SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                                    String name = sharedPreferences.getString("studentName", "");
                                    name = Utility.formatNameForDebis(name);

                                    RequestBody loginBodyRetry = new FormBody.Builder()
                                            .add("username", name)
                                            .add("password", m_Text)
                                            .add("emailHost", "ogr.deu.edu.tr")
                                            .add("tamam", "Gönder")
                                            .build();

                                    loginCall(client, loginBodyRetry, m_Text);
                                }
                            });
                            builder.setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                            //todo: uncomment this
                            builder.show();
                        }
                    });
                    //builder.show();
                } else {
                    SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();

                    if(debisPass != null) {
                        myEdit.putString("debisPass", debisPass);
                        myEdit.apply();
                    }

                    getSemesterCall(client);
                }
            }
        });
    }

    public void getSemesterCall(OkHttpClient client) {
        okhttp3.Request getSemesterRequest = new okhttp3.Request.Builder()
                .url("https://debis.deu.edu.tr/OgrenciIsleri/Ogrenci/OgrenciNotu/index.php")
                .build();

        Call call = client.newCall(getSemesterRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Log.d("getSmstrCall_rsp_size", String.valueOf(responseBodyString.length()));

                if(responseBodyString.contains("isim.soyisim@ogr.deu.edu.tr")) {
                    // something is wrong.
                    Log.d("getSemesterCall_FAIL", "something is wrong");
                } else {
                    SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                    String studentID = sharedPreferences.getString("username", "");

                    Document doc = Jsoup.parse(responseBodyString);
                    Elements semester = doc.getElementsByAttributeValue("name", "ogretim_donemi_id");
                    Elements options;
                    if(semester.size() != 0) {
                        options = semester.get(0).children();
                        String semesterID = options.get(1).val();

                        RequestBody requestBodyWithSemester = new FormBody.Builder()
                                .add("ogretim_donemi_id", semesterID)
                                .add("relative", "../../../")
                                .add("ogrenci_no", studentID)
                                .add("liste", "")
                                .add("Origin", "https://debis.deu.edu.tr")
                                .add("Referer", "https://debis.deu.edu.tr/OgrenciIsleri/Ogrenci/OgrenciNotu/index.php")
                                .build();

                        getClassesCall(client, requestBodyWithSemester, semesterID);

                    } else {
                        Log.d("something", "something went wrong");
                    }


                }
            }
        });
    }

    public void getClassesCall(OkHttpClient client, okhttp3.RequestBody requestBody, String semesterID) {
        okhttp3.Request getClassesRequest = new okhttp3.Request.Builder()
                .url("https://debis.deu.edu.tr/OgrenciIsleri/Ogrenci/OgrenciNotu/index.php")
                .post(requestBody)
                .build();

        Call call = client.newCall(getClassesRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Log.d("getClassesCall_rsp_size", String.valueOf(responseBodyString.length()));

                SharedPreferences sharedPreferences = getContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                String studentID = sharedPreferences.getString("username", "");

                if(responseBodyString.contains("isim.soyisim@ogr.deu.edu.tr")) {
                    Log.d("getClassesCall_FAIL", "failed");
                } else {
                    Document doc = Jsoup.parse(responseBodyString);
                    Elements options = doc.getElementsByAttributeValue("name", "ders").get(0).children();
                    options.remove(0);
                    for (Element x :
                            options) {

                        Log.d("ERSIN_UZGUN", x.text());

                        RequestBody classBody = new FormBody.Builder()
                                .add("ders", x.val())
                                .add("ogretim_donemi_id", semesterID)
                                .add("relative", "../../../")
                                .add("ogrenci_no", studentID)
                                .add("liste", "")
                                .add("Origin", "https://debis.deu.edu.tr")
                                .add("Referer", "https://debis.deu.edu.tr/OgrenciIsleri/Ogrenci/OgrenciNotu/index.php")
                                .build();

                        getClassInformationCall(client, classBody, x.text());
                    }
                }
            }
        });
    }

    public void getClassInformationCall(OkHttpClient client, okhttp3.RequestBody requestBody, String classID) {
        okhttp3.Request getClassInformationRequest = new okhttp3.Request.Builder()
                .url("https://debis.deu.edu.tr/OgrenciIsleri/Ogrenci/OgrenciNotu/index.php")
                .post(requestBody)
                .build();

        Call call = client.newCall(getClassInformationRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Log.d("getClsInfoCall_rsp_size", String.valueOf(responseBodyString.length()));

                if(responseBodyString.contains("isim.soyisim@ogr.deu.edu.tr")) {
                    // failed
                } else {
                    Document doc = Jsoup.parse(responseBodyString);
                    Elements withoutID = doc.select("td:not([id])~td");

                    String begin = "Fakültesi";
                    int facultyIndex = withoutID.toString().indexOf(begin);
                    String facultyName = withoutID.toString().substring(facultyIndex-10, facultyIndex);
                    Log.d("faculty_name", facultyName);
                    if(facultyName.contains("Tıp")) {
                        TextView loadingTextView = getView().findViewById(R.id.loadingTextView);
                        if(loadingTextView != null) {
                            loadingTextView.setText("Tıp öğrencileri için not desteği yakında eklenecektir.");
                        }
                        return;
                    }

                    String first = "<tbody>"; String second = "</tbody>";
                    int lastInd = withoutID.toString().lastIndexOf(first);
                    int lastIndEnd = withoutID.toString().lastIndexOf(second) + second.length();
                    String unformattedInformation = withoutID.toString().substring(lastInd, lastIndEnd);
                    Log.d("unformatted_information", unformattedInformation);
                    String lines[] = unformattedInformation.split("\\r?\\n");
                    for (String foo :
                            lines) {
                        Log.d("lines", foo);
                    }
                    List<String> linesList = Arrays.asList(lines);

                    if(linesList.size() <= 8) {
                        return;
                    }
                    linesList = linesList.subList(8, linesList.size()-1);

                    LinearLayout gradesFragmentLinearLayout = getView().findViewById(R.id.grades_fragment_parent_layout);

                    View classCardView = LayoutInflater.from(getContext()).inflate(R.layout.grades_card_view, null);
                    LinearLayout parentLayout = classCardView.findViewById(R.id.parent_grade_layout);
                    TextView classNameView = parentLayout.findViewById(R.id.class_name);
                    classNameView.setText(classID);

                    TextView loadingTextView = getView().findViewById(R.id.loadingTextView);
                    if(loadingTextView != null) {

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                loadingTextView.setVisibility(View.INVISIBLE);
                                //todo: commented thisi check
                                //((ViewGroup) loadingTextView.getParent()).removeView(loadingTextView);
                            }
                        });
                        //loadingTextView.setVisibility(View.INVISIBLE);
                        //((ViewGroup) loadingTextView.getParent()).removeView(loadingTextView);
                    }

                    int repeat = linesList.size() / 7;
                    for (int i = 0; i < repeat; i++) {
                        List<String> fooList = linesList.subList(1,6); // get values between tr /tr
                        linesList = linesList.subList(7, linesList.size()); // delete leftover tr /tr

                        Log.d("check", fooList.get(1).toLowerCase());

                        if(!fooList.get(1).contains("İLMEM")) {
                            View gradesTextLayout = LayoutInflater.from(getContext()).inflate(R.layout.grades_card_layout_view, null);
                            TextView examName = gradesTextLayout.findViewById(R.id.exam_name);
                            TextView date = gradesTextLayout.findViewById(R.id.date);
                            TextView firstGrade = gradesTextLayout.findViewById(R.id.first_grade);
                            TextView secondGrade = gradesTextLayout.findViewById(R.id.second_grade);
                            TextView thirdGrade = gradesTextLayout.findViewById(R.id.third_grade);

                            examName.setText(fooList.get(0).replaceAll("\\<.*?\\>", ""));
                            date.setText(fooList.get(1).replaceAll("\\<.*?\\>", ""));
                            firstGrade.setText(fooList.get(2).replaceAll("\\<.*?\\>", ""));
                            secondGrade.setText(fooList.get(3).replaceAll("\\<.*?\\>", ""));
                            thirdGrade.setText(fooList.get(4).replaceAll("\\<.*?\\>", ""));

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    parentLayout.addView(gradesTextLayout);
                                }
                            });
                            //parentLayout.addView(gradesTextLayout);
                        }
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            gradesFragmentLinearLayout.addView(classCardView);
                        }
                    });
                    //gradesFragmentLinearLayout.addView(classCardView);
                }
            }
        });
    }
}