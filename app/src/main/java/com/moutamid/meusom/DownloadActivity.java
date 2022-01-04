package com.moutamid.meusom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadActivity extends AppCompatActivity {
    private static final String TAG = "DownloadActivity";
    private Context context = DownloadActivity.this;
    private Utils utils = new Utils();

    private boolean isIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
            utils.changeLanguage(context, "en");
        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
            utils.changeLanguage(context, "pr");
        }
        setContentView(R.layout.activity_download);

        editText = findViewById(R.id.edittextdownload);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if ("android.intent.action.SEND".equals(action) && "text/plain".equals(type)) {
            editText.setText(intent.getStringExtra("android.intent.extra.TEXT"));
            isIntent = true;
            executeDownloadTask();
        }

        findViewById(R.id.backBtnDownload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.gotoCommandActivityBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DownloadActivity.this, CommandExampleActivity.class);
                startActivity(intent);
            }
        });

        setAddTaskButton();
    }

    private EditText editText;

    private void setAddTaskButton() {
        findViewById(R.id.ajgh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                executeDownloadTask();
            }
        });
    }

    String url;

    private void executeDownloadTask() {
        url = editText.getText().toString().trim();

        if (TextUtils.isEmpty(url)) {
            editText.setError("Please enter a url!");
            return;
        }

        if (TextUtils.isEmpty(getVideoId(url))) {
            editText.setError("Wrong url!");
        } else {
//            GetSongMetaData getSongMetaData = new GetSongMetaData();
//            getSongMetaData.setUrl(url);
            new GetSongMetaData().execute();

//            new GetSongMetaData().setUrl("");

//                    Toast.makeText(DownloadActivity.this, getVideoId(url), Toast.LENGTH_SHORT).show();

        }
    }

    private class GetSongMetaData extends AsyncTask<String, Void, String> {

        //        private String url;
        private String error = null;
        private String songName, songAlbumName, songCoverUrl;
        private ProgressDialog progressDialog;


//        public void setUrl(String url) {
//            this.url = url;
//        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please wait...");
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
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    e.printStackTrace();
                    error = e.getMessage();
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                error = "Couldn't get json from server.";
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressDialog.dismiss();

            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(DownloadActivity.this, CommandExampleActivity.class);
            intent.putExtra(Constants.URL, getVideoId(url));
            intent.putExtra(Constants.SONG_NAME, songName);
            intent.putExtra(Constants.SONG_ALBUM_NAME, songAlbumName);
            intent.putExtra(Constants.SONG_COVER_URL, songCoverUrl);
            intent.putExtra(Constants.FROM_INTENT, isIntent);

            editText.setText("");

            startActivity(intent);

        }
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

}