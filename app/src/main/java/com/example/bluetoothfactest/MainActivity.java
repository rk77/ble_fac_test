package com.example.bluetoothfactest;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.google.android.material.tabs.TabLayout;
import com.rk.commonlib.NoBleBaseFragmentActivity;
import com.rk.commonlib.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionCallback;
import me.weyye.hipermission.PermissionItem;

public class MainActivity extends NoBleBaseFragmentActivity {

    private TabLayout mTab;
    private ViewPager mViewPager;
    private String[] mTitles = {"台区台变管理单元"};


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void handleIntent() {

    }

    @Override
    protected void initView() {
        mTab = findViewById(R.id.tab);
        mViewPager = findViewById(R.id.view_pager);

    }

    @Override
    protected void initEvent() {
        LogUtils.i("initEvent");
        mTab.setupWithViewPager(mViewPager);
        mViewPager.setAdapter(new ParaFragmentAdapter(getSupportFragmentManager()));
        PageChangeListener listener = new PageChangeListener();
        mViewPager.addOnPageChangeListener(listener);
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            LogUtils.i("onPageSelected, pos: " + position);
            switch (position) {
                case 0:
                    break;
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }


    private class ParaFragmentAdapter extends FragmentPagerAdapter {
        public ParaFragmentAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            if (mTitles != null && mTitles.length > 0) {
                return mTitles.length;
            }
            return 0;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return CourtMgrUnitFragment.newInstance(MainActivity.this);
            }
            return null;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (mTitles != null && mTitles.length > position) {
                return mTitles[position];
            }
            return "Default";
        }
    }

    @Override
    protected void handleNonUiMessage(Message msg) {

    }

    @Override
    protected void handleUiMessage(Message msg) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    private void requestPermissions() {
        List<PermissionItem> permissionItems = new ArrayList<PermissionItem>();
        permissionItems.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Storage", R.drawable.permission_ic_storage));
        permissionItems.add(new PermissionItem(Manifest.permission.ACCESS_COARSE_LOCATION, "Location", R.drawable.permission_ic_location));
        HiPermission.create(this)
                .permissions(permissionItems)
                .checkMutiPermission(new PermissionCallback() {
                    @Override
                    public void onClose() {
                        finish();
                    }

                    @Override
                    public void onFinish() {
                        //UpdateUtils.checkUpdate(MainActivity.this, UPDATE_URL);
                        LogUtils.i("requestPermissions, finish");
                    }

                    @Override
                    public void onDeny(String permission, int position) {
                        LogUtils.i("requestPermissions, onDeny");
                    }

                    @Override
                    public void onGuarantee(String permission, int position) {
                        LogUtils.i("requestPermissions, onGuarantee");
                    }
                });
    }
}
