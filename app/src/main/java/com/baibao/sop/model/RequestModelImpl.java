package com.baibao.sop.model;

import com.baibao.sop.bean.SynergyCustomerOrderInfo;
import com.baibao.sop.bean.SynergyMallResponse;
import com.baibao.sop.bean.SynergyPayBack;
import com.baibao.sop.bean.SynergyPayBackResult;
import com.baibao.sop.bean.SynergyRequest;
import com.baibao.sop.bean.xml_check_info_root;
import com.baibao.sop.bean.xml_login_info_root;
import com.baibao.sop.bean.xml_pay_info_root;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;


/**
 * Info:接口实现类
 * Created by xiaoyl
 * 创建时间:2017/4/7 9:42
 */

public class RequestModelImpl implements IRequestMode {
    IRequestMode iRequestMode;

    @Override
    public Observable<SynergyCustomerOrderInfo> QueryOrder(@Url String url, @Body SynergyRequest synergyRequest) {
        return iRequestMode.QueryOrder(url,synergyRequest);
    }

    @Override
    public Observable<SynergyPayBackResult> SendPay(@Url String url,@Body SynergyPayBack synergyPayBack) {
        return iRequestMode.SendPay(url,synergyPayBack);
    }

    @Override
    public Observable<SynergyMallResponse> SendPayToMall(@Query("ddh") String ddh) {
        return iRequestMode.SendPayToMall(ddh);
    }


    @Override
    public Observable<xml_login_info_root> Login(@Field("user_login") String user_login, @Field("user_pass") String user_pass) {
        return iRequestMode.Login(user_login, user_pass);
    }

    @Override
    public Observable<xml_pay_info_root> Pay(@Path("type") String type, @Field("username") String username, @Field("password") String password, @Field("numscreen") String numscreen, @Field("code") String code) {
        return iRequestMode.Pay(type, username, password, numscreen, code);
    }

    @Override
    public Observable<xml_check_info_root> CheckPayA(@Path("type") String type, @Field("username") String username, @Field("password") String password, @Field("order_no") String order_no) {
        return iRequestMode.CheckPayA(type, username, password, order_no);
    }

    @Override
    public Observable<xml_check_info_root> CheckPayB(@Path("type") String type, @Field("username") String username, @Field("password") String password, @Field("auth_code") String auth_code) {
        return iRequestMode.CheckPayB(type, username, password, auth_code);
    }


}
