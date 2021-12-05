package com.moutamid.meusom;

import static com.bumptech.glide.Glide.with;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.DATA;
import static com.moutamid.meusom.R.color.lightBlack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import musicplayer.SongsManager;
import musicplayer.Utilities;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MainActivity";
    private Context context = MainActivity.this;

    private Utils utils = new Utils();

    private LinearLayout bottom_music_layout;
    private RelativeLayout music_player_layout;

    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

//    private boolean isAllowed = true;

    //-----------------------------------------------------------------
    private ImageView btnPlay;
    private ImageView btnPlaySmall;
    //    private ImageView btnForward;
//    private ImageView btnBackward;
    private ImageView btnNext;
    private ImageView btnNextSmall;
    private ImageView btnPrevious;
    //    private ImageView btnPlaylist;
    private ImageView btnRepeat;
    private ImageView btnShuffle;
    private SeekBar songProgressBar;
    private SeekBar songProgressBarSmall;
    private TextView songTitleLabel;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    // Media Player
    private MediaPlayer mp;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();

    //    private SongsManager songManager;
    private Utilities utilities;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds
    private int currentSongIndex = 0;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    //    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private ArrayList<SongModel> songsList = new ArrayList<>();
//------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
            utils.changeLanguage(context, "en");
        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
            utils.changeLanguage(context, "pr");
        }

        setContentView(R.layout.activity_main);

        initViewsAndLayouts();

        btnPlay = findViewById(R.id.btnPlay);
        btnPlaySmall = findViewById(R.id.playPauseBtnSmall);
//        btnForward =  findViewById(R.id.btnForward);
//        btnBackward =  findViewById(R.id.btnBackward);
        btnNext = findViewById(R.id.btnNext);
        btnNextSmall = findViewById(R.id.nextBtnSmall);
        btnPrevious = findViewById(R.id.btnPrevious);
//        btnPlaylist =  findViewById(R.id.btnPlaylist);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnShuffle = findViewById(R.id.btnShuffle);
        songProgressBar = findViewById(R.id.songProgressBar);
        songProgressBarSmall = findViewById(R.id.songProgressBar1);
        songTitleLabel = findViewById(R.id.songTitle);
        songTitleLabel.setSelected(true);
        songCurrentDurationLabel = findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = findViewById(R.id.songTotalDurationLabel);

        // Mediaplayer
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        songManager = new SongsManager();
        utilities = new Utilities();

        // Listeners
        songProgressBar.setOnSeekBarChangeListener(this); // Important
        songProgressBarSmall.setOnSeekBarChangeListener(this); // Important
        mp.setOnCompletionListener(this); // Important

        // Getting all songs list
//        getSongsList();
//        songsList = songManager.getPlayList();

        /**
         * Play button click event
         * plays a song and changes button to pause image
         * pauses a song and changes button to play image
         * */
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!isStoragePermissionGranted()) {
                    Toast.makeText(context, "grant storage permission and retry", Toast.LENGTH_LONG).show();
                    return;
                }

                if (checkIfAdsAreWatched()) {
                    return;
                }

                // check for already playing
                if (mp.isPlaying()) {
                    if (mp != null) {
                        mp.pause();
                        // Changing button image to play button
                        btnPlay.setImageResource(R.drawable.play);
                        btnPlaySmall.setImageResource(R.drawable.play);
                    }
                } else {
                    // Resume song
                    if (mp != null) {
                        mp.start();
                        // Changing button image to pause button
                        btnPlay.setImageResource(R.drawable.pause);
                        btnPlaySmall.setImageResource(R.drawable.pause);
                    }
                }

            }
        });
        btnPlaySmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!isStoragePermissionGranted()) {
                    Toast.makeText(context, "grant storage permission and retry", Toast.LENGTH_LONG).show();
                    return;
                }

                if (checkIfAdsAreWatched()) {
                    return;
                }

                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }
                // check for already playing
                if (mp.isPlaying()) {
                    if (mp != null) {
                        mp.pause();
                        // Changing button image to play button
                        btnPlay.setImageResource(R.drawable.play);
                        btnPlaySmall.setImageResource(R.drawable.play);
                    }
                } else {
                    // Resume song
                    if (mp != null) {
                        mp.start();
                        // Changing button image to pause button
                        btnPlay.setImageResource(R.drawable.pause);
                        btnPlaySmall.setImageResource(R.drawable.pause);
                    }
                }

            }
        });

        /**
         * Forward button click event
         * Forwards song specified seconds
         * */
