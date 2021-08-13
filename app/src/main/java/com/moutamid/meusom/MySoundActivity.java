package com.moutamid.meusom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.tabs.TabLayout;

public class MySoundActivity extends AppCompatActivity {
    private SectionsPageAdapter mSectionsPageAdapter;
    private ViewPager viewPager;
    private static final String TAG = "MySoundActivity";
    private Context context = MySoundActivity.this;
    private Utils utils = new Utils();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.ENGLISH)) {
            utils.changeLanguage(context,"en");
        } else if (utils.getStoredString(context, Constants.LANGUAGE).equals(Constants.PORTUGUESE)) {
            utils.changeLanguage(context,"pr");
        }
        setContentView(R.layout.activity_my_sound);
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        findViewById(R.id.backBtnMySOund).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        viewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayoutMySound);
        tabLayout.setupWithViewPager(viewPager);

    }

    public void setupViewPager(ViewPager viewPager) {

        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new TracksFragment(), "Tracks");
        adapter.addFragment(new AlbumFragment(), "Album");
        adapter.addFragment(new PlaylistFragment(), "Playlist");

        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(adapter);

    }
}