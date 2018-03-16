package com.lingtuan.firefly.wallet.fragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.lingtuan.firefly.NextApplication;
import com.lingtuan.firefly.R;
import com.lingtuan.firefly.base.BaseFragment;
import com.lingtuan.firefly.custom.gesturelock.ACache;
import com.lingtuan.firefly.login.LoginUtil;
import com.lingtuan.firefly.quickmark.CaptureActivity;
import com.lingtuan.firefly.quickmark.QuickMarkShowUI;
import com.lingtuan.firefly.raiden.RaidenChannelList;
import com.lingtuan.firefly.setting.CreateGestureActivity;
import com.lingtuan.firefly.setting.GestureLoginActivity;
import com.lingtuan.firefly.ui.AlertActivity;
import com.lingtuan.firefly.ui.WalletModeLoginUI;
import com.lingtuan.firefly.util.Constants;
import com.lingtuan.firefly.util.MySharedPrefs;
import com.lingtuan.firefly.util.MyToast;
import com.lingtuan.firefly.util.Utils;
import com.lingtuan.firefly.util.netutil.NetRequestUtils;
import com.lingtuan.firefly.wallet.AccountAdapter;
import com.lingtuan.firefly.wallet.ManagerWalletActivity;
import com.lingtuan.firefly.wallet.TransactionRecordsActivity;
import com.lingtuan.firefly.wallet.WalletCopyActivity;
import com.lingtuan.firefly.wallet.WalletCreateActivity;
import com.lingtuan.firefly.wallet.WalletSendActivity;
import com.lingtuan.firefly.wallet.util.WalletStorage;
import com.lingtuan.firefly.wallet.vo.StorableWallet;
import com.lingtuan.firefly.xmpp.XmppAction;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created  on 2017/8/23.
 * account
 */

