package com.hzncc.kevin.frareddemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.hzncc.kevin.frareddemo.ir_sdk.CameraSDK;
import com.hzncc.kevin.frareddemo.ir_sdk.CameraUtil;
import com.hzncc.kevin.frareddemo.ir_sdk.Device_w324_h256;
import com.hzncc.kevin.frareddemo.ir_sdk.Device_w336_h256;
import com.hzncc.kevin.frareddemo.ir_sdk.Device_w80_h60;
import com.hzncc.kevin.frareddemo.ir_sdk.LeptonStatus;
import com.hzncc.kevin.frareddemo.utils.SharedPreferencesUtil;
import com.hzncc.kevin.robot_ir.data.IR_ImageData;
import com.hzncc.kevin.robot_ir.renderers.GLBitmapRenderer;
import com.hzncc.kevin.robot_ir.renderers.GLRGBRenderer;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.nio.ShortBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private boolean isRuning;
    private CameraUtil cameraUtil;
    private Spinner devices;
    private Spinner colorTable;
    private EditText ipAddr;
    private int port = 6666;
    private int viewWidth, viewHeight;
    private float rateW, rateH;
    private GLSurfaceView glSurfaceView;
    private GLRGBRenderer irRender;

    private PermissionListener listener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, List<String> grantedPermissions) {
            // 权限申请成功回调。

            // 这里的requestCode就是申请时设置的requestCode。
            // 和onActivityResult()的requestCode一样，用来区分多个不同的请求。
            if (requestCode == 200) {

            }
        }

        @Override
        public void onFailed(int requestCode, List<String> deniedPermissions) {
            // 权限申请失败回调。
            if (requestCode == 200) {
                checkPermission();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        init();
        openThread();
    }

    private void checkPermission() {
        AndPermission.with(this)
                .requestCode(200)
                .permission(Permission.STORAGE)
                .rationale(new RationaleListener() {
                    @Override
                    public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                        // 此对话框可以自定义，调用rationale.resume()就可以继续申请。
                        AndPermission.rationaleDialog(MainActivity.this, rationale).show();
                    }
                })
                .callback(listener)
                .start();
    }

    @Override
    protected void onDestroy() {
        closeThread();
        cameraUtil.close();
        super.onDestroy();
    }

    private void init() {
        findViewById(R.id.search).setOnClickListener(this);
        findViewById(R.id.open).setOnClickListener(this);
        findViewById(R.id.close).setOnClickListener(this);
        findViewById(R.id.shut).setOnClickListener(this);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        final View view = findViewById(R.id.btn_layout);
        glSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.setVisibility(view.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        ipAddr = (EditText) findViewById(R.id.ip);
        ipAddr.setText((String) SharedPreferencesUtil.getParam(this, "ip", "192.168.3.231"));
        devices = (Spinner) findViewById(R.id.devices);
        colorTable = (Spinner) findViewById(R.id.colorTable);
        if (null == cameraUtil) {
            cameraUtil = new CameraUtil();
        }
        devices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ipAddr.setText(position == 0 ? "192.168.1.34" : "192.168.3.231");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        colorTable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (null != cameraUtil)
                    cameraUtil.setColorName(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        glSurfaceView.setEGLContextClientVersion(2);
        irRender = new GLRGBRenderer(glSurfaceView);
        glSurfaceView.setRenderer(irRender);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void openThread() {
        isRuning = true;
        new Thread(new MyRunnable()).start();
    }

    private void closeThread() {
        isRuning = false;
    }

    private IR_ImageData getNext(boolean is8byte) {
        IR_ImageData ir_ImageData = new IR_ImageData();
        short[] raw = new short[cameraUtil.getDeviceInfo().raw_length];
        cameraUtil.nextFrame(raw);
        if (is8byte) {
            byte[] bmp = new byte[cameraUtil.getDeviceInfo().bmp_length];
            cameraUtil.img_14To8(raw, bmp);
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.length);
            ir_ImageData.setBitmap(bitmap);
        } else {
            short[] bmp = new short[cameraUtil.getDeviceInfo().raw_length];
            cameraUtil.img_14To565(raw, bmp);
            ir_ImageData.setBuffer(ShortBuffer.wrap(bmp));
        }
        ir_ImageData.setWidth(cameraUtil.getDeviceInfo().width);
        ir_ImageData.setHeight(cameraUtil.getDeviceInfo().height);
        ir_ImageData.setMax_x(cameraUtil.getMaxX());
        ir_ImageData.setMax_y(cameraUtil.getMaxY());
        ir_ImageData.setMin_x(cameraUtil.getMinX());
        ir_ImageData.setMin_y(cameraUtil.getMinY());
        ir_ImageData.setMax_temp(cameraUtil.getMaxTemp());
        ir_ImageData.setMin_temp(cameraUtil.getMinTemp());
        return ir_ImageData;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search:
                if (cameraUtil.search() < 0) {
                    Log.e("Main", "search is error");
                }
                break;
            case R.id.open:
                SharedPreferencesUtil.setParam(this, "ip", ipAddr.getText().toString());
                int pos = devices.getSelectedItemPosition();
                if (cameraUtil.getStatus() >= LeptonStatus.STATUS_OPENED) {
                    cameraUtil.close();
                }
                port = (pos == 0 ? 6666 : 50001);
                switch (pos) {
                    case 0:
                        cameraUtil.setDeviceInfo(new Device_w80_h60());
                        break;
                    case 1:
                        cameraUtil.setDeviceInfo(new Device_w324_h256());
                        break;
                    case 2:
                        cameraUtil.setDeviceInfo(new Device_w336_h256());
                        break;
                }
//                measureRate();
                cameraUtil.open(ipAddr.getText().toString(), port);
                cameraUtil.start();


                break;
            case R.id.close:
                if (null != cameraUtil) {
                    cameraUtil.close();
                }

                break;
            case R.id.shut:
                if (null != cameraUtil) {
                    cameraUtil.correct();
                }
                break;
        }
    }

//    class StartCallBack implements CameraSDK.StartCB {
//
//        @Override
//        public void callback(short[] raw, int width, int height, int length) {
//            Log.d("一帧来了", "一帧来了");
//        }
//    }

//    public void measureRate() {
//        if (null == cameraUtil || null == cameraUtil.getDeviceInfo()) {
//            return;
//        }
//        rateW = (float) viewWidth / cameraUtil.getDeviceInfo().width;
//        rateH = (float) viewHeight / cameraUtil.getDeviceInfo().height;
//    }

    private class MyRunnable implements Runnable {

        @Override
        public void run() {
            while (isRuning) {
                if (cameraUtil.getStatus() == LeptonStatus.STATUS_RUNNING) {
                    IR_ImageData ir_imageData = getNext(false);
                    irRender.update(ir_imageData);
                }
            }
        }
    }

}
