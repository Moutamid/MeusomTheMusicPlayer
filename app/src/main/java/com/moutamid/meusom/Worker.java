package com.moutamid.meusom;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Worker extends androidx.work.Worker {
    private static final String TAG = "Worker";

    public Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private Utils utils = new Utils();

    //    private Button btnRunCommand;
//    private EditText etCommand;
//    private ProgressBar progressBar;
//    private TextView tvCommandStatus;
//    private TextView tvCommandOutput;
//    private ProgressBar pbLoading;
    private ArrayList<SongModel> songModelArrayList = new ArrayList<>();
    private String currentDownloadName = "null";
    private String currentDownloadUrl = "null";

    private boolean running = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();


    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
            runOnUiThread(() -> {
//                        progressBar.setProgress((int) progress);
                        tvCommandStatus.setText(String.valueOf(progress) + "% (ETA " + String.valueOf(etaInSeconds) + " seconds)");
//                        tvCommandStatus.setText(String.valueOf(progress) + "% (ETA " + String.valueOf(etaInSeconds) + " seconds)");
                        if (isInBackground) {
                            NotificationHelper helper = new NotificationHelper(context);
                            helper.sendDownloadingNotification(currentDownloadName,
                                    progress + "% (ETA " + etaInSeconds + " seconds)", currentDownloadUrl);
                        }
                    }
            );
        }
    };

    private SongModel songModel = new SongModel();
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    @NonNull
    @Override
    public Result doWork() {

        int count = 0;

        for (int i = 0; i <= 20000; i++) {
            count++;
        }

        BigInteger veryBig = new BigInteger(9999, 9, new Random());
        veryBig.nextProbablePrime();

        Log.e("TAG", "doWork: WORKER TRIGGERED!");


        runCommand(model.getSongYTUrl(), holder, model.getSongPushKey());

        return Result.success();
    }

    String url;

    private void executeDownloadTask() {
        /*url = editText.getText().toString().trim();

        if (TextUtils.isEmpty(url)) {
            editText.setError("Please enter a url!");
            return;
        }*/

        if (TextUtils.isEmpty(getVideoId(url))) {
//            editText.setError("Wrong url!");
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
//        private ProgressDialog progressDialog;


//        public void setUrl(String url) {
//            this.url = url;
//        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

//            progressDialog = new ProgressDialog(context);
//            progressDialog.setCancelable(false);
//            progressDialog.setMessage("Please wait...");
//            progressDialog.show();
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

//            progressDialog.dismiss();

            if (error != null) {
//                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                return;
            }

//            Intent intent = new Intent(DownloadActivity.this, CommandExampleActivity.class);
//            intent.putExtra(Constants.URL, url);
//            intent.putExtra(Constants.SONG_NAME, songName);
//            intent.putExtra(Constants.SONG_ALBUM_NAME, songAlbumName);
//            intent.putExtra(Constants.SONG_COVER_URL, songCoverUrl);
//            intent.putExtra(Constants.FROM_INTENT, isIntent);

//            editText.setText("");

//            startActivity(intent);

//            if (getIntent().hasExtra(Constants.URL)) {
            songModel.setSongYTUrl(url);
            songModel.setSongName(songName);
            songModel.setSongAlbumName(songAlbumName);
            songModel.setSongCoverUrl(songCoverUrl);


            databaseReference.child(Constants.SONGS)
                    .child(auth.getCurrentUser().getUid()).push()
                    .setValue(songModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
//                    Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show();
                }
            });
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

    private void runCommand(String songYTUrll,
                            CommandExampleActivity.RecyclerViewAdapterMessages.ViewHolderRightMessage holder,
                            String songPushKey) {

        NotificationHelper helper = new NotificationHelper(context);

        if (running) {
            Toast.makeText(CommandExampleActivity.this, "Please wait. A download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(CommandExampleActivity.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

//        String command = "--extract-audio --audio-format mp3 -o /sdcard/Download/Meusom./%(title)s.%(ext)s " + songYTUrll;
        String command = "--extract-audio --audio-format mp3 -o " + utils.getPath() + "%(title)s.%(ext)s " + songYTUrll;
//        String command = etCommand.getText().toString().trim();
        if (TextUtils.isEmpty(command)) {
            etCommand.setError(getString(R.string.command_error));
            return;
        }

        // this is not the recommended way to add options/flags/url and might break in future
        // use the constructor for url, addOption(key) for flags, addOption(key, value) for options
        YoutubeDLRequest request = new YoutubeDLRequest(Collections.emptyList());
        String commandRegex = "\"([^\"]*)\"|(\\S+)";
        Matcher m = Pattern.compile(commandRegex).matcher(command);
        while (m.find()) {
            if (m.group(1) != null) {
                request.addOption(m.group(1));
            } else {
                request.addOption(m.group(2));
            }
        }

//        showStart();

        holder.downloadStatus.setText("Starting download...");
        if (isInBackground)
            helper.sendDownloadingNotification(holder.songName.getText().toString(), "Starting download...", currentDownloadUrl);

        tvCommandStatus = holder.downloadStatus;

        running = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
//                    pbLoading.setVisibility(View.GONE);
//                    progressBar.setProgress(100);
                    tvCommandStatus.setText(Constants.COMPLETED);
                    tvCommandOutput.setText(youtubeDLResponse.getOut());
                    if (isInBackground)
                        helper.sendDownloadingNotification(holder.songName.getText().toString(), "Download Completed!", currentDownloadUrl);
                    String outputStr = youtubeDLResponse.getOut();
                    extractNewNameAndUpload(outputStr, holder, songPushKey);
                    holder.downloadButton.setImageResource(R.drawable.off_track);
                    Toast.makeText(CommandExampleActivity.this, "Download successful", Toast.LENGTH_LONG).show();
                    running = false;
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e(TAG, "command failed", e);
                    pbLoading.setVisibility(View.GONE);
                    tvCommandStatus.setText(getString(R.string.command_failed));
                    tvCommandOutput.setText(e.getMessage());
                    holder.downloadButton.setImageResource(R.drawable.donwloadtrack);
                    Toast.makeText(CommandExampleActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                    Toast.makeText(CommandExampleActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    running = false;
                });
        compositeDisposable.add(disposable);

    }

    private void extractNewNameAndUpload(String outputStr,
                                         CommandExampleActivity.RecyclerViewAdapterMessages.
                                                 ViewHolderRightMessage holder,
                                         String songPushKey) {
        Pattern urlP = Pattern.compile("Meusom./(.*?).mp3");
        Matcher urlM = urlP.matcher(outputStr);

        String urlStr = "null";

        while (urlM.find()) {
            urlStr = urlM.group(1);
        }

        databaseReference.child(Constants.SONGS)
                .child(auth.getCurrentUser().getUid())
                .child(songPushKey)
                .child(Constants.SONG_NAME)
                .setValue(urlStr);

        holder.songName.setText(urlStr);
    }

    private void runCommand(String songYTUrll,
                            CommandExampleActivity.RecyclerViewAdapterMessages.ViewHolderRightMessage holder,
                            String songPushKey) {

        NotificationHelper helper = new NotificationHelper(context);

        if (running) {
            Toast.makeText(CommandExampleActivity.this, "Please wait. A download is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isStoragePermissionGranted()) {
            Toast.makeText(CommandExampleActivity.this, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

//        String command = "--extract-audio --audio-format mp3 -o /sdcard/Download/Meusom./%(title)s.%(ext)s " + songYTUrll;
        String command = "--extract-audio --audio-format mp3 -o " + utils.getPath() + "%(title)s.%(ext)s " + songYTUrll;
//        String command = etCommand.getText().toString().trim();
        if (TextUtils.isEmpty(command)) {
            etCommand.setError(getString(R.string.command_error));
            return;
        }

        // this is not the recommended way to add options/flags/url and might break in future
        // use the constructor for url, addOption(key) for flags, addOption(key, value) for options
        YoutubeDLRequest request = new YoutubeDLRequest(Collections.emptyList());
        String commandRegex = "\"([^\"]*)\"|(\\S+)";
        Matcher m = Pattern.compile(commandRegex).matcher(command);
        while (m.find()) {
            if (m.group(1) != null) {
                request.addOption(m.group(1));
            } else {
                request.addOption(m.group(2));
            }
        }

//        showStart();

        holder.downloadStatus.setText("Starting download...");
        if (isInBackground)
            helper.sendDownloadingNotification(holder.songName.getText().toString(), "Starting download...", currentDownloadUrl);

        tvCommandStatus = holder.downloadStatus;

        running = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
//                    pbLoading.setVisibility(View.GONE);
//                    progressBar.setProgress(100);
                    tvCommandStatus.setText(Constants.COMPLETED);
                    tvCommandOutput.setText(youtubeDLResponse.getOut());
                    if (isInBackground)
                        helper.sendDownloadingNotification(holder.songName.getText().toString(), "Download Completed!", currentDownloadUrl);
                    String outputStr = youtubeDLResponse.getOut();
                    extractNewNameAndUpload(outputStr, holder, songPushKey);
                    holder.downloadButton.setImageResource(R.drawable.off_track);
                    Toast.makeText(CommandExampleActivity.this, "Download successful", Toast.LENGTH_LONG).show();
                    running = false;
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e(TAG, "command failed", e);
                    pbLoading.setVisibility(View.GONE);
                    tvCommandStatus.setText(getString(R.string.command_failed));
                    tvCommandOutput.setText(e.getMessage());
                    holder.downloadButton.setImageResource(R.drawable.donwloadtrack);
                    Toast.makeText(CommandExampleActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                    Toast.makeText(CommandExampleActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    running = false;
                });
        compositeDisposable.add(disposable);

    }

    private void extractNewNameAndUpload(String outputStr,
                                         CommandExampleActivity.RecyclerViewAdapterMessages.
                                                 ViewHolderRightMessage holder,
                                         String songPushKey) {
        Pattern urlP = Pattern.compile("Meusom./(.*?).mp3");
        Matcher urlM = urlP.matcher(outputStr);

        String urlStr = "null";

        while (urlM.find()) {
            urlStr = urlM.group(1);
        }

        databaseReference.child(Constants.SONGS)
                .child(auth.getCurrentUser().getUid())
                .child(songPushKey)
                .child(Constants.SONG_NAME)
                .setValue(urlStr);

        holder.songName.setText(urlStr);
    }

}
