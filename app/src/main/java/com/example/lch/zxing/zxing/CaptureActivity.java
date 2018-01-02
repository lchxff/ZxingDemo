package com.example.lch.zxing.zxing;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.lch.zxing.R;
import com.example.lch.zxing.zxing.camera.CameraManager;
import com.example.lch.zxing.zxing.decode.CaptureActivityHandler;
import com.google.zxing.Result;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    @BindView(R.id.preview_view)
    SurfaceView previewView;
    @BindView(R.id.viewfinder_view)
    ViewfinderView viewfinderView;
    @BindView(R.id.contents_text_view)
    TextView contentsTextView;
    @BindView(R.id.result_view)
    LinearLayout resultView;

    private CameraManager cameraManager;

    private CaptureActivityHandler handler;

    private boolean hasSurface;

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        ButterKnife.bind(this);
        hasSurface = false;
    }


    @Override
    protected void onResume() {
        super.onResume();

        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        resetStatusView();


        SurfaceHolder holder = previewView.getHolder();
        if (hasSurface) {
            initCamera(holder);
        } else {
            holder.addCallback(this);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    private void initCamera(SurfaceHolder holder) {
        if (holder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(holder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void handleDecode(Result rawResult) {
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);
        contentsTextView.setText(rawResult.getText());


    }


    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.VISIBLE);
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }


}
