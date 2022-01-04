package com.moutamid.meusom;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class YourService extends Service {
    public int counter = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    private DatabaseReference databaseReference = getInstance().getReference();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private String songName;

    private String urlYT, pushKeyYT;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        songName = intent.getStringExtra(Constants.TITLE);

        urlYT = intent.getStringExtra(Constants.YT_URL);

        pushKeyYT = intent.getStringExtra(Constants.PUSH_KEY);

        runCommand(urlYT, getApplicationContext(), pushKeyYT);

        return START_STICKY;
    }

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
//                        progressBar.setProgress((int) progress);

            NotificationHelper helper = new NotificationHelper(getApplicationContext());
            helper.sendDownloadingNotification(songName,
                    progress + "% (ETA " + etaInSeconds + " seconds)");

        }
    };

    private void runCommand(String songYTUrll, Context context, String songPushKey) {

        NotificationHelper helper = new NotificationHelper(context);

        if (!songYTUrll.contains("http")) {
            songYTUrll = "https://www.youtube.com/watch?v=" + songYTUrll;
        }

//        String command = "--extract-audio --audio-format mp3 -o /sdcard/Download/Meusom./%(title)s.%(ext)s " + songYTUrll;
        String command = "--extract-audio --audio-format mp3 -o " + new Utils().getPath() + "%(title)s.%(ext)s " + songYTUrll;

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

        helper.sendDownloadingNotification(songName, "loading...");

        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    helper.sendDownloadingNotification(songName, "Uploading new name...");
                    String outputStr = youtubeDLResponse.getOut();
                    extractNewNameAndUpload(helper, outputStr, songPushKey);
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e("TAG", "command failed", e);
                    stopSelf();
                });
        compositeDisposable.add(disposable);

    }

    private void extractNewNameAndUpload(NotificationHelper helper, String outputStr,
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
                .setValue(urlStr).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                completed++;
                helper.sendDownloadingNotification(songName, "Download Completed!");
                stopSelf();
            }
        });
    }

    int completed = 0;

    @Override
    public void onDestroy() {
        Log.e("TAG", "onDestroy: ");
        compositeDisposable.dispose();
        super.onDestroy();

        if (completed > 0) {
            return;
        }
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        broadcastIntent.putExtra(Constants.YT_URL, urlYT);
        broadcastIntent.putExtra(Constants.PUSH_KEY, pushKeyYT);
        broadcastIntent.putExtra(Constants.TITLE, songName);
        this.sendBroadcast(broadcastIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("onTaskRemoved: ", "called.");
        Intent restartServiceIntent = new Intent(getApplicationContext(),
                this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent =
                PendingIntent.getService(getApplicationContext(),
                        1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);
        if (completed > 0) {
            return;
        }
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        broadcastIntent.putExtra(Constants.YT_URL, urlYT);
        broadcastIntent.putExtra(Constants.PUSH_KEY, pushKeyYT);
        broadcastIntent.putExtra(Constants.TITLE, songName);
        this.sendBroadcast(broadcastIntent);

        super.onTaskRemoved(rootIntent);
    }
}
