/**
 * @author Julian Mueller
 */
package ntu.com.wholeskyimager;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    protected Button loadImage;
    protected Button startEdgeDetection;
    protected TextView mainLabel;
    protected ImageView inputImage;
    protected ImageView outputImage;
    protected Switch hdrSwitch;
    protected SeekBar evSeekbar;
    private ImageSurfaceView mImageSurfaceView;
    //Picture Callback Method
    PictureCallback pictureCallback = new PictureCallback() {
        @Override
        //action if picture is taken
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap == null) {
                Toast.makeText(MainActivity.this, "Captured image is empty", Toast.LENGTH_LONG).show();
                return;
            }
            //actual image file: pictureFile
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
            outputImage.setImageBitmap(scaleDownBitmapImage(bitmap, 400, 300));
            mImageSurfaceView.refreshCamera();
            //outputImage.setRotation(90);
        }
    };
    private Camera camera;
    private boolean hdrModeOn;
    private int exposureCompensationValue;
    private FrameLayout cameraPreviewLayout;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mClient;

    public static void dumpParameters(Camera.Parameters parameters) {
        String flattened = parameters.flatten();
        StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
        Log.d(TAG, "Dump all camera parameters:");
        while (tokenizer.hasMoreElements()) {
            Log.d(TAG, tokenizer.nextToken());
        }
    }

    //get picture data (no writing)
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "WSI");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                Log.d("WholeSkyImager", "failed to create directory");
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //Connect interface elements to properties
        loadImage = (Button) findViewById(R.id.buttonImport);
        startEdgeDetection = (Button) findViewById(R.id.buttonEdgeDetect);
        mainLabel = (TextView) findViewById(R.id.textView);
        //inputImage = (ImageView) findViewById(R.id.imageInput);
        outputImage = (ImageView) findViewById(R.id.imageOutput);
        hdrSwitch = (Switch) findViewById(R.id.switchHDR);
        evSeekbar = (SeekBar) findViewById(R.id.seekBarEV);

        hdrSwitch.setChecked(false);

        hdrSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("Switch toggle", "Switch is currently ON");
                } else {
                    Log.d("Switch toggle", "Switch is currently OFF");
                }
            }
        });

        /*
        evSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                exposureCompensationValue = progress;
                Log.e(TAG, "Seekbar changed to: " + String.valueOf(progress));
            }
        });
        */

        //from Tutorial
        camera = checkDeviceCamera();

        mImageSurfaceView = new ImageSurfaceView(MainActivity.this, camera);
        cameraPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        cameraPreviewLayout.addView(mImageSurfaceView);

        /*
        double horAngle =  getHVA();
        double vertAngle = getVVA();
        String horAngleS = String.valueOf(horAngle);
        String vertAngleS = String.valueOf(vertAngle);
        Log.e("Camera horAngle: ", horAngleS);
        Log.e("Camera vertAngle: ", vertAngleS);
        */

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(MainActivity.this, "SETTINGS",
                        Toast.LENGTH_SHORT).show();
                //Intent intent = new Intent(this, DisplaySettingsAcitvity.class);
                //startActivity(intent);
                return true;

            case R.id.action_about:
                Toast.makeText(MainActivity.this, "ABOUT",
                        Toast.LENGTH_SHORT).show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    //check if cam is available and use back facing camera
    private Camera checkDeviceCamera() {
        int cameraId = findBackFacingCamera();
        Camera mCamera = null;
        try {
            mCamera = Camera.open(cameraId);  //try to open camera

            Camera.Parameters params = mCamera.getParameters();

            //set highest resolution as output
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            Camera.Size size = sizes.get(0);
            //Camera.Size size1 = sizes.get(0);
            //find largest size
            for (int i = 0; i < sizes.size(); i++) {

                if (sizes.get(i).width > size.width)
                    size = sizes.get(i);
            }
            params.setPictureSize(size.width, size.height);

            //check if Focus mode infinity is available and set it
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            }
            params.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
            params.setRotation(90);
            params.setJpegQuality(100);
            // TODO: find Balance modes
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

            // save settings
            mCamera.setParameters(params);
            Log.e(this.getClass().getSimpleName(), "Camera opened successfully!");
        } catch (Exception e) {
            e.printStackTrace(); //show error if camera can't be accessed
            Log.e(this.getClass().getSimpleName(), "Could not start camera");
        }
        return mCamera;
    }

    /**
     * Find back facing camera
     * @return cameraId
     */
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

    /**
     * HVA Method
     * @return horizontalViewAngle
     */
    public double getHVA() {
        return camera.getParameters().getHorizontalViewAngle();
    }

    /**
     * VVA Method
     * @return verticalViewAngle
     */
    public double getVVA() {
        return camera.getParameters().getVerticalViewAngle();
    }

    private Bitmap scaleDownBitmapImage(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return resizedBitmap;
    }

    //button method
    public void startEdgeDetection(View view) {
        // Start OpenCV HDR Algo
        /* native code:

        Mat hdr;
        Ptr<MergeDebevec> merge_debevec = createMergeDebevec();
        merge_debevec->process(images, hdr, times, response);
        */
        Mat hdr;
//        MergeDebevec

        //mImageSurfaceView.refreshCamera();
        /*
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
        */
    }

    //button method (take picture)
    public void importImage(View view) throws InterruptedException {
        //do something
        Log.d("Button Pressed", "Image loading should be started!");
        //check the current state before we display the screen
        Camera.Parameters params = camera.getParameters();

        //max value: +12, step size: exposure-compensation-step=0.166667. EV: +2
        int maxExposureComp = params.getMaxExposureCompensation();
        int minExposureComp = params.getMinExposureCompensation();

        if (hdrSwitch.isChecked()) {
            Log.d(TAG, "Changed to HDR mode");

            //params.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
            params.set("mode", "m");
            params.set("iso", "ISO100");

            Log.d(TAG, "SeekBar Value: " + String.valueOf(evSeekbar.getProgress()));

            switch (evSeekbar.getProgress()) {
                case 0:
                    params.setExposureCompensation(minExposureComp);
                    break;
                case 1:
                    params.setExposureCompensation(0);
                    break;
                case 2:
                    params.setExposureCompensation(maxExposureComp);
            }
            camera.setParameters(params);
            Log.d(TAG, "set ExposureCompensation to: " + params.getExposureCompensation());
            camera.takePicture(null, null, pictureCallback);

        } else {
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            Log.d(TAG, "Changed to auto mode");
        }
        //camera.setParameters(params);
        //dumpParameters(params);
        //camera.takePicture(null, null, pictureCallback);
    }

    //camera part
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bp = (Bitmap) data.getExtras().get("data");
        outputImage.setImageBitmap(bp);
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

    //Activity Lifecycle Methods
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

    @Override
    protected void onPause() {
        super.onPause();
        //releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    protected void onResume() {
        super.onResume();
        //releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
