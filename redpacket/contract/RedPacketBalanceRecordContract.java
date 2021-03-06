package com.lingtuan.firefly.redpacket.contract;

import com.lingtuan.firefly.base.BasePresenter;
import com.lingtuan.firefly.base.BaseView;
import com.lingtuan.firefly.redpacket.bean.RedPacketRecordBean;

import java.util.ArrayList;

public interface RedPacketBalanceRecordContract {

    interface Presenter extends BasePresenter {

        /**
         * get red packet balance record
         * */
        void loadData(int page);
    }

    interface View extends BaseView<Presenter> {

        void success(ArrayList<RedPacketRecordBean> redPacketRecords,int currentPage,boolean resetFooterState);

        void error(int errorCode, String errorMsg);
    }
}