//        btnForward.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//                // get current song position
//                int currentPosition = mp.getCurrentPosition();
//                // check if seekForward time is lesser than song duration
//                if(currentPosition + seekForwardTime <= mp.getDuration()){
//                    // forward song
//                    mp.seekTo(currentPosition + seekForwardTime);
//                }else{
//                    // forward to end position
//                    mp.seekTo(mp.getDuration());
//                }
//            }
//        });

        /**
         * Backward button click event
         * Backward song to specified seconds
         * */
//        btnBackward.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//                // get current song position
//                int currentPosition = mp.getCurrentPosition();
//                // check if seekBackward time is greater than 0 sec
//                if(currentPosition - seekBackwardTime >= 0){
//                    // forward song
//                    mp.seekTo(currentPosition - seekBackwardTime);
//                }else{
//                    // backward to starting position
//                    mp.seekTo(0);
//                }
//
//            }
//        });

        /**
         * Next button click event
         * Plays next song by taking currentSongIndex + 1
         * */
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkIfAdsAreWatched()) {
                    return;
                }

                if (currentSongIndex > 0) {
                    playSong(currentSongIndex - 1);
                    currentSongIndex = currentSongIndex - 1;
                } else {
                    // play last song
                    playSong(songsList.size() - 1);
                    currentSongIndex = songsList.size() - 1;
                }

            }
        });
        btnNextSmall.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkIfAdsAreWatched()) {
                    return;
                }

                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }

                if (currentSongIndex > 0) {
                    playSong(currentSongIndex - 1);
                    currentSongIndex = currentSongIndex - 1;
                } else {
                    // play last song
                    playSong(songsList.size() - 1);
                    currentSongIndex = songsList.size() - 1;
                }

            }
        });

        /**
         * Back button click event
         * Plays previous song by currentSongIndex - 1
         * */
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkIfAdsAreWatched()) {
                    return;
                }

                // check if next song is there or not
                if (currentSongIndex < (songsList.size() - 1)) {
                    playSong(currentSongIndex + 1);
                    currentSongIndex = currentSongIndex + 1;
                } else {
                    // play first song
                    playSong(0);
                    currentSongIndex = 0;
                }
            }
        });

        /**
         * Button Click event for Repeat button
         * Enables repeat flag to true
         * */
        btnRepeat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (isRepeat) {
                    isRepeat = false;
                    Toast.makeText(getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                    btnRepeat.setImageResource(R.drawable.repeat_off);
                } else {
                    // make repeat to true
                    isRepeat = true;
                    Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isShuffle = false;
                    btnRepeat.setImageResource(R.drawable.repeat_current_track);
                    btnShuffle.setImageResource(R.drawable.radom_off);
                }
            }
        });

        /**
         * Button Click event for Shuffle button
         * Enables shuffle flag to true
         * */
        btnShuffle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (isShuffle) {
                    isShuffle = false;
                    Toast.makeText(getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                    btnShuffle.setImageResource(R.drawable.radom_off);
                } else {
                    // make repeat to true
                    isShuffle = true;
                    Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    isRepeat = false;
                    btnShuffle.setImageResource(R.drawable.radom_on);
                    btnRepeat.setImageResource(R.drawable.repeat_off);
                }
            }
        });

        /**
         * Button Click event for Play list click event
         * Launches list activity which displays list of songs
         * */
