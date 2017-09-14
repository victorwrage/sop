package com.baibao.sop.present;


import com.baibao.sop.bean.CheckPayInfo;
import com.baibao.sop.bean.LoginInfoRequest;
import com.baibao.sop.bean.SynergyPayBack;
import com.baibao.sop.bean.PayInfo;
import com.baibao.sop.bean.SynergyRequest;


/**
 * Info:
 * Created by xiaoyl
 * 创建时间:2017/4/7 9:46
 */

public interface IRequestPresent {
    void QueryOder(SynergyRequest synergyRequest);
    void SendPay(SynergyPayBack synergyPayBack);
    void SendToMall(String ddh);
    void Login(LoginInfoRequest request);

    void Pay(String path,PayInfo info);
    void CheckPay(String path,CheckPayInfo info);

}
