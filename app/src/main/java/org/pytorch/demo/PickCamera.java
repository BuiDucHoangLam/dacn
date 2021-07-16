package org.pytorch.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.R;
import org.pytorch.torchvision.TensorImageUtils;
import org.pytorch.demo.vision.ImageClassificationActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PickCamera extends AppCompatActivity {

    // One Button
    Button BSelectImage;
    Uri uri;
    // One Preview Image
    ImageView IVPreviewImage;

    Bitmap bitmap = null;
    Module module = null;
    Bitmap selectedImage;
    // InputStream imageStream = null;

    // constant to compare
    // the activity result code
    int SELECT_PICTURE = 200;

    public void predict(InputStream imageStream){
        try {
            // creating bitmap from packaged into app android asset 'image.jpg',
            // app/src/main/assets/image.jpg
//                bitmap = BitmapFactory.decodeStream(getAssets().open("image.jpg"));
            bitmap = BitmapFactory.decodeStream(imageStream);
            Log.i("imageStream", imageStream+"");
            Log.i("bitmap", bitmap+"");

            // loading serialized torchscript module from packaged into app android asset model.pt,
            // app/src/model/assets/model.pt
            module = Module.load(assetFilePath(this, "model_21flowers_resnet18_2.pt"));
        } catch (IOException e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            finish();
        }

        // showing image on UI

        IVPreviewImage.setImageBitmap(bitmap);
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        // preparing input tensor
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resized,224,224,200,200,20.0f,20.0f);
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resized,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB, MemoryFormat.CHANNELS_LAST);
        // running the model
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resized,TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();

        // searching for the index with maximum score
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }

        String className = Constants.IMAGENET_LABEL[maxScoreIdx][0];
        Log.i("maxScoreTakePhoto", maxScore+"");

        // showing className on UI
        TextView textView = findViewById(R.id.rsText);
        if(maxScore<3.5){
            textView.setText("Không thể nhận diện");
        }
        else{
            textView.setText(className +"\nClick để xem thông tin");
//            textView.setOnClickListener(v ->
//                    startActivities(new Intent[]{new Intent(PickCamera.this,Introduce.class)}));
            Log.i("constant", Constants.IMAGENET_LABEL[maxScoreIdx]+"");
            Intent intent = new Intent(PickCamera.this,InfoFlower.class);
            intent.putExtra("flower",Constants.IMAGENET_LABEL[maxScoreIdx]);
            textView.setOnClickListener(v-> startActivity(intent));
        }
    }

    // this function is triggered when
    // the Select Image Button is clicked
    void imageChooser() {

        // create an instance of the
        // intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        // pass the constant to compare it
        // with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                try {


                    Uri selectedImageUri = data.getData();
                    InputStream  inputStream = getContentResolver().openInputStream(selectedImageUri);
                    if (null != selectedImageUri) {
                        // update the preview image in the layout
                        IVPreviewImage.setImageURI(selectedImageUri);
                        predict(inputStream);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // Get the url of the image from data

            }
        }
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_camera);

        // register the UI widgets with their appropriate IDs
        BSelectImage = findViewById(R.id.BSelectImage);
        IVPreviewImage = findViewById(R.id.IVPreviewImage);

        imageChooser();

        // handle the Choose Image button to trigger
        // the image chooser function
        BSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageChooser();
            }
        });

    }

}