public class AccountFragment extends BaseFragment implements View.OnClickListener, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    /**
     * root view
     */
    private View view = null;
    private boolean isDataFirstLoaded;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private TextView walletGesture;//wallet gesture
    private TextView walletManager;//Account management
    private TextView createWallet;//Create a wallet
    private TextView showQuicMark;//Flicking a
    private ListView walletListView;//The wallet list
    private AccountAdapter mAdapter;

    private ImageView walletImg;//The wallet picture
    private TextView walletName;//Name of the wallet
    private TextView walletBackup;//backup of the wallet
    private TextView walletAddress;//The wallet address
    private TextView qrCode;//Qr code
    private TextView transRecord;//Transaction records
    private TextView copyAddress;//Copy the address

    private LinearLayout walletNameBody;

    private SwipeRefreshLayout swipe_refresh;

    private StorableWallet storableWallet;

    //ETH SMT parts
    private TextView ethBalance,fftBalance,meshBalance;//eth、smt balance
    private LinearLayout ethTransfer,fftTransfer,meshTransfer;//eth、smt transfer
    private LinearLayout ethQrCode,fftQrCode,meshQrCode;//eth、smt Qr code collection

    private LinearLayout raidenTransfer;//raiden

    private int index = -1;//Which one is selected

    private boolean isChecked;

    private Timer timer;
    private TimerTask timerTask;
    private int timerLine = 10;

    private PopupWindow homePop;

    public AccountFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        isDataFirstLoaded = true;
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!isDataFirstLoaded && view != null) {
            ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        view = inflater.inflate(R.layout.wallet_account_fragment,container,false);
        findViewById();
        setListener();
        initData();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!isDataFirstLoaded) {
            return;
        }
        isDataFirstLoaded = false;
    }


    private void findViewById() {

        swipe_refresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);

        //The sidebar related
        mDrawerLayout = (DrawerLayout) view.findViewById(R.id.account_drawerlayout);
        walletListView = (ListView) view.findViewById(R.id.walletList);
        createWallet = (TextView) view.findViewById(R.id.createWallet);
        showQuicMark = (TextView) view.findViewById(R.id.showQuicMark);
        walletManager = (TextView) view.findViewById(R.id.walletManager);
        walletGesture = (TextView) view.findViewById(R.id.walletGesture);

        //The main related
        walletImg = (ImageView) view.findViewById(R.id.walletImg);
        walletName = (TextView) view.findViewById(R.id.walletName);
        walletNameBody = (LinearLayout) view.findViewById(R.id.walletNameBody);
        walletBackup = (TextView) view.findViewById(R.id.walletBackup);
        walletAddress = (TextView) view.findViewById(R.id.walletAddress);
        qrCode = (TextView) view.findViewById(R.id.qrCode);
        transRecord = (TextView) view.findViewById(R.id.transRecord);
        copyAddress = (TextView) view.findViewById(R.id.copyAddress);

        //eth smt related
        ethBalance = (TextView) view.findViewById(R.id.ethBalance);
        fftBalance = (TextView) view.findViewById(R.id.fftBalance);
        meshBalance = (TextView) view.findViewById(R.id.meshBalance);
        ethTransfer = (LinearLayout) view.findViewById(R.id.ethTransfer);
        fftTransfer = (LinearLayout) view.findViewById(R.id.fftTransfer);
        meshTransfer = (LinearLayout) view.findViewById(R.id.meshTransfer);
        ethQrCode = (LinearLayout) view.findViewById(R.id.ethQrCode);
        fftQrCode = (LinearLayout) view.findViewById(R.id.fftQrCode);
        meshQrCode = (LinearLayout) view.findViewById(R.id.meshQrCode);

        raidenTransfer = (LinearLayout) view.findViewById(R.id.raidenTransfer);
    }

    private void setListener(){
        walletManager.setOnClickListener(this);
        walletGesture.setOnClickListener(this);
        walletAddress.setOnClickListener(this);
        createWallet.setOnClickListener(this);
        showQuicMark.setOnClickListener(this);
        walletListView.setOnItemClickListener(this);

        walletImg.setOnClickListener(this);
        walletNameBody.setOnClickListener(this);
        qrCode.setOnClickListener(this);
        transRecord.setOnClickListener(this);
        copyAddress.setOnClickListener(this);

        raidenTransfer.setOnClickListener(this);
        ethTransfer.setOnClickListener(this);
        fftTransfer.setOnClickListener(this);
        meshTransfer.setOnClickListener(this);
        ethQrCode.setOnClickListener(this);
        fftQrCode.setOnClickListener(this);
        meshQrCode.setOnClickListener(this);

        swipe_refresh.setOnRefreshListener(this);
    }

    private void initData() {

        swipe_refresh.setColorSchemeResources(R.color.black);
        mAdapter = new AccountAdapter(getActivity());
        walletListView.setAdapter(mAdapter);
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, R.string.drawer_open,R.string.drawer_close);
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        setWalletGesture();
        initWalletInfo();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.WALLET_REFRESH_DEL);//Refresh the page
        filter.addAction(Constants.WALLET_SUCCESS);//Refresh the page
        filter.addAction(Constants.WALLET_REFRESH_BACKUP);//Refresh the page
        filter.addAction(Constants.WALLET_REFRESH_GESTURE);//Refresh the page
        filter.addAction(Constants.WALLET_REFRESH_SHOW_HINT);//trans
        filter.addAction(Constants.ACTION_GESTURE_LOGIN);//trans
        filter.addAction(Constants.CHANGE_LANGUAGE);//Update language refresh the page
        filter.addAction(XmppAction.ACTION_TRANS);//trans
        getActivity().registerReceiver(mBroadcastReceiver, filter);

//        writePassToSdcard();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String keystorePath = new File(getActivity().getFilesDir(), SDCardCtrl.WALLERPATH).getPath();
//                String raidenDataPath = new File(getActivity().getFilesDir(), SDCardCtrl.RAIDEN_DATA).getPath();
//                String raidenPassPath = new File(getActivity().getFilesDir(), SDCardCtrl.RAIDEN_PASS).getPath() + "/pass";
//                Mobile.mobileStartUp("0x70aefe8d97ef5984b91b5169418f3db283f65a29", keystorePath,"ws://192.168.0.131:8546",raidenDataPath,raidenPassPath);
//            }
//        }).start();
    }
