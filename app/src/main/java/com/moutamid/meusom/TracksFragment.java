package com.moutamid.meusom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media2.widget.VideoView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static com.bumptech.glide.Glide.with;
import static com.bumptech.glide.load.engine.DiskCacheStrategy.DATA;
import static com.moutamid.meusom.R.color.darkerGrey;
import static com.moutamid.meusom.R.color.darkgray;


public class TracksFragment extends Fragment {
    private static final String TAG = "TracksFragment";
    private Utils utils = new Utils();

    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private ArrayList<SongModel> songModelArrayList = new ArrayList<>();

    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getActivity();

        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
            utils.changeLanguage(context,"en");
        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
            utils.changeLanguage(context,"pr");
        }

        View view = inflater.inflate(R.layout.tracks_fragment, container, false);

        initRecyclerView(view);

        return view;
    }

    private void initRecyclerView(View view) {

        conversationRecyclerView = view.findViewById(R.id.tracksRecyclerView);
        conversationRecyclerView.addItemDecoration(new DividerItemDecoration(conversationRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
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

                            if (utils.fileExists(songModel1.getSongName())) {

                                songModel1.setSongPushKey(dataSnapshot.getKey());
                                songModelArrayList.add(songModel1);
                            }
                        }

                        adapter = new RecyclerViewAdapterMessages();
                        conversationRecyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getActivity(), error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public RecyclerViewAdapterMessages.ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_download_item, parent, false);
            return new RecyclerViewAdapterMessages.ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerViewAdapterMessages.ViewHolderRightMessage holder, int position1) {

            int position = holder.getAdapterPosition();

            SongModel model = songModelArrayList.get(position);

            holder.songName.setText(model.getSongName());
            holder.songAlbumName.setText(model.getSongAlbumName());

            with(getActivity())
                    .asBitmap()
                    .load(model.getSongCoverUrl())
                    .apply(new RequestOptions()
                            .placeholder(darkgray)
                            .error(darkerGrey)
                    )
                    .diskCacheStrategy(DATA)
                    .into(holder.songCoverImage);

            holder.parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    utils.storeBoolean(getActivity(), Constants.IS_PLAYLIST, false);

                    Intent intent = getActivity().getIntent();
                    intent.putExtra(Constants.SONG_INDEX, position);
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();

                }
            });

            holder.downloadButton.setVisibility(GONE);
            holder.downloadStatus.setVisibility(GONE);
            holder.deleteBtn.setVisibility(GONE);
/*
            if (fileExists(model.getSongName() + ".mp3")) {
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

                    Toast.makeText(context, model.getSongYTUrl(), Toast.LENGTH_SHORT).show();
//                        runCommand(model.getSongYTUrl(), holder, model.getSongPushKey());

                    }
                }
            });
*/

        }

/*
        private void showDeleteDialog(String fileName) {
            utils.showDialog(context,
                    "Are you sure?",
                    "Do you want to delete this file?",
                    "Yes",
                    "No",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }, true);
        }
*/

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
            RelativeLayout parentLayout;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                downloadButton = v.findViewById(R.id.downloadBtnCommand);
                songCoverImage = v.findViewById(R.id.song_cover_Command);
                songName = v.findViewById(R.id.song_nameCommand);
                songAlbumName = v.findViewById(R.id.song_albumCommand);
                downloadStatus = v.findViewById(R.id.download_statusCommand);
                parentLayout = v.findViewById(R.id.parentLayout_downloadItem);
                deleteBtn = v.findViewById(R.id.deleteBtnCommand);

            }
        }

    }

}
