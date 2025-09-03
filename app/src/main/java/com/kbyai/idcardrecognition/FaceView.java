package com.kbyai.idcardrecognition;


import static io.fotoapparat.util.CameraUtilsKt.projectImage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.fotoapparat.parameter.Resolution;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.util.CameraUtils;

public class FaceView extends View {

    private final Object lock = new Object();

    private final List<Graphic> graphics = new ArrayList<>();

    private Paint realPaint;
    private Paint spoofPaint;

    private Size frameSize = new Size(0, 0);

    private Rect position;
    private ScaleType scaleType;

    private String documentName;

    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    private float scaleFactor = 1.0f;
    private boolean isImageFlipped;
    private boolean needUpdateTransformation = true;
    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private float postScaleWidthOffset;
    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private float postScaleHeightOffset;
    // Matrix for transforming from image coordinates to overlay view coordinates.
    private final Matrix transformationMatrix = new Matrix();

    public FaceView(Context context) {
        this(context, null);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        realPaint = new Paint();
        realPaint.setStyle(Paint.Style.STROKE);
        realPaint.setStrokeWidth(3);
        realPaint.setColor(Color.GREEN);
        realPaint.setAntiAlias(true);
        realPaint.setTextSize(50);

        spoofPaint = new Paint();
        spoofPaint.setStyle(Paint.Style.STROKE);
        spoofPaint.setStrokeWidth(3);
        spoofPaint.setColor(Color.RED);
        spoofPaint.setAntiAlias(true);
        spoofPaint.setTextSize(50);

        scaleType = ScaleType.CenterCrop;
    }

    public void setScaleType(ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public int getImageWidth() {
        return frameSize.getWidth();
    }

    public int getImageHeight() {
        return frameSize.getHeight();
    }

    public void setDocumentInfos(Rect position, String documentName)
    {
        this.position = position;
        this.documentName = documentName;
        invalidate();
    }

    /** Removes all graphics from the overlay. */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    /** Adds a graphic to the overlay. */
    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    /** Removes a graphic from the overlay. */
    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    private void updateTransformationIfNeeded() {
        if (!needUpdateTransformation || frameSize.getWidth() <= 0 || frameSize.getHeight() <= 0) {
            return;
        }
        float viewAspectRatio = (float) getWidth() / getHeight();
        float imageAspectRatio = (float) frameSize.getWidth() / frameSize.getHeight();
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (float) getWidth() / frameSize.getWidth();
            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (float) getHeight() / frameSize.getHeight();
            postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
        }

        transformationMatrix.reset();
        transformationMatrix.setScale(scaleFactor, scaleFactor);
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset);

        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
        }

        needUpdateTransformation = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {

//            updateTransformationIfNeeded();

            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }


            if (position != null && !position.isEmpty()) {


                Rect rect = projectImage(scaleType,
                        canvas.getWidth(), canvas.getHeight(),
                        position.width(), position.height()

                );

                float cvw = (float) canvas.getWidth();
                float cvh = (float) canvas.getHeight();

                float x_scale = this.frameSize.getWidth() / cvw;
                float y_scale = this.frameSize.getHeight() / cvh;

                realPaint.setStrokeWidth(3);
                realPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                //            canvas.drawText(documentName, (position.left / x_scale) + 10, (position.top / y_scale) - 30, realPaint);
             //   canvas.drawText(documentName, (rect.left) + 10, (rect.top) - 30, realPaint);

                realPaint.setStyle(Paint.Style.STROKE);
                realPaint.setStrokeWidth(5);

                //            canvas.drawRect(new Rect((int)(position.left / x_scale), (int)(position.top / y_scale),
                //                    (int)(position.right / x_scale), (int)(position.bottom / y_scale)), realPaint);
                //            canvas.drawRect(rect, realPaint);
                canvas.drawRect(position, realPaint);
            }
        }
    }

    public abstract static class Graphic {
        private FaceView overlay;

        public Graphic(FaceView overlay) {
            this.overlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas. Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         *
         * <ol>
         *   <li>{@link Graphic#scale(float)} adjusts the size of the supplied value from the image
         *       scale to the view scale.
         *   <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         *       coordinate from the image's coordinate system to the view coordinate system.
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /** Adjusts the supplied value from the image scale to the view scale. */
        public float scale(float imagePixel) {
            return imagePixel * overlay.scaleFactor;
        }

        /** Returns the application context of the app. */
        public Context getApplicationContext() {
            return overlay.getContext().getApplicationContext();
        }

        public boolean isImageFlipped() {
            return overlay.isImageFlipped;
        }

        /**
         * Adjusts the x coordinate from the image's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (overlay.isImageFlipped) {
                return overlay.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
            } else {
                return scale(x) - overlay.postScaleWidthOffset;
            }
        }

        /**
         * Adjusts the y coordinate from the image's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scale(y) - overlay.postScaleHeightOffset;
        }

        /**
         * Returns a {@link Matrix} for transforming from image coordinates to overlay view
         * coordinates.
         */
        public Matrix getTransformationMatrix() {
            return overlay.transformationMatrix;
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }

        public int getImageWidth() {
            return overlay.getImageWidth();
        }

        public int getImageHeight() {
            return overlay.getImageHeight();
        }
    }


}
