package com.salih.sakainotifier.utilities;

import static android.content.Context.MODE_PRIVATE;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.salih.sakainotifier.MainActivity;
import com.salih.sakainotifier.MyApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.Normalizer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utility {
    private static Toast mToast = null;
    private static String m_Text = "";
    private static final Context myContext = MyApplication.getAppContext();

    public static String formatNameForDebis(String text) {
        Log.d("language_test_first", text);
        text = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");

        Log.d("beforereplace", text);
        text = text.toLowerCase();
        Log.d("after_lower", text);
        text = text.replaceAll("ü", "u");
        text = text.replaceAll("ç", "c");
        text = text.replaceAll("ı", "i");
        text = text.replaceAll("ş", "s");
        text = text.replaceAll("ö", "o");
        text = text.replaceAll("ğ", "g");

        Log.d("afterreplace", text);

        int lastSpace = text.lastIndexOf(" ");
        text = text.substring(0, lastSpace) + "." + text.substring(lastSpace+1);
        text = text.replaceAll("\\s+","");

        Log.d("language_test_second", text);
        return text;
    }

    public static void downloadTranscript() {
        SharedPreferences sharedPreferences = MyApplication.getAppContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String pass = sharedPreferences.getString("password", "");
        String name = sharedPreferences.getString("studentName", "");
        String debisPass = sharedPreferences.getString("debisPass", pass);
        name = Utility.formatNameForDebis(name);
        Log.d("transcriptError", name);

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        final OkHttpClient client = new OkHttpClient.Builder()
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

    private static void loginCall(OkHttpClient client, okhttp3.RequestBody requestBody, @Nullable String debisPass) {
        SharedPreferences sharedPreferences = myContext.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
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
                Log.d("transcript", "loginCall fail");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Log.d("loginCall_response_size", String.valueOf(responseBodyString.length()));

                if(responseBodyString.contains("isim.soyisim@ogr.deu.edu.tr")) {
                    Log.d("transcript", "loginCall login fail");

                    new Handler(Looper.getMainLooper()).post(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
                        builder.setTitle("DEBIS Şifreniz:");

                        final EditText input = new EditText(myContext);
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        builder.setView(input);

                        builder.setPositiveButton("Devam et", (dialog, which) -> {
                            m_Text = input.getText().toString();
                            SharedPreferences sharedPreferences1 = myContext.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                            String name1 = sharedPreferences1.getString("studentName", "");
                            name1 = Utility.formatNameForDebis(name1);

                            RequestBody loginBodyRetry = new FormBody.Builder()
                                    .add("username", name1)
                                    .add("password", m_Text)
                                    .add("emailHost", "ogr.deu.edu.tr")
                                    .add("tamam", "Gönder")
                                    .build();

                            loginCall(client, loginBodyRetry, m_Text);
                        });
                        builder.setNegativeButton("Vazgeç", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                    });
                } else {
                    SharedPreferences sharedPreferences = myContext.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();

                    if(debisPass != null) {
                        myEdit.putString("debisPass", debisPass);
                        myEdit.apply();
                    }

                    transcriptPageCall(client);
                }
            }
        });
    }
    private static void transcriptPageCall(OkHttpClient client) {
        okhttp3.Request getSemesterRequest = new okhttp3.Request.Builder()
                .url("https://debis.deu.edu.tr/OgrenciIsleri/Ogrenci/transcript/index.php")
                .build();

        Call call = client.newCall(getSemesterRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
                Log.d("transcript", "transcriptPageCall failure");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                String responseBodyString = response.body().string();
                Log.d("trscpPageCall_rsp_size", String.valueOf(responseBodyString.length()));

                if(responseBodyString.contains("isim.soyisim@ogr.deu.edu.tr")) {
                    // something is wrong.
                    Log.d("transcript", "transcriptPageCall fail");
                } else {

                    // download transcript
                    transcriptDownloadCall(client);
                }
            }
        });
    }
    private static void transcriptDownloadCall(OkHttpClient client) throws IOException {
        okhttp3.Request getSemesterRequest = new okhttp3.Request.Builder()
                .url("https://debis.deu.edu.tr/OgrenciIsleri/Rapor/ogrenci_bazli_listeler/en_yeni_transcript/transcript.php")
                .build();

        Call call = client.newCall(getSemesterRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // do nothing for now...
                Log.d("transcript", "transcriptDownloadCall failure");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // removed login check for now, because response.body.string and response.body.bytes can not be used at the same time
                try {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        //showShortToast("İndiriliyor...");
                        Toast.makeText(myContext.getApplicationContext(), "İndiriliyor...", Toast.LENGTH_SHORT).show();
                    });

                    // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" +
                    File transcriptFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "transcript.pdf");
                    FileOutputStream fos = new FileOutputStream(transcriptFile);
                    fos.write(response.body().bytes());
                    fos.close();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        showShortToast("Hazır!");
                    });

                } catch (Exception e) {
                    Log.d("download_trying", "failed");
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        showShortToast(e.getMessage());
                    });
                }
            }
        });
    }

    public static void logout() {
        //Context myContext = MyApplication.getAppContext();
        SharedPreferences sharedPreferences =  myContext.getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.clear();
        myEdit.apply();

        File file = new File(myContext.getFilesDir(),"alerts.txt");
        file.delete();

        Intent loggedInIntent = new Intent(myContext, MainActivity.class);
        loggedInIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myContext.startActivity(loggedInIntent);
    }

    public static void contact() {
        //Context myContext = MyApplication.getAppContext();
        Uri uri = Uri.parse("https://www.instagram.com/salihasimov/");
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

        likeIng.setPackage("com.instagram.android");

        try {
            myContext.startActivity(likeIng);
        } catch (ActivityNotFoundException e) {
            myContext.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.instagram.com/salihasimov/")));
        }
    }

    public static void showShortToast(String message) {
        //mToast = Toast.makeText(myContext, "", Toast.LENGTH_SHORT);

        if(mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(myContext, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void writeToFile(String data, @NonNull Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("alerts.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String readFromFile(@NonNull Context context) {
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
            Log.e("read file", "File not found: " + e.toString());
            return "";
        } catch (IOException e) {
            Log.e("read file", "Can not read file: " + e.toString());
        }
        return ret;
    }
}
