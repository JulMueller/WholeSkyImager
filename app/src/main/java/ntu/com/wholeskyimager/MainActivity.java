package ntu.com.wholeskyimager;

import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera; //camera
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ImageSurfaceView mImageSurfaceView;
    private Camera camera;

    protected Button loadImage;
    protected Button startEdgeDetection;
    protected TextView mainLabel;
    protected ImageView inputImage;
    protected ImageView outputImage;
    private FrameLayout cameraPreviewLayout;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);

        //Connect interface elements to properties
        loadImage = (Button) findViewById(R.id.buttonImport);
        startEdgeDetection = (Button) findViewById(R.id.buttonEdgeDetect);
        mainLabel = (TextView) findViewById(R.id.textView);
        //inputImage = (ImageView) findViewById(R.id.imageInput);
        outputImage = (ImageView) findViewById(R.id.imageOutput);
        //inputImage.setVisibility(View.VISIBLE);


        //from Tutorial
        camera = checkDeviceCamera();
        mImageSurfaceView = new ImageSurfaceView(MainActivity.this, camera);
        cameraPreviewLayout = (FrameLayout)findViewById(R.id.camera_preview);
        cameraPreviewLayout.addView(mImageSurfaceView);

        double horAngle =  getHVA();
        double vertAngle = getVVA();
        String horAngleS = String.valueOf(horAngle);
        String vertAngleS = String.valueOf(vertAngle);
        Log.e("Camera horAngle: ", horAngleS);
        Log.e("Camera vertAngle: ", vertAngleS);

        //cameraPreviewLayout.addView(mImageSurfaceView);

        //Check if OpenCV works properly
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }



    //check if cam is available
    private Camera checkDeviceCamera(){
        int cameraId = findBackFacingCamera();
        Camera mCamera = null;
        try {
            mCamera = Camera.open(cameraId);
            Log.e(this.getClass().getSimpleName(), "Camera opened successfully!");
            //String horAngleS = String.valueOf(horAngle);
            //Log.e("Camera horAngle: ", horAngleS);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().getSimpleName(), "Could not start camera");
        }
        return mCamera;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    //get FOV parameter
    public double getHVA() {
        return camera.getParameters().getHorizontalViewAngle();
    }
    public double getVVA() {
        return camera.getParameters().getVerticalViewAngle();
    }


    //Picture Callback Method
    PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if(bitmap==null){
                Toast.makeText(MainActivity.this, "Captured image is empty", Toast.LENGTH_LONG).show();
                return;
            }
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                //write the file
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast toast = Toast.makeText(MainActivity.this, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
                toast.show();

            } catch (FileNotFoundException e) {
            } catch (IOException e) {
             }
            outputImage.setImageBitmap(scaleDownBitmapImage(bitmap, 400, 300 ));
            outputImage.setRotation(90);
        }
    };

    private Bitmap scaleDownBitmapImage(Bitmap bitmap, int newWidth, int newHeight){
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return resizedBitmap;
    }

    //button method
    public void startEdgeDetection(View view) {
        //do something
        Log.d("Button Pressed", "Edge detection should be started!");

        //File organization
        String inputFileName = "NTU_1_GS";
        String inputExtension = "png";
        String inputDir = getCacheDir().getAbsolutePath();  // use the cache directory for i/o
        String outputDir = getCacheDir().getAbsolutePath();
        String outputExtension = "png";
        String inputFilePath = inputDir + File.separator + inputFileName + "." + inputExtension;

        Log.d(this.getClass().getSimpleName(), "loading " + inputFilePath + "...");
        Mat image = Imgcodecs.imread(inputFilePath);
        Log.d(this.getClass().getSimpleName(), "width of " + inputFileName + ": " + image.width());

        int threshold1 = 50;
        int threshold2 = 100;

        Mat im_canny = new Mat();
        Imgproc.Canny(image, im_canny, threshold1, threshold2);
        String cannyFilename = outputDir + File.separator + inputFileName + "_canny-" + threshold1 + "-" + threshold2 + "." + outputExtension;
        Log.d(this.getClass().getSimpleName(), "Writing " + cannyFilename);
        Imgcodecs.imwrite(cannyFilename, im_canny);
        //inputImage.setImageResource(R.drawable.my_image);
        Bitmap bitmapToDisplay = null;
        bitmapToDisplay = BitmapFactory.decodeFile(cannyFilename);
        outputImage.setImageBitmap(BitmapFactory.decodeFile(cannyFilename));
        //outputImage.getLayoutParams().width = inputImage.getWidth();
        Log.d("Statusupdate", "Edge detection finished");
    }

    //button method
    public void importImage(View view) {
        //do something
        Log.d("Button Pressed", "Image loading should be started!");
        //intent for camera
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //startActivityForResult(intent, 0);
        camera.takePicture(null, null, pictureCallback);
    }

    //camera part
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bp = (Bitmap) data.getExtras().get("data");
        outputImage.setImageBitmap(bp);
    }

    //write file method
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "WSI");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://www.wholesky.ch"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mClient.connect();
        AppIndex.AppIndexApi.start(mClient, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(mClient, getIndexApiAction());
        mClient.disconnect();
    }
}

/*
public class Showcamera extends SurfaceView implements SurfaceHolder.Callback {
    private Camera theCamera;

    public void surfaceCreated(SurfaceHolder holder) {
        theCamera.setPreviewDisplay(holder);
        theCamera.startPreview();
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3){
    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
    }
}
*/