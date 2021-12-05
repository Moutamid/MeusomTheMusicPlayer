package com.moutamid.meusom;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "MainActivity2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Context context = MainActivity2.this;

        Toast.makeText(context, "Broadcast Listened", Toast.LENGTH_SHORT).show();
        if (context == null) {
            Log.i("onReceive: ", "Context is null");
            Toast.makeText(context, "Context is null", Toast.LENGTH_SHORT).show();
            return;
        }

        String action = intent.getAction();
        String type = intent.getType();

        String url = "No data received!";

        if ("android.intent.action.SEND".equals(action) && "text/plain".equals(type)) {

            url = intent.getStringExtra("android.intent.extra.TEXT");
        }

//        Toast.makeText(context, url, Toast.LENGTH_SHORT).show();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(Worker.class)
//                .setConstraints(constraints)
                .build();

        WorkManager.getInstance().enqueue(oneTimeWorkRequest);

        finish();

    }

}