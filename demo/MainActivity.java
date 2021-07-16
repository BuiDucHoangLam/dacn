package org.pytorch.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;
import org.pytorch.demo.vision.ImageClassificationActivity;
import org.pytorch.demo.vision.VisionListActivity;

import androidx.appcompat.app.AppCompatActivity;
import org.pytorch.demo.PickCamera;

public class MainActivity extends AppCompatActivity {

  private Button btnShow;
  private Button btnCapture;
  private Button btnRealtime;
  private Button btnGetImage;

  ImageView IVPreviewImage;

  // constant to compare
  // the activity result code
  int SELECT_PICTURE = 200;

  @Override
  protected void onCreate(Bundle savedInstanceState) {


    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

//    findViewById(R.id.btnShow).setOnClickListener(v3 -> startActivities(new Intent[]{new Intent(MainActivity.this,HelloWorld.class)}));
//    findViewById(R.id.btnGetImage).setOnClickListener(v1 -> startActivities(new Intent[]{new Intent(MainActivity.this, PickCamera.class)}));
//    findViewById(R.id.btnRealtime).setOnClickListener(v -> startActivities(new Intent[]{new Intent(MainActivity.this,ImageClassificationActivity.class)}));
//    findViewById(R.id.btnCapture).setOnClickListener(v2 -> startActivities(new Intent[]{new Intent(MainActivity.this,TakePhoto.class)}));
      findViewById(R.id.lineShow).setOnClickListener(v -> startActivities(new Intent[]{new Intent(MainActivity.this,ImageClassificationActivity.class)}));
  }
}