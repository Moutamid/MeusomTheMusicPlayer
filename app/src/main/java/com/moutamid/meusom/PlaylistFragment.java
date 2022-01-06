package com.moutamid.meusom;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PlaylistFragment extends Fragment {
    private Utils utils = new Utils();


    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private ArrayList<SongModel> currentPlaylistList = new ArrayList<>();

    private ArrayList<SongModel> songModelArrayList = new ArrayList<>();

    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private ArrayList<PlayListModel> playlistArrayList = new ArrayList<>();

    private RecyclerView playlistRecyclerView;
    private RecyclerViewAdapterplaylist playlistadapter;

    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Context context = getActivity();

        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
            utils.changeLanguage(context,"en");
        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
            utils.changeLanguage(context,"pr");
        }

        view = inflater.inflate(R.layout.playlist_fragment, container, false);

        getAllPlaylists();

        getAllTracks();

        initAddPlaylistButton();

        return view;
    }

    private void initAddPlaylistButton() {
        view.findViewById(R.id.add_playlist_button_my_sound).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_add_playlist);
                dialog.setCancelable(true);
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(dialog.getWindow().getAttributes());
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

                EditText namePlaylistEt = dialog.findViewById(R.id.name_playlist_et);

                initRecyclerView(dialog);

                dialog.findViewById(R.id.create_playlist_button_my_sound).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String name = namePlaylistEt.getText().toString();

                        if (name.isEmpty()) {
                            Toast.makeText(getActivity(), "Please enter a name!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (currentPlaylistList.isEmpty()) {
                            Toast.makeText(getActivity(), "Please add atleast one song!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        uploadPlaylist(dialog, name);

                        // CODE HERE
//                        dialog.dismiss();
                    }
                });
                dialog.findViewById(R.id.cancel_button_dialog_add_playlist).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // CODE HERE
                        dialog.dismiss();
                    }
                });
                dialog.show();
                dialog.getWindow().setAttributes(layoutParams);

            }

            private void uploadPlaylist(Dialog dialog, String namee) {
                ProgressDialog progressDialog;
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setCancelable(false);
                progressDialog.setMessage("Loading...");
                progressDialog.show();

                 Utils.databaseReference().child(Constants.PLAYLIST)
                        .child(auth.getCurrentUser().getUid())
                        .child(Constants.NAME)
                        .push()
                        .child(Constants.NAME)
                        .setValue(namee);

                for (int i = 0; i <= currentPlaylistList.size() - 1; i++) {

                     Utils.databaseReference().child(Constants.PLAYLIST)
                            .child(auth.getCurrentUser().getUid())
                            .child(namee)
                            .child(currentPlaylistList.get(i).getSongPushKey())
                            .setValue(currentPlaylistList.get(i));

                }

                progressDialog.dismiss();
                dialog.dismiss();
                Toast.makeText(getActivity(), "Done", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getAllTracks() {
         Utils.databaseReference().child(Constants.SONGS)
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

//                        Collections.sort(songModelArrayList, new Comparator<SongModel>() {
//                            @Override
//                            public int compare(SongModel songModel, SongModel t1) {
//                                return songModel.getSongAlbumName().compareTo(t1.getSongAlbumName());
//                            }
//                        });

//                        adapter = new AlbumFragment.RecyclerViewAdapterMessages();
//                        conversationRecyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getActivity(), error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initRecyclerView(Dialog dialog) {

        conversationRecyclerView = dialog.findViewById(R.id.add_playlist_dialog_recyclerview);
        adapter = new RecyclerViewAdapterMessages();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        conversationRecyclerView.setLayoutManager(linearLayoutManager);
        conversationRecyclerView.setHasFixedSize(true);
        conversationRecyclerView.setNestedScrollingEnabled(false);
        conversationRecyclerView.setItemViewCacheSize(20);

        conversationRecyclerView.setAdapter(adapter);

    }

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public RecyclerViewAdapterMessages.ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_add_playlist_items, parent, false);
            return new RecyclerViewAdapterMessages.ViewHolderRightMessage(view);
        }

//        RelativeLayout prevLayout;

        @Override
        public void onBindViewHolder(@NonNull final RecyclerViewAdapterMessages.ViewHolderRightMessage holder, int position1) {

            int position = holder.getAdapterPosition();

            holder.title.setText(songModelArrayList.get(position).getSongName());

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Toast.makeText(getActivity(), songModelArrayList.get(position).getSongName() + ": " + b, Toast.LENGTH_SHORT).show();

                    if (b) {
                        currentPlaylistList.add(songModelArrayList.get(position));
                    } else {
                        currentPlaylistList.remove(songModelArrayList.get(position));
                    }
                }
            });

        }

        @Override
        public int getItemCount() {
            if (songModelArrayList == null)
                return 0;
            return songModelArrayList.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {

            TextView title;
            MaterialCheckBox checkBox;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.songNameDialogPlaylist);
                checkBox = v.findViewById(R.id.checkbox_dialog_playlist_item);

            }
        }

    }

    //------------------------------------------------------------------------

    private void getAllPlaylists() {
         Utils.databaseReference().child(Constants.PLAYLIST)
                .child(auth.getCurrentUser().getUid())
                .child(Constants.NAME)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }

                        playlistArrayList.clear();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                            PlayListModel model = new PlayListModel();
                            model.setKey(dataSnapshot.getKey());
                            model.setName(dataSnapshot.child(Constants.NAME).getValue(String.class));

                            playlistArrayList.add(model);

                        }

                        initRecyclerViewplaylist();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getActivity(), error.toException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initRecyclerViewplaylist() {

        playlistRecyclerView = view.findViewById(R.id.playlistRecyclerView);
        playlistadapter = new RecyclerViewAdapterplaylist();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        playlistRecyclerView.setLayoutManager(linearLayoutManager);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setNestedScrollingEnabled(false);

        playlistRecyclerView.setAdapter(playlistadapter);

    }

    private class RecyclerViewAdapterplaylist extends RecyclerView.Adapter
            <RecyclerViewAdapterplaylist.ViewHolderRightMessage> {

        @NonNull
        @Override
        public RecyclerViewAdapterplaylist.ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_queue_items, parent, false);
            return new RecyclerViewAdapterplaylist.ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerViewAdapterplaylist.ViewHolderRightMessage holder, int position1) {

            int position = holder.getAdapterPosition();

            PlayListModel model = playlistArrayList.get(position);

            holder.title.setText(model.getName());

            holder.parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    utils.storeBoolean(getActivity(), Constants.IS_PLAYLIST, true);
                    utils.storeString(getActivity(), Constants.NAME, model.getName());
//                    Toast.makeText(getActivity(), model.getKey(), Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            });

        }

        @Override
        public int getItemCount() {
            if (playlistArrayList == null)
                return 0;
            return playlistArrayList.size();
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

    private static class PlayListModel {

        private String name, key;

        public PlayListModel(String name, String key) {
            this.name = name;
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        PlayListModel() {
        }
    }

}
