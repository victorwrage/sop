package com.baibao.sop.present;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.baibao.sop.bean.CheckPayInfo;
import com.baibao.sop.bean.LoginInfoRequest;
import com.baibao.sop.bean.PayInfo;
import com.baibao.sop.bean.SynergyCustomerOrderInfo;
import com.baibao.sop.bean.SynergyMallResponse;
import com.baibao.sop.bean.SynergyPayBack;
import com.baibao.sop.bean.SynergyPayBackResult;
import com.baibao.sop.bean.SynergyRequest;
import com.baibao.sop.bean.xml_check_info_root;
import com.baibao.sop.bean.xml_login_info_root;
import com.baibao.sop.bean.xml_pay_info_root;
import com.baibao.sop.model.IRequestMode;
import com.baibao.sop.model.converter.CustomGsonConverter;
import com.baibao.sop.model.converter.CustomXmlConverter;
import com.baibao.sop.utils.Constant;
import com.baibao.sop.view.ILoginView;
import com.baibao.sop.view.IOrderView;
import com.baibao.sop.view.IPayView;
import com.baibao.sop.view.IView;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by Administrator on 2017/4/6.
 */

public class QueryPresent implements IRequestPresent {
    private IView iView;
    private Context context;
    private IRequestMode iRequestMode;

    private static QueryPresent instance = null;

    public void setView(Activity activity) {
        iView = (IView) activity;
    }

    public void setView(Fragment fragment) {
        iView = (IView) fragment;
    }

    private QueryPresent(Context context_) {
        context = context_;
    }

    public static synchronized QueryPresent getInstance(Context context) {
        if (instance == null) {
            return new QueryPresent(context);
        }
        return instance;
    }

    public void initRetrofit(String url, boolean isXml) {
        try {
            if (isXml) {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(CustomXmlConverter.create())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build();
                iRequestMode = retrofit.create(IRequestMode.class);
            } else {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(CustomGsonConverter.create())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .build();
                iRequestMode = retrofit.create(IRequestMode.class);
            }

        } catch (IllegalArgumentException e) {
            e.fillInStackTrace();
        }
    }

    public void initRetrofit2(String url, boolean isXml) {
        try {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(new OkHttpClient.Builder()
                            .addNetworkInterceptor(
                                    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                            .addNetworkInterceptor(
                                    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                            .addNetworkInterceptor(
                                    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build())
                    .build();
            iRequestMode = retrofit.create(IRequestMode.class);

        } catch (IllegalArgumentException e) {
            e.fillInStackTrace();
        }
    }

    @Override
    public void QueryOder(SynergyRequest synergyRequest) {

        iRequestMode.QueryOrder(Constant.URL_SYNERGY +"zsap_pos?sap-client="+ Constant.URL_SYNERGY_PARAM+"&method=ZCOD_ORDER_INFO", synergyRequest)
                .onErrorReturn(s -> new SynergyCustomerOrderInfo())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> ((IOrderView) iView).ResolveCustomerOrder(s));
    }

    @Override
    public void SendPay(SynergyPayBack synergyPayBack) {
        iRequestMode.SendPay(Constant.URL_SYNERGY+"zsap_pos?sap-client="+Constant.URL_SYNERGY_PARAM+"&method=ZCOD_PAYMENT_RET",synergyPayBack)
                .onErrorReturn(s -> new SynergyPayBackResult())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> ((IPayView) iView).ResolveSynergyPayInfo(s));
    }


    @Override
    public void SendToMall(String ddh) {
        iRequestMode.SendPayToMall(ddh)
                .onErrorReturn(s -> new SynergyMallResponse())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> ((IPayView) iView).ResolveMallInfo(s));
    }

    @Override
    public void Login(LoginInfoRequest request) {
        iRequestMode.Login(request.getUser_login(), request.getUser_pass())
                .onErrorReturn(s -> new xml_login_info_root())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> ((ILoginView) iView).ResolveLoginInfo(s));
    }

    @Override
    public void Pay(String path, PayInfo info) {
        iRequestMode.Pay(path, info.getUsername(), info.getPassword(), info.getNumscreen(), info.getCode())
                .onErrorReturn(s -> new xml_pay_info_root())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> ((IPayView) iView).ResolvePayInfo(s));
    }

    @Override
    public void CheckPay(String path, CheckPayInfo info) {
        if (info.getOrder_no().equals("")) {
            iRequestMode.CheckPayB(path, info.getUsername(), info.getPassword(), info.getOrder_no())
                    .onErrorReturn(s -> new xml_check_info_root())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> ((IPayView) iView).ResolveCheckPayInfo(s));
        } else {
            iRequestMode.CheckPayA(path, info.getUsername(), info.getPassword(), info.getOrder_no())
                    .onErrorReturn(s -> new xml_check_info_root())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> ((IPayView) iView).ResolveCheckPayInfo(s));
        }
    }


}
