package com.moutamid.meusom;

public class SongModel {

    private String songName, songAlbumName, songCoverUrl, songYTUrl, songPushKey;

    public SongModel(String songName, String songAlbumName, String songCoverUrl, String songYTUrl, String songPushKey) {
        this.songName = songName;
        this.songAlbumName = songAlbumName;
        this.songCoverUrl = songCoverUrl;
        this.songYTUrl = songYTUrl;
        this.songPushKey = songPushKey;
    }

    public SongModel() {
    }

    public String getSongPushKey() {
        return songPushKey;
    }

    public void setSongPushKey(String songPushKey) {
        this.songPushKey = songPushKey;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getSongAlbumName() {
        return songAlbumName;
    }

    public void setSongAlbumName(String songAlbumName) {
        this.songAlbumName = songAlbumName;
    }

    public String getSongCoverUrl() {
        return songCoverUrl;
    }

    public void setSongCoverUrl(String songCoverUrl) {
        this.songCoverUrl = songCoverUrl;
    }

    public String getSongYTUrl() {
        return songYTUrl;
    }

    public void setSongYTUrl(String songYTUrl) {
        this.songYTUrl = songYTUrl;
    }
}
