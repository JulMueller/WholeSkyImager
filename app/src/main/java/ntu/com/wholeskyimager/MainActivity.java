/**
 * @author Julian Mueller
 */
package ntu.com.wholeskyimager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.os.Build.VERSION_CODES.M;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getName(); //gets the name of the current class eg. "MyActivity".
    private static int wahrsisModelNr = 5;
    private boolean sPreviewing;
    protected Button loadImage;
    protected Button startEdgeDetection;
    protected TextView mainLabel, tvConnectionStatus, tvStatusInfo;
    protected ImageView inputImage;
    protected ImageView outputImage;
    SharedPreferences sharedPref;
    private ImageSurfaceView mImageSurfaceView;
    private Camera camera;

    WSIServerClient serverClient = new WSIServerClient(this, "https://www.visuo.adsc.com.sg/api/skypicture/");

    private boolean hdrModeOn;
    private int exposureCompensationValue;
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

        setupActionBar();

        outputImage = (ImageView) findViewById(R.id.imageOutput);
        tvConnectionStatus  = (TextView) findViewById(R.id.tvConnectionStatus);
        tvStatusInfo = (TextView) findViewById(R.id.tvStatusInfo);

        camera = checkDeviceCamera();

//        mImageSurfaceView = new ImageSurfaceView(MainActivity.this, camera);
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

        //Check if OpenCV works properly
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }
        // set preferences
        getWSISettings();
        checkNetworkStatus();
        tvStatusInfo.setText("idle");

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

    //get picture data (no writing)

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(MainActivity.this, "SETTINGS", Toast.LENGTH_SHORT).show();
                Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                return true;

            case R.id.action_refresh:
                getWSISettings();
                checkNetworkStatus();
                Toast.makeText(MainActivity.this, "Refreshed Settings", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_help:
                Toast.makeText(MainActivity.this, "HELP", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_about:
                Toast.makeText(MainActivity.this, "ABOUT", Toast.LENGTH_SHORT).show();
                Intent intentAbout = new Intent(this, DisplayAboutActivity.class);
                startActivity(intentAbout);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

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
            if(mImageSurfaceView.getPreviewState()) {
                mImageSurfaceView.refreshCamera();
            }
            //outputImage.setRotation(90);
        }
    };

    @Nullable //this denotes that the method might legitimately return null
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "WSI");

        //if folder could not be created
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                Log.d("WholeSkyImager", "failed to create directory");
                return null;
            }
        }
        //naming convention: 2016-11-22-14-20-01-wahrsis5.jpg
        //naming convention: YYYY-MM-DD-HH-MM-SS-wahrsisN.jpg
        //take the current timeStamp
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + timeStamp + "-wahrsis" + wahrsisModelNr + ".jpg");

        return mediaFile;
    }


    //check if cam is available and use back facing camera

    /**
     * Check avaiablity of camera
     *
     * @return Camera
     */
    @SuppressWarnings("deprecation")
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
            Log.d(this.getClass().getSimpleName(), "Camera started successfully.");
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
    @SuppressWarnings("deprecation")
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

    }

    /**
     * Button Method Upload Image
     */
    public void postData(View view) {
        Toast.makeText(this, "POST execution started.", Toast.LENGTH_LONG).show();
        //post image series (Low, Med, High EV) to specific URL and receive HTTP Status Code
        // TODO: replace name with global name
        int responseCode = serverClient.httpPOST("2016-11-22-14-20-01-wahrsis5");
        Log.d(TAG, "POST execution finished. Response code: " + responseCode);

        // for testing connection
//        Toast.makeText(this, "HTTP GET execution started.", Toast.LENGTH_LONG).show();
//        int responseCode = serverClient.httpGET();
//        Log.d(TAG, "GET execution finished. Response corde:" + responseCode);
    }

    /**
     * Button Method Take Picture
     */
    @SuppressWarnings("deprecation")
    public void importImage(View view) throws InterruptedException {
        //do something
        Log.d("Button Pressed", "Image loading should be started!");
        //check the current state before we display the screen
        Camera.Parameters params = camera.getParameters();

        //max value: +12, step size: exposure-compensation-step=0.166667. EV: +2
        int maxExposureComp = params.getMaxExposureCompensation();
        int minExposureComp = params.getMinExposureCompensation();

        if (sharedPref.getBoolean("createHDR", false)) {
            Log.d(TAG, "HDR mode active.");

            //params.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
            params.set("mode", "m");
            params.set("iso", "ISO100");

//            Log.d(TAG, "SeekBar Value: " + String.valueOf(evSeekbar.getProgress()));
//
//            switch (evSeekbar.getProgress()) {
//                case 0:
//                    params.setExposureCompensation(minExposureComp);
//                    break;
//                case 1:
//                    params.setExposureCompensation(0);
//                    break;
//                case 2:
//                    params.setExposureCompensation(maxExposureComp);
//            }
            camera.setParameters(params);
            Log.d(TAG, "set ExposureCompensation to: " + params.getExposureCompensation());
            camera.takePicture(null, null, pictureCallback);

        } else {
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            Log.d(TAG, "HDR mode inactive.");
            camera.takePicture(null, null, pictureCallback);
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
        mImageSurfaceView.refreshCamera();
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
        mImageSurfaceView.releaseCamera();
        super.onPause();
//        camera.stopPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.startPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setupActionBar() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_more_vert_white_24dp);
        myToolbar.setOverflowIcon(drawable);
        android.support.v7.app.ActionBar menu = getSupportActionBar();
        menu.setDisplayHomeAsUpEnabled(false);
        menu.setLogo(R.mipmap.ic_launcher);
        menu.setDisplayUseLogoEnabled(true);
        menu.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorActionBar)));
        menu.setTitle(Html.fromHtml("<font color='#ffffff'>Whole Sky Imager</font>"));

        if (Build.VERSION.SDK_INT > 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorStatusBar));
        }
    }

    public void checkNetworkStatus() {
        // check internet connection
        if (serverClient.isConnected()) {
            tvConnectionStatus.setText("online");
            tvConnectionStatus.setTextColor(getResources().getColor(R.color.darkGreen));
            Log.d(TAG, "Device is online.");
        } else {
            tvConnectionStatus.setText("offline");
            tvConnectionStatus.setTextColor(Color.BLACK);
            Log.d(TAG, "Device is offline.");
        }
    }
    /**
     * set up preferences
     */
    private void getWSISettings() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG, "Model Nr in pref xml: " + Integer.parseInt(sharedPref.getString("wahrsisNo", "0")));
        // Set wahrsis model number according to settings activity
        if (Integer.parseInt(sharedPref.getString("wahrsisNo", "0")) != 0) {
            wahrsisModelNr = Integer.parseInt(sharedPref.getString("wahrsisNo", "404"));
            Log.d(TAG, "Model Nr set to: " + wahrsisModelNr);
        }
    }
}
