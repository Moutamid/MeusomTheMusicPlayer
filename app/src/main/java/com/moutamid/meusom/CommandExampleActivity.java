package com.moutamid.meusom;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.bumptech.glide.Glide.with;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.DATA;
import static com.moutamid.meusom.R.color.darkerGrey;
import static com.moutamid.meusom.R.color.darkgray;
import static com.moutamid.meusom.R.color.lighterGrey;


public class CommandExampleActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CommandExampleActivity";
    private Context context = CommandExampleActivity.this;

    private Utils utils = new Utils();

    private Button btnRunCommand;
    private EditText etCommand;
    private ProgressBar progressBar;
    private TextView tvCommandStatus;
    private TextView tvCommandOutput;
    private ProgressBar pbLoading;

    private boolean running = false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onBackPressed() {
        if (!running)
            super.onBackPressed();
        else
            Toast.makeText(context, "Don't close activity before completing your download!", Toast.LENGTH_SHORT).show();
    }

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

//    private String YTUrl;

    private boolean isIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
            utils.changeLanguage(context, "en");
        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
            utils.changeLanguage(context, "pr");
        }
        setContentView(R.layout.activity_command_example);

        if (getIntent().hasExtra(Constants.URL)) {
            songModel.setSongYTUrl(getIntent().getStringExtra(Constants.URL));
            songModel.setSongName(getIntent().getStringExtra(Constants.SONG_NAME));
            songModel.setSongAlbumName(getIntent().getStringExtra(Constants.SONG_ALBUM_NAME));
            songModel.setSongCoverUrl(getIntent().getStringExtra(Constants.SONG_COVER_URL));
            isIntent = getIntent().getBooleanExtra(Constants.FROM_INTENT, false);

            databaseReference.child(Constants.SONGS)
                    .child(auth.getCurrentUser().getUid()).push()
                    .setValue(songModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
//                    Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show();
                }
            });
        }

        initViews();
        initListeners();

        initRecyclerView();
    }

    private ArrayList<SongModel> songModelArrayList = new ArrayList<>();

    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private void initRecyclerView() {

        conversationRecyclerView = findViewById(R.id.downloadRecyclerView);
        conversationRecyclerView.addItemDecoration(new DividerItemDecoration(conversationRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);

        conversationRecyclerView.setLayoutManager(linearLayoutManager);
        conversationRecyclerView.setHasFixedSize(true);
        conversationRecyclerView.setNestedScrollingEnabled(false);

        databaseReference.child(Constants.SONGS)
                .child(auth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }

                        songModelArrayList.clear();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                            SongModel songModel1 = dataSnapshot.getValue(SongModel.class);
                            songModel1.setSongPushKey(dataSnapshot.getKey());
                            songModelArrayList.add(songModel1);

                        }

//                        Toast.makeText(context, songModelArrayList.size() + "", Toast.LENGTH_SHORT).show();

                        adapter = new RecyclerViewAdapterMessages();
                        conversationRecyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CommandExampleActivity.this, error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private String currentDownloadName = "null";
    private String currentDownloadUrl = "null";


    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_download_item, parent, false);
            return new ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRightMessage holder, int position) {

            SongModel model = songModelArrayList.get(position);

            holder.songName.setText(model.getSongName());
            holder.songAlbumName.setText(model.getSongAlbumName());

            with(context)
                    .asBitmap()
                    .load(model.getSongCoverUrl())
                    .apply(new RequestOptions()
                            .placeholder(darkgray)
                            .error(darkerGrey)
                    )
                    .diskCacheStrategy(DATA)
                    .into(holder.songCoverImage);

            if (utils.fileExists(model.getSongName())) {
                holder.downloadStatus.setText(Constants.COMPLETED);
                holder.downloadButton.setImageResource(R.drawable.off_track);
            } else {
                holder.downloadStatus.setText(Constants.NOT_DOWNLOADED);
                holder.downloadButton.setImageResource(R.drawable.donwloadtrack);
            }

            holder.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (holder.downloadStatus.getText()
                            .toString().equals(Constants.COMPLETED)) {
                        Toast.makeText(context, "This song is already available offline!", Toast.LENGTH_SHORT).show();
//                        showDeleteDialog(model.getSongName() + ".mp3");
                    } else {

//                    Toast.makeText(context, model.getSongYTUrl(), Toast.LENGTH_SHORT).show();
                        runCommand(model.getSongYTUrl(), holder, model.getSongPushKey());

                    }
                }
            });

            holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteDialog(model);
                }
            });

            // AUTO DOWNLOADING SONG
            if (isIntent && position == songModelArrayList.size() - 1) {
                if (!holder.downloadStatus.getText()
                        .toString().equals(Constants.COMPLETED)) {
                    runCommand(model.getSongYTUrl(), holder, model.getSongPushKey());
                    currentDownloadName = model.getSongName();
                    currentDownloadUrl = model.getSongCoverUrl();
                }
            }

        }

        private void showDeleteDialog(SongModel model) {
            utils.showDialog(context,
                    "Are you sure?",
                    "Do you want to delete this file?",
                    "Yes",
                    "No",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            databaseReference.child(Constants.SONGS)
                                    .child(auth.getCurrentUser().getUid())
                                    .child(model.getSongPushKey())
                                    .removeValue();

                            File fdelete = new File(utils.getSongPath(model.getSongName()));
                            if (fdelete.exists()) {
                                if (fdelete.delete()) {
                                    Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
                                }
                            }

                            dialogInterface.dismiss();
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }, true);
        }

//        private boolean fileExists(String name) {
//            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +
//                    File.separator + "Meusom." + File.separator
//                    + name;
////                    + "Shot on iPhone mem.mp3";
//
//            File file = new File(path);
//            return file.exists();
//
//        }

        @Override
        public int getItemCount() {
            if (songModelArrayList == null)
                return 0;
            return songModelArrayList.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {

            ImageView downloadButton, songCoverImage, deleteBtn;
            TextView songName, songAlbumName, downloadStatus;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                downloadButton = v.findViewById(R.id.downloadBtnCommand);
                songCoverImage = v.findViewById(R.id.song_cover_Command);
                songName = v.findViewById(R.id.song_nameCommand);
                songAlbumName = v.findViewById(R.id.song_albumCommand);
                downloadStatus = v.findViewById(R.id.download_statusCommand);
                deleteBtn = v.findViewById(R.id.deleteBtnCommand);

            }
        }

    }

    private void initViews() {
        btnRunCommand = findViewById(R.id.btn_run_command);
        etCommand = findViewById(R.id.et_command);
        progressBar = findViewById(R.id.progress_bar);
//        tvCommandStatus = findViewById(R.id.tv_status);
        pbLoading = findViewById(R.id.pb_status);
        tvCommandOutput = findViewById(R.id.tv_command_output);

        findViewById(R.id.backBtnDownloadCommand).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initListeners() {
        btnRunCommand.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_run_command: {
//                runCommand();
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInBackground = true;
    }

    private boolean isInBackground = false;

    private void runCommand(String songYTUrll,
                            RecyclerViewAdapterMessages.ViewHolderRightMessage holder,
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
                    Toast.makeText(CommandExampleActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                    Toast.makeText(CommandExampleActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    running = false;
                });
        compositeDisposable.add(disposable);

    }

    private void extractNewNameAndUpload(String outputStr,
                                         RecyclerViewAdapterMessages.
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

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void showStart() {
        tvCommandStatus.setText(getString(R.string.command_start));
        progressBar.setProgress(0);
        pbLoading.setVisibility(View.VISIBLE);

    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }
}
