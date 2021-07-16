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
    InputStream imageStream = null;

    // constant to compare
    // the activity result code
    int SELECT_PICTURE = 200;

    public void predict(){
        try {
            // creating bitmap from packaged into app android asset 'image.jpg',
            // app/src/main/assets/image.jpg
//                bitmap = BitmapFactory.decodeStream(getAssets().open("image.jpg"));
            bitmap = BitmapFactory.decodeStream(imageStream);
            Log.i("imageStream", imageStream+"");
            Log.i("bitmap", bitmap+"");

            // loading serialized torchscript module from packaged into app android asset model.pt,
            // app/src/model/assets/model.pt
            module = Module.load(assetFilePath(this, "model.pt"));
        } catch (IOException e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            finish();
        }

        // showing image on UI

        IVPreviewImage.setImageBitmap(bitmap);

        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB, MemoryFormat.CHANNELS_LAST);

        // running the model
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

        String className = ImageNet.IMAGENET_CLASSES[maxScoreIdx];

        // showing className on UI
        TextView textView = findViewById(R.id.rsText);
        textView.setText(className);
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

    // this function is triggered when user
    // selects the image from the imageChooser

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        String path = "";
        if (getContentResolver() != null) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        }
        return path;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if (resultCode == RESULT_OK) {
//            try {
//                final Uri imageUri = data.getData();
//
//                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
//                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
//                IVPreviewImage.setImageBitmap(selectedImage);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//                Toast.makeText(PickCamera.this, "Something went wrong", Toast.LENGTH_LONG).show();
//            }
//
//        }else {
//            Toast.makeText(PickCamera.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
//        }
        if (resultCode == RESULT_OK) {
            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                Log.i("selectedImageUri", selectedImageUri+"");
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Log.i("photo", photo+"");
                IVPreviewImage.setImageBitmap(photo);

                // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
                Uri tempUri = getImageUri(getApplicationContext(), photo);
                Log.i("tempUri", tempUri+"");
                // CALL THIS METHOD TO GET THE ACTUAL PATH
                File finalFile = new File(getRealPathFromURI(tempUri));
                Log.i("finalFile", finalFile+"");

                Log.i("try-selected", selectedImage+"");

//                try {
//                    imageStream = getContentResolver().openInputStream(selectedImageUri);
//                    Log.i("try-imageStream", imageStream+"");

//                    if (null != selectedImageUri) {
                        // update the preview image in the layout
//                    IVPreviewImage.setImageURI(selectedImageUri);
//                    }
//                }
//                catch (FileNotFoundException fe){
//                    fe.printStackTrace();
//                    Toast.makeText(PickCamera.this, "Something went wrong", Toast.LENGTH_LONG).show();
//                }
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
        Button btnPredict = findViewById(R.id.recognizeButton);

        imageChooser();
        // handle the Choose Image button to trigger
        // the image chooser function
        BSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageChooser();
            }
        });

        findViewById(R.id.recognizeButton).setOnClickListener(v -> {
//          new ImageClassificationActivity().predict(uri);
            predict();
        });
    }
}
