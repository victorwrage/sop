package com.baibao.sop;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.baibao.sop.bean.CheckPayInfo;
import com.baibao.sop.fragment.FragmentMain;
import com.baibao.sop.fragment.FragmentPay;
import com.baibao.sop.utils.Constant;
import com.baibao.sop.utils.D2000V1ScanInitUtils;
import com.baibao.sop.utils.VToast;
import com.pos.api.Printer;
import com.socks.library.KLog;

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity implements FragmentMain.IMainListener, FragmentPay.IPayListener {
    private static final int RECORD_PROMPT_MSG = 0x06;

    private static final int SCAN_CLOSED = 1020;
    String ORDER_PA = "";

    private static final int TRADE_STATE_TYPE_ALIPAY = 0;//  支付宝类型
    private static final int TRADE_STATE_TYPE_WXPAY = TRADE_STATE_TYPE_ALIPAY + 1;// 微信类型
    private static final int TRADE_STATE_TYPE_CARDPAY = TRADE_STATE_TYPE_WXPAY + 1;// 刷卡类型
    private static final int TRADE_STATE_TYPE_CASHPAY = TRADE_STATE_TYPE_CARDPAY + 1;// 现金类型

    private Printer printer;
    IntentFilter filter;

    FragmentMain fragment0;
    FragmentPay fragment1;
    private String merchant_name;
    Double receive_cash = 0.00;
    private int cur_page = 0;

    String termianl, card, tenant;
    D2000V1ScanInitUtils d2000V1ScanInitUtils;
    Boolean isInit = false;
    String TRACE_NUM = "";
    String PATCH_NUM = "";
    private Executor executor;

    private void sendData(String obj) {
        KLog.v("sendData" + obj);
        switch (cur_page) {
            case 0:
                fragment0.fetchScanResult(obj.trim());
                break;
            case 1:
                fragment1.newPay(obj.trim());
                break;
            default:
                VToast.toast(context, "没有处理");
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (fragment1 != null && fragment1.isVisible()) {
            gotoMain(0);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Exit();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        executor = Executors.newSingleThreadScheduledExecutor();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        fragment0 = new FragmentMain();
        ft.add(R.id.fragment_container, fragment0, PAGE_0);
        ft.show(fragment0);
        ft.commit();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (filter == null) {
            filter = new IntentFilter();
            filter.addAction("android.print.message");
            filter.addAction("android.intent.action.PayAmountBroadcastReceiver");
            filter.addAction("android.intent.action.D2000PayAmountBroadcastReceiver");
            registerReceiver(mPrtReceiver, filter);
        }
        if (!ORDER_PA.equals("")) {//防止未安装收货APP时扫描器开启
            ORDER_PA = "";
            return;
        }
        if (card != null) return;
        if (!isInit) {
            isInit = true;
        } else {
            KLog.v("请稍后");
            showWaitDialog("请稍后");
            promptHandler.postDelayed(() -> hideWaitDialog(), 5000);
        }
        KLog.v("onResume" + d2000V1ScanInitUtils.getStart());
        executor.execute(() -> startScan());
        merchant_name = Constant.cookie.get("user_name");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (d2000V1ScanInitUtils.getStart()) {
            d2000V1ScanInitUtils.setStart(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (d2000V1ScanInitUtils == null) {
            d2000V1ScanInitUtils = D2000V1ScanInitUtils.getInstance(MainActivity.this, promptHandler);
        }
    }

    private void Exit() {
        showDialog(EXIT_CONFIRM, "提示", "是否退出?", "确认", "取消");
    }

    @Override
    protected void onDestroy() {
        if (null != printer) printer.DLL_PrnRelease();
        d2000V1ScanInitUtils.close();
        unregisterReceiver(mPrtReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver mPrtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.D2000PayAmountBroadcastReceiver")) {
                KLog.v(intent.getStringExtra("bn") + "---" + intent.getStringExtra("result"));
                if (intent.getStringExtra("result").equals("success")) {

                    PATCH_NUM = intent.getStringExtra("bn") == null ? "" : intent.getStringExtra("bn");
                    TRACE_NUM = intent.getStringExtra("pc") == null ? "" : intent.getStringExtra("pc");
                    String BANK_NUM = card = intent.getStringExtra("card") == null ? "" : intent.getStringExtra("card");
                    termianl = intent.getStringExtra("termianl") == null ? "" : intent.getStringExtra("termianl");
                    tenant = intent.getStringExtra("tenant") == null ? "" : intent.getStringExtra("tenant");
                    fragment1.SynchronizePay(BANK_NUM, PATCH_NUM, TRACE_NUM);
                } else {
                    startScan();
                }
                return;
            }
            if (intent.getAction().equals("android.intent.action.PayAmountBroadcastReceiver")) {
                String status = intent.getStringExtra("checkStatus");
                receive_cash = intent.getDoubleExtra("checkOrderAmount", 0);
                ORDER_PA = "";
                if (status.equals("1001")) {
                    // VToast.toast(context, "确认收货成功");

                    KLog.v(receive_cash + "");
                    fragment0.viewBottom(receive_cash);
                    // gotoPay();//d
                } else {//新利源回调失败
                    VToast.toast(context, "确认收货失败");
                }
            } else {

            }
        }
    };

    private Handler promptHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECORD_PROMPT_MSG:
                    sendData((String) msg.obj);
                    break;
                case SCAN_CLOSED:
                    if (fragment1 != null) fragment1.print();
                    break;
                default:
                    break;
            }
        }
    };


    private void startScan() {
        if (!d2000V1ScanInitUtils.getStart()) {
            d2000V1ScanInitUtils.open();
        }
        d2000V1ScanInitUtils.d2000V1ScanOpen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        KLog.v("onActivityResult" + ORDER_PA);
        if (requestCode == 2202 && !ORDER_PA.equals("")) {
            showWaitDialog("请稍后");
            promptHandler.postDelayed(() -> {
                scan();
                hideWaitDialog();
            }, 2000);
        }
        if (ORDER_PA.equals("")) {
            ORDER_PA = "000";
        }
    }

    @Override
    protected void confirm(int type, DialogInterface dialog) {
        super.confirm(type, dialog);
        switch (type) {
            case EXIT_CONFIRM:
                System.exit(0);
                break;
        }
    }

    @Override
    public void finishMain() {
        Exit();
    }

    @Override
    public void gotoPay(HashMap<String, String> intent) {
        cur_page = 1;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment1 == null) {
            fragment1 = new FragmentPay();
            ft.add(R.id.fragment_container, fragment1, PAGE_1);
        }
        fragment1.initState(intent);
        ft.show(fragment1);
        ft.hide(fragment0);
        ft.commit();
    }

    @Override
    public void gotoSynergy(String order_no) {
        ORDER_PA = order_no;
        try {
            String pn = "com.synergymall.driver";
            String an = "com.synergymall.driver.order.OrderHistoryDetailActivity";
            Intent intent = new Intent();
            ComponentName component = new ComponentName(pn, an);
            intent.setComponent(component);
            intent.putExtra("orderNo", order_no); //此处传入出库订单编号
            startActivityForResult(intent, 2202);
        } catch (ActivityNotFoundException e) {
            VToast.toast(context, "没有安装【确认收获】APP");
            ORDER_PA = "";
            showWaitDialog("请稍后");
            promptHandler.postDelayed(() -> {
                scan();
                hideWaitDialog();
            }, 2000);
            e.fillInStackTrace();
        }

    }

    public void gotoYLpay(String order) {
        d2000V1ScanInitUtils.setScanState();
        try {
            String pn = "com.qhw.swishcardapp";
            String an = "com.qhw.swishcardapp.activity.LoginActivity";
            Intent intent = new Intent();
            ComponentName component = new ComponentName(pn, an);
            intent.setComponent(component);
            intent.setFlags(101);
            intent.putExtra("money", order); //此处传入出库订单编号
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.fillInStackTrace();
            showWaitDialog("请稍后");
            promptHandler.postDelayed(() -> {
                scan();
                hideWaitDialog();
            }, 2000);
            VToast.toast(context, "没有安装银联支付模块");
        }
    }

    @Override
    public void scan() {
        startScan();
    }

    @Override
    public void startOpenDevice() {
        d2000V1ScanInitUtils.open();
    }

    private void doPrint(int cur_pay_type, HashMap<String, String> order_info, CheckPayInfo checkPayInfo, String BANK_NUM) {
        printer = new Printer(this, bRet -> executor.execute(() -> {
            int iRet = -1;
            iRet = printer.DLL_PrnInit();
            KLog.v("setScanState" + iRet);
            if (iRet == 0) {
                printStr(cur_pay_type, order_info, checkPayInfo, BANK_NUM);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                printStr(cur_pay_type, order_info, checkPayInfo, BANK_NUM);
            } else {
                VToast.toast(context, "打印错误");
            }
        }));
        showWaitDialog("请等待打印完成");
        promptHandler.postDelayed(() -> {
            hideWaitDialog();
            gotoMain(1);
        }, 15000);
    }

    public void closeScanThenPrint() {
        new Thread(() -> d2000V1ScanInitUtils.setScanState()).start();
        showWaitDialog("请稍等");
        new Thread(() -> {
            hideWaitDialog();
            promptHandler.sendEmptyMessageDelayed(SCAN_CLOSED, 2000);
        }).start();
    }

    private void printStr(int cur_pay_type, HashMap<String, String> order_info, CheckPayInfo checkPayInfo, String BANK_NUM) {
        String pay_tp = "";
        Bitmap bitmap = readBitMap(R.drawable.print_icon);
        switch (cur_pay_type) {
            case TRADE_STATE_TYPE_CARDPAY:
                pay_tp = "银联支付";
                break;
            case TRADE_STATE_TYPE_CASHPAY:
                pay_tp = "现金支付";
                break;
            case TRADE_STATE_TYPE_WXPAY:
                pay_tp = "微信支付";
                break;
            case TRADE_STATE_TYPE_ALIPAY:
                pay_tp = "支付宝支付";
                break;
        }

        printer.DLL_PrnBmp(bitmap);
        /*printer.DLL_PrnSetFont((byte)16, (byte)16, (byte)0x33);
        printer.DLL_PrnStr("    云仓POS支付\n");*/
        printer.DLL_PrnSetFont((byte) 24, (byte) 24, (byte) 0x00);
        printer.DLL_PrnStr("-------------------------------\n");
        printer.DLL_PrnStr("商户名:" + order_info.get("customer_name") + "\n");
        printer.DLL_PrnStr("商户编号:" + order_info.get("customer_num") + "\n");
        printer.DLL_PrnStr("-------------------------------\n");
        printer.DLL_PrnStr("订单号:" + order_info.get("order_no") + "\n");
        printer.DLL_PrnStr("支付类型:" + pay_tp + "\n");
        printer.DLL_PrnStr("支付渠道:" + merchant_name + "\n");
        printer.DLL_PrnStr("机器序号:" + order_info.get("seri_no") + "\n");
        if (order_info.get("buyer_logon_id") != null && !order_info.get("buyer_logon_id").equals("")) {
            printer.DLL_PrnStr("支付账号:" + order_info.get("buyer_logon_id") + "\n");
        }
        if (order_info.get("buyer_logon_id") != null && !order_info.get("buyer_logon_id").equals("")) {
            printer.DLL_PrnStr("支付凭证:" + checkPayInfo.getAuth_code() + "\n");
        }
        if (checkPayInfo.getOrder_no() != null && !checkPayInfo.getOrder_no().equals("")) {
            printer.DLL_PrnStr("支付单号:" + checkPayInfo.getOrder_no() + "\n");
        }
        if (TRACE_NUM != null && !TRACE_NUM.equals("")) {
            printer.DLL_PrnStr("流水号:" + TRACE_NUM + "\n");
        }
        if (PATCH_NUM != null && !PATCH_NUM.equals("")) {
            printer.DLL_PrnStr("批次号:" + PATCH_NUM + "\n");
        }
        if (card != null) {
            printer.DLL_PrnStr("银行卡号:" + util.getIDCardEncrypt(card) + "\n");
        }
        if (termianl != null) {
            printer.DLL_PrnStr("终端号:" + termianl + "\n");
        }
        if (tenant != null) {
            printer.DLL_PrnStr("门店号:" + tenant + "\n");
        }
        printer.DLL_PrnStr("-------------------------------\n");
        printer.DLL_PrnStr("支付日期:" + currentDate("yyyyMMdd HH:mm:ss") + "\n");
        printer.DLL_PrnStr("总计(Total): RMB" + order_info.get("cash_cur") + "\n");
        printer.DLL_PrnStr("  \n");
        printer.DLL_PrnSetFont((byte) 16, (byte) 16, (byte) 0x00);
        printer.DLL_PrnStr("备注:" + "  \n");
        printer.DLL_PrnStr("  \n");
        printer.DLL_PrnStr("          签名：" + "  \n");
        Bitmap bitmap2 = readBitMap(R.drawable.blank);
        printer.DLL_PrnBmp(bitmap2);
        printer.DLL_PrnStr("-------------------------------\n");
        printer.DLL_PrnStart();
    }

    public Bitmap readBitMap(int resId) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        // 获取资源图片
        InputStream is = getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is, null, opt);
    }

    private void toMain() {
        cur_page = 0;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment0 == null) {
            fragment0 = new FragmentMain();
            ft.add(R.id.fragment_container, fragment0, PAGE_0);
        }
        ft.show(fragment0);
        ft.hide(fragment1);
        ft.commit();
    }

    @Override
    public void gotoMain(int state) {
        toMain();
        switch (state) {
            case 0:
                break;
            case 1:
                card = null;
                termianl = null;
                tenant = null;
                PATCH_NUM = null;
                TRACE_NUM = null;
                fragment0.initState();
                break;
        }
    }

    @Override
    public void print(int cur_pay_type, HashMap<String, String> order_info, CheckPayInfo checkPayInfo, String BANK_NUM) {
        promptHandler.postDelayed(() -> doPrint(cur_pay_type, order_info, checkPayInfo, BANK_NUM), 1000);
    }

}
