
package com.lingtuan.firefly.setting;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingtuan.firefly.NextApplication;
import com.lingtuan.firefly.R;
import com.lingtuan.firefly.base.BaseActivity;
import com.lingtuan.firefly.custom.gesturelock.ACache;
import com.lingtuan.firefly.custom.switchbutton.SwitchButton;
import com.lingtuan.firefly.fragment.MySelfFragment;
import com.lingtuan.firefly.offline.AppNetService;
import com.lingtuan.firefly.setting.contract.SettingContract;
import com.lingtuan.firefly.setting.presenter.SettingPresenterImpl;
import com.lingtuan.firefly.ui.WebViewUI;
import com.lingtuan.firefly.util.Constants;
import com.lingtuan.firefly.util.LoadingDialog;
import com.lingtuan.firefly.util.MySharedPrefs;
import com.lingtuan.firefly.util.MyViewDialogFragment;
import com.lingtuan.firefly.util.Utils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created on 2017/9/13.
 * Settings page
 * {@link MySelfFragment}
 */

public class SettingUI extends BaseActivity implements CompoundButton.OnCheckedChangeListener ,SettingContract.View{

    @BindView(R.id.startSmartMeshWorkButton)
    SwitchButton startSmartMeshWorkButton;//stealth  、no net work.  gesture
    @BindView(R.id.gestureButton)
    SwitchButton gestureButton;//stealth  、no net work.  gesture
    @BindView(R.id.startSmartMeshWorkBody)
    RelativeLayout startSmartMeshWorkBody;
    @BindView(R.id.versionCheck)
    TextView versionCheck;//The version number
    @BindView(R.id.versionCircle)
    TextView versionCircle;//The version circle

    private SettingContract.Presenter mPresenter;

    @Override
    protected void setContentView() {
        setContentView(R.layout.setting_layout);
    }

    @Override
    protected void findViewById() {

    }

    @Override
    protected void setListener() {
        // -1 default , 0 close , 1 open
        startSmartMeshWorkButton.setOnCheckedChangeListener(null);
        if (NextApplication.myInfo != null && !TextUtils.isEmpty(NextApplication.myInfo.getLocalId())){
            int noNetWork = MySharedPrefs.readInt1(NextApplication.mContext,MySharedPrefs.FILE_USER,MySharedPrefs.KEY_NO_NETWORK_COMMUNICATION + NextApplication.myInfo.getLocalId());
            if (noNetWork == 1){//is open
                startSmartMeshWorkButton.setChecked(true);
                startSmartMeshWorkButton.setBackColor(getResources().getColorStateList(R.color.switch_button_green));
            }else{
                startSmartMeshWorkButton.setChecked(false);
                startSmartMeshWorkButton.setBackColor(getResources().getColorStateList(R.color.switch_button_gray));
            }
        }
        startSmartMeshWorkButton.setOnCheckedChangeListener(this);
    }

