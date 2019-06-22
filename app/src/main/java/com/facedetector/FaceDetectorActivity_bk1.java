package com.facedetector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facedetector.customview.DrawFacesView;

import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageAlphaBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageEmbossFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.Rotation;

/**
 * <pre>
 *      author : zouweilin
 *      e-mail : zwl9517@hotmail.com
 *      time   : 2017/07/17
 *      version:
 *      desc   : 由于使用的是camera1，在P以上的版本可能无法使用
 * </pre>
 */
public class FaceDetectorActivity_bk1 extends AppCompatActivity {

    private static final String TAG = FaceDetectorActivity_bk1.class.getSimpleName();
    private static final int REQUEST_CAMERA_CODE = 0x100;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private DrawFacesView facesView;
    private SurfaceTexture surfacetexture;

    private GPUImageAlphaBlendFilter gpuImageAlphaBlendFilter = new GPUImageAlphaBlendFilter() ;
    private GPUImageView gpuImageView ;
    private GPUImageFilter gpuImageFilter ;

    public static void start(Context context) {
        Intent intent = new Intent(context, FaceDetectorActivity_bk1.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_face);
        initViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
                }
                return;
            }
            setUpCamera();

        }
        startOrientationListener();
    }

    private void setUpCamera(){
        try {
            surfacetexture = new SurfaceTexture(10);
            int frontIndex = 0;
            int cameraCount = Camera.getNumberOfCameras();
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
                Camera.getCameraInfo(cameraIndex, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    frontIndex = cameraIndex;
                }
            }

            if (mCamera == null) {
                mCamera = Camera.open(frontIndex);
                mCamera.setPreviewTexture(surfacetexture);
                mCamera.setFaceDetectionListener(new FaceDetectorListener());
                startFaceDetection();
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        Log.d(TAG, "onPreviewFrame" + bytes.length + "");
                        gpuImageView.updatePreviewFrame(bytes,640,480);
                    }
                });
                setCameraParms(mCamera, 480, 640);
                mCamera.startPreview();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private int getCameraOrientation() {
        int degrees = 0 ;
        switch (this.getWindowManager().getDefaultDisplay().getRotation()){
            case Surface
                    .ROTATION_0:
                degrees = 0 ;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return (90 + degrees) % 360;
    }

    private Rotation getRotation(int orientation) {
        if(orientation == 90){
            return  Rotation.ROTATION_90;
        } else if(orientation == 180){
            return Rotation.ROTATION_180;
        } else if(orientation == 270){
            return Rotation.ROTATION_270;
        } else {
            return Rotation.NORMAL ;
        }
    }


    private void initViews() {
        gpuImageView = findViewById(R.id.gpuimage);
        gpuImageView.setRotation(getRotation(getCameraOrientation()));
        gpuImageView.setRenderMode(GPUImageView.RENDERMODE_CONTINUOUSLY);
        setFilter();
        facesView = new DrawFacesView(this);
        addContentView(facesView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void setFilter(){
        gpuImageFilter = new GPUImageEmbossFilter();
        gpuImageView.setFilter(gpuImageFilter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                finish();
            }
        }
    }

    /**
     * 脸部检测接口
     */
    private class FaceDetectorListener implements Camera.FaceDetectionListener {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Camera.Face face = faces[0];
                Rect rect = face.rect;
                Log.d("FaceDetection", "confidence：" + face.score + "face detected: " + faces.length +
                        " Face 1 Location X: " + rect.centerX() +
                        "Y: " + rect.centerY() + "   " + rect.left + " " + rect.top + " " + rect.right + " " + rect.bottom);
                Matrix matrix = updateFaceRect();
                facesView.updateFaces(matrix, faces);
            } else {
                // 只会执行一次
                Log.e("tag", "【FaceDetectorListener】类的方法：【onFaceDetection】: " + "没有脸部");
                facesView.removeRect();
            }
        }
    }

    /**
     * 因为对摄像头进行了旋转，所以同时也旋转画板矩阵
     * 详细请查看{@link Camera.Face#rect}
     * @return
     */
    private Matrix updateFaceRect() {
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        // Need mirror for front camera.
        boolean mirror = true ;//(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(90);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
//        matrix.postScale(surfaceView.getWidth() / 2000f, surfaceView.getHeight() / 2000f);
//        matrix.postTranslate(surfaceView.getWidth() / 2f, surfaceView.getHeight() / 2f);
        matrix.postScale(480 / 2000f, 640 / 2000f);
        matrix.postTranslate(480 / 2f, 640 / 2f);
        return matrix;
    }

    public void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();
        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // mCamera supports face detection, so can start it:
            try {
                mCamera.startFaceDetection();
            } catch (Exception e) {
                e.printStackTrace();
                // Invoked this method throw exception on some devices but also can detect.
            }
        } else {
            Toast.makeText(this, "Device not support face detection", Toast.LENGTH_SHORT).show();
        }
    }

    private void startOrientationListener() {
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                //计算手机当前方向的角度值
//                int phoneDegree = 0;
//                if (((orientation >= 0) && (orientation <= 45)) || (orientation > 315) && (orientation <= 360)) {
//                    phoneDegree = 0;
//                } else if ((orientation > 45) && (orientation <= 135)) {
//                    phoneDegree = 90;
//                } else if ((orientation > 135) && (orientation <= 225)) {
//                    phoneDegree = 180;
//                } else if ((orientation > 225) && (orientation <= 315)) {
//                    phoneDegree = 270;
//                }
//                //分别计算前后置摄像头需要旋转的角度
//                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//                Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
//                int mOrientation = (cameraInfo.orientation - phoneDegree + 360) % 360;
//                if(mCamera != null){
//                    mCamera.setDisplayOrientation(mOrientation);
//                }
                if(gpuImageView != null){
                    gpuImageView.setRotation(getRotation(getCameraOrientation()));
                }
            }
        };
       // orientationEventListener.enable();
    }


        /**
         * 在摄像头启动前设置参数
         *
         * @param camera
         * @param width
         * @param height
         */
    private void setCameraParms(Camera camera, int width, int height) {
        // 获取摄像头支持的pictureSize列表
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        // 从列表中选择合适的分辨率
        Camera.Size pictureSize = getProperSize(pictureSizeList, (float) height / width);
        if (null == pictureSize) {
            pictureSize = parameters.getPictureSize();
        }
        // 根据选出的PictureSize重新设置SurfaceView大小
        float w = pictureSize.width;
        float h = pictureSize.height;
        parameters.setPictureSize(pictureSize.width, pictureSize.height);


        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = getProperSize(previewSizeList, (float) height / width);
//        if (null != preSize) {
//            parameters.setPreviewSize(preSize.width, preSize.height);
//        }
        parameters.setPreviewSize(640, 480);
        parameters.setJpegQuality(100);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // 连续对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.cancelAutoFocus();
//        camera.setDisplayOrientation(270);
        camera.setParameters(parameters);
    }

    private Camera.Size getProperSize(List<Camera.Size> pictureSizes, float screenRatio) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizes) {
            float currenRatio = ((float) size.width) / size.height;
            if (currenRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }
        if (null == result) {
            for (Camera.Size size : pictureSizes) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {
                    result = size;
                    break;
                }
            }
        }
        return result;
    }

}
