package org.pytorch.demo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import org.pytorch.demo.vision.ImageClassificationActivity;

import androidx.appcompat.app.AppCompatActivity;

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

    findViewById(R.id.btnShow).setOnClickListener(v3 -> startActivities(new Intent[]{new Intent(MainActivity.this,Introduce.class)}));
    findViewById(R.id.btnGetImage).setOnClickListener(v1 -> startActivities(new Intent[]{new Intent(MainActivity.this, PickCamera.class)}));
    findViewById(R.id.btnRealtime).setOnClickListener(v -> startActivities(new Intent[]{new Intent(MainActivity.this,ImageClassificationActivity.class)}));
    findViewById(R.id.btnCapture).setOnClickListener(v2 -> startActivities(new Intent[]{new Intent(MainActivity.this,TakePhoto.class)}));
  }
}