    @Override
    protected void initData() {
        new SettingPresenterImpl(this);
        setTitle(getString(R.string.setting));
        versionCheck.setText(Utils.getVersionName(SettingUI.this));

        String tempVersion = MySharedPrefs.readString(NextApplication.mContext,MySharedPrefs.FILE_USER,MySharedPrefs.KEY_UPDATE_VERSION);
        tempVersion = tempVersion.replace(".","").trim();
        if (!TextUtils.isEmpty(tempVersion)){
            String currentVersion = Utils.getVersionName(SettingUI.this).replace(".","").trim();
            try {
                if (Integer.parseInt(tempVersion) > Integer.parseInt(currentVersion)){
                    versionCircle.setVisibility(View.VISIBLE);
                }else{
                    versionCircle.setVisibility(View.GONE);
                }
            }catch (Exception e){
                e.printStackTrace();
                versionCircle.setVisibility(View.GONE);
            }
        }

        int version = Build.VERSION.SDK_INT;
        if(version < 16){
            startSmartMeshWorkBody.setVisibility(View.GONE);
        }else{
            startSmartMeshWorkBody.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        gestureButton.setOnCheckedChangeListener(null);
        byte[] gestureByte  = ACache.get(NextApplication.mContext).getAsBinary(Constants.GESTURE_PASSWORD + NextApplication.myInfo.getLocalId());
        if (gestureByte != null && gestureByte.length > 0){
            gestureButton.setChecked(true);
            gestureButton.setBackColor(getResources().getColorStateList(R.color.switch_button_green));
        }else{
            gestureButton.setChecked(false);
            gestureButton.setBackColor(getResources().getColorStateList(R.color.switch_button_gray));
        }
        gestureButton.setOnCheckedChangeListener(this);
    }

    @OnClick({R.id.languageSelectionBody,R.id.useAgree,R.id.blackListBody,R.id.exit,R.id.versionBody})
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.languageSelectionBody:
                startActivity(new Intent(SettingUI.this, LanguageSelectionUI.class));
                Utils.openNewActivityAnim(SettingUI.this,false);
                break;
            case R.id.useAgree:
                String result = mPresenter.getUserAgreement();
                Intent intent = new Intent(SettingUI.this, WebViewUI.class);
                intent.putExtra("loadUrl", result);
                intent.putExtra("title", getString(R.string.use_agreement));
                startActivity(intent);
                Utils.openNewActivityAnim(SettingUI.this,false);
                break;
            case R.id.blackListBody:
                startActivity(new Intent(SettingUI.this, BlackListUI.class));
                Utils.openNewActivityAnim(SettingUI.this,false);
                break;
            case R.id.exit:
                checkLogOut();
                break;
            case R.id.versionBody:
                if (versionCircle.getVisibility() != View.VISIBLE){
                    return;
                }
                mPresenter.checkVersion();
                break;
            default:
                super.onClick(v);
                break;

        }
    }

    /**
     * check logout
     * */
    private void checkLogOut(){
        if (NextApplication.myInfo != null && TextUtils.isEmpty(NextApplication.myInfo.getMid())&& TextUtils.isEmpty(NextApplication.myInfo.getMobile())&& TextUtils.isEmpty(NextApplication.myInfo.getEmail())) {
            MyViewDialogFragment mdf = new MyViewDialogFragment(MyViewDialogFragment.DIALOG_CHECK_LOGOUT);
            mdf.setTitleAndContentText(getString(R.string.account_logout_mid_warn), getString(R.string.account_logout_mid_hint));
            mdf.setOkCallback(new MyViewDialogFragment.OkCallback() {
                @Override
                public void okBtn() {
                    checkLogOutAgain();
                }
            });
            mdf.setCancelCallback(new MyViewDialogFragment.CancelCallback() {
                @Override
                public void cancelBtn() {
                    startActivity(new Intent(SettingUI.this, SecurityUI.class));
                    Utils.openNewActivityAnim(SettingUI.this,false);
                }
            });
            mdf.show(getSupportFragmentManager(), "mdf");
        }else{
            MyViewDialogFragment mdf = new MyViewDialogFragment();
            mdf.setTitleAndContentText(getString(R.string.account_logout_warn), getString(R.string.account_logout_hint));
            mdf.setOkCallback(new MyViewDialogFragment.OkCallback() {
                @Override
                public void okBtn() {
                    if (NextApplication.myInfo == null || TextUtils.isEmpty(NextApplication.myInfo.getToken())){
                        exitApp();
                    }else{
                        mPresenter.logOutMethod();
                    }

                }
            });
            mdf.show(getSupportFragmentManager(), "mdf");
        }

    }

    /**
     * check logout again
     * */
    private void checkLogOutAgain() {
        MyViewDialogFragment mdf = new MyViewDialogFragment(MyViewDialogFragment.DIALOG_CHECK_LOGOUT_AGAIBN);
        mdf.setTitleAndContentText(getString(R.string.account_logout_mid_warn), getString(R.string.account_logout_mid_hint_again));
        mdf.setOkCallback(new MyViewDialogFragment.OkCallback() {
            @Override
            public void okBtn() {
                if (TextUtils.isEmpty(NextApplication.myInfo.getToken())){
                    exitApp();
                }else{
                    mPresenter.logOutMethod();
                }

            }
        });
        mdf.setCancelCallback(new MyViewDialogFragment.CancelCallback() {
            @Override
            public void cancelBtn() {
                finish();
            }
        });
        mdf.show(getSupportFragmentManager(), "mdf");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.startSmartMeshWorkButton:
                MySharedPrefs.writeInt(NextApplication.mContext,MySharedPrefs.FILE_USER,MySharedPrefs.KEY_NO_NETWORK_COMMUNICATION + NextApplication.myInfo.getLocalId(),isChecked ? 1 : 0);
                if (isChecked){
                    startSmartMeshWorkButton.setBackColor(getResources().getColorStateList(R.color.switch_button_green));
                    //Start without social network service
                    startService(new Intent(this, AppNetService.class));
                    Utils.sendBroadcastReceiver(SettingUI.this,new Intent(Constants.OPEN_SMARTMESH_NETWORE), false);
                }else{
                    startSmartMeshWorkButton.setBackColor(getResources().getColorStateList(R.color.switch_button_gray));
                    int version =android.os.Build.VERSION.SDK_INT;
                    if(version >= 16){
                        Utils.sendBroadcastReceiver(SettingUI.this,new Intent(Constants.CLOSE_SMARTMESH_NETWORE), false);
                        Intent offlineservice = new Intent(NextApplication.mContext, AppNetService.class);
                        stopService(offlineservice);
                    }
                }
                break;
            case R.id.gestureButton:
                if (isChecked){
                    gestureButton.setBackColor(getResources().getColorStateList(R.color.switch_button_green));
                }else{
                    gestureButton.setBackColor(getResources().getColorStateList(R.color.switch_button_gray));
                }
                byte[] gestureByte  = ACache.get(NextApplication.mContext).getAsBinary(Constants.GESTURE_PASSWORD + NextApplication.myInfo.getLocalId());
                if (gestureByte != null && gestureByte.length > 0){
                    Intent intent = new Intent(SettingUI.this,GestureLoginActivity.class);
                    intent.putExtra("type",isChecked ? 1 : 2);
                    startActivity(intent);
                }else{
                    Intent intent = new Intent(SettingUI.this,CreateGestureActivity.class);
                    startActivity(intent);
                }
                break;
        }

    }

    @Override
    public void setPresenter(SettingContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void start() {
        LoadingDialog.show(SettingUI.this,"");
    }

    @Override
    public void success() {
        LoadingDialog.close();
        exitApp();
    }

    @Override
    public void sendBroadcast(Bundle bundle) {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_UPDATE_VERSION);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    @Override
    public void error(int errorCode, String errorMsg,boolean exitApp) {
        LoadingDialog.close();
        if (exitApp){
            exitApp();
        }else{
            showToast(errorMsg);
        }
    }
}
