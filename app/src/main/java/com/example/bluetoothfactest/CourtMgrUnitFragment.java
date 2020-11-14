package com.example.bluetoothfactest;

import android.app.Activity;
import android.content.Intent;
import android.os.Message;
import android.view.View;

import com.rk.commonlib.CommonBaseFragment;


public class CourtMgrUnitFragment extends CommonBaseFragment {

    public static CourtMgrUnitFragment newInstance(Activity activity) {
        sParentActivity = activity;
        CourtMgrUnitFragment f = new CourtMgrUnitFragment();
        return f;
    }

    @Override
    protected void initEvent() {

    }

    @Override
    protected void initView(View view) {

    }

    @Override
    protected void handleUIMessage(Message msg) {

    }

    @Override
    protected void handleNonUIMessage(Message msg) {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.court_fragment;
    }

}