//        btnPlaylist.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//                Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
//                startActivityForResult(i, 100);
//            }
//        });

        findViewById(R.id.queueBtnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(MainActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_queue);
                dialog.setCancelable(true);
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(dialog.getWindow().getAttributes());
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

                initRecyclerView(dialog);

                dialog.findViewById(R.id.downMusicBtnQueue).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // CODE HERE
                        dialog.dismiss();
                    }
                });

                dialog.show();
                dialog.getWindow().setAttributes(layoutParams);
            }
        });

        // INIT VOLUME SEEKBAR
        initVolumeSeekbar();

    }

    private void initVolumeSeekbar() {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        SeekBar volControl = (SeekBar)findViewById(R.id.volumeSeekbar);
        volControl.setMax(maxVolume);
        volControl.setProgress(curVolume);
        volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
            }
        });
    }

    //-----------------------------------------------------

    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private void initRecyclerView(Dialog dialog) {

        conversationRecyclerView = dialog.findViewById(R.id.queueRecyclerView);
        //conversationRecyclerView.addItemDecoration(new DividerItemDecoration(conversationRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        adapter = new RecyclerViewAdapterMessages();
        //        LinearLayoutManager layoutManagerUserFriends = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        //    int numberOfColumns = 3;
        //int mNoOfColumns = calculateNoOfColumns(getApplicationContext(), 50);
        //  recyclerView.setLayoutManager(new GridLayoutManager(this, mNoOfColumns));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        conversationRecyclerView.setLayoutManager(linearLayoutManager);
        conversationRecyclerView.setHasFixedSize(true);
        conversationRecyclerView.setNestedScrollingEnabled(false);

        conversationRecyclerView.setAdapter(adapter);

        //    if (adapter.getItemCount() != 0) {

        //        noChatsLayout.setVisibility(View.GONE);
        //        chatsRecyclerView.setVisibility(View.VISIBLE);

        //    }

    }

    /*public static int calculateNoOfColumns(Context context, float columnWidthDp) { // For example columnWidthdp=180
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (screenWidthDp / columnWidthDp + 0.5); // +0.5 for correct rounding to int.
        return noOfColumns;
    }*/

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_queue_items, parent, false);
            return new ViewHolderRightMessage(view);
        }

        RelativeLayout prevLayout;

        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRightMessage holder, int position1) {

            int position = holder.getAdapterPosition();

            holder.title.setText(songsList.get(position).getSongName());

            if (position == currentSongIndex) {
                prevLayout = holder.parentLayout;
                prevLayout.setBackgroundColor(getResources().getColor(R.color.lightBlack));
            } else {
                holder.parentLayout.setBackgroundColor(getResources().getColor(R.color.black));
            }

            holder.parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (prevLayout != null) {
                        setBgColor(R.color.black);
                    }
                    prevLayout = holder.parentLayout;
                    setBgColor(R.color.lightBlack);

                    // CODE HERE

                    currentSongIndex = position;
                    playSong(position);
                }

                private void setBgColor(int p) {
                    prevLayout.setBackgroundColor(getResources().getColor(p));
                }

            });

        }

        @Override
        public int getItemCount() {
            if (songsList == null)
                return 0;
            return songsList.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {

            TextView title;
            RelativeLayout parentLayout;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.songNameDialogQueueHome);
                parentLayout = v.findViewById(R.id.parentLayoutQueueItem);

            }
        }

    }

    //-----------------------------------------------------
    private void getSongsList() {
        databaseReference.child(Constants.SONGS)
                .child(auth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }

                        songsList.clear();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                            SongModel songModel1 = dataSnapshot.getValue(SongModel.class);

                            if (utils.fileExists(songModel1.getSongName())) {
                                songModel1.setSongPushKey(dataSnapshot.getKey());
                                songsList.add(songModel1);
                            }

                        }

                        // PLAYLIST LOADED
                        // By default play first song
                        currentSongIndex = utils.getStoredInteger(MainActivity.this, Constants.LAST_SONG_INDEX);
                        playSong(currentSongIndex);

                        // check for already playing
                        if (mp.isPlaying()) {
                            if (mp != null) {
                                mp.pause();
                                // Changing button image to play button
                                btnPlay.setImageResource(R.drawable.play);
                                btnPlaySmall.setImageResource(R.drawable.play);
                            }
                        }

