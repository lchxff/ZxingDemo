/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.lch.zxing.zxing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.example.lch.zxing.R;
import com.example.lch.zxing.util.PxUtil;
import com.example.lch.zxing.zxing.camera.CameraManager;
import com.google.zxing.ResultPoint;


import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    private final int triAngleLength = (int) PxUtil.dpToPx(getContext(), 20);                                    //每个角的点距离
    private final int triAngleWidth = (int) PxUtil.dpToPx(getContext(), 4);                                            //每个角的点宽度
    private final int textMarinTop = (int) PxUtil.dpToPx(getContext(), 30);
    private int lineOffsetCount = 0;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(PxUtil.dpToPx(getContext(), 14));
        frame.width();
        textPaint.setTextAlign(Paint.Align.CENTER);
        String text = "将二维码放入取景框内，即可自动扫描";
        canvas.drawText(text, frame.centerX(), frame.bottom + textMarinTop, textPaint);
        // 四个角落的三角
        Paint traAnglePaint = new Paint();
        traAnglePaint.setColor(Color.WHITE);
        traAnglePaint.setStrokeWidth(triAngleWidth);
        traAnglePaint.setStyle(Paint.Style.STROKE);

        Path leftTopPath = new Path();
        leftTopPath.moveTo(frame.left + triAngleLength, frame.top - triAngleWidth / 2);
        leftTopPath.lineTo(frame.left - triAngleWidth / 2, frame.top - triAngleWidth / 2);
        leftTopPath.lineTo(frame.left - triAngleWidth / 2, frame.top + triAngleLength);
        canvas.drawPath(leftTopPath, traAnglePaint);

        Path rightTopPath = new Path();
        rightTopPath.moveTo(frame.right - triAngleLength, frame.top - triAngleWidth / 2);
        rightTopPath.lineTo(frame.right + triAngleWidth / 2, frame.top - triAngleWidth / 2);
        rightTopPath.lineTo(frame.right + triAngleWidth / 2, frame.top + triAngleLength);
        canvas.drawPath(rightTopPath, traAnglePaint);

        Path leftBottomPath = new Path();
        leftBottomPath.moveTo(frame.left - triAngleWidth / 2, frame.bottom - triAngleLength);
        leftBottomPath.lineTo(frame.left - triAngleWidth / 2, frame.bottom + triAngleWidth / 2);
        leftBottomPath.lineTo(frame.left + triAngleLength, frame.bottom + triAngleWidth / 2);
        canvas.drawPath(leftBottomPath, traAnglePaint);

        Path rightBottomPath = new Path();
        rightBottomPath.moveTo(frame.right - triAngleLength, frame.bottom + triAngleWidth / 2);
        rightBottomPath.lineTo(frame.right + triAngleWidth / 2, frame.bottom + triAngleWidth / 2);
        rightBottomPath.lineTo(frame.right + triAngleWidth / 2, frame.bottom - triAngleLength);
        canvas.drawPath(rightBottomPath, traAnglePaint);


        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(Color.BLUE);
            //循环划线，从上到下
            if (lineOffsetCount > frame.bottom - frame.top - PxUtil.dpToPx(getContext(), 10)) {
                lineOffsetCount = 0;
            } else {
                lineOffsetCount = lineOffsetCount + 6;
//            canvas.drawLine(frame.left, frame.top + lineOffsetCount, frame.right, frame.top + lineOffsetCount, linePaint);    //画一条红色的线
                Rect lineRect = new Rect();
                lineRect.left = frame.left;
                lineRect.top = frame.top + lineOffsetCount;
                lineRect.right = frame.right;
                lineRect.bottom = (int) (frame.top + PxUtil.dpToPx(getContext(), 10) + lineOffsetCount);
                canvas.drawBitmap(((BitmapDrawable) (getResources().getDrawable(R.mipmap.scanline))).getBitmap(), null, lineRect, linePaint);
            }
            postInvalidateDelayed(10L, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
