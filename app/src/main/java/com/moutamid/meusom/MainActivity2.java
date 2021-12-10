package com.moutamid.meusom;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "MainActivity2";

    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Context context = MainActivity2.this;
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");

//        Toast.makeText(context, "Broadcast Listened", Toast.LENGTH_SHORT).show();
//        if (context == null) {
//            Log.i("onReceive: ", "Context is null");
//            Toast.makeText(context, "Context is null", Toast.LENGTH_SHORT).show();
//            return;
//        }

        String action = intent.getAction();
        String type = intent.getType();

        url = "No data received!";

        if ("android.intent.action.SEND".equals(action) && "text/plain".equals(type)) {
            url = intent.getStringExtra("android.intent.extra.TEXT");
        }

/*        Toast.makeText(context, url, Toast.LENGTH_SHORT).show();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(Worker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance().enqueue(oneTimeWorkRequest);*/

        if (TextUtils.isEmpty(getVideoId(url))) {
            doneLoading("INCORRECT URL");
        } else {

            new GetSongMetaData().execute();
        }

    }

    private ProgressDialog progressDialog;

    Intent mServiceIntent;
    private YourService mYourService;
    private SongModel songModel = new SongModel();

    private class GetSongMetaData extends AsyncTask<String, Void, String> {

        private String error = Constants.NULL;
        private String songName, songAlbumName, songCoverUrl;
        private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        private FirebaseAuth auth = FirebaseAuth.getInstance();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpHandler sh = new HttpHandler();

            String urlYT = "https://www.youtube.com/oembed?format=json&url=" + url;//https://www.youtube.com/watch?v=" + id;

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(urlYT);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject o = new JSONObject(jsonStr);

                    songName = o.getString("title");

                    songAlbumName = o.getString("author_name");

                    songCoverUrl = o.getString("thumbnail_url");

                    return "";

                } catch (final JSONException e) {
                    error = e.getMessage();
                }
            } else {
                error = "Couldn't get json from server.";
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (!error.equals(Constants.NULL)) {
                doneLoading(error);
                return;
            }

/*            Intent intent = new Intent(DownloadActivity.this, CommandExampleActivity.class);
            intent.putExtra(Constants.URL, url);
            intent.putExtra(Constants.SONG_NAME, songName);
            intent.putExtra(Constants.SONG_ALBUM_NAME, songAlbumName);
            intent.putExtra(Constants.SONG_COVER_URL, songCoverUrl);
            intent.putExtra(Constants.FROM_INTENT, isIntent);

            editText.setText("");

            startActivity(intent);

            if (getIntent().hasExtra(Constants.URL)) {*/


            String key = databaseReference.child(Constants.SONGS)
                    .child(auth.getCurrentUser().getUid()).push().getKey();

            songModel.setSongYTUrl(url);
            songModel.setSongName(songName);
            songModel.setSongAlbumName(songAlbumName);
            songModel.setSongCoverUrl(songCoverUrl);
            songModel.setSongPushKey(key);

            databaseReference.child(Constants.SONGS)
                    .child(auth.getCurrentUser().getUid()).child(key)
                    .setValue(songModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
//                    Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show();


                    if (task.isSuccessful())
                        startInitService(url, key, songName);

                    else doneLoading(task.getException().getMessage());

                }
            });
        }

    }

    private void startInitService(String url, String pushKey, String title) {
        mYourService = new YourService();
        mServiceIntent = new Intent(getApplicationContext(), mYourService.getClass());
        mServiceIntent.putExtra(Constants.YT_URL, url);
        mServiceIntent.putExtra(Constants.PUSH_KEY, pushKey);
        mServiceIntent.putExtra(Constants.TITLE, title);

//        if (!isMyServiceRunning(mYourService.getClass())) {
        startService(mServiceIntent);
        doneLoading("Download started");
//            return;
//        }
    }

    private void doneLoading(String e) {
        Log.e(TAG, "Json parsing error: " + e);

        if (progressDialog.isShowing())
            progressDialog.dismiss();

        Toast.makeText(getApplicationContext(), e, Toast.LENGTH_LONG).show();

        finish();
    }


    private static class HttpHandler {

        public HttpHandler() {
        }

        public String makeServiceCall(String reqUrl) {
            String response = null;
            try {
                URL url = new URL(reqUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // read the response
                InputStream in = new BufferedInputStream(conn.getInputStream());

                response = convertStreamToString(in);
            } catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURLException: " + e.getMessage());
            } catch (ProtocolException e) {
                Log.e(TAG, "ProtocolException: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
            return response;
        }

        private String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            try {

                while ((line = reader.readLine()) != null) {

                    sb.append(line).append('\n');

                }

            } catch (IOException e) {

                e.printStackTrace();

            } finally {

                try {

                    is.close();

                } catch (IOException e) {

                    e.printStackTrace();
                }
            }

            return sb.toString();
        }
    }

    private static String getVideoId(@NonNull String videoUrl) {
        String videoId = "";
        String regex = "http(?:s)?:\\/\\/(?:m.)?(?:www\\.)?youtu(?:\\.be\\/|be\\.com\\/(?:watch\\?(?:feature=youtu.be\\&)?v=|v\\/|embed\\/|user\\/(?:[\\w#]+\\/)+))([^&#?\\n]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(videoUrl);
        if (matcher.find()) {
            videoId = matcher.group(1);
        }
        return videoId;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("Service status", "Running");
                return true;
            }
        }
        Log.i("Service status", "Not running");
        return false;
    }

}