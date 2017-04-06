package com.baibao.sop.model;

import android.content.Context;

import com.baibao.sop.bean.Result;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by xyl on 2017/4/6.
 */

public interface IRequest {
    @GET("zsap_pos")
    Observable<List<Result>> QueryOrder(Context context, String sap_client, String method);
}