//                        Toast.makeText(context, "Playlist loaded! You can now tap on the play button to play your music!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private boolean fileExists(String name) {
//        String path = Environment
//                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                + File.separator
//                + "Meusom."
//                + File.separator
//                + name
//                + ".mp3";
//
//        File file = new File(path);
//        return file.exists();
//
//    }

    /**
     * Receiving song index from playlist view
     * and play the song
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE && resultCode == RESULT_OK) {
            currentSongIndex = data.getIntExtra(Constants.SONG_INDEX, 0);
            playSong(currentSongIndex);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (utils.getStoredBoolean(context, Constants.IS_PLAYLIST)) {
            String playListName = utils.getStoredString(context, Constants.NAME);

            getPlaylist(playListName);
        } else {
            getSongsList();
        }
    }

    private void getPlaylist(String playListName) {
//        Toast.makeText(context, playListName, Toast.LENGTH_SHORT).show();
        databaseReference.child(Constants.PLAYLIST)
                .child(auth.getCurrentUser().getUid())
                .child(playListName)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(context, "Data doesn't exist!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        songsList.clear();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                            SongModel songModel1 = dataSnapshot.getValue(SongModel.class);

                            if (utils.fileExists(songModel1.getSongName())) {
                                songModel1.setSongPushKey(dataSnapshot.getKey());
                                songsList.add(songModel1);
                            }

                        }

//                        Toast.makeText(context, "Playlist loaded.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: " + error.toException().getMessage());
                        Toast.makeText(context, error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private String getSongPath1(int songIndex, String name) {
//        String path = Environment
//                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//                + File.separator
//                + "Meusom."
//                + File.separator
//                + name
//                + ".mp3";
//
////        File file = new File(path);
////        if (!file.exists()) {
////            Toast.makeText(context, "if (!file.exists()) {", Toast.LENGTH_SHORT).show();
////            path = songsList.get(songIndex).getSongYTUrl();
////            Toast.makeText(context, path, Toast.LENGTH_SHORT).show();
////        }
//
//        return path;
//    }

    /**
     * Function to play a song
     *
     * @param songIndex - index of song
     */
    public void playSong(int songIndex) {

        if (!isStoragePermissionGranted()) {
            Toast.makeText(context, "grant storage permission and retry", Toast.LENGTH_LONG).show();
            return;
        }

        if (checkIfAdsAreWatched()) {
            return;
        }

        if (songIndex >= songsList.size()) {
            Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        utils.storeInteger(MainActivity.this, Constants.LAST_SONG_INDEX, songIndex);

        SongModel currentSongModel = songsList.get(songIndex);
        try {
            mp.reset();
            mp.setDataSource(utils.getSongPath(currentSongModel.getSongName()));
            mp.prepare();
            mp.start();

            // TITLE BIG PLAYER
            String songTitle = currentSongModel.getSongName();
            songTitleLabel.setText(songTitle);

            //TITLE SMALL PLAYER
            TextView titleSmall = findViewById(R.id.title_small_playerHome);
//            titleSmall.setSelected(true);
            titleSmall.setText(songTitle);

            // ALBUM NAME SMALL PLAYER
            TextView albumName = findViewById(R.id.albumNameHome);
//            albumName.setSelected(true);
            albumName.setText(currentSongModel.getSongAlbumName());

            // COVER IMAGE BIG PLAYER
            with(context)
                    .asBitmap()
                    .load(currentSongModel.getSongCoverUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.color.lightBlack)
                            .error(lightBlack)
                    )
                    .diskCacheStrategy(DATA)
                    .into((ImageView) findViewById(R.id.songCoverImage));

            // COVER IMAGE SMALL PLAYER
            with(context)
                    .asBitmap()
                    .load(currentSongModel.getSongCoverUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.color.lightBlack)
                            .error(lightBlack)
                    )
                    .diskCacheStrategy(DATA)
                    .into((ImageView) findViewById(R.id.current_music_player_image_view));


            // Changing Button Image to pause image
            btnPlay.setImageResource(R.drawable.pause);
            btnPlaySmall.setImageResource(R.drawable.pause);

            // set Progress bar values
            songProgressBar.setProgress(0);
            songProgressBarSmall.setProgress(0);
            songProgressBar.setMax(100);
            songProgressBarSmall.setMax(100);

            // Updating progress bar
            updateProgressBar();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private boolean checkIfAdsAreWatched() {
        //TODO: THESE BELOW LINES SHOULD NOT BE COMMENTED
//        if (utils.getAdsInteger(context, utils.getLastSunday()) > 0) {
//            Toast.makeText(context, "Please watch ads first!", Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(context, AdvertisementACtivity.class));
//            return true;
//        }
//
//        if (utils.isTodaySunday()) {
//
//            if (utils.getAdsInteger(context, utils.getDate()) > 0) {
//                Toast.makeText(context, "Please watch ads first!", Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(context, AdvertisementACtivity.class));
//                return true;
//            }
//
//        }

        return false;
    }

    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mp.getDuration();
            long currentDuration = mp.getCurrentPosition();

            TextView currentDurationTv = findViewById(R.id.currentDurationSmallPlayer);
            TextView totalDurationTv = findViewById(R.id.totalDurationSmallPlayer);

            // Displaying Total Duration time
            songTotalDurationLabel.setText("" + utilities.milliSecondsToTimer(totalDuration));
            totalDurationTv.setText("" + utilities.milliSecondsToTimer(totalDuration));

            // Displaying time completed playing
            songCurrentDurationLabel.setText("" + utilities.milliSecondsToTimer(currentDuration));
            currentDurationTv.setText("" + utilities.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int) (utilities.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            songProgressBar.setProgress(progress);
            songProgressBarSmall.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    /**
     * When user stops moving the progress hanlder
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mp.getDuration();
        int currentPosition = utilities.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mp.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    /**
     * On Song Playing completed
     * if repeat is ON play same song again
     * if shuffle is ON play random song
     */
    @Override
    public void onCompletion(MediaPlayer arg0) {

        // check for repeat is ON or OFF
        if (isRepeat) {
            // repeat is on play same song again
            playSong(currentSongIndex);
        } else if (isShuffle) {
            // shuffle is on - play a random song
            Random rand = new Random();
            currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
            playSong(currentSongIndex);
        } else {
            // no repeat or shuffle ON - play next song
            /*if (currentSongIndex < (songsList.size() - 1)) {
                playSong(currentSongIndex + 1);
                currentSongIndex = currentSongIndex + 1;
            } else {
                // play first song
                playSong(0);
                currentSongIndex = 0;
            }*/

            if (currentSongIndex > 0) {
                playSong(currentSongIndex - 1);
                currentSongIndex = currentSongIndex - 1;
            } else {
                // play last song
                playSong(songsList.size() - 1);
                currentSongIndex = songsList.size() - 1;
            }

        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mp.release();
    }

    //--------------------------------------------------------------------------------
    private LinearLayout buttonsLayout;

    private void initViewsAndLayouts() {
        databaseReference.keepSynced(true);

        bottom_music_layout = findViewById(R.id.bottom_music_layout);
        music_player_layout = findViewById(R.id.music_player_layout);

        buttonsLayout = findViewById(R.id.buttonsLayoutHome);

        findViewById(R.id.title_small_playerHome).setSelected(true);
        findViewById(R.id.albumNameHome).setSelected(true);

        bottom_music_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!slideUpB)
                    slideUp();
            }
        });

        findViewById(R.id.downMusicLayoutBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slideDown();
            }
        });

        findViewById(R.id.downloadBtnHomeScreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }
                startActivity(new Intent(MainActivity.this, DownloadActivity.class));
            }
        });

        findViewById(R.id.settingsButtonHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        findViewById(R.id.advertisementBtnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }
                startActivity(new Intent(MainActivity.this, AdvertisementACtivity.class));
            }
        });

        findViewById(R.id.mySoundBtnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }
                startActivityForResult(new Intent(MainActivity.this, MySoundActivity.class), Constants.REQUEST_CODE);
            }
        });

        findViewById(R.id.exitButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //LARGE MUSIC PLAYER IS SHOWN
                if (slideUpB) {
                    return;
                }
                finish();
            }
        });

        findViewById(R.id.equalizerBtnMain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openEqualizerSettings();
            }
        });

        //TODO: BELOW LINES SHOULD NOT BE COMMENTED
