package com.kbyai.idcardrecognition;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class FaceGraphicOverlay extends FaceView.Graphic {

    private final Bitmap bitmap;

    public FaceGraphicOverlay(FaceView overlay, Bitmap bitmap) {
        super(overlay);
        this.bitmap = bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
//        bitmap.recycle();
    }
}
