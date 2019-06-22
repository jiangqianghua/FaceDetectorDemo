package com.facedetector.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.View;

import com.facedetector.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 绘制脸部方框的view，实测发现返回的脸部数据中没有具体的眼睛，嘴巴等数据
 */
public class DrawFacesView_bk2 extends View {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    public interface OnDrawFacesViewListener{
        void onBitMap(Bitmap bitmap);
    }

    private OnDrawFacesViewListener onDrawFacesViewListener ;

    public void setOnDrawFacesViewListener(OnDrawFacesViewListener onDrawFacesViewListener) {
        this.onDrawFacesViewListener = onDrawFacesViewListener;
    }

    private Matrix matrix;
    private Paint paint;
    private Camera.Face[] faces;
    private boolean isClear;

    private Bitmap bitmap ;

    public DrawFacesView_bk2(Context context) {
        this(context, null);
    }

    public DrawFacesView_bk2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawFacesView_bk2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        faces = new Camera.Face[]{};
        bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.mipmap.d);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        //canvas.setMatrix(matrix);
        for (final Camera.Face face : faces) {
            if (face == null) break;
           // canvas.drawRect(face.rect, paint);
//            canvas.drawBitmap(bitmap,face.rect.left,face.rect.top,null);
//            if (face.leftEye != null)
//                canvas.drawPoint(face.leftEye.x, face.leftEye.y, paint);
//            if (face.rightEye != null)
//                canvas.drawPoint(face.rightEye.x, face.rightEye.y, paint);
//            if (face.mouth != null)
//                canvas.drawPoint(face.mouth.x, face.mouth.y, paint);

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth() , canvas.getHeight(),Bitmap.Config.ARGB_4444);
                    Canvas myCanvas = new Canvas(bitmap);
                    myCanvas.setMatrix(matrix);
                    myCanvas.drawRect(face.rect, paint);
                    if(onDrawFacesViewListener != null){
                        onDrawFacesViewListener.onBitMap(bitmap);
                    }
                }
            });

            // 因为旋转了画布矩阵，所以字体也跟着旋转
//            canvas.drawText(String.valueOf("id:" + face.id + "\n置信度:" + face.score), face.rect.left, face.rect.bottom + 10, paint);
        }
        if (isClear) {
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            isClear = false;
        }
    }

    /**
     * 绘制脸部方框
     *
     * @param matrix 旋转画布的矩阵
     * @param faces 脸部信息数组
     */
    public void updateFaces(Matrix matrix, Camera.Face[] faces) {
        this.matrix = matrix;
        this.faces = faces;
        invalidate();
    }

    /**
     * 清除已经画上去的框
     */
    public void removeRect() {
        isClear = true;
        invalidate();
    }
}