//        TextView textview = findViewById(R.id.email_Txt_home);
//        if (auth.getCurrentUser() != null)
//            textview.setText(auth.getCurrentUser().getEmail());

    }

    private void openEqualizerSettings() {
        Intent intent = new Intent(AudioEffect
                .ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if ((intent.resolveActivity(getPackageManager()) != null)) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your device does not support an equalizer!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean slideUpB = false;

    private void slideDown() {
        YoYo.with(Techniques.SlideOutDown).duration(1500).onStart(new YoYo.AnimatorCallback() {
            @Override
            public void call(Animator animator) {

            }
        }).onEnd(new YoYo.AnimatorCallback() {
            @Override
            public void call(Animator animator) {
                slideUpB = false;
                music_player_layout
                        .setVisibility(View.GONE);

                bottom_music_layout.setEnabled(true);
                buttonsLayout.setEnabled(true);
            }
        }).playOn(music_player_layout);
    }

    private void slideUp() {
        YoYo.with(Techniques.SlideInUp).duration(1500).onStart(new YoYo.AnimatorCallback() {
            @Override
            public void call(Animator animator) {
                music_player_layout
                        .setVisibility(View.VISIBLE);
            }
        }).onEnd(new YoYo.AnimatorCallback() {
            @Override
            public void call(Animator animator) {
                slideUpB = true;
                bottom_music_layout.setEnabled(false);
                buttonsLayout.setEnabled(false);

            }
        }).playOn(music_player_layout);
    }

    @Override
    public void onBackPressed() {
        if (slideUpB) {
            slideDown();
            return;
        }

        super.onBackPressed();
    }
}