//
//    private void writePassToSdcard() {
//        try {
//            File dir = new File(getActivity().getFilesDir() + SDCardCtrl.RAIDEN_PASS);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
//            File f = new File(dir, "pass");
//            if (!f.exists()) {
//                f.createNewFile();
//            }
//            copyString("123456", f);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void copyString(String fileContents, File outputFile) throws FileNotFoundException {
//        OutputStream output = new FileOutputStream(outputFile);
//        PrintWriter p = new PrintWriter(output);
//        p.println(fileContents);
//        p.flush();
//        p.close();
//    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null){
                if (Constants.WALLET_REFRESH_DEL.equals(intent.getAction()) || Constants.WALLET_REFRESH_BACKUP.equals(intent.getAction()) ) {
                    initWalletInfo();
                }else if (Constants.WALLET_REFRESH_GESTURE.equals(intent.getAction()) || Constants.WALLET_SUCCESS.equals(intent.getAction())) {
                    initWalletInfo();
                }else if (Constants.ACTION_GESTURE_LOGIN.equals(intent.getAction())) {
                    initWalletInfo();
                }else if (Constants.CHANGE_LANGUAGE.equals(intent.getAction())) {
                    Utils.updateViewLanguage(view.findViewById(R.id.account_drawerlayout));
                }else if (XmppAction.ACTION_TRANS.equals(intent.getAction())) {
                    loadData(false);
                }else if (Constants.WALLET_REFRESH_SHOW_HINT.equals(intent.getAction())){
                    if (mDrawerLayout != null){
                        mDrawerLayout.closeDrawer(GravityCompat.END);
                    }
                    initHomePop();
                    showPowTimer();
                }
            }
        }
    };


    /**
     * Initialize the Pop layout
     */
    private void initHomePop() {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.account_more_popup_layout, null);
        homePop = new PopupWindow(view, Utils.dip2px(getActivity(), 160), LinearLayout.LayoutParams.WRAP_CONTENT);
        homePop.setBackgroundDrawable(new BitmapDrawable());
        homePop.setOutsideTouchable(true);
        homePop.setFocusable(true);
        view.findViewById(R.id.txt_home_pop_1).setOnClickListener(this);
        if (homePop.isShowing()) {
            homePop.dismiss();
        } else {
            // On the coordinates of a specific display PopupWindow custom menu
            homePop.showAsDropDown(walletImg, Utils.dip2px(getActivity(), -118), Utils.dip2px(getActivity(), 0));
        }
    }

    private void dismissHomePop() {
        if (homePop != null && homePop.isShowing()) {
            homePop.dismiss();
            homePop = null;
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mBroadcastReceiver);
        LoginUtil.getInstance().destory();
        cancelTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        setWalletGesture();
    }

    /**
     * set wallet gesture  open or close
     * */
    private void setWalletGesture(){
        int walletMode = MySharedPrefs.readInt(getActivity(), MySharedPrefs.FILE_USER, MySharedPrefs.KEY_IS_WALLET_PATTERN);
        byte[] gestureByte;
        if (walletMode == 0 && NextApplication.myInfo != null){
            gestureByte = ACache.get(NextApplication.mContext).getAsBinary(Constants.GESTURE_PASSWORD + NextApplication.myInfo.getLocalId());
        }else{
            gestureByte = ACache.get(NextApplication.mContext).getAsBinary(Constants.GESTURE_PASSWORD);
        }
        if (gestureByte != null && gestureByte.length > 0){
            isChecked = true;
            walletGesture.setText(getString(R.string.gesture_wallet_open));
        }else{
            isChecked = false;
            walletGesture.setText(getString(R.string.gesture_wallet_close));
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.walletImg://Open the sidebar
                mDrawerLayout.openDrawer(GravityCompat.END);
                dismissHomePop();
                cancelTimer();
                break;
            case R.id.txt_home_pop_1://Open the sidebar
                dismissHomePop();
                cancelTimer();
                break;
            case R.id.walletManager://Account management
                mDrawerLayout.closeDrawer(GravityCompat.END);
                dismissHomePop();
                cancelTimer();
                startActivity(new Intent(getActivity(), ManagerWalletActivity.class));
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.walletGesture://Account management
                int walletMode = MySharedPrefs.readInt(getActivity(), MySharedPrefs.FILE_USER, MySharedPrefs.KEY_IS_WALLET_PATTERN);
                byte[] gestureByte;
                if (walletMode == 0 && NextApplication.myInfo != null){
                    gestureByte = ACache.get(NextApplication.mContext).getAsBinary(Constants.GESTURE_PASSWORD + NextApplication.myInfo.getLocalId());
                }else{
                    gestureByte = ACache.get(NextApplication.mContext).getAsBinary(Constants.GESTURE_PASSWORD);
                }
                if (gestureByte != null && gestureByte.length > 0){
                    Intent intent = new Intent(getActivity(),GestureLoginActivity.class);
                    intent.putExtra("type",isChecked ? 2 : 1);
                    startActivity(intent);
                    isChecked = !isChecked;
                }else{
                    Intent intent = new Intent(getActivity(),CreateGestureActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.createWallet://Create a wallet
                mDrawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(getActivity(), WalletCreateActivity.class));
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.showQuicMark://Flicking a
                mDrawerLayout.closeDrawer(GravityCompat.END);
                startActivity(new Intent(getActivity(), CaptureActivity.class));
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.walletNameBody://Backup the purse
                if (storableWallet == null){
                    return;
                }
                Intent copyIntent = new Intent(getActivity(),WalletCopyActivity.class);
                copyIntent.putExtra(Constants.WALLET_INFO, storableWallet);
                copyIntent.putExtra(Constants.WALLET_ICON, storableWallet.getImgId());
                copyIntent.putExtra(Constants.WALLET_TYPE, 1);
                startActivity(copyIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.walletAddress://Qr code
                if (storableWallet == null){
                    return;
                }
                Intent qrCodeIntent = new Intent(getActivity(),QuickMarkShowUI.class);
                qrCodeIntent.putExtra("type", 0);
                qrCodeIntent.putExtra("address", storableWallet.getPublicKey());
                startActivity(qrCodeIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.transRecord://Transaction records
                if (NextApplication.myInfo == null){
                    startActivity(new Intent(getActivity(),WalletModeLoginUI.class));
                    Utils.openNewActivityAnim(getActivity(),false);
                }else{
                    Intent transIntent = new Intent(getActivity(), TransactionRecordsActivity.class);
                    transIntent.putExtra("address",walletAddress.getText().toString());
                    transIntent.putExtra("name",storableWallet.getWalletName());
                    startActivity(transIntent);
                    Utils.openNewActivityAnim(getActivity(),false);
                }
                break;
            case R.id.copyAddress://Copy the address
                if (storableWallet != null && !storableWallet.isBackup()){
                    Intent intent = new Intent(getActivity(), AlertActivity.class);
                    intent.putExtra("type", 5);
                    intent.putExtra("strablewallet", storableWallet);
                    startActivity(intent);
                    getActivity().overridePendingTransition(0, 0);
                    return;
                }
                Utils.copyText(getActivity(),walletAddress.getText().toString());
                break;
            case R.id.ethTransfer://The eth transfer
                Intent ethIntent = new Intent(getActivity(),WalletSendActivity.class);
                ethIntent.putExtra("sendtype", 0);
                startActivity(ethIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.fftTransfer://SMT transfer
                Intent fftIntent = new Intent(getActivity(),WalletSendActivity.class);
                fftIntent.putExtra("sendtype", 1);
                startActivity(fftIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.meshTransfer://SMT transfer
                Intent meshIntent = new Intent(getActivity(),WalletSendActivity.class);
                meshIntent.putExtra("sendtype", 2);
                startActivity(meshIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.ethQrCode://The eth qr code collection
                if (storableWallet == null){
                    return;
                }
                Intent qrEthIntent = new Intent(getActivity(),QuickMarkShowUI.class);
                qrEthIntent.putExtra("type", 1);
                qrEthIntent.putExtra("address", storableWallet.getPublicKey());
                startActivity(qrEthIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.fftQrCode://SMT qr code collection
                if (storableWallet == null){
                    return;
                }
                Intent fftEthIntent = new Intent(getActivity(),QuickMarkShowUI.class);
                fftEthIntent.putExtra("type", 2);
                fftEthIntent.putExtra("address", storableWallet.getPublicKey());
                startActivity(fftEthIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.meshQrCode://SMT qr code collection
                if (storableWallet == null){
                    return;
                }
                Intent meshEthIntent = new Intent(getActivity(),QuickMarkShowUI.class);
                meshEthIntent.putExtra("type", 3);
                meshEthIntent.putExtra("address", storableWallet.getPublicKey());
                startActivity(meshEthIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
            case R.id.raidenTransfer://SMT qr code collection
                if (storableWallet == null){
                    return;
                }
                Intent raidenIntent = new Intent(getActivity(),RaidenChannelList.class);
                raidenIntent.putExtra("storableWallet", storableWallet);
                startActivity(raidenIntent);
                Utils.openNewActivityAnim(getActivity(),false);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!WalletStorage.getInstance(getActivity().getApplicationContext()).get().get(position).isSelect()){
            for (int i = 0 ; i < WalletStorage.getInstance(getActivity().getApplicationContext()).get().size(); i++){
                if (i != position){
                    WalletStorage.getInstance(getActivity().getApplicationContext()).get().get(i).setSelect(false);
                }else{
                    WalletStorage.getInstance(getActivity().getApplicationContext()).get().get(i).setSelect(true);
                }
            }
            initWalletInfo();
            mDrawerLayout.closeDrawer(GravityCompat.END);
        }
    }

    /**
     * Load or refresh the wallet information
     * */
    private void initWalletInfo(){
        ArrayList<StorableWallet> storableWallets = WalletStorage.getInstance(getActivity().getApplicationContext()).get();
        for (int i = 0 ; i < storableWallets.size(); i++){
            if (storableWallets.get(i).isSelect() ){
                WalletStorage.getInstance(NextApplication.mContext).updateWalletToList(NextApplication.mContext,storableWallets.get(i).getPublicKey(),false);
                index = i;
                int imgId = Utils.getWalletImg(getActivity(),i);
                walletImg.setImageResource(imgId);
                storableWallet = storableWallets.get(i);
                storableWallet.setImgId(imgId);
                break;
            }
        }
        if (index == -1 && storableWallets.size() > 0){
            int imgId = Utils.getWalletImg(getActivity(),0);
            walletImg.setImageResource(imgId);
            storableWallet = storableWallets.get(0);
            storableWallet.setImgId(imgId);
        }

        if (storableWallet != null){
            walletName.setText(storableWallet.getWalletName());

            if (storableWallet.isBackup()){
                walletBackup.setVisibility(View.GONE);
            }else{
                walletBackup.setVisibility(View.VISIBLE);
            }

            String address = storableWallet.getPublicKey();
            if(!address.startsWith("0x")){
                address = "0x"+address;
            }
            walletAddress.setText(address);

            if (storableWallet.getEthBalance() > 0){
                BigDecimal ethDecimal = new BigDecimal(storableWallet.getEthBalance()).setScale(10,BigDecimal.ROUND_DOWN);
                ethBalance.setText(ethDecimal.toPlainString());
            }else{
                ethBalance.setText(storableWallet.getEthBalance() +"");
            }
            if (storableWallet.getFftBalance() > 0){
                BigDecimal fftDecimal = new BigDecimal(storableWallet.getFftBalance()).setScale(5,BigDecimal.ROUND_DOWN);
                fftBalance.setText(fftDecimal.toPlainString());
            }else{
                fftBalance.setText(storableWallet.getFftBalance() +"");
            }

            if (storableWallet.getMeshBalance() > 0){
                BigDecimal meshDecimal = new BigDecimal(storableWallet.getMeshBalance()).setScale(5,BigDecimal.ROUND_DOWN);
                meshBalance.setText(meshDecimal.toPlainString());
            }else{
                meshBalance.setText(storableWallet.getMeshBalance() +"");
            }
        }

        mAdapter.resetSource(WalletStorage.getInstance(NextApplication.mContext).get());

        new Handler().postDelayed(new Runnable(){
            public void run() {
                swipe_refresh.setRefreshing(true);
                loadData(true);
            }
        }, 500);
    }
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable(){
            public void run() {
                loadData(true);
            }
        }, 500);
    }

    /**
     * Access to the account balance
     * */
    private void loadData(final boolean isShowToast){
        try {
            NetRequestUtils.getInstance().getBalance(getActivity(),walletAddress.getText().toString(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (isShowToast){
                        mHandler.sendEmptyMessage(0);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = response.body().string();
                    mHandler.sendMessage(message);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    showToast(getString(R.string.error_get_balance));
                    swipe_refresh.setRefreshing(false);
                    break;
                case 1:
                    swipe_refresh.setRefreshing(false);
                    parseJson((String)msg.obj);
                    break;
                case 2:
                    dismissHomePop();
                    break;
            }
        }
    };

    /**
     * parse json
     * */
    private void parseJson(String jsonString){
        if (TextUtils.isEmpty(jsonString)){
            return;
        }
        try {
            JSONObject object = new JSONObject(jsonString);
            int errcod = object.optInt("errcod");
            if (errcod == 0){
                double ethBalance1 = object.optJSONObject("data").optDouble("eth");
                double fftBalance1 = object.optJSONObject("data").optDouble("smt");
                double meshBalance1 = object.optJSONObject("data").optDouble("mesh");
                if (ethBalance1 > 0){
                    BigDecimal ethDecimal = new BigDecimal(ethBalance1).setScale(10,BigDecimal.ROUND_DOWN);
                    ethBalance.setText(ethDecimal.toPlainString());
                }else{
                    ethBalance.setText(ethBalance1 +"");
                }
                if (fftBalance1 > 0){
                    BigDecimal fftDecimal = new BigDecimal(fftBalance1).setScale(5,BigDecimal.ROUND_DOWN);
                    fftBalance.setText(fftDecimal.toPlainString());
                }else{
                    fftBalance.setText(fftBalance1 + "");
                }

                if (meshBalance1 > 0){
                    BigDecimal meshDecimal = new BigDecimal(meshBalance1).setScale(5,BigDecimal.ROUND_DOWN);
                    meshBalance.setText(meshDecimal.toPlainString());
                }else{
                    meshBalance.setText(meshBalance1 + "");
                }

                storableWallet.setEthBalance(ethBalance1);
                storableWallet.setFftBalance(fftBalance1);
                storableWallet.setMeshBalance(meshBalance1);
            }else{
                if(errcod == -2){
                    long difftime = object.optJSONObject("data").optLong("difftime");
                    long tempTime =  MySharedPrefs.readLong(getActivity(),MySharedPrefs.FILE_APPLICATION,MySharedPrefs.KEY_REQTIME);
                    MySharedPrefs.writeLong(getActivity(),MySharedPrefs.FILE_APPLICATION,MySharedPrefs.KEY_REQTIME,difftime + tempTime);
                }
                MyToast.showToast(getActivity(),object.optString("msg"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Control the popup countdown
     * */
    private void showPowTimer(){
        if(timer == null){
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    timerLine--;
                    if(timerLine < 0){
                        mHandler.sendEmptyMessage(2);
                        if (timer != null){
                            timer.cancel();
                            timer = null;
                        }
                        if (timerTask != null){
                            timerTask.cancel();
                            timerTask = null;
                        }
                    }
                }
            };
            timer.schedule(timerTask,1000,1000);
        }
    }

    private void cancelTimer(){
        if (timer != null){
            timer.cancel();
            timer = null;
        }
        if (timerTask != null){
            timerTask.cancel();
            timerTask = null;
        }
    }

}
