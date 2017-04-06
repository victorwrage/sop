package com.baibao.sop;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        GetOrdernoTask task = new GetOrdernoTask();
        task.execute("18670841076");
    }

    private String getSap() throws Exception {
        String url = "http://www.synergyiclub.com:8999/zsap_pos?sap-client=500&method=ZCOD_ORDER_INFO";

        Sap sap = new Sap();

        sap.setSCFID("");

        sap.setVBELN("3000204844");

        HttpClient httpClient = HttpClients.createDefault();

        HttpPost post = new HttpPost(url);

        StringEntity postingString = new StringEntity(new Gson().toJson(sap));

        post.setEntity(postingString);

        HttpResponse response = httpClient.execute(post);

        String content = EntityUtils.toString(response.getEntity());

        Log.i("test", content);

        System.out.println(content);

        return content;
    }


    /**
     * 手机号段归属地查询
     *
     * @param phoneSec 手机号段
     */
    public String getRemoteInfo(String phoneSec) throws Exception {
        String WSDL_URI = "http://webservice.webxml.com.cn/WebServices/MobileCodeWS.asmx?WSDL";//wsdl 的uri
        String namespace = "http://WebXml.com.cn/";//namespace
        String methodName = "getMobileCodeInfo";//要调用的方法名称

        SoapObject request = new SoapObject(namespace, methodName);
        // 设置需调用WebService接口需要传入的两个参数mobileCode、userId
        request.addProperty("mobileCode", phoneSec);
        request.addProperty("userId", "");

        //创建SoapSerializationEnvelope 对象，同时指定soap版本号(之前在wsdl中看到的)
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapSerializationEnvelope.VER12);
        envelope.bodyOut = request;//由于是发送请求，所以是设置bodyOut
        envelope.dotNet = true;//由于是.net开发的webservice，所以这里要设置为true

        HttpTransportSE httpTransportSE = new HttpTransportSE(WSDL_URI);
        httpTransportSE.call(null, envelope);//调用

        // 获取返回的数据
        SoapObject object = (SoapObject) envelope.bodyIn;
        // 获取返回的结果
        String result = object.getProperty(0).toString();
        Log.v("debug", result);
        return result;

    }

    private class GetOrdernoTask extends AsyncTask<String, Integer, String> {


        @Override
        protected String doInBackground(String... params) {
            String r = "no response";
            try {
                r = getSap();
                // r = getRemoteInfo(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return r;
        }

        @Override
        protected void onPostExecute(final String s) {
            super.onPostExecute(s);
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(s)
                    .setTitle("消息")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();


        }
    }

    private class Sap {
        public String getSCFID() {
            return scfid;
        }

        public void setSCFID(String scfid) {
            this.scfid = scfid;
        }

        public String getVBELN() {
            return vbeln;
        }

        public void setVBELN(String vbeln) {
            this.vbeln = vbeln;
        }

        private String scfid;
        private String vbeln;
    }
}
