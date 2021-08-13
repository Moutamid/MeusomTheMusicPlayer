package com.moutamid.meusom;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = "MainActivity2";
    private Context context = MainActivity2.this;

    private TextView currentBytesTextView;
    //    private TextView totalBytesTextView;
    private ProgressBar dialogProgressBar;

    private Dialog dialog;
    private int downloadId;

    //    final ArrayList<VideoList> downloadedList = new ArrayList<>();
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"};
    private int REQUEST_CODE_PERMISSIONS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
//            utils.changeLanguage(context,"en");
//        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
//            utils.changeLanguage(context,"pr");
//        }
        setContentView(R.layout.activity_main2);
//        clickedDownload("videoNameTest", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
    }

//    private void showDownloadProgressDialog() {
//        dialog = new Dialog(context);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.dialog_downloading_progress);
//        dialog.setCancelable(true);
//        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
//        layoutParams.copyFrom(dialog.getWindow().getAttributes());
//        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
//        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
//
//        currentBytesTextView = dialog.findViewById(R.id.current_bytes_textview);
////        totalBytesTextView = dialog.findViewById(R.id.total_bytes_textview);
//        dialogProgressBar = dialog.findViewById(R.id.progressBarOne);
//        dialogProgressBar.setIndeterminate(true);
//
//        dialog.setCanceledOnTouchOutside(true);
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialogInterface) {
//                PRDownloader.cancel(downloadId);
//                dialogInterface.dismiss();
//                Toast.makeText(context, "Cancelled!", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        dialog.show();
//        dialog.getWindow().setAttributes(layoutParams);
//
//    }
//
//    public static String getProgressDisplayLine(long currentBytes, long totalBytes) {
//        return getBytesToMBString(currentBytes) + "/" + getBytesToMBString(totalBytes);
//    }
//
//    private static String getBytesToMBString(long bytes) {
//        return String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00));
//    }
//
//    public void clickedDownload(String videoname, String url) {
////        Toast.makeText(this, "videoname: " + videoname + " url: " + url, Toast.LENGTH_LONG).show();
//
//        Dexter.withActivity(MainActivity2.this)
//                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                .withListener(new PermissionListener() {
//                    @Override
//                    public void onPermissionGranted(PermissionGrantedResponse response) {
////                        Toast.makeText(VideoPlayActivity.this, "Permission granted successfully!", Toast.LENGTH_SHORT).show();
//                        startDownloadingVideo(videoname, url);
//                    }
//
//                    @Override
//                    public void onPermissionDenied(PermissionDeniedResponse response) {
//                        if (response.isPermanentlyDenied()) {
//                            // open device settings when the permission is
//                            // denied permanently
//                            Toast.makeText(context, "You need to provide permission!", Toast.LENGTH_SHORT).show();
//
//                            Intent intent = new Intent();
//                            intent.setAction(
//                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                            Uri uri = Uri.fromParts("package",
//                                    BuildConfig.APPLICATION_ID, null);
//                            intent.setData(uri);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                        }
//                    }
//
//                    @Override
//                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
//                        token.continuePermissionRequest();
//                    }
//                }).check();
//
//    }
//
//    private void startDownloadingVideo(String videoname, String url) {
//
//        showDownloadProgressDialog();
//
//        final String fileNameStr = videoname + "-" + System.currentTimeMillis() + ".mp3";
//
//        downloadId = PRDownloader.download(url, getFilePathString(), fileNameStr)
//                .build()
//                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
//                    @Override
//                    public void onStartOrResume() {
//                        dialogProgressBar.setIndeterminate(false);
////                        progressDialog.show();
//                    }
//                })
//                .setOnProgressListener(new OnProgressListener() {
//                    @Override
//                    public void onProgress(Progress progress) {
//                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
//                        dialogProgressBar.setProgress((int) progressPercent);
//                        currentBytesTextView.setText(getProgressDisplayLine(progress.currentBytes, progress.totalBytes));
//                        dialogProgressBar.setIndeterminate(false);
//
////                        progressDialog.setMax((int) progress.totalBytes);
////                        progressDialog.setProgress((int) progress.currentBytes);
//                    }
//                })
//                .start(new OnDownloadListener() {
//                    @Override
//                    public void onDownloadComplete() {
////                        progressDialog.dismiss();
//                        dialog.dismiss();
//                        Toast t;
//                        t = Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT);
//                        t.show();
////                        Toast.makeText(VideoPlayActivity.this, "", Toast.LENGTH_SHORT).show();
//
////                        shareVideoUri(fileNameStr);
//                    }
//
//                    @Override
//                    public void onError(Error error) {
//                        Toast.makeText(context, error.getServerErrorMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//
//    private void shareVideoUri(String fileNameStr) {
//
//        File fileWithinMyDir = new File(getFilePathString(), fileNameStr);
//
//        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
//
//        if (fileWithinMyDir.exists()) {
//            Uri videoURI = FileProvider.getUriForFile(context,
//                    BuildConfig.APPLICATION_ID + ".provider",
//                    fileWithinMyDir);
//
//            intentShareFile.setType("video/mp4");
////            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + getFilePathString() + fileNameStr));
//            intentShareFile.putExtra(Intent.EXTRA_STREAM, videoURI);
//            intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
//                    "Sharing File...");
//            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
//
//            startActivity(Intent.createChooser(intentShareFile, "Share File"));
//        }
//    }
//
//    private String getFilePathString() {
//        String path_save_vid = "";
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            path_save_vid =
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +
//                            File.separator +
//                            getResources().getString(R.string.app_name) +
//                            File.separator + "videos";
//
//        } else {
//            path_save_vid =
//                    Environment.getExternalStorageDirectory().getAbsolutePath() +
//                            File.separator +
//                            getResources().getString(R.string.app_name) +
//                            File.separator + "videos";
//
//        }
//        return path_save_vid;
//    }

}