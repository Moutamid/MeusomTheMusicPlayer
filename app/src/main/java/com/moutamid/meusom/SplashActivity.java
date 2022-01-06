package com.moutamid.meusom;

import static com.google.firebase.auth.FirebaseAuth.getInstance;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SplashActivity extends AppCompatActivity {
    private Context context = SplashActivity.this;

    private Utils utils = new Utils();
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        doWorkOfSplashScreen();

//        new GetSongMetaData().execute();

    }

    private void doWorkOfSplashScreen() {
        FirebaseAuth mAuth = getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (mAuth.getCurrentUser() == null) {
                    // USER IS NOT SIGNED IN

                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);

                } else {
                    // USER IS SIGNED IN

                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }, 500);
    }

    private class GetSongMetaData extends AsyncTask<String, Void, String> {

        //        private String url;
//        private String error = null;
//        private String songName, songAlbumName, songCoverUrl;
        private ProgressDialog progressDialog;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mAuth = getInstance();

        ArrayList<String> nullList = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please wait...");
            progressDialog.show();
        }

        private void updateProgress(String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage(msg);
                }
            });
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpHandler sh = new HttpHandler();

            fillUpArrayList();

            for (int i = 28; i <= songLinksList.size() - 1; i++) {
                Log.i(TAG, "doInBackground: index: " + i);
                String urlYT = "https://www.youtube.com/oembed?format=json&url=" + songLinksList.get(i);//https://www.youtube.com/watch?v=" + id;

                String jsonStr = sh.makeServiceCall(urlYT);

                if (jsonStr != null) {
                    try {
                        JSONObject o = new JSONObject(jsonStr);

                        SongModel songModel = new SongModel();

                        songModel.setSongYTUrl(getVideoId(songLinksList.get(i)));
                        songModel.setSongName(o.getString("title"));
                        songModel.setSongAlbumName(o.getString("author_name"));
                        songModel.setSongCoverUrl(o.getString("thumbnail_url"));

                        Log.i(TAG, "doInBackground: getterSuccess: " + o.getString("title") + " id: " + getVideoId(songLinksList.get(i)));

                        Utils.databaseReference().child(Constants.SONGS)
                                .child(mAuth.getCurrentUser().getUid()).push()
                                .setValue(songModel);
                        updateProgress("Please wait... " + i);

                    } catch (final JSONException e) {
                        Log.e(TAG, "ERROR AT: " + i + " (" + getVideoId(songLinksList.get(i)) + ")");
                        Log.e(TAG, "ERROR MSG: " + e.getMessage());
                        nullList.add(getVideoId(songLinksList.get(i)));
//                        return "";
                    }
                } else {
                    Log.e(TAG, "ERROR AT: " + i + " (" + getVideoId(songLinksList.get(i)) + ")");
                    Log.e(TAG, "ERROR MSG: jsonStr IS NULL");
                    nullList.add(getVideoId(songLinksList.get(i)));
//                    return "";
                }
                Log.e(TAG, "ERROR LIST: " + nullList.toString());
                //E/SplashActivity: ERROR AT: 28 (fzzxd2NvwHA)
                //E/SplashActivity: ERROR MSG: jsonStr IS NULL
                //E/SplashActivity: ERROR AT: 50 (2rFcW75NyqI)
                //E/SplashActivity: ERROR MSG: jsonStr IS NULL
                //E/SplashActivity: ERROR AT: 176 (xf5rgfur4gw)
                //E/SplashActivity: ERROR MSG: jsonStr IS NULL
                //E/SplashActivity: ERROR AT: 453 (vIyP_joi3AI)
                //E/SplashActivity: ERROR MSG: jsonStr IS NULL
                //E/SplashActivity: ERROR AT: 556 (ifCAfAzOBJM)
                //E/SplashActivity: ERROR MSG: jsonStr IS NULL
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressDialog.dismiss();

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


    ArrayList<String> songLinksList = new ArrayList<>();

    private void fillUpArrayList() {
        songLinksList.add("https://www.youtube.com/watch?v=9yfMplVU0m0");
        songLinksList.add("https://www.youtube.com/watch?v=iLBBRuVDOo4");
        songLinksList.add("https://www.youtube.com/watch?v=SXiSVQZLje8");
        songLinksList.add("https://www.youtube.com/watch?v=0n_vz0ddoT8");
        songLinksList.add("https://www.youtube.com/watch?v=aBn7bjy9c4U");
        songLinksList.add("https://www.youtube.com/watch?v=UuCq8mtK8J4");
        songLinksList.add("https://www.youtube.com/watch?v=EHkozMIXZ8w");
        songLinksList.add("https://www.youtube.com/watch?v=KgmeL_xuB0I");
        songLinksList.add("https://www.youtube.com/watch?v=UuCq8mtK8J4");
        songLinksList.add("https://www.youtube.com/watch?v=HMUDVMiITOU");
        songLinksList.add("https://www.youtube.com/watch?v=fh78YZ3P6Ys");
        songLinksList.add("https://www.youtube.com/watch?v=YBsPE6yHH9c");
        songLinksList.add("https://www.youtube.com/watch?v=g5qU7p7yOY8");
        songLinksList.add("https://www.youtube.com/watch?v=OfS1jFck8YQ");
        songLinksList.add("https://www.youtube.com/watch?v=UqON44CLaQM");
        songLinksList.add("https://www.youtube.com/watch?v=aCH1eyWq9B0");
        songLinksList.add("https://www.youtube.com/watch?v=BAWJ_6KdkV0");
        songLinksList.add("https://www.youtube.com/watch?v=Od-6uzcLGqw");
        songLinksList.add("https://www.youtube.com/watch?v=pxjZM-d_ShI");
        songLinksList.add("https://www.youtube.com/watch?v=ft4jcPSLJfY");
        songLinksList.add("https://www.youtube.com/watch?v=GxBSyx85Kp8");
        songLinksList.add("https://www.youtube.com/watch?v=UqON44CLaQM");
        songLinksList.add("https://www.youtube.com/watch?v=mywyuiAbww4");
        songLinksList.add("https://www.youtube.com/watch?v=iS1g8G_njx8");
        songLinksList.add("https://www.youtube.com/watch?v=ax9ge-ymWIQ");
        songLinksList.add("https://www.youtube.com/watch?v=9yfMplVU0m0");
        songLinksList.add("https://www.youtube.com/watch?v=k2qgadSvNyU");
        songLinksList.add("https://www.youtube.com/watch?v=Hg2Kl04ITxc");
        songLinksList.add("https://www.youtube.com/watch?v=fzzxd2NvwHA");
        songLinksList.add("https://www.youtube.com/watch?v=U1ovRUrC9bs");
        songLinksList.add("https://www.youtube.com/watch?v=sC2nElyx7Ds");
        songLinksList.add("https://www.youtube.com/watch?v=b8I-7Wk_Vbc");
        songLinksList.add("https://www.youtube.com/watch?v=iLBBRuVDOo4");
        songLinksList.add("https://www.youtube.com/watch?v=XCBPKRaJgz4");
        songLinksList.add("https://www.youtube.com/watch?v=LaVPQe8Zf9o");
        songLinksList.add("https://www.youtube.com/watch?v=UqON44CLaQM");
        songLinksList.add("https://www.youtube.com/watch?v=uPHKkewD1G0");
        songLinksList.add("https://www.youtube.com/watch?v=989-7xsRLR4");
        songLinksList.add("https://www.youtube.com/watch?v=CfkxLRuSteI");
        songLinksList.add("https://www.youtube.com/watch?v=sC2nElyx7Ds");
        songLinksList.add("https://www.youtube.com/watch?v=Zd68AthoNIw");
        songLinksList.add("https://www.youtube.com/watch?v=kivuDS-6HbQ");
        songLinksList.add("https://www.youtube.com/watch?v=JGwWNGJdvx8");
        songLinksList.add("https://www.youtube.com/watch?v=8MkClXv2vKs");
        songLinksList.add("https://www.youtube.com/watch?v=gmqkCs6Ycoo");
        songLinksList.add("https://www.youtube.com/watch?v=yJg-Y5byMMw");
        songLinksList.add("https://www.youtube.com/watch?v=k-TBmwz6GB4");
        songLinksList.add("https://www.youtube.com/watch?v=zrV5of2p-oc");
        songLinksList.add("https://www.youtube.com/watch?v=6DtPF9W3ejI");
        songLinksList.add("https://www.youtube.com/watch?v=yJg-Y5byMMw");
        songLinksList.add("https://www.youtube.com/watch?v=2rFcW75NyqI");
        songLinksList.add("https://www.youtube.com/watch?v=MD_C1QVkljk");
        songLinksList.add("https://www.youtube.com/watch?v=wnJ6LuUFpMo");
        songLinksList.add("https://www.youtube.com/watch?v=gmqkCs6Ycoo");
        songLinksList.add("https://www.youtube.com/watch?v=CGyEd0aKWZE");
        songLinksList.add("https://www.youtube.com/watch?v=DGVghfG1mDk");
        songLinksList.add("https://www.youtube.com/watch?v=Df2abB1Z-e0");
        songLinksList.add("https://www.youtube.com/watch?v=-1M5EMtit3k");
        songLinksList.add("https://www.youtube.com/watch?v=NCclOXqCxA8");
        songLinksList.add("https://www.youtube.com/watch?v=wnJ6LuUFpMo");
        songLinksList.add("https://www.youtube.com/watch?v=AJtDXIazrMo");
        songLinksList.add("https://www.youtube.com/watch?v=Ktv43aUvb1c");
        songLinksList.add("https://www.youtube.com/watch?v=cJQC8Cn2Dko");
        songLinksList.add("https://www.youtube.com/watch?v=AJtDXIazrMo");
        songLinksList.add("https://www.youtube.com/watch?v=xzCEdSKMkdU");
        songLinksList.add("https://www.youtube.com/watch?v=hsXeFqj5p7Q");
        songLinksList.add("https://www.youtube.com/watch?v=5x4Lwdkxu0c");
        songLinksList.add("https://www.youtube.com/watch?v=IcrbM1l_BoI");
        songLinksList.add("https://www.youtube.com/watch?v=cwLRQn61oUY");
        songLinksList.add("https://www.youtube.com/watch?v=RgKAFK5djSk");
        songLinksList.add("https://www.youtube.com/watch?v=P0_vIWWcvNg");
        songLinksList.add("https://www.youtube.com/watch?v=XVNPdmwOfKI");
        songLinksList.add("https://www.youtube.com/watch?v=5GL9JoH4Sws");
        songLinksList.add("https://www.youtube.com/watch?v=2GADx4Hy-Gg");
        songLinksList.add("https://www.youtube.com/watch?v=LaIEfcUCywk");
        songLinksList.add("https://www.youtube.com/watch?v=nlcIKh6sBtc");
        songLinksList.add("https://www.youtube.com/watch?v=y2tEPmwWEiI");
        songLinksList.add("https://www.youtube.com/watch?v=2GADx4Hy-Gg");
        songLinksList.add("https://www.youtube.com/watch?v=3PdILZ_1P74");
        songLinksList.add("https://www.youtube.com/watch?v=jK2aIUmmdP4");
        songLinksList.add("https://www.youtube.com/watch?v=8fLhBT_6YBk");
        songLinksList.add("https://www.youtube.com/watch?v=qrO4YZeyl0I");
        songLinksList.add("https://www.youtube.com/watch?v=B7xai5u_tnk");
        songLinksList.add("https://www.youtube.com/watch?v=jXQitd7ahV4");
        songLinksList.add("https://www.youtube.com/watch?v=bESGLojNYSo");
        songLinksList.add("https://www.youtube.com/watch?v=3nQNiWdeH2Q");
        songLinksList.add("https://www.youtube.com/watch?v=qrO4YZeyl0I");
        songLinksList.add("https://www.youtube.com/watch?v=12CeaxLiMgE");
        songLinksList.add("https://www.youtube.com/watch?v=aBt8fN7mJNg");
        songLinksList.add("https://www.youtube.com/watch?v=rNsP4nE1_FA");
        songLinksList.add("https://www.youtube.com/watch?v=5UtOVnsZPF4");
        songLinksList.add("https://www.youtube.com/watch?v=IoJlIriz1jk");
        songLinksList.add("https://www.youtube.com/watch?v=VCLxJd1d84s");
        songLinksList.add("https://www.youtube.com/watch?v=kJQP7kiw5Fk");
        songLinksList.add("https://www.youtube.com/watch?v=1xwr7Jw-dqM");
        songLinksList.add("https://www.youtube.com/watch?v=1__CAdTJ5JU");
        songLinksList.add("https://www.youtube.com/watch?v=Srqs4CitU2U");
        songLinksList.add("https://www.youtube.com/watch?v=5UtOVnsZPF4");
        songLinksList.add("https://www.youtube.com/watch?v=Dst9gZkq1a8");
        songLinksList.add("https://www.youtube.com/watch?v=eIc4mqyN1Q8");
        songLinksList.add("https://www.youtube.com/watch?v=GGvBZIyTxEI");
        songLinksList.add("https://www.youtube.com/watch?v=nCS0bC62PjE");
        songLinksList.add("https://www.youtube.com/watch?v=Zk7Dg30tCDU");
        songLinksList.add("https://www.youtube.com/watch?v=Zk7Dg30tCDU");
        songLinksList.add("https://www.youtube.com/watch?v=Zk7Dg30tCDU");
        songLinksList.add("https://www.youtube.com/watch?v=axySrE0Kg6k");
        songLinksList.add("https://www.youtube.com/watch?v=3AtDnEC4zak");
        songLinksList.add("https://www.youtube.com/watch?v=lXX53y9ydos");
        songLinksList.add("https://www.youtube.com/watch?v=FuXNumBwDOM");
        songLinksList.add("https://www.youtube.com/watch?v=CwfoyVa980U");
        songLinksList.add("https://www.youtube.com/watch?v=n1a7o44WxNo");
        songLinksList.add("https://www.youtube.com/watch?v=IxxstCcJlsc");
        songLinksList.add("https://www.youtube.com/watch?v=qdpXxGPqW-Y");
        songLinksList.add("https://www.youtube.com/watch?v=X46t8ZFqUB4");
        songLinksList.add("https://www.youtube.com/watch?v=AR5ZNqYB69A");
        songLinksList.add("https://www.youtube.com/watch?v=4cgEOO2viak");
        songLinksList.add("https://www.youtube.com/watch?v=2i2khp_npdE");
        songLinksList.add("https://www.youtube.com/watch?v=WA4iX5D9Z64");
        songLinksList.add("https://www.youtube.com/watch?v=VuNIsY6JdUw");
        songLinksList.add("https://www.youtube.com/watch?v=nchCX7o7kuE");
        songLinksList.add("https://www.youtube.com/watch?v=QcIy9NiNbmo");
        songLinksList.add("https://www.youtube.com/watch?v=3yy-dKTmyOo");
        songLinksList.add("https://www.youtube.com/watch?v=k_bHd2-QhuQ");
        songLinksList.add("https://www.youtube.com/watch?v=50VNCymT-Cs");
        songLinksList.add("https://www.youtube.com/watch?v=tQU9EBhodGA");
        songLinksList.add("https://www.youtube.com/watch?v=GODUXCWRyVs");
        songLinksList.add("https://www.youtube.com/watch?v=9PQmIioYedw");
        songLinksList.add("https://www.youtube.com/watch?v=8SQ8ZJR8opU");
        songLinksList.add("https://www.youtube.com/watch?v=pvP_OwVSFpk");
        songLinksList.add("https://www.youtube.com/watch?v=dISNgvVpWlo");
        songLinksList.add("https://www.youtube.com/watch?v=BQZAvoLixiM");
        songLinksList.add("https://www.youtube.com/watch?v=3CEg1clLNNM");
        songLinksList.add("https://www.youtube.com/watch?v=5GL9JoH4Sws");
        songLinksList.add("https://www.youtube.com/watch?v=FNp8329unFU");
        songLinksList.add("https://www.youtube.com/watch?v=CBsJGk6upDc");
        songLinksList.add("https://www.youtube.com/watch?v=y2tEPmwWEiI");
        songLinksList.add("https://www.youtube.com/watch?v=o9JJvH-vw9A");
        songLinksList.add("https://www.youtube.com/watch?v=YBHQbu5rbdQ");
        songLinksList.add("https://www.youtube.com/watch?v=hp_labuiDHY");
        songLinksList.add("https://www.youtube.com/watch?v=YBHQbu5rbdQ");
        songLinksList.add("https://www.youtube.com/watch?v=hCiEyDRN3Qc");
        songLinksList.add("https://www.youtube.com/watch?v=cMg8KaMdDYo");
        songLinksList.add("https://www.youtube.com/watch?v=nlcIKh6sBtc");
        songLinksList.add("https://www.youtube.com/watch?v=8uidkYkkNZU");
        songLinksList.add("https://www.youtube.com/watch?v=8yGKJl_U4Gg");
        songLinksList.add("https://www.youtube.com/watch?v=3PdILZ_1P74");
        songLinksList.add("https://www.youtube.com/watch?v=0yW7w8F2TVA");
        songLinksList.add("https://www.youtube.com/watch?v=gxApr8QnlGY");
        songLinksList.add("https://www.youtube.com/watch?v=JFcgOboQZ08");
        songLinksList.add("https://www.youtube.com/watch?v=B7xai5u_tnk");
        songLinksList.add("https://www.youtube.com/watch?v=jXQitd7ahV4");
        songLinksList.add("https://www.youtube.com/watch?v=lEi_XBg2Fpk");
        songLinksList.add("https://www.youtube.com/watch?v=EoCTV78coGE");
        songLinksList.add("https://www.youtube.com/watch?v=kg1BljLu9YY");
        songLinksList.add("https://www.youtube.com/watch?v=hNvye5RyVXg");
        songLinksList.add("https://www.youtube.com/watch?v=v6IAJOOmDMg");
        songLinksList.add("https://www.youtube.com/watch?v=acvIVA9-FMQ");
        songLinksList.add("https://www.youtube.com/watch?v=_eQQKVKjifQ");
        songLinksList.add("https://www.youtube.com/watch?v=n-FVcnbvCJA");
        songLinksList.add("https://www.youtube.com/watch?v=4DsBbtOwvFo");
        songLinksList.add("https://www.youtube.com/watch?v=Srqs4CitU2U");
        songLinksList.add("https://www.youtube.com/watch?v=fHhe--7B7wA");
        songLinksList.add("https://www.youtube.com/watch?v=4DsBbtOwvFo");
        songLinksList.add("https://www.youtube.com/watch?v=fHhe--7B7wA");
        songLinksList.add("https://www.youtube.com/watch?v=acvIVA9-FMQ");
        songLinksList.add("https://www.youtube.com/watch?v=nCS0bC62PjE");
        songLinksList.add("https://www.youtube.com/watch?v=GGvBZIyTxEI");
        songLinksList.add("https://www.youtube.com/watch?v=ixkoVwKQaJg");
        songLinksList.add("https://www.youtube.com/watch?v=8l75Z8wil_U");
        songLinksList.add("https://www.youtube.com/watch?v=Zk7Dg30tCDU");
        songLinksList.add("https://www.youtube.com/watch?v=3zrZE68Qrrg");
        songLinksList.add("https://www.youtube.com/watch?v=0gg4H180mBk");
        songLinksList.add("https://www.youtube.com/watch?v=lXX53y9ydos");
        songLinksList.add("https://www.youtube.com/watch?v=1-xGerv5FOk");
        songLinksList.add("https://www.youtube.com/watch?v=5hEh9LiSzow");
        songLinksList.add("https://www.youtube.com/watch?v=XPBwXKgDTdE");
        songLinksList.add("https://www.youtube.com/watch?v=xf5rgfur4gw");
        songLinksList.add("https://www.youtube.com/watch?v=yHLtE1wFeRQ");
        songLinksList.add("https://www.youtube.com/watch?v=nN6VR92V70M");
        songLinksList.add("https://www.youtube.com/watch?v=jn40gqhxoSY");
        songLinksList.add("https://www.youtube.com/watch?v=RCaoEIy-sNY");
        songLinksList.add("https://www.youtube.com/watch?v=BjsjIkSb0cM");
        songLinksList.add("https://www.youtube.com/watch?v=cMPEd8m79Hw");
        songLinksList.add("https://www.youtube.com/watch?v=3tmd-ClpJxA");
        songLinksList.add("https://www.youtube.com/watch?v=nN6VR92V70M");
        songLinksList.add("https://www.youtube.com/watch?v=8JnfIa84TnU");
        songLinksList.add("https://www.youtube.com/watch?v=VuNIsY6JdUw");
        songLinksList.add("https://www.youtube.com/watch?v=YykjpeuMNEk");
        songLinksList.add("https://www.youtube.com/watch?v=Xlha0OmVCSg");
        songLinksList.add("https://www.youtube.com/watch?v=nwd7Sn0cGG0");
        songLinksList.add("https://www.youtube.com/watch?v=sUmZvAafs3Y");
        songLinksList.add("https://www.youtube.com/watch?v=ERMRWk1bwqo");
        songLinksList.add("https://www.youtube.com/watch?v=GvB3OzTicQo");
        songLinksList.add("https://www.youtube.com/watch?v=xseXbA2N6D0");
        songLinksList.add("https://www.youtube.com/watch?v=A-m3aMcjx00");
        songLinksList.add("https://www.youtube.com/watch?v=kffacxfA7G4");
        songLinksList.add("https://www.youtube.com/watch?v=7h2SsusfQmE");
        songLinksList.add("https://www.youtube.com/watch?v=zVl2oQ__GDE");
        songLinksList.add("https://www.youtube.com/watch?v=tKKZevWj_0A");
        songLinksList.add("https://www.youtube.com/watch?v=tKKZevWj_0A");
        songLinksList.add("https://www.youtube.com/watch?v=tD4HCZe-tew");
        songLinksList.add("https://www.youtube.com/watch?v=jzD_yyEcp0M");
        songLinksList.add("https://www.youtube.com/watch?v=tt2k8PGm-TI");
        songLinksList.add("https://www.youtube.com/watch?v=pXvoeCgi59o");
        songLinksList.add("https://www.youtube.com/watch?v=rjBsQ9SygnE");
        songLinksList.add("https://www.youtube.com/watch?v=n1a7o44WxNo");
        songLinksList.add("https://www.youtube.com/watch?v=BjsjIkSb0cM");
        songLinksList.add("https://www.youtube.com/watch?v=L8eRzOYhLuw");
        songLinksList.add("https://www.youtube.com/watch?v=vZ_NpLWuL00");
        songLinksList.add("https://www.youtube.com/watch?v=X46t8ZFqUB4");
        songLinksList.add("https://www.youtube.com/watch?v=hu2M5vdfs1A");
        songLinksList.add("https://www.youtube.com/watch?v=iS1g8G_njx8");
        songLinksList.add("https://www.youtube.com/watch?v=p9LDnPyY9zA");
        songLinksList.add("https://www.youtube.com/watch?v=hVHZI_IydU8");
        songLinksList.add("https://www.youtube.com/watch?v=M3mJkSqZbX4");
        songLinksList.add("https://www.youtube.com/watch?v=zDB8dM3MBvg");
        songLinksList.add("https://www.youtube.com/watch?v=fRh_vgS2dFE");
        songLinksList.add("https://www.youtube.com/watch?v=qV5lzRHrGeg");
        songLinksList.add("https://www.youtube.com/watch?v=gwpTPrPoy4Y");
        songLinksList.add("https://www.youtube.com/watch?v=ogSRY_1I9LE");
        songLinksList.add("https://www.youtube.com/watch?v=1xwr7Jw-dqM");
        songLinksList.add("https://www.youtube.com/watch?v=NaEbWyb9bOA");
        songLinksList.add("https://www.youtube.com/watch?v=1xwr7Jw-dqM");
        songLinksList.add("https://www.youtube.com/watch?v=Io0fBr1XBUA");
        songLinksList.add("https://www.youtube.com/watch?v=p07Tfjs2mEM");
        songLinksList.add("https://www.youtube.com/watch?v=uwznBOgyDy0");
        songLinksList.add("https://www.youtube.com/watch?v=T82OEZmCr1o");
        songLinksList.add("https://www.youtube.com/watch?v=aUKXa1u0VYk");
        songLinksList.add("https://www.youtube.com/watch?v=OVZO6ArBKNY");
        songLinksList.add("https://www.youtube.com/watch?v=EmI6b8xFSX4");
        songLinksList.add("https://www.youtube.com/watch?v=u1yVCeXYya4");
        songLinksList.add("https://www.youtube.com/watch?v=RhU9MZ98jxo");
        songLinksList.add("https://www.youtube.com/watch?v=SmM0653YvXU");
        songLinksList.add("https://www.youtube.com/watch?v=yw04QD1LaB0");
        songLinksList.add("https://www.youtube.com/watch?v=YuPzpoC3QNc");
        songLinksList.add("https://www.youtube.com/watch?v=rNsP4nE1_FA");
        songLinksList.add("https://www.youtube.com/watch?v=nfs8NYg7yQM");
        songLinksList.add("https://www.youtube.com/watch?v=_hMQe2U4c6w");
        songLinksList.add("https://www.youtube.com/watch?v=TdyllLZeviY");
        songLinksList.add("https://www.youtube.com/watch?v=l9T-2Fb_ZlY");
        songLinksList.add("https://www.youtube.com/watch?v=EUoe7cf0HYw");
        songLinksList.add("https://www.youtube.com/watch?v=m65jhGwtWrg");
        songLinksList.add("https://www.youtube.com/watch?v=e-ORhEE9VVg");
        songLinksList.add("https://www.youtube.com/watch?v=8qLL2Gx3I_k");
        songLinksList.add("https://www.youtube.com/watch?v=EUoe7cf0HYw");
        songLinksList.add("https://www.youtube.com/watch?v=6sxDpnrWSUo");
        songLinksList.add("https://www.youtube.com/watch?v=zFA1VS8dzb4");
        songLinksList.add("https://www.youtube.com/watch?v=3WTeNpg_nVA");
        songLinksList.add("https://www.youtube.com/watch?v=xseXbA2N6D0");
        songLinksList.add("https://www.youtube.com/watch?v=dS1Gf7qq2sI");
        songLinksList.add("https://www.youtube.com/watch?v=lwnoSeiAFSY");
        songLinksList.add("https://www.youtube.com/watch?v=PhG-vBxkuJk");
        songLinksList.add("https://www.youtube.com/watch?v=FM7MFYoylVs");
        songLinksList.add("https://www.youtube.com/watch?v=25ROFXjoaAU");
        songLinksList.add("https://www.youtube.com/watch?v=rc4dHR3Ang0");
        songLinksList.add("https://www.youtube.com/watch?v=yMaCwJxy-Rg");
        songLinksList.add("https://www.youtube.com/watch?v=Il-an3K9pjg");
        songLinksList.add("https://www.youtube.com/watch?v=Il-an3K9pjg");
        songLinksList.add("https://www.youtube.com/watch?v=ANS9sSJA9Yc");
        songLinksList.add("https://www.youtube.com/watch?v=fWb2bypvyhM");
        songLinksList.add("https://www.youtube.com/watch?v=gdx7gN1UyX0");
        songLinksList.add("https://www.youtube.com/watch?v=tp1ZluX4aYs");
        songLinksList.add("https://www.youtube.com/watch?v=aMKtzB7zNrg");
        songLinksList.add("https://www.youtube.com/watch?v=i-gyZ35074k");
        songLinksList.add("https://www.youtube.com/watch?v=W-TE_Ys4iwM");
        songLinksList.add("https://www.youtube.com/watch?v=i-gyZ35074k");
        songLinksList.add("https://www.youtube.com/watch?v=cBDt_-tIfLI");
        songLinksList.add("https://www.youtube.com/watch?v=-j0dlcfekqw");
        songLinksList.add("https://www.youtube.com/watch?v=Bvz67EULTJc");
        songLinksList.add("https://www.youtube.com/watch?v=lUotEgzKmG8");
        songLinksList.add("https://www.youtube.com/watch?v=AUVRyAXaqVk");
        songLinksList.add("https://www.youtube.com/watch?v=kOkQ4T5WO9E");
        songLinksList.add("https://www.youtube.com/watch?v=VNKU4hSz3SU");
        songLinksList.add("https://www.youtube.com/watch?v=ecvKEhiDi1c");
        songLinksList.add("https://www.youtube.com/watch?v=xZC8ixkVvg8");
        songLinksList.add("https://www.youtube.com/watch?v=df314N1zu9o");
        songLinksList.add("https://www.youtube.com/watch?v=fWNaR-rxAic");
        songLinksList.add("https://www.youtube.com/watch?v=kffacxfA7G4");
        songLinksList.add("https://www.youtube.com/watch?v=HcVv9R1ZR84");
        songLinksList.add("https://www.youtube.com/watch?v=cBOE1aUNZVo");
        songLinksList.add("https://www.youtube.com/watch?v=Zi_XLOBDo_Y");
        songLinksList.add("https://www.youtube.com/watch?v=PsO6ZnUZI0g");
        songLinksList.add("https://www.youtube.com/watch?v=LrUvu1mlWco");
        songLinksList.add("https://www.youtube.com/watch?v=fWNaR-rxAic");
        songLinksList.add("https://www.youtube.com/watch?v=aUKXa1u0VYk");
        songLinksList.add("https://www.youtube.com/watch?v=99qOnljEjnY");
        songLinksList.add("https://www.youtube.com/watch?v=1xwr7Jw-dqM");
        songLinksList.add("https://www.youtube.com/watch?v=Vf78alvpxRM");
        songLinksList.add("https://www.youtube.com/watch?v=HoCwa6gnmM0");
        songLinksList.add("https://www.youtube.com/watch?v=cBOE1aUNZVo");
        songLinksList.add("https://www.youtube.com/watch?v=HYoxmS-uQ9k");
        songLinksList.add("https://www.youtube.com/watch?v=kgUpRwMeRr4");
        songLinksList.add("https://www.youtube.com/watch?v=_hMQe2U4c6w");
        songLinksList.add("https://www.youtube.com/watch?v=qgy7vEje5-w");
        songLinksList.add("https://www.youtube.com/watch?v=i7wveOu5hkQ");
        songLinksList.add("https://www.youtube.com/watch?v=wAgZVLk6J4M");
        songLinksList.add("https://www.youtube.com/watch?v=2XJphXsDrIA");
        songLinksList.add("https://www.youtube.com/watch?v=VCLxJd1d84s");
        songLinksList.add("https://www.youtube.com/watch?v=6Mgqbai3fKo");
        songLinksList.add("https://www.youtube.com/watch?v=papuvlVeZg8");
        songLinksList.add("https://www.youtube.com/watch?v=wVCZmXiK-6k");
        songLinksList.add("https://www.youtube.com/watch?v=9mQk7Evt6Vs");
        songLinksList.add("https://www.youtube.com/watch?v=I66oFXdf0KU");
        songLinksList.add("https://www.youtube.com/watch?v=dS1Gf7qq2sI");
        songLinksList.add("https://www.youtube.com/watch?v=vBGiFtb8Rpw");
        songLinksList.add("https://www.youtube.com/watch?v=iaWGvoW5M6U");
        songLinksList.add("https://www.youtube.com/watch?v=sUmZvAafs3Y");
        songLinksList.add("https://www.youtube.com/watch?v=ho0WBKPJtfc");
        songLinksList.add("https://www.youtube.com/watch?v=0zGcUoRlhmw");
        songLinksList.add("https://www.youtube.com/watch?v=Io0fBr1XBUA");
        songLinksList.add("https://www.youtube.com/watch?v=gdFqPis3ChA");
        songLinksList.add("https://www.youtube.com/watch?v=aJOTlE1K90k");
        songLinksList.add("https://www.youtube.com/watch?v=fRh_vgS2dFE");
        songLinksList.add("https://www.youtube.com/watch?v=lGhAZW-y8a8");
        songLinksList.add("https://www.youtube.com/watch?v=eC-F_VZ2T1c");
        songLinksList.add("https://www.youtube.com/watch?v=jzD_yyEcp0M");
        songLinksList.add("https://www.youtube.com/watch?v=U9cD4-ZgRy4");
        songLinksList.add("https://www.youtube.com/watch?v=tD4HCZe-tew");
        songLinksList.add("https://www.youtube.com/watch?v=U-PXEe-qeK4");
        songLinksList.add("https://www.youtube.com/watch?v=ALZHF5UqnU4");
        songLinksList.add("https://www.youtube.com/watch?v=-whp15J2n_M");
        songLinksList.add("https://www.youtube.com/watch?v=HBttoBTxkAs");
        songLinksList.add("https://www.youtube.com/watch?v=bC3WAxiLnDY");
        songLinksList.add("https://www.youtube.com/watch?v=MkhotmP0ij4");
        songLinksList.add("https://www.youtube.com/watch?v=J-dv_DcDD_A");
        songLinksList.add("https://www.youtube.com/watch?v=MkhotmP0ij4");
        songLinksList.add("https://www.youtube.com/watch?v=IxxstCcJlsc");
        songLinksList.add("https://www.youtube.com/watch?v=fyLIHH-iqFI");
        songLinksList.add("https://www.youtube.com/watch?v=-j0dlcfekqw");
        songLinksList.add("https://www.youtube.com/watch?v=JudqK1hL18w");
        songLinksList.add("https://www.youtube.com/watch?v=H7HmzwI67ec");
        songLinksList.add("https://www.youtube.com/watch?v=bg7RjxsghNY");
        songLinksList.add("https://www.youtube.com/watch?v=_P7S2lKif-A");
        songLinksList.add("https://www.youtube.com/watch?v=eIpLAwON_R4");
        songLinksList.add("https://www.youtube.com/watch?v=GoLPidSeD6Q");
        songLinksList.add("https://www.youtube.com/watch?v=RnBT9uUYb1w");
        songLinksList.add("https://www.youtube.com/watch?v=2-MBfn8XjIU");
        songLinksList.add("https://www.youtube.com/watch?v=L7_jYl8A73g");
        songLinksList.add("https://www.youtube.com/watch?v=LrUvu1mlWco");
        songLinksList.add("https://www.youtube.com/watch?v=9vbddLNUOMs");
        songLinksList.add("https://www.youtube.com/watch?v=Az-mGR-CehY");
        songLinksList.add("https://www.youtube.com/watch?v=zUfVJ7c22r8");
        songLinksList.add("https://www.youtube.com/watch?v=Az-mGR-CehY");
        songLinksList.add("https://www.youtube.com/watch?v=7mHO_oFqQ4o");
        songLinksList.add("https://www.youtube.com/watch?v=kthhAjR4CBs");
        songLinksList.add("https://www.youtube.com/watch?v=5wyW-w1ikK0");
        songLinksList.add("https://www.youtube.com/watch?v=tkJ_koXPZzs");
        songLinksList.add("https://www.youtube.com/watch?v=YPTp4okj-eM");
        songLinksList.add("https://www.youtube.com/watch?v=YPTp4okj-eM");
        songLinksList.add("https://www.youtube.com/watch?v=WNeLUngb-Xg");
        songLinksList.add("https://www.youtube.com/watch?v=kdFdeRoVPg0");
        songLinksList.add("https://www.youtube.com/watch?v=nfs8NYg7yQM");
        songLinksList.add("https://www.youtube.com/watch?v=SmM0653YvXU");
        songLinksList.add("https://www.youtube.com/watch?v=2Vv-BfVoq4g");
        songLinksList.add("https://www.youtube.com/watch?v=x0dNpGO1pCk");
        songLinksList.add("https://www.youtube.com/watch?v=hVHZI_IydU8");
        songLinksList.add("https://www.youtube.com/watch?v=pWT8Snks6oo");
        songLinksList.add("https://www.youtube.com/watch?v=rtOvBOTyX00");
        songLinksList.add("https://www.youtube.com/watch?v=eMcMbWl0fDk");
        songLinksList.add("https://www.youtube.com/watch?v=50VNCymT-Cs");
        songLinksList.add("https://www.youtube.com/watch?v=vNoKguSdy4Y");
        songLinksList.add("https://www.youtube.com/watch?v=XPBwXKgDTdE");
        songLinksList.add("https://www.youtube.com/watch?v=vZ_NpLWuL00");
        songLinksList.add("https://www.youtube.com/watch?v=aatr_2MstrI");
        songLinksList.add("https://www.youtube.com/watch?v=ho0WBKPJtfc");
        songLinksList.add("https://www.youtube.com/watch?v=lEi_XBg2Fpk");
        songLinksList.add("https://www.youtube.com/watch?v=JMd_51WictU");
        songLinksList.add("https://www.youtube.com/watch?v=7Gi_AS13OBo");
        songLinksList.add("https://www.youtube.com/watch?v=ft4jcPSLJfY");
        songLinksList.add("https://www.youtube.com/watch?v=aR-KAldshAE");
        songLinksList.add("https://www.youtube.com/watch?v=tp1ZluX4aYs");
        songLinksList.add("https://www.youtube.com/watch?v=lEgTtQFMjWw");
        songLinksList.add("https://www.youtube.com/watch?v=17i3bKKI4M0");
        songLinksList.add("https://www.youtube.com/watch?v=r8dBp0E7X-E");
        songLinksList.add("https://www.youtube.com/watch?v=7vJBau24uUE");
        songLinksList.add("https://www.youtube.com/watch?v=eC-F_VZ2T1c");
        songLinksList.add("https://www.youtube.com/watch?v=rn9AQoI7mYU");
        songLinksList.add("https://www.youtube.com/watch?v=Ho32Oh6b4jc");
        songLinksList.add("https://www.youtube.com/watch?v=U-PXEe-qeK4");
        songLinksList.add("https://www.youtube.com/watch?v=YqeW9_5kURI");
        songLinksList.add("https://www.youtube.com/watch?v=jaUu5tVtEhA");
        songLinksList.add("https://www.youtube.com/watch?v=zDB8dM3MBvg");
        songLinksList.add("https://www.youtube.com/watch?v=UpsKGvPjAgw");
        songLinksList.add("https://www.youtube.com/watch?v=vZ_NpLWuL00");
        songLinksList.add("https://www.youtube.com/watch?v=hYbHzzWmKUs");
        songLinksList.add("https://www.youtube.com/watch?v=g5qU7p7yOY8");
        songLinksList.add("https://www.youtube.com/watch?v=S_E2EHVxNAE");
        songLinksList.add("https://www.youtube.com/watch?v=h--P8HzYZ74");
        songLinksList.add("https://www.youtube.com/watch?v=H7HmzwI67ec");
        songLinksList.add("https://www.youtube.com/watch?v=PVxc5mIHVuQ");
        songLinksList.add("https://www.youtube.com/watch?v=XyL3YKK_1BI");
        songLinksList.add("https://www.youtube.com/watch?v=RBumgq5yVrA");
        songLinksList.add("https://www.youtube.com/watch?v=tSc8WROtNfc");
        songLinksList.add("https://www.youtube.com/watch?v=e2vBLd5Egnk");
        songLinksList.add("https://www.youtube.com/watch?v=_V3_7R-oB0g");
        songLinksList.add("https://www.youtube.com/watch?v=fRh_vgS2dFE");
        songLinksList.add("https://www.youtube.com/watch?v=qV5lzRHrGeg");
        songLinksList.add("https://www.youtube.com/watch?v=K4DyBUG242c");
        songLinksList.add("https://www.youtube.com/watch?v=SMs0GnYze34");
        songLinksList.add("https://www.youtube.com/watch?v=2pPozfpsUhk");
        songLinksList.add("https://www.youtube.com/watch?v=7YT5F20NnsI");
        songLinksList.add("https://www.youtube.com/watch?v=35gCiF22P0k");
        songLinksList.add("https://www.youtube.com/watch?v=gwpTPrPoy4Y");
        songLinksList.add("https://www.youtube.com/watch?v=qgy7vEje5-w");
        songLinksList.add("https://www.youtube.com/watch?v=ZrM9JmKwpHs");
        songLinksList.add("https://www.youtube.com/watch?v=xh8Jp7QOEeQ");
        songLinksList.add("https://www.youtube.com/watch?v=TdyllLZeviY");
        songLinksList.add("https://www.youtube.com/watch?v=2YvG0NbYJpE");
        songLinksList.add("https://www.youtube.com/watch?v=DyDfgMOUjCI");
        songLinksList.add("https://www.youtube.com/watch?v=m70voYPCBQk");
        songLinksList.add("https://www.youtube.com/watch?v=VJ2rlci9PE0");
        songLinksList.add("https://www.youtube.com/watch?v=e3lgNmuajrg");
        songLinksList.add("https://www.youtube.com/watch?v=R4VJyfCcOJc");
        songLinksList.add("https://www.youtube.com/watch?v=YQHsXMglC9A");
        songLinksList.add("https://www.youtube.com/watch?v=hEdvvTF5js4");
        songLinksList.add("https://www.youtube.com/watch?v=LdH7aFjDzjI");
        songLinksList.add("https://www.youtube.com/watch?v=DJqWZ3Zy5CU");
        songLinksList.add("https://www.youtube.com/watch?v=ZJlQ92HpdFA");
        songLinksList.add("https://www.youtube.com/watch?v=CwfoyVa980U");
        songLinksList.add("https://www.youtube.com/watch?v=YQHsXMglC9A");
        songLinksList.add("https://www.youtube.com/watch?v=xeUA7Hn9DsE");
        songLinksList.add("https://www.youtube.com/watch?v=LkGV1D-0YA8");
        songLinksList.add("https://www.youtube.com/watch?v=ckIM58Ecpcw");
        songLinksList.add("https://www.youtube.com/watch?v=1OPql3ks-0s");
        songLinksList.add("https://www.youtube.com/watch?v=GzU8KqOY8YA");
        songLinksList.add("https://www.youtube.com/watch?v=fKopy74weus");
        songLinksList.add("https://www.youtube.com/watch?v=K5KAc5CoCuk");
        songLinksList.add("https://www.youtube.com/watch?v=wzumsXtyCW4");
        songLinksList.add("https://www.youtube.com/watch?v=coQ95u7w_18");
        songLinksList.add("https://www.youtube.com/watch?v=qk9ZChZ-1S8");
        songLinksList.add("https://www.youtube.com/watch?v=6ttobrfMnyQ");
        songLinksList.add("https://www.youtube.com/watch?v=RLcdPpjKKHo");
        songLinksList.add("https://www.youtube.com/watch?v=AeA0ZKzAeH4");
        songLinksList.add("https://www.youtube.com/watch?v=CwfoyVa980U");
        songLinksList.add("https://www.youtube.com/watch?v=9bZkp7q19f0");
        songLinksList.add("https://www.youtube.com/watch?v=9bZkp7q19f0");
        songLinksList.add("https://www.youtube.com/watch?v=KnuWiuPX0Z4");
        songLinksList.add("https://www.youtube.com/watch?v=YJVmu6yttiw");
        songLinksList.add("https://www.youtube.com/watch?v=4uAKDBsq434");
        songLinksList.add("https://www.youtube.com/watch?v=3AtDnEC4zak");
        songLinksList.add("https://www.youtube.com/watch?v=6Mgqbai3fKo");
        songLinksList.add("https://www.youtube.com/watch?v=ZHMDLOOP69I");
        songLinksList.add("https://www.youtube.com/watch?v=2ll1DrlZgqk");
        songLinksList.add("https://www.youtube.com/watch?v=ZJlQ92HpdFA");
        songLinksList.add("https://www.youtube.com/watch?v=84yTsE4eNYQ");
        songLinksList.add("https://www.youtube.com/watch?v=1CptfMEEC8g");
        songLinksList.add("https://www.youtube.com/watch?v=4ughEPQGd8w");
        songLinksList.add("https://www.youtube.com/watch?v=B1Jrf2Pz5vU");
        songLinksList.add("https://www.youtube.com/watch?v=R4VJyfCcOJc");
        songLinksList.add("https://www.youtube.com/watch?v=D5drYkLiLI8");
        songLinksList.add("https://www.youtube.com/watch?v=LjhCEhWiKXk");
        songLinksList.add("https://www.youtube.com/watch?v=Jr4TMIU9oQ4");
        songLinksList.add("https://www.youtube.com/watch?v=D9syciL3Xsg");
        songLinksList.add("https://www.youtube.com/watch?v=vIyP_joi3AI");
        songLinksList.add("https://www.youtube.com/watch?v=Z8eXaXoUJRQ");
        songLinksList.add("https://www.youtube.com/watch?v=ij_0p_6qTss");
        songLinksList.add("https://www.youtube.com/watch?v=wJnBTPUQS5A");
        songLinksList.add("https://www.youtube.com/watch?v=IAuRoAUV19o");
        songLinksList.add("https://www.youtube.com/watch?v=S_E2EHVxNAE");
        songLinksList.add("https://www.youtube.com/watch?v=dhYOPzcsbGM");
        songLinksList.add("https://www.youtube.com/watch?v=Z8eXaXoUJRQ");
        songLinksList.add("https://www.youtube.com/watch?v=z8OdasLT_BM");
        songLinksList.add("https://www.youtube.com/watch?v=6ttobrfMnyQ");
        songLinksList.add("https://www.youtube.com/watch?v=UCkkt2sBvKM");
        songLinksList.add("https://www.youtube.com/watch?v=peByeoQhjMM");
        songLinksList.add("https://www.youtube.com/watch?v=6VC1kCodFtI");
        songLinksList.add("https://www.youtube.com/watch?v=dT2owtxkU8k");
        songLinksList.add("https://www.youtube.com/watch?v=CLiXUT3MS34");
        songLinksList.add("https://www.youtube.com/watch?v=59Vf7oQV9pg");
        songLinksList.add("https://www.youtube.com/watch?v=CLiXUT3MS34");
        songLinksList.add("https://www.youtube.com/watch?v=QcIy9NiNbmo");
        songLinksList.add("https://www.youtube.com/watch?v=eMcMbWl0fDk");
        songLinksList.add("https://www.youtube.com/watch?v=9eakEQSBR8o");
        songLinksList.add("https://www.youtube.com/watch?v=YyhKdOCwD7s");
        songLinksList.add("https://www.youtube.com/watch?v=WihX3peej0Q");
        songLinksList.add("https://www.youtube.com/watch?v=-2U0Ivkn2Ds");
        songLinksList.add("https://www.youtube.com/watch?v=0KSOMA3QBU0");
        songLinksList.add("https://www.youtube.com/watch?v=BmRtyiSlqiA");
        songLinksList.add("https://www.youtube.com/watch?v=2S24-y0Ij3Y");
        songLinksList.add("https://www.youtube.com/watch?v=IHNzOHi8sJs");
        songLinksList.add("https://www.youtube.com/watch?v=hEdvvTF5js4");
        songLinksList.add("https://www.youtube.com/watch?v=e2CnyvhNu8g");
        songLinksList.add("https://www.youtube.com/watch?v=EjaVLrF7cB8");
        songLinksList.add("https://www.youtube.com/watch?v=HibDj27DHMI");
        songLinksList.add("https://www.youtube.com/watch?v=G_7CprD5dg4");
        songLinksList.add("https://www.youtube.com/watch?v=qkEW-1jK_rw");
        songLinksList.add("https://www.youtube.com/watch?v=uAVUl0cAKpo");
        songLinksList.add("https://www.youtube.com/watch?v=7wtfhZwyrcc");
        songLinksList.add("https://www.youtube.com/watch?v=UxxajLWwzqY");
        songLinksList.add("https://www.youtube.com/watch?v=vOXZkm9p_zY");
        songLinksList.add("https://www.youtube.com/watch?v=60ItHLz5WEA");
        songLinksList.add("https://www.youtube.com/watch?v=kJQP7kiw5Fk");
        songLinksList.add("https://www.youtube.com/watch?v=f3SM3_JdlyM");
        songLinksList.add("https://www.youtube.com/watch?v=rBcP024h1Mo");
        songLinksList.add("https://www.youtube.com/watch?v=yw04QD1LaB0");
        songLinksList.add("https://www.youtube.com/watch?v=mBZdHuZCfic");
        songLinksList.add("https://www.youtube.com/watch?v=ffxKSjUwKdU");
        songLinksList.add("https://www.youtube.com/watch?v=NuHTYUG76sg");
        songLinksList.add("https://www.youtube.com/watch?v=pvP_OwVSFpk");
        songLinksList.add("https://www.youtube.com/watch?v=cK47V9iYXLI");
        songLinksList.add("https://www.youtube.com/watch?v=lY2yjAdbvdQ");
        songLinksList.add("https://www.youtube.com/watch?v=m-el0pQLQE4");
        songLinksList.add("https://www.youtube.com/watch?v=BmRtyiSlqiA");
        songLinksList.add("https://www.youtube.com/watch?v=lmylJZ5WMsQ");
        songLinksList.add("https://www.youtube.com/watch?v=F7fBsN_E60Q");
        songLinksList.add("https://www.youtube.com/watch?v=FNx_yqUbAFM");
        songLinksList.add("https://www.youtube.com/watch?v=1OPql3ks-0s");
        songLinksList.add("https://www.youtube.com/watch?v=r1Fx0tqK5Z4");
        songLinksList.add("https://www.youtube.com/watch?v=x8F5dz8kv1w");
        songLinksList.add("https://www.youtube.com/watch?v=_s7rwesgVRQ");
        songLinksList.add("https://www.youtube.com/watch?v=wIft-t-MQuE");
        songLinksList.add("https://www.youtube.com/watch?v=u09k1EdDmlY");
        songLinksList.add("https://www.youtube.com/watch?v=wJnBTPUQS5A");
        songLinksList.add("https://www.youtube.com/watch?v=Vf78alvpxRM");
        songLinksList.add("https://www.youtube.com/watch?v=psoxDNP7EL0");
        songLinksList.add("https://www.youtube.com/watch?v=OfS1jFck8YQ");
        songLinksList.add("https://www.youtube.com/watch?v=9bZkp7q19f0");
        songLinksList.add("https://www.youtube.com/watch?v=zsQNyyJcR4A");
        songLinksList.add("https://www.youtube.com/watch?v=zczlDTKvOa8");
        songLinksList.add("https://www.youtube.com/watch?v=OjDuQ7O9XUo");
        songLinksList.add("https://www.youtube.com/watch?v=e-ORhEE9VVg");
        songLinksList.add("https://www.youtube.com/watch?v=2YvG0NbYJpE");
        songLinksList.add("https://www.youtube.com/watch?v=xh8Jp7QOEeQ");
        songLinksList.add("https://www.youtube.com/watch?v=Rb6Scz-5YOs");
        songLinksList.add("https://www.youtube.com/watch?v=N1kpeRhqVzI");
        songLinksList.add("https://www.youtube.com/watch?v=3AtDnEC4zak");
        songLinksList.add("https://www.youtube.com/watch?v=NfTS7gM7zQ0");
        songLinksList.add("https://www.youtube.com/watch?v=HibDj27DHMI");
        songLinksList.add("https://www.youtube.com/watch?v=yaJx0Gj_LCY");
        songLinksList.add("https://www.youtube.com/watch?v=ZlH78Tm_oUk");
        songLinksList.add("https://www.youtube.com/watch?v=heVJaMvCmR0");
        songLinksList.add("https://www.youtube.com/watch?v=4qeaBFFq3to");
        songLinksList.add("https://www.youtube.com/watch?v=W7ey2JlJ_GM");
        songLinksList.add("https://www.youtube.com/watch?v=lUotEgzKmG8");
        songLinksList.add("https://www.youtube.com/watch?v=-35jibKqbEo");
        songLinksList.add("https://www.youtube.com/watch?v=kCArjJPYs-U");
        songLinksList.add("https://www.youtube.com/watch?v=GmdBXhT3-mQ");
        songLinksList.add("https://www.youtube.com/watch?v=M-P4QBt-FWw");
        songLinksList.add("https://www.youtube.com/watch?v=YPTp4okj-eM");
        songLinksList.add("https://www.youtube.com/watch?v=n-D1EB74Ckg");
        songLinksList.add("https://www.youtube.com/watch?v=R7xbhKIiw4Y");
        songLinksList.add("https://www.youtube.com/watch?v=K5KAc5CoCuk");
        songLinksList.add("https://www.youtube.com/watch?v=NdZGajBmIWM");
        songLinksList.add("https://www.youtube.com/watch?v=E3x_dLVTEuA");
        songLinksList.add("https://www.youtube.com/watch?v=A-lebYNcgBk");
        songLinksList.add("https://www.youtube.com/watch?v=rjBsQ9SygnE");
        songLinksList.add("https://www.youtube.com/watch?v=6Mgqbai3fKo");
        songLinksList.add("https://www.youtube.com/watch?v=f1knjN8U1mA");
        songLinksList.add("https://www.youtube.com/watch?v=i7wveOu5hkQ");
        songLinksList.add("https://www.youtube.com/watch?v=ngORmvyvAaI");
        songLinksList.add("https://www.youtube.com/watch?v=4e8w1dT2npE");
        songLinksList.add("https://www.youtube.com/watch?v=FN3bTP84GCU");
        songLinksList.add("https://www.youtube.com/watch?v=sV4VuT1p2xs");
        songLinksList.add("https://www.youtube.com/watch?v=1z8NpmCqvZE");
        songLinksList.add("https://www.youtube.com/watch?v=nfs8NYg7yQM");
        songLinksList.add("https://www.youtube.com/watch?v=ckIM58Ecpcw");
        songLinksList.add("https://www.youtube.com/watch?v=ifCAfAzOBJM");
        songLinksList.add("https://www.youtube.com/watch?v=FRjOSmc01-M");
        songLinksList.add("https://www.youtube.com/watch?v=R91JRfu5sWc");
        songLinksList.add("https://www.youtube.com/watch?v=e_vl5aFXB4Q");
        songLinksList.add("https://www.youtube.com/watch?v=G_7CprD5dg4");
        songLinksList.add("https://www.youtube.com/watch?v=RLcdPpjKKHo");
        songLinksList.add("https://www.youtube.com/watch?v=bKDdT_nyP54");
        songLinksList.add("https://www.youtube.com/watch?v=4qeaBFFq3to");
        songLinksList.add("https://www.youtube.com/watch?v=6RLLOEzdxsM");
        songLinksList.add("https://www.youtube.com/watch?v=SmM0653YvXU");
        songLinksList.add("https://www.youtube.com/watch?v=rdXZ0kkVk4k");
        songLinksList.add("https://www.youtube.com/watch?v=j9V78UbdzWI");
        songLinksList.add("https://www.youtube.com/watch?v=-II6fv8NMfY");
        songLinksList.add("https://www.youtube.com/watch?v=5TJ5-6lqLFw");
        songLinksList.add("https://www.youtube.com/watch?v=n-D1EB74Ckg");
        songLinksList.add("https://www.youtube.com/watch?v=vtNJMAyeP0s");
        songLinksList.add("https://www.youtube.com/watch?v=z8OdasLT_BM");
        songLinksList.add("https://www.youtube.com/watch?v=6sxDpnrWSUo");
        songLinksList.add("https://www.youtube.com/watch?v=Mc2-YM9Bhu4");
        songLinksList.add("https://www.youtube.com/watch?v=6VC1kCodFtI");
        songLinksList.add("https://www.youtube.com/watch?v=6iz9dUKDSMU");
        songLinksList.add("https://www.youtube.com/watch?v=Pkh8UtuejGw");
        songLinksList.add("https://www.youtube.com/watch?v=dT2owtxkU8k");
        songLinksList.add("https://www.youtube.com/watch?v=PonUS87Yeqw");
        songLinksList.add("https://www.youtube.com/watch?v=AgFeZr5ptV8");
        songLinksList.add("https://www.youtube.com/watch?v=rTogiSQy4Lw");
        songLinksList.add("https://www.youtube.com/watch?v=0KSOMA3QBU0");
        songLinksList.add("https://www.youtube.com/watch?v=1z8NpmCqvZE");
        songLinksList.add("https://www.youtube.com/watch?v=ioNng23DkIM");
        songLinksList.add("https://www.youtube.com/watch?v=FRjOSmc01-M");
        songLinksList.add("https://www.youtube.com/watch?v=17pWrHK3udE");
        songLinksList.add("https://www.youtube.com/watch?v=ZlH78Tm_oUk");
        songLinksList.add("https://www.youtube.com/watch?v=ASO_zypdnsQ");
        songLinksList.add("https://www.youtube.com/watch?v=LwjzmdiRFpc");
        songLinksList.add("https://www.youtube.com/watch?v=2i2khp_npdE");
        songLinksList.add("https://www.youtube.com/watch?v=oOlmtJRB8oI");
        songLinksList.add("https://www.youtube.com/watch?v=1-xGerv5FOk");
        songLinksList.add("https://www.youtube.com/watch?v=xiwRprisLwY");
        songLinksList.add("https://www.youtube.com/watch?v=pXvoeCgi59o");
        songLinksList.add("https://www.youtube.com/watch?v=cK47V9iYXLI");
        songLinksList.add("https://www.youtube.com/watch?v=lY2yjAdbvdQ");
        songLinksList.add("https://www.youtube.com/watch?v=yTv0PiRCPjg");
        songLinksList.add("https://www.youtube.com/watch?v=D5drYkLiLI8");
        songLinksList.add("https://www.youtube.com/watch?v=IHNzOHi8sJs");
        songLinksList.add("https://www.youtube.com/watch?v=yIIGQB6EMAM");
        songLinksList.add("https://www.youtube.com/watch?v=Hh2Sxuzqqw0");
        songLinksList.add("https://www.youtube.com/watch?v=kOkQ4T5WO9E");
        songLinksList.add("https://www.youtube.com/watch?v=bE1Fz03cT6o");
        songLinksList.add("https://www.youtube.com/watch?v=X-YtdTMJfhM");
        songLinksList.add("https://www.youtube.com/watch?v=Bvz67EULTJc");
        songLinksList.add("https://www.youtube.com/watch?v=cH4E_t3m3xM");
        songLinksList.add("https://www.youtube.com/watch?v=ox4tmEV6-QU");
        songLinksList.add("https://www.youtube.com/watch?v=EjaVLrF7cB8");
        songLinksList.add("https://www.youtube.com/watch?v=Mc2-YM9Bhu4");
        songLinksList.add("https://www.youtube.com/watch?v=BQ0mxQXmLsk");
        songLinksList.add("https://www.youtube.com/watch?v=78gZvPCM4Ho");
        songLinksList.add("https://www.youtube.com/watch?v=Jr4TMIU9oQ4");
        songLinksList.add("https://www.youtube.com/watch?v=rDWuqrJAyGw");
        songLinksList.add("https://www.youtube.com/watch?v=eoFkN53mMPA");
        songLinksList.add("https://www.youtube.com/watch?v=NqI1uCwOdkY");
        songLinksList.add("https://www.youtube.com/watch?v=LaVPQe8Zf9o");
    }

    /*private class GetSongMetaData extends AsyncTask<String, Void, String> {

        private String error = null;
        private String songName, songAlbumName, songCoverUrl;
        private ProgressDialog progressDialog;

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

            String input = "/sdcard/Songs/Indila - Dernière Danse (Clip Officiel) ( 160kbps ).mp3";

            input = input.replace("-", " ")
                    .replace("_", " ")
                    .replace(".mp3", " ")
                    .replace(".m4a", " ")
                    .replace("( 256kbps cbr )", " ")
                    .replace("( 160kbps )", " ")
                    .replace("(Official Video)", " ")
                    .replace("(256k)", " ")
                    .replace("( 128kbps )", " ")
                    .replace("y2mate.com", " ")
                    .replace(",", " ")
                    .replace("/sdcard/Songs/", " ")
                    .trim();

            if (Character.isDigit(input.charAt(0)))
                input = input.substring(1);

            if (Character.isDigit(input.charAt(0)))
                input = input.substring(1);

            String output = input.replaceAll("\\s+", " ").trim();
            Log.i(TAG, "doInBackground: simple output: " + output);
            try {
                output = URLEncoder.encode(output, "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "doInBackground: encoded output: " + output);

            String wholeUrl = "https://www.youtube.com/results?search_query=" + output;
            Log.i(TAG, "doInBackground: wholeUrl: " + wholeUrl);

            String htmlData = sh.makeServiceCall(wholeUrl);
//            String htmlData = getHtmlString(wholeUrl);
            Log.i(TAG, "doInBackground: htmlData: " + htmlData);

            String ytID = getWebsiteUrl(htmlData);
            Log.i(TAG, "doInBackground: ytID: " + ytID);

            String url = "https://www.youtube.com/watch?v=" + ytID;

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

            Toast.makeText(context, "Name: " + songName + "\nAlbumName: " + songAlbumName + "\nCoverUrl: " + songCoverUrl, Toast.LENGTH_LONG).show();

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

    private String getHtmlString(String url) {
        URL google = null;
        try {
            google = new URL(url);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(google != null ? google.openStream() : null));
        } catch (final IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        String input = null;
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if ((input = in != null ? in.readLine() : null) == null) break;
                }
            } catch (final IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            stringBuffer.append(input);
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (final IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        String htmlData = stringBuffer.toString();

        return htmlData;
    }

    private String getWebsiteUrl(String data) {
        Pattern p = Pattern.compile("href=\"/watch?v=(.*?)\"");
        Matcher m = p.matcher(data);
//            Matcher m = p.matcher(splitResult[0]);

        String str = "null";

        while (m.find()) {
            str = m.group(1);
            break;
        }

        return str;
    }*/

//    ArrayList<String> song_names = new ArrayList<>();

    /*private void fillArrayList() {
        ArrayList<String> song_names = new ArrayList<>();

        song_names.add("/sdcard/Songs/Nightcore - Helplessly - (Lyrics) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/DJ Snake - You Know You Like It.mp3");
        song_names.add("/sdcard/Songs/DJ Snake, Lauv - A Different Way (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Eminem ft. Rihanna - The Monster (Explicit) _Official Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Twist- Love Aaj Kal ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/DJ Snake, Lauv - A Different Way (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/DJ Snake, Lil Jon - Turn Down for What ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Enchantress_-_Everybody_wants_to_rule_the_world__28From_Suicide_Squad_3A_The_Album_29_.mp3");
        song_names.add("/sdcard/Songs/Tyga x Curtis Roach - Bored In The House (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Ariana_Grande,_The_Weeknd_-_Love_Me_Harder.mp3");
        song_names.add("/sdcard/Songs/INNA_-_Nirvana_-_Official_Music_Video(256k).mp3");
        song_names.add("/sdcard/Songs/Enrique Iglesias - Do You Know");
        song_names.add("/sdcard/Songs/Unknown Brain - Inspiration.mp3");
        song_names.add("/sdcard/Songs/Do Bol Official OST _ Nabeel Shaukat & Aima Baig _ ARY Digital ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/David Guetta, Bebe Rexha & J Balvin - Say My Name (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Usher_-_Yeah!_(Official_Music_Video)_ft._Lil_Jon,_Ludacris(256k).mp3");
        song_names.add("/sdcard/Songs/Enrique Iglesias - Do You Know_ (The Ping Pong Song) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Don_t_Let_Me_Down-_The_Chainsmokers_ft._Daya_Lyrics.mp3");
        song_names.add("/sdcard/Songs/Ariana_Grande_-_Problem_ft._Iggy_Azalea.mp3");
        song_names.add("/sdcard/Songs/INNA_-_Ruleta_(feat._Erik)_-_Official_Music_Video(256k).mp3");
        song_names.add("/sdcard/Songs/Daya - New ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Dua Lipa - New Rules.mp3");
        song_names.add("/sdcard/Songs/Enrique Iglesias - Heart Attack.mp3");
        song_names.add("/sdcard/Songs/Vaaste Full Song With Lyrics Dhvani Bhanushali _ Nikhil D’Souza ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/dummyDiagnosisFile1");
        song_names.add("/sdcard/Songs/Enrique Iglesias Dance Mix 2014 ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Daya - New.mp3");
        song_names.add("/sdcard/Songs/Vicetone_&_Tony_Igy_-_Astronomia(256k).mp3");
        song_names.add("/sdcard/Songs/Ariana_Grande_-_Side_To_Side_ft._Nicki_Minaj.mp3");
        song_names.add("/sdcard/Songs/INNA_feat._Yandel_-_In_Your_Eyes_-_Official_Music_Video(256k).mp3");
        song_names.add("/sdcard/Songs/Walk_It_Out.mp3");
        song_names.add("/sdcard/Songs/Echos - Graves ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/violin kate chruscicka.mp3");
        song_names.add("/sdcard/Songs/Enrique_Iglesias_-_Do_You_Know__(The_Ping_Pong_Song).mp3");
        song_names.add("/sdcard/Songs/Daya - Sit Still, Look Pretty ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Vitas - The 7th Element ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Ed Sheeran & Travis Scott - Antisocial _Official Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Enrique_Iglesias_-_Heart_Attack.mp3");
        song_names.add("/sdcard/Songs/Inner Circle- Bad Boys ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Armin van Buuren - Turn It Up (Official Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Ed Sheeran - Shape of You.mp3");
        song_names.add("/sdcard/Songs/Enrique_Iglesias_-_Maybe.mp3");
        song_names.add("/sdcard/Songs/Daya - Sit Still, Look Pretty _LYRICS_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Warriyo - Mortals (feat. Laura Brehm) _NCS Release_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Elefante_-_El_Tiger_ft._Neha_Khankriyal_(Official_Video)(256k).mp3");
        song_names.add("/sdcard/Songs/Everybody Knows - Sigrid Clip HD (Justice League) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Asim Azhar - Jo Tu Na Mila ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Warriyo - Mortals (feat. Laura Brehm) _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Ellie Goulding - Burn (Audio) _HQ_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Eyes Wide Open - Red (Official Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/J Balvin, Willy William - Mi Gente (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Daya-_sit_still_look_pretty_lyrics.mp3");
        song_names.add("/sdcard/Songs/Ellie Goulding - Burn.mp3");
        song_names.add("/sdcard/Songs/We_ain_t_ever_getting_older_-_Damon_and_Elena.mp3");
        song_names.add("/sdcard/Songs/ASPHALT 9 LEGENDS - NOOB VS PRO VS HACKER PT - 2 ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Faith Marie - Devil On My Shoulder (Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Daya_-_New.mp3");
        song_names.add("/sdcard/Songs/J Balvin, Willy William - Mi Gente (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Ellie Goulding - Love Me Like You Do (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/What So Not x GANZ - Lone (feat. JOY.) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/FAST_AND_FURIOUS_8_-TRAILER_SONG_(_Bassnectar_-_Speakerbox_ft._Lafa_Taylor_-_INT.mp3");
        song_names.add("/sdcard/Songs/Ellie_Goulding_-_Love_Me_Like_You_Do_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Assassin_27s_Creed_Unity_E3_2014_World_Premiere_Cinematic_Trailer__5BEUROPE_5D.mp3");
        song_names.add("/sdcard/Songs/Defqwop - Heart Afire (ft. Strix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Fast___Furious_7_-_Get_Low_Extended_Version_Video.mp3");
        song_names.add("/sdcard/Songs/Avicii - Wake Me Up (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Demi Lovato - Confident (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Wiz Khalifa - See You Again (feat. Charlie Puth).mp3");
        song_names.add("/sdcard/Songs/Woh_Beete_Din_Yaad_Hain_-_Purana_Mandir(256k).mp3");
        song_names.add("/sdcard/Songs/Emdi x Coorby - Lonewolf (feat. Kristi-Leah) _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Fifth Harmony - Work from Home ft. Ty Dolla $ign ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Avicii - You Make Me.mp3");
        song_names.add("/sdcard/Songs/y2mate.com - Phirta Rahoon Dar Badar Milta Nahin (720p) The Killer HD_LaIEfcUCywk.mp3");
        song_names.add("/sdcard/Songs/Lorde - Royals.mp3");
        song_names.add("/sdcard/Songs/DHARIA - Sugar & Brownies (by Monoir) _Official Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Avicii_-_You_Make_Me_(Official).mp3");
        song_names.add("/sdcard/Songs/Lorde - Yellow Flicker Beat.mp3");
        song_names.add("/sdcard/Songs/Different Heaven & EH!DE - My Heart _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/TheFatRat - Fly Away feat. Anjulie KARAOKE ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Lady Gaga - Bad Romance (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/TheFatRat - Monody (feat. Laura Brehm) ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Lost Kings - First Love (Official Video) ft. Sabrina Carpenter ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lady Gaga - Poker Face.mp3");
        song_names.add("/sdcard/Songs/Janji - Heroes Tonight (feat. Johnning) _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lady GaGa - The Fame Monster - Bad Romance ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Dillon Francis - Get Low.mp3");
        song_names.add("/sdcard/Songs/Backstreet Boys - Show Me The Meaning Of Being Lonely (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The_Chainsmokers_-_Waterbed.mp3");
        song_names.add("/sdcard/Songs/Dj Kantik - Kul (Original Mix) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Laung Laachi Lyrics With Translation   Mannat Noor   Ammy Virk, Neeru Bajwa ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/The_Pussycat_Dolls_-_Buttons_ft._Snoop_Dogg.mp3");
        song_names.add("/sdcard/Songs/Luis Fonsi - Despacito ft. Daddy Yankee ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Lawson - Where My Love Goes ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Tones_And_I_-_Dance_Monkey_(Lyrics)(256k).mp3");
        song_names.add("/sdcard/Songs/Jarico - Landscape _NCS BEST OF_ ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Dj Kantik - Kul (Original Mix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Travis_Scott_feat._Kendrick_Lamar_-_Goosebumps__28NGHTMRE_Remix_29.mp3");
        song_names.add("/sdcard/Songs/Trevor Daniel - Falling (Lyrics) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Bashie - Waaraf ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Triplo_Max_-_Shadow_(Official_Music_Video)(256k).mp3");
        song_names.add("/sdcard/Songs/TroyBoi - Afterhours (feat. Diplo & Nina Sky) _Official Music Video_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Beauty and the Beast (From _Beauty and the Beast__Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie_Puth_-_We_Don_t_Talk_Anymore_(feat._Selena_Gomez)_[Official_Video].mp3");
        song_names.add("/sdcard/Songs/TroyBoi - Remember ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - ME! (feat. Brendon Urie of Panic! At The Disco) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie_Puth_-__How_Long__[Official_Video].mp3");
        song_names.add("/sdcard/Songs/Zedd_-_Beautiful_Now_ft._Jon_Bellion.mp3");
        song_names.add("/sdcard/Songs/Zedd_-_Clarity_(Official_Video)_ft._Foxes.mp3");
        song_names.add("/sdcard/Songs/Alan_Walker_-_Faded_(Lyrics_Video).mp3");
        song_names.add("/sdcard/Songs/Zedd_-_I_Want_You_To_Know_ft._Selena_Gomez.mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - This Is Why We Can’t Have Nice Things.mp3");
        song_names.add("/sdcard/Songs/Choti Si Qayamat OST By Ahmed Jahanzeb.mp3");
        song_names.add("/sdcard/Songs/Alan_Walker_-_Sing_Me_To_Sleep.mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - We Are Never Ever Getting Back Together.mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - You Belong With Me.mp3");
        song_names.add("/sdcard/Songs/Alan_Walker_-_Sing_Me_To_Sleep__28marshmello_Remix_29.mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Bad_Blood_ft._Kendrick_Lamar.mp3");
        song_names.add("/sdcard/Songs/ZOMBIES - Someday (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Citizen_Way_-_I_Will__28Official_Lyric_Video_29.mp3");
        song_names.add("/sdcard/Songs/Alec Benjamin - Let Me Down Slowly _Official Music Video_ ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/CIX, What You Wanted _THE SHOW 190730_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Cyndi Wang - 當你.mp3");
        song_names.add("/sdcard/Songs/What_a_wonderful_world_-_girl_21_aMaZinG_-_AnnA_GracemaN.mp3");
        song_names.add("/sdcard/Songs/Ja Humse Juda Hoke Lyrics _ Jubin Nautiyal _  Full HD ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Ellie_Goulding_-_Still_Falling_For_You.mp3");
        song_names.add("/sdcard/Songs/WHISTLE - Blackpink ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Feki - Into the Light ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Emaan _ OST _ LTN Family _ New Drama Serial ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Fifth Harmony - Work from Home (Official Video) ft. Ty Dolla $ign ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Jackie Evancho - What a Wonderful World (from Music of the Movies) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Avicii - Wake Me Up - Radio Edit.mp3");
        song_names.add("/sdcard/Songs/DHARIA - Sugar & Brownies (by Monoir) _Official Video_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Jackie_Evancho_-_What_a_Wonderful_World_-_Lancaster_2C_KY_11-21_2015.mp3");
        song_names.add("/sdcard/Songs/Fifth Harmony - Worth It (Official Video) ft. Kid Ink ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Zack Knight - Galtiyan (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Fifth Harmony - Worth It ft. Kid Ink ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/JADO TAK - NACHHATAR GILL - FULL HD VIDEO ( 480 X 640 ).mp4");
        song_names.add("/sdcard/Songs/TheFatRat - Fly Away feat. Anjulie ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lorde_-_Royals__28US_Version_29.mp3");
        song_names.add("/sdcard/Songs/Flux Pavilion - Blow The Roof _Official EP Mix Video_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Axwell Λ Ingrosso - More Than You Know (Lyrics) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lorde_-_Yellow_Flicker_Beat__28Hunger_Games_29.mp3");
        song_names.add("/sdcard/Songs/James Arthur - Say You Won't Let Go (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Baauer - One Touch (ft. AlunaGeorge & Rae Sremmurd) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/DILBAR Lyrical _ Satyameva Jayate _John Abraham, Nora Fatehi,Tanishk B, Neha Kakkar,Dhvani, Ikka ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/TheFatRat - Monody (feat. Laura Brehm) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lost_Kings_-_First_Love_(Official_Video)_ft._Sabrina_Carpenter.mp3");
        song_names.add("/sdcard/Songs/The_Chainsmokers_-_All_We_Know_ft._Phoebe_Ryan.mp3");
        song_names.add("/sdcard/Songs/Mackenzie Ziegler - TEAMWORK.mp3");
        song_names.add("/sdcard/Songs/LSD - Thunderclouds (Official Video) ft. Sia, Diplo, Labrinth ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Latin Pop All-Stars - Maybe (Made Famous by Enrique Iglesias).mp3");
        song_names.add("/sdcard/Songs/The_Chainsmokers_-_Setting_Fires__28Lyric_29_ft._XYL_C3_98.mp3");
        song_names.add("/sdcard/Songs/Lucky (feat. Colbie Caillat) Jason Mraz  _Official Video_ ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Tiptoe_through_the_tulips-_Tiny_Tim_LYRICS_ON_SCREEN.mp3");
        song_names.add("/sdcard/Songs/Top 5 Amazing Covers By Kurt Hugo Schneider & Alex Goot _ KHS India ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Bang Bang ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Jarico_-_Landscape_[NCS_BEST_OF](256k).mp3");
        song_names.add("/sdcard/Songs/Dj Kantik - Shenai (Original) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Bang_Bang.mp3");
        song_names.add("/sdcard/Songs/Dj Kantik - Shenai (Original) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Jason Mraz - Lucky (feat. Colbie Caillat) _Official Video_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Triplo Max - Shadow (Official Music Video) ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Bashie - Waaraf ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/DJ Snake - Taki Taki ft. Selena Gomez, Ozuna, Cardi B (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Jedi Mind Tricks _Design in Malice_ feat. Young Zee & Pacewon - Official Video ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/TroyBoi - Afterhours feat. Diplo & Nina Sky.mp3");
        song_names.add("/sdcard/Songs/Jessie Ware - Alone.mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - Look What You Made Me Do (Haschak Sisters Cover) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/TroyBoi - Remember ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan_Walker_-_Alone.mp3");
        song_names.add("/sdcard/Songs/Jetta - I'd Love to Change the World (Matstubs Remix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - Mine ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan_Walker_-_Faded_(Karaoke_Version)(128k).m4a");
        song_names.add("/sdcard/Songs/Jim Yosef & Anna Yvette - Linked _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - The Story Of Us.mp3");
        song_names.add("/sdcard/Songs/Cheat Codes - No Promises (feat. Demi Lovato).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - This Is Why We Can't Have Nice Things Audio ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Zedd_-_Done_With_Love_(Audio).mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Begin_Again.mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Look_What_You_Made_Me_Do.mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_The_Story_Of_Us.mp3");
        song_names.add("/sdcard/Songs/Clean Bandit - Solo (feat. Demi Lovato) _Official Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_You_Belong_With_Me.mp3");
        song_names.add("/sdcard/Songs/Coldplay - Hymn For The Weekend (Official Video) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Teriyaki_Boyz_-_Tokyo_Drift_(Dj_Kantik_Remix)(256k).mp3");
        song_names.add("/sdcard/Songs/Jocelyn Alice - I Know (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The Bilz & Kashif - Tera Nasha _ Official Video HD ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Jonas Blue – Mama (Lyrics) _musical_note_ ft. William Singe ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Danny Avila - End Of The Night (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Dizzy_-_Joakim_Karud_·_[Free_Copyright-safe_Music](256k).mp3");
        song_names.add("/sdcard/Songs/Maluma_-_Qué_Chimba_(Dj_Nev_&_Mula_Deejay_Remix)(256k).mp3");
        song_names.add("/sdcard/Songs/Justin Bieber - Baby.mp3");
        song_names.add("/sdcard/Songs/Mainu Ishq Da Lagya Rog VIDEO Song _ Tulsi Kumar _ Khushali Kumar _ T-Series ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Bassnectar - Speakerbox ft. Lafa Taylor - INTO THE SUN(MP3_160K)_1.mp3");
        song_names.add("/sdcard/Songs/OMER BALIK - Eyes Of You ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/OMER_BALIK_-_Eyes_Of_You(256k).mp3");
        song_names.add("/sdcard/Songs/Zara_Larsson_-_Lush_Life.mp3");
        song_names.add("/sdcard/Songs/Marshmello - FRIENDS.mp3");
        song_names.add("/sdcard/Songs/ZAYN - Dusk Till Dawn ft. Sia (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/ARASH_LABAF_feat._SNOOP_DOGG_-_OMG.mp3");
        song_names.add("/sdcard/Songs/ARASH_LABAF_feat_Helena_-_ONE_DAY.mp3");
        song_names.add("/sdcard/Songs/Zedd - Beautiful Now (Official Music Video) ft. Jon Bellion ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Zedd - Done With Love (Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Ariana Grande - Break Free.mp3");
        song_names.add("/sdcard/Songs/Manali Trance _ Yo Yo Honey Singh & Neha Kakkar _ The Shaukeens _ Lisa Haydon _ Akshay Kumar ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Zedd - I Want You To Know (Official Music Video) ft. Selena Gomez ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Marshvll_-_Eight_(feat._Sevnty).mp3");
        song_names.add("/sdcard/Songs/Ariana Grande - Problem.mp3");
        song_names.add("/sdcard/Songs/OneRepublic_-_What_You_Wanted__28Audio_29.mp3");
        song_names.add("/sdcard/Songs/Zedd - Transmission (Audio) ft. Logic, X Ambassadors ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Zedd, Maren Morris, Grey - The Middle (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Marshvll_-_Miss_Me_.mp3");
        song_names.add("/sdcard/Songs/Justin Bieber - Sorry.mp3");
        song_names.add("/sdcard/Songs/Carly Rae Jepsen - I Really Like You.mp3");
        song_names.add("/sdcard/Songs/Kane Cooper - You Only Live Once (Yolo) _Lyric video_ ft. Alex Holmes ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Meri Kahani Meri Zubani _ Bollywood Mix _ Music Video ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Lawson - Where My Love Goes ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/More Than Friends ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lawson_-_Where_My_Love_Goes.mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Don't Let Me Down ft. Daya (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Phoebe Ryan - Chronic (Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Nicki Minaj - Bed of Lies (Lyrics) ft. Skylar Grey ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Phoebe Ryan - Mine (Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Kaash tere ishq mein Full Song Gulam Jugni   New Song 2018   White Hill Music ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Everybody Hates Me - Khrebto Remix.mp3");
        song_names.add("/sdcard/Songs/Cash_Cash_-_How_To_Love_ft._Sofia_Reyes__28Boombox_Cartel_Remix_29.mp3");
        song_names.add("/sdcard/Songs/Lewis Capaldi - Someone You Loved (Lyrics) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Paris (Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Pitbull - Rain Over Me ft. Marc Anthony (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Nick_Jonas_-_Jealous.mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - This Feeling ft. Kelsea Ballerini (Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Waterbed.mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - Attention _Official Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - You Owe Me (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - Done For Me (feat. Kehlani) _Official Video_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/LORDE - Everybody Wants To Rule The World ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/8 - Taylor Swift - Gorgeous.mp3");
        song_names.add("/sdcard/Songs/Indila_-_S.O.S(256k).mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Blank_Space.mp3");
        song_names.add("/sdcard/Songs/_In The End_ Linkin Park Cinematic Cover (feat. Fleurie & Jung Youth) __ Produced by Tommee Profitt ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Gorgeous_(Lyric_Video).mp3");
        song_names.add("/sdcard/Songs/ANDREW_ALLEN_-_What_you_wanted__5BOfficial_lyric_video_5D.mp3");
        song_names.add("/sdcard/Songs/王心凌 Cyndi Wang _ 當你 _(2003)HD ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Teamwork - Mackenzie Ziegler (Official Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Dizzy_-_Joakim_Karud_·_[Free_Copyright-safe_Music](128k).mp3");
        song_names.add("/sdcard/Songs/Cleopatra_Stratan_-_Zunea-zunea_(Official_Video)(256k).mp3");
        song_names.add("/sdcard/Songs/That Poppy - Lowlife (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/d-devils_(_the_devil_ıs_a_dj_)(256k).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers & Coldplay - Something Just Like This (Lyric) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Closer (Lyric) ft. Halsey ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Jungle and Rainforest Sound Effects - Tropical Forest Ambiences from Costa Rica ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Mainu Ishq Da Lagya Rog   Tulsi Kumar lyrics ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Anne-Marie - 2002 _Official Video_ ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Anne-Marie - 2002.mp3");
        song_names.add("/sdcard/Songs/Maroon 5 - Don't Wanna Know ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Such_a_Whore_(Stellular_Remix)_[Bass_Boosted](256k).mp3");
        song_names.add("/sdcard/Songs/Justin Bieber - Company (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Mars Argo - Using You (Official) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Zara Larsson - All the Time ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Zedd - Stay The Night - Featuring Hayley Williams Of Paramore.mp3");
        song_names.add("/sdcard/Songs/One Direction - Story of My Life.mp3");
        song_names.add("/sdcard/Songs/Zedd - Stay The Night ft. Hayley Williams (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Riley Clemmons - Broken Prayers (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Otilia - Bilionera (official video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Otilia - Bilionera - Radio Edit.mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - All We Have Is Love (Audio Only) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Marshvll_-_I_Want_(feat._Sevnty).mp3");
        song_names.add("/sdcard/Songs/Calvin_Harris_-_This_Is_What_You_Came_For__28Official_Video_29_ft._Rihanna.mp3");
        song_names.add("/sdcard/Songs/MattyBRaps - Little Bit (feat. Haschak Sisters) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Meg Donnelly, Trevor Tordjman - Stand (From _ZOMBIES_) (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Cardi B, Bad Bunny & J Balvin - I Like It _Dillon Francis Remix_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Par us ladki ka thoda alag andaz _ Commerce ka ladka Science ki ladki _ Meri Kahani _ Hustler _ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Carly Rae Jepsen - Call Me Maybe ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Justin_Bieber_-_Baby_ft._Ludacris.mp3");
        song_names.add("/sdcard/Songs/Melanie Martinez - Dollhouse (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Carly Rae Jepsen - Tonight I'm Getting Over You ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Michael Jackson Billie Jean ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Kanye West - Stronger ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Miley Cyrus - We Can't Stop.mp3");
        song_names.add("/sdcard/Songs/Carly_Rae_Jepsen_-_Call_Me_Maybe.mp3");
        song_names.add("/sdcard/Songs/Kash Tere Ishq Mein Neelam ho jao ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Monster Nightcore (Lyrics) _NMV_ ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Lawson - Where My Love Goes.mp3");
        song_names.add("/sdcard/Songs/Carly_Rae_Jepsen_-_This_Kiss.mp3");
        song_names.add("/sdcard/Songs/Nashe Si Chadh Gayi - Full Song _ Befikre _ Ranveer Singh _ Vaani Kapoor _ Arijit Singh ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Carly_Rae_Jepsen_-_Tonight_I_m_Getting_Over_You.mp3");
        song_names.add("/sdcard/Songs/Kaho ek Din ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Little Mix - Hair (Official Video) ft. Sean Paul ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - You Owe Me ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Little Mix - Secret Love Song (Official Video) ft. Jason Derulo ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/6 - Inna - Yalla.mp3");
        song_names.add("/sdcard/Songs/The Cuppy Cake Song 2D Animation ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/8 - Ellie Goulding - Holding on for Life.mp3");
        song_names.add("/sdcard/Songs/The Pussycat Dolls - Buttons ft. Snoop Dogg (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Shakira - Chantaje (Official video) ft. Maluma ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Clean Bandit - Rockabye (feat. Sean Paul & Anne-Marie).mp3");
        song_names.add("/sdcard/Songs/_maa_da_ladla_full_song_dostana_.mp3.mp3.mp3");
        song_names.add("/sdcard/Songs/_MV_ (G)I-DLE ((여자)아이들) _ LATATA ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/_MV_ (G)I-DLE((여자)아이들) _ Uh-Oh ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Cleopatra Stratan - Zunea-zunea (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Culture Code - Make Me Move (feat. Karra) _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/That Poppy - Lowlife karaoke Official with Backing Vocals ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/The Bilz & Kashif - Tera Nasha _Official Video HQ_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Johnny Orlando, Mackenzie Ziegler - What If (I Told You I Like You) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Closer ft. Halsey (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - Don't Let Me Down ft. Daya (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Arabic Remix   Khalouni N3ich Yusuf Ekşioğlu Remix _heart_️_fire_   YouTube ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Maroon 5 - Girls Like You ft. Cardi B ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Justin Bieber - Sorry (PURPOSE _ The Movement) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Bassnectar - Speakerbox ft. Lafa Taylor [F8](MP3_160K)_1.mp3");
        song_names.add("/sdcard/Songs/Zara Larsson - Ain't My Fault (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Marshmello & Anne-Marie - FRIENDS (Music Video) _OFFICIAL FRIENDZONE ANTHEM_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/NOIXES_&_Miles_Monaco_-_Backwards_(Magic_Free_Release)(256k).mp3");
        song_names.add("/sdcard/Songs/Zara Larsson - Lush Life (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Zara Larsson - Uncover.mp3");
        song_names.add("/sdcard/Songs/Marshmello - Alone (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Arash - Broken Angel - Feat. Helena.mp3");
        song_names.add("/sdcard/Songs/One Direction - A.M. (Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/ARASH_LABAF_-_I_m_So_Lonely_Broken_Angel.mp3");
        song_names.add("/sdcard/Songs/Marshvll - Kiss ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/ZAYN - Let Me (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/marshvll-kiss[songsx.pk]-2.mp3");
        song_names.add("/sdcard/Songs/Zedd - Clarity (Official Music Video) ft. Foxes ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Marshvll_-_Hope_(feat._Sevnty).mp3");
        song_names.add("/sdcard/Songs/Otilia _ Bilionera ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - Almost Love.mp3");
        song_names.add("/sdcard/Songs/Owl_City___Carly_Rae_Jepsen_-_Good_Time.mp3");
        song_names.add("/sdcard/Songs/Camila Cabello - Real Friends.mp3");
        song_names.add("/sdcard/Songs/MC Fioti - Bum Bum Tam Tam (KondZilla) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Martin Garrix - Animals (Bass Boosted) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Justin Bieber Mashup - My World (2.0) - djNicoWuzHere ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Martin Garrix - In the Name of Love.mp3");
        song_names.add("/sdcard/Songs/Meghan Trainor - Like I'm Gonna Lose You (Official Music Video) ft. John Legend ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Kanye West - Runaway (Video Version) ft. Pusha T ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Miley_Cyrus_-_We_Can_t_Stop.mp3");
        song_names.add("/sdcard/Songs/MattyBRaps - Friend Zone (ft Gracie Haschak) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/K-391 & Alan Walker - Ignite (feat. Julie Bergan & Seungri) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Peking Duk - Let You Down.mp3");
        song_names.add("/sdcard/Songs/K-391 & Alan Walker - Ignite (feat. Julie Bergan & Seungri) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Nick Jonas - Jealous (Remix) (Audio) ft. Tinashe ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Lay - Sheep (Alan Walker Relift) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/LAY LAY REMIX by Gabidulin _ FAST & FURIOUS _Chase Scene_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Nicki_Minaj_-_Bed_of_lies_Ft._Skylar_Grey.mp3.mp3");
        song_names.add("/sdcard/Songs/Nicky Romero vs. Krewella - Legacy (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Nicky_Romero_vs._Krewella_-_Legacy_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Linkin Park - In The End (Mellen Gi & Tommee Profitt Remix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Kamal Raja - Havana _OFFICIAL MUSIC VIDEO_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - Attention _Official Video_ ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Pitbull - Rain Over Me ft. Marc Anthony (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/5 - Ed Sheeran - Perfect.mp3");
        song_names.add("/sdcard/Songs/Poppy - Moshi Moshi (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Zedd_-_Transmission_(Audio)_ft._Logic,_X_Ambassadors.mp3");
        song_names.add("/sdcard/Songs/Zindagi aee tere nalllll..... tu mera ae pyar ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Christina Perri - A Thousand Years _Official Music Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Everything_Has_Changed_ft._Ed_Sheeran.mp3");
        song_names.add("/sdcard/Songs/Alec_Benjamin_-_Let_Me_Down_Slowly_[Official_Music_Video](256k).mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_I_Knew_You_Were_Trouble.mp3");
        song_names.add("/sdcard/Songs/Taylor_Swift_-_Mine.mp3");
        song_names.add("/sdcard/Songs/_Manali_Trance____Official_Dance_VIDEO_The_Shaukeens__ cc61b0e2.mp3");
        song_names.add("/sdcard/Songs/Clean Bandit - Symphony (feat. Zara Larsson).mp3");
        song_names.add("/sdcard/Songs/Johnny Orlando - What If.mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers - All We Know ft. Phoebe Ryan (Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Daaku Mashup _ DJ Shadow Dubai _ 2017 _ Bollywood Iconic Villian Dialogues ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Jorja Smith - Don't Watch Me Cry (Lyrics) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/David Guetta, Bebe Rexha & J Balvin - Say My Name (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Warriors___Season_2020_Cinematic_-_League_of_Legends__ft._2WEI_and_Edda_Hayes_(256k).mp3");
        song_names.add("/sdcard/Songs/Mars Argo - Using You (Official) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Zack Knight x Jasmin Walia - Bom Diggy (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/NK - ELEFANTE (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Arabic Remix - Fi Ha ( Burak Balkan Remix ) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Majnoon Naboodam ReMix 2020 - Super Trap Majnoon Naboodam Songs Music 2020 (MAJNUN NABUDUM REMİX) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Zara_Larsson_-_Ain_t_My_Fault_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Major Lazer & DJ Snake - Lean On (feat. MØ) (Official Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/One Direction - Perfect.mp3");
        song_names.add("/sdcard/Songs/Zara_Larsson_-_Uncover.mp3");
        song_names.add("/sdcard/Songs/Major Lazer & DJ Snake - Lean On (feat. MØ) (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Mama ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Marshvll - Miss Me");
        song_names.add("/sdcard/Songs/One Direction - Steal My Girl.mp3");
        song_names.add("/sdcard/Songs/Manali Trance _ Yo Yo Honey Singh & Neha Kakkar _ The Shaukeens _ Lisa Haydon _ Akshay Kumar ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Mandy Moore & Zachary Levi (Ost.Tangled_Rapunzel) - I See The Light ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Ariana Grande - Love Me Harder.mp3");
        song_names.add("/sdcard/Songs/Richard_Marx_-_Right_Here_Waiting.mp3");
        song_names.add("/sdcard/Songs/Zedd, Alessia Cara - Stay (Official Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Owl City - Good Time.mp3");
        song_names.add("/sdcard/Songs/Pachtaoge (Full Video Song) _ Arijit Singh _ Vicky K & Nora Fatehi _ Jaani _B Praak _ Bada Pachtaoge ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Meghan Trainor - I'm a Lady (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Passenger _ Let Her Go (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Peder_B._Helland_-_Always_(Official_Audio)(256k).mp3");
        song_names.add("/sdcard/Songs/Martin Garrix - Scared to Be Lonely.mp3");
        song_names.add("/sdcard/Songs/Matoma - Slow (feat. Noah Cyrus).mp3");
        song_names.add("/sdcard/Songs/Justin_Bieber_-_Sorry__28PURPOSE__3A_The_Movement_29.mp3");
        song_names.add("/sdcard/Songs/Carly_Rae_Jepsen_-_I_Really_Like_You.mp3");
        song_names.add("/sdcard/Songs/Cartoon - On & On feat. Daniel Levi ( Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Let_Me_Love_You_-_Lyrics.mp3");
        song_names.add("/sdcard/Songs/Phoebe_Ryan_-_Chronic.mp3");
        song_names.add("/sdcard/Songs/Kamelia - Come again.mp3");
        song_names.add("/sdcard/Songs/5 - OneRepublic - What You Wanted.mp3");
        song_names.add("/sdcard/Songs/Kane Cooper - You Only Live Once (Yolo) _Lyric video_ ft. Alex Holmes ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Little_Mix_-_Secret_Love_Song__28Official_Video_29_ft._Jason_Derulo.mp3");
        song_names.add("/sdcard/Songs/The Chainsmokers, XYLØ - Setting Fires (Acoustic Version) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Post Malone - rockstar ft. 21 savage PARODY ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - Done For Me (feat. Kehlani).mp3");
        song_names.add("/sdcard/Songs/HRVY - Personal.mp3");
        song_names.add("/sdcard/Songs/Billie Eilish - bad guy ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/A Thousand Years  - Boyce Avenue (New Acoustic Sessions Vol  5) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Shape_Of_You_-_Ed_Sheeran_Lyrics.mp3");
        song_names.add("/sdcard/Songs/Aa Re Pritam Pyaare..mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - Eyes Wide Open.mp3");
        song_names.add("/sdcard/Songs/Adele_-_Hello.mp3");
        song_names.add("/sdcard/Songs/Kygo - Stargazing.mp3");
        song_names.add("/sdcard/Songs/Bebe Rexha - I'm A Mess (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Billie Eilish - bury a friend (Lyrics) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/HRVY, Malu Trevejo - Hasta Luego (Official video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - _How Long_ _Official Video_ ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Adele - Hello ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Krewella - Runaway (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Ahmed-Siddiq-Sher-Khan.mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_On_Purpose_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Gabrielle Aplin - Miss You (Nick Talos Remix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Sean Paul - No Lie ft. Dua Lipa ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Imagine Dragons - Thunder.mp3");
        song_names.add("/sdcard/Songs/Indila - Dernière Danse (Clip Officiel) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Good Time - OWL CITY ft. CARLY RAE JEPSEN _LYRICS!_ (THE MIDSUMMER STATION) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/04 - MTH - Galat Baat Hai.mp3");
        song_names.add("/sdcard/Songs/Selena_Gomez_-_Kill_Em_With_Kindness_(Bass_Boosted)(256k).mp3");
        song_names.add("/sdcard/Songs/INNA - Bad Boys _ Exclusive Online Video ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/3 - Sabrina Carpenter - The Middle of Starting Over.mp3");
        song_names.add("/sdcard/Songs/Haschak Sisters - Gossip Girl ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/4 - Charlie Puth - How Long.mp3");
        song_names.add("/sdcard/Songs/PSY - GANGNAM STYLE(강남스타일) M_V ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/PSY - GANGNAM STYLE(강남스타일) M_V ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Shounen Ki (Childhood) - Song from Doraemon movie_ Nobita's Little Star War ( 128kbps ).m4a");
        song_names.add("/sdcard/Songs/SKRILLEX_-_Bangarang_feat._Sirah_[Official_Music_Video].mp3");
        song_names.add("/sdcard/Songs/Tatiana - Helplessly (SpeedUp) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - We Don't Talk Anymore (feat. Selena Gomez) _Official Video_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Shakira - Chantaje.mp3");
        song_names.add("/sdcard/Songs/Shakira - Trap (Audio Oficial) ft. Maluma ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Kelsea Ballerini - Miss Me More (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/HRVY, Malu Trevejo - Hasta Luego (Official video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/KIKI DO YOU LOVE ME _DRAKE_ (MUSIC VIDEO) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/HUGEL feat. Amber van Day - WTF (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter, Jonas Blue - Alien (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Bol_Kaffara_Kya_Hoga_Complete_Song_Extended_-_Parlour_Wali_Larki_OST_-_BOL_Entertainment_-_BOL_Music(256k).mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_Eyes_Wide_Open_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Kygo, Selena Gomez - It Ain't Me ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Bruno_Mars_-_Just_The_Way_You_Are_[OFFICIAL_VIDEO].mp3");
        song_names.add("/sdcard/Songs/1 - Inna - Gimme Gimme.mp3");
        song_names.add("/sdcard/Songs/Alan Walker - Fade _NCS Release_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan Walker - See Your Face (Official Video 2018) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Selena Gomez - Slow Down.mp3");
        song_names.add("/sdcard/Songs/Selena Gomez - The Heart Wants What It Wants.mp3");
        song_names.add("/sdcard/Songs/Alan Walker - The Spectre ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/12 - Céline Dion - My Heart Will Go On.mp3");
        song_names.add("/sdcard/Songs/16 - Richard Marx - Right Here Waiting.mp3");
        song_names.add("/sdcard/Songs/Alan Walker, Sabrina Carpenter & Farruko  - On My Way ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Selena_Gomez_-_Slow_Down_(Official).mp3");
        song_names.add("/sdcard/Songs/Serena - Safari (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/INNA - Bad Boys.mp3");
        song_names.add("/sdcard/Songs/INNA - Crazy Sexy Wild (OFFICIAL VIDEO) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/1 - Cash Cash - How to Love.mp3");
        song_names.add("/sdcard/Songs/Sevinch Mo'minova - Ne bo'ldi (Official music) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Shawn Mendes - There's Nothing Holdin' Me Back.mp3");
        song_names.add("/sdcard/Songs/1 - Heuse & Zeus X Crona - Pill (feat. Emma Sameth).mp3");
        song_names.add("/sdcard/Songs/Helplessly _ Tatiana Manaois OFFICIAL MUSIC VIDEO ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Heuse & Zeus x Crona - Pill (feat. Emma Sameth) _NCS Release_ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - Bad Blood.mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - Everything Has Changed ft. Ed Sheeran ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - I Knew You Were Trouble Lyrics (HD) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/5 Haunted Places on Earth ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/8 Letters Acoustic - Why Don't We _Official Music Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/A Great Big World & Christina Aguilera - Say Something (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Katy Perry - Dark Horse (Official) ft. Juicy J ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Adele_-_Hello_(Cover_by_Sabrina_Carpenter).mp3");
        song_names.add("/sdcard/Songs/BLACKPINK - 'Kill This Love' M_V ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/BLACKPINK - ‘뚜두뚜두 (DDU-DU DDU-DU)’ M_V ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Kygo & Justin Jesso - Stargazing (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Akcent Feat Amira   Push With Lyrics ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Akcent feat Lidia Buble & DDY Nunes - Kamelia (Official Music Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_Can_t_Blame_a_Girl_for_Trying_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/La Vie Ne Ment Past ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Akhiyan_Vich_Band_Kar_Ke)_By_Zeeshan_Ali_HD___Tune.pk.mp3.mp3.mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_Thumbs_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Imagine Dragons - Believer.mp3");
        song_names.add("/sdcard/Songs/1 - Icona Pop - I Love It (feat. Charli XCX).mp3");
        song_names.add("/sdcard/Songs/Imagine Dragons - Birds (Animated Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan Walker - Faded.mp3");
        song_names.add("/sdcard/Songs/1 - Luis Fonsi - Despacito.mp3");
        song_names.add("/sdcard/Songs/Gioni & Marshvll - Rude ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Gul panra Meherban Original Full HD Song - Gul Panra new Song 2016 ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/2 - Nick Jonas - Jealous.mp3");
        song_names.add("/sdcard/Songs/2 - The Chainsmokers - Roses.mp3");
        song_names.add("/sdcard/Songs/1 - Ariana Grande - No Tears Left to Cry.mp3");
        song_names.add("/sdcard/Songs/5 - Dua Lipa - IDGAF (Remixes).mp3");
        song_names.add("/sdcard/Songs/1 - Ellie Goulding - Still Falling for You.mp3");
        song_names.add("/sdcard/Songs/Heart touching background  Love song sad female version hindi whatsapp status ( 720 X 1280 ).mp4");
        song_names.add("/sdcard/Songs/Shawn_Mendes_-_Treat_You_Better.mp3");
        song_names.add("/sdcard/Songs/Rauf_&_Faik_-_детство_(Official_video)(256k).mp3");
        song_names.add("/sdcard/Songs/Adele - Hello (Cover by Sabrina Carpenter) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Akcent - How Many Times (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Akcent feat. Amira - Push _Love The Show_ (Official Music Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Brenda Shankey - Summary and Conclusion.mp3");
        song_names.add("/sdcard/Songs/Gabrielle_Aplin_-_Miss_You_(Nick_Talos_Remix)_[No_Copyright_Music].mp3");
        song_names.add("/sdcard/Songs/Sasha Sloan - Older (Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/02 - RM2 - Chaar Botal Vodka.mp3");
        song_names.add("/sdcard/Songs/Alan Walker - Sing Me to Sleep - Marshmello Remix.mp3");
        song_names.add("/sdcard/Songs/1 - Taylor Swift - …Ready for It.mp3");
        song_names.add("/sdcard/Songs/02- Bruno Mars - Just The Way You Are - _Doo-Wops & Hooligans_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan Walker - The Spectre.mp3");
        song_names.add("/sdcard/Songs/2 - Carly Rae Jepsen - This Kiss.mp3");
        song_names.add("/sdcard/Songs/Happy - MBB _Vlog No Copyright Music_ ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/INNA - Nirvana _ Official Music Video ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/PSY - Gangnam Style (강남스타일).mp3");
        song_names.add("/sdcard/Songs/INNA_-_Heaven_(DJ_Asher_Remix)_(Official_Video)(256k).mp3");
        song_names.add("/sdcard/Songs/Rang _ Rahim Pardesi ft Ezu _ Full Video _ VIP Records _ 360 Worldwide ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Hotel Transylvania 3 (2018) - Dracula vs. the Kraken Scene (9_10) _ Movieclips ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - Blank Space ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/HRVY - Personal (Official Video) ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Post Malone ft. 21 Savage - _Rockstar_ PARODY ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/BENEE - Supalonely ft. Gus Dapperton ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Beth - Don't You Worry Child (Charming Horses Remix Edit) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie Puth - We Don't Talk Anymore (feat. Selena Gomez).mp3");
        song_names.add("/sdcard/Songs/A Thousand Years Lyrics.  Christina Perri ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - Can't Blame a Girl for Trying (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Billie Eilish - wish you were gay (Audio) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - Smoke and Fire.mp3");
        song_names.add("/sdcard/Songs/Kris Kross Amsterdam x The Boy Next Door - Whenever (feat. Conor Maynard) _Official Music Video_ ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - Why.mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter _Christmas The Whole Year Round_ - Radio Disney Fa-La-La-Lidays ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_All_We_Have_Is_Love_(Audio_Only).mp3");
        song_names.add("/sdcard/Songs/Kygo - Stranger Things ft. OneRepublic (Alan Walker Remix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Boyce Avenue (Cover Sessions, Vol. 2 - Single) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Icona Pop - I love it (SICK INDIVIDUALS Remix) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan Walker - Darkside (feat. Au_Ra and Tomine Harket) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/- Nicky Romero - Legacy.mp3");
        song_names.add("/sdcard/Songs/Selena Gomez - Come & Get It.mp3");
        song_names.add("/sdcard/Songs/1 - Pitbull - Greenlight (feat. Flo Rida & LunchMoney Lewis).mp3");
        song_names.add("/sdcard/Songs/Indila - Dernière Danse (Clip Officiel) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/08_-_Sone_Ka_Paani(dailymaza.com).....mp3");
        song_names.add("/sdcard/Songs/Alan x Walkers - Unity ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/1 - Alex Goot - Closer (feat. ATC).mp3");
        song_names.add("/sdcard/Songs/1 - Arash - One Day (feat. Helena) [Radio Edit].mp3");
        song_names.add("/sdcard/Songs/Shakira - Chantaje (Official Video) ft. Maluma ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/1 - Hailee Steinfeld - Starving (acoustic).mp3");
        song_names.add("/sdcard/Songs/INNA - Yalla _ Official Music Video ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Shawn_Mendes_2C_Camila_Cabello_-_I_Know_What_You_Did_Last_Summer__28Official_Video_29.mp3");
        song_names.add("/sdcard/Songs/Rauf___Faik_-_Never_Lie_To_Me__%D0%B4%D0%B5%D1%82%D1%81%D1%82%D0%B2%D0%BE___Lyrics___Lyrics_Video_(256k).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - In My Bed ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - No Words (Audio Only) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Bizzey - Traag ft. Jozo & Kraantje Pappie (prod. Ramiks & Bizzey) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Charlie_Puth_-_Attention_[Official_Video].mp3");
        song_names.add("/sdcard/Songs/Sabrina Carpenter - On Purpose (Official Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Frozen - Let It Go (Idina Menzel) (Karaoke Version) ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Kygo - Remind Me to Forget.mp3");
        song_names.add("/sdcard/Songs/Gabbar_Singh_(SHOLAY)_Funky_Mix_2018_-_Dj_Harshit_&_Dj_Jeet(256k).mp3");
        song_names.add("/sdcard/Songs/Akh Lad Jaave Video _ Aayush Sharma _ Warina Hussain _ Badshah, Tanishk Bagchi,Jubin N, ,Asees K ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/La Vie Ne Ment Past ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_The_Middle_of_Starting_Over_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Akon_-_Smack_That_(Official_Video)_ft._Eminem(256k).mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_Why_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/Alan Walker - All Falls Down.mp3");
        song_names.add("/sdcard/Songs/- Cover Nation - Rain Over Me (Tribute to Pitbull feat. Marc Anthony).mp3");
        song_names.add("/sdcard/Songs/1 - Liam Payne - Strip That Down (acoustic).mp3");
        song_names.add("/sdcard/Songs/Ghana Pallbearers Astronomia Coffin Dancing Song _ Tiktok Viral Song _ ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/SeaVolution & Wave Rider mix Hotel Transilvania 3 Kraken ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/-Tera_Milna_Pal_Do_Pal_Ka-_Video_Song_Sonu_Nigam_Feat._Bipasha_Basu_Super_Hit_Hindi_Album_-JAAN-(128k).mp3");
        song_names.add("/sdcard/Songs/Selena_Gomez_-_Come___Get_It.mp3");
        song_names.add("/sdcard/Songs/Indila_-_Tourner_Dans_Le_Vide(256k).mp3");
        song_names.add("/sdcard/Songs/Serena - Safari (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/1 - Andrew Allen - What You Wanted.mp3");
        song_names.add("/sdcard/Songs/Serhat_Durmus_-_Hislerim_(ft._Zerrin)(256k).mp3");
        song_names.add("/sdcard/Songs/Sevinch Mo'minova - Ne bo'ldi (Official Music Video) 2005 ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Prezioso & Marvin - The Riddle (Dopedrop Bootleg) _Bass Boosted_ ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Shawn Mendes, Camila Cabello - Señorita ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Shawn_Mendes_-_There_27s_Nothing_Holdin_27_Me_Back.mp3");
        song_names.add("/sdcard/Songs/Sia - Cheap Thrills ft. Sean Paul (Lyric Video) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Taylor Swift - 22 ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/7 - Natural Vibrations - Can't Stop.mp3");
        song_names.add("/sdcard/Songs/Lorde - Pure Heroine Audio (Official) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Katy Perry - Dark Horse (Official) ft. Juicy J ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Bizzey - Traag ft. Jozo & Kraantje Pappie (prod. Ramiks & Bizzey) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/BLACKPINK - 'How You Like That' M_V ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Kygo, Miguel - Remind Me to Forget (Official Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Akh Lad Jaave With Lyrics _ Loveyatri _ Aayush S _ Warina H _Badshah,Tanishk Bagchi,Jubin N,Asees K ( 128kbps )conv.mp3");
        song_names.add("/sdcard/Songs/Sabrina_Carpenter_-_Smoke_and_Fire_(Official_Video).mp3");
        song_names.add("/sdcard/Songs/GENTLEMAN PSY M_V ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Scary Horror ♫ No Copyright Royalty Free ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Alan Walker - Sing Me To Sleep ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/2 - Avicii - Lonely Together (Remixes).mp3");
        song_names.add("/sdcard/Songs/1 - Alan Walker - Alone.mp3");
        song_names.add("/sdcard/Songs/3 - Sabrina Carpenter - Thumbs (acoustic).mp3");
        song_names.add("/sdcard/Songs/1 - Arash - OMG (feat. Snoop Dogg).mp3");
        song_names.add("/sdcard/Songs/Heart touching background  Love song sad female version hindi whatsapp status ( 192kbps ).mp3");
        song_names.add("/sdcard/Songs/Shawn Mendes - Treat You Better.mp3");
        song_names.add("/sdcard/Songs/Hero Movie Song_Main Hoon Hero Tera Full song ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Kygo & Selena Gomez - It Ain't Me (Audio) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/BLACKPINK - ‘뚜두뚜두 (DDU-DU DDU-DU)’ M_V ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/Bom Diggy Diggy  (VIDEO) _ Zack Knight _ Jasmin Walia _ Sonu Ke Titu Ki Sweety ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/BÖ_&_Serhat_Durmus_-_Elimi_Tut_(ft._Ecem_Telli)(256k).mp3");
        song_names.add("/sdcard/Songs/Calvin Harris - This Is What You Came For.mp3");
        song_names.add("/sdcard/Songs/Alan Walker - Fade (Hell's Speaker Remix).mp3");
        song_names.add("/sdcard/Songs/- HLMusicTOP Cheat Codes – No Promises (Lyrics _ Lyric Video) ft. Demi Lovato ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/1 - Otilia - Bilionera (Radio Edit).mp3");
        song_names.add("/sdcard/Songs/Selena Gomez - Wolves.mp3");
        song_names.add("/sdcard/Songs/Alan Walker, K-391 & Emelie Hollow - Lily (Lyrics) ( 160kbps ).mp3");
        song_names.add("/sdcard/Songs/1 - Akcent, Lidia Buble & Ddy Nunes - Kamelia (feat. Lidia Buble & Ddy Nunes).mp3");
        song_names.add("/sdcard/Songs/Serhat Durmus - Hislerim (ft. Zerrin) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/4 - Camila Cabello - Havana (feat. Young Thug).mp3");
        song_names.add("/sdcard/Songs/5 - Ariana Grande - Side to Side (DJ DSpin Ben remix).mp3");
        song_names.add("/sdcard/Songs/INNA - Gimme Gimme _ Official Music Video ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Hello - Adele - Conor Maynord.mp3");
        song_names.add("/sdcard/Songs/Ranjha Ranjha with lyrics ( 256kbps cbr ).mp3");
        song_names.add("/sdcard/Songs/Rauf & Faik - Never Lie To Me (детство) (Lyrics _ Lyrics Video) ( 128kbps ).mp3");
        song_names.add("/sdcard/Songs/Requiem For A Dream   Electric Violinist   Kate Chruscicka ( 128kbps )conv.mp3");
    }*/
}
