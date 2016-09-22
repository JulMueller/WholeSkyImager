package ntu.com.wholeskyimager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnClickListener;

import java.io.File;

public class MainActivity extends AppCompatActivity  {

    protected Button loadImage;
    protected Button startEdgeDetection;
    protected TextView mainLabel;
    protected ImageView inputImage;
    protected ImageView outputImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect interface elements to properties
        loadImage = (Button) findViewById(R.id.buttonImport);
        startEdgeDetection = (Button) findViewById(R.id.buttonEdgeDetect);
        mainLabel = (TextView) findViewById(R.id.textView);
        inputImage = (ImageView) findViewById(R.id.imageInput);
        outputImage = (ImageView) findViewById(R.id.imageOutput);


        //Check if OpenCV works properly
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

    }

    public void startEdgeDetection(View view) {
        //do something
        Log.d("Button Pressed", "Edge detection should be started!");

        //File organization
        String inputFileName="NTU_1_GS";
        String inputExtension = "png";
        String inputDir = getCacheDir().getAbsolutePath();  // use the cache directory for i/o
        String outputDir = getCacheDir().getAbsolutePath();
        String outputExtension = "png";
        String inputFilePath = inputDir + File.separator + inputFileName + "." + inputExtension;

        Log.d (this.getClass().getSimpleName(), "loading " + inputFilePath + "...");
        Mat image = Imgcodecs.imread(inputFilePath);
        Log.d (this.getClass().getSimpleName(), "width of " + inputFileName + ": " + image.width());
        // if width is 0 then it did not read your image.

        int threshold1 = 50;
        int threshold2 = 100;

        Mat im_canny = new Mat();  // you have to initialize output image before giving it to the Canny method
        Imgproc.Canny(image, im_canny, threshold1, threshold2);
        String cannyFilename = outputDir + File.separator + inputFileName + "_canny-" + threshold1 + "-" + threshold2 + "." + outputExtension;
        Log.d (this.getClass().getSimpleName(), "Writing " + cannyFilename);
        Imgcodecs.imwrite(cannyFilename, im_canny);
        //inputImage.setImageResource(R.drawable.my_image);
        Bitmap bitmapToDisplay = null;
        bitmapToDisplay = BitmapFactory.decodeFile(cannyFilename);
        outputImage.setImageBitmap(BitmapFactory.decodeFile(cannyFilename));
        outputImage.getLayoutParams().width = inputImage.getWidth();
        Log.d("Statusupdate", "Edge detection finished");
    }
    public void importImage(View view) {
        //do something
        Log.d("Button Pressed", "Image loading should be started!");
    }
}
