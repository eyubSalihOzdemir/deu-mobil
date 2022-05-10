package com.salih.sakainotifier.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.salih.sakainotifier.worker.GetAlertsWorker;
import com.salih.sakainotifier.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AlertsFragment extends Fragment {

    public AlertsFragment() {
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
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String alerts = readFromFile(getContext());

        Handler handler = new Handler();

        //todo: do not show this, instead show a loading animation in a fragment component
        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Bildirimler kontrol ediliyor...");
        pd.setCanceledOnTouchOutside(false);
        //pd.show();

        //todo: if app fails to get the alerts on the first time, it won't try to get them again on following launchs
        // because on down here: it only tries to read the saved alerts
        // if there is no saved alert, it doesn't do anything to fix it

        //todo: check if it was succesfull to read the file or not, if not run setWork() (migrated from GetAlertsWorker)

        final Runnable r = () -> {
            try {
                JSONArray savedAlertsJsonArray = new JSONArray(alerts);
                //todo: try to fix that issue with this. maybe if its null, then it means it couldnt read the file

                LinearLayout parentLayout = getView().findViewById(R.id.parentLayout);

                // create new cardview for every alert
                for (int i = savedAlertsJsonArray.length()-1; i >= 0; i--) {
                    JSONObject obj = (JSONObject) savedAlertsJsonArray.get(i);

                    View cardView = LayoutInflater.from(getContext()).inflate(R.layout.card_view, null);
                    TextView siteTitle = cardView.findViewById(R.id.site_title);
                    TextView alertExplanation = cardView.findViewById(R.id.alert_explanation);
                    TextView classTeacher = cardView.findViewById(R.id.class_teacher);
                    TextView alertTime = cardView.findViewById(R.id.alert_time);

                    siteTitle.setText(obj.getString("siteTitle"));
                    alertExplanation.setText(obj.getString("title"));
                    classTeacher.setText(obj.getString("fromDisplayName"));

                    String alertCalendar = epochToLocal(obj);
                    alertTime.setText(alertCalendar);

                    String url = obj.getString("url");
                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view1) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                        }
                    });

                    parentLayout.addView(cardView);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            //handler.postDelayed(this, 1000);

            pd.dismiss();
        };

        handler.postDelayed(r, 1000);
    }

    private String epochToLocal(JSONObject obj) throws JSONException {
        String epoch = obj.getJSONObject("eventDate").getString("epochSecond");

        Date alertDate = new Date(Long.parseLong(epoch) * 1000); // epoch is in seconds format, so multiply by 1000 is necessary
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm");

        Calendar c = Calendar.getInstance();
        c.setTime(alertDate);
        c.add(Calendar.HOUR, 3);

        return dateFormat.format(c.getTime());
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
            Log.e("read file", "File not found: " + e.toString());
            return "";
        } catch (IOException e) {
            Log.e("read file", "Can not read file: " + e.toString());
        }
        return ret;
    }

    public void setWork() {
        WorkManager.getInstance(getContext()).cancelAllWork();

        PeriodicWorkRequest alertsRequest =
                new PeriodicWorkRequest.Builder(GetAlertsWorker.class, 15, TimeUnit.MINUTES) // ,PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
                        //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // makes the app crash on startup
                        //.setInitialDelay(10, TimeUnit.SECONDS)
                        .build();

        WorkManager.getInstance(getContext()).enqueueUniquePeriodicWork("alertRequest", ExistingPeriodicWorkPolicy.REPLACE, alertsRequest);
    }
}