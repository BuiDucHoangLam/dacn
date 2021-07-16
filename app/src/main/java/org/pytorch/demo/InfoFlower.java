package org.pytorch.demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class InfoFlower extends AppCompatActivity {
    String[] flower;
    TextView nameFlower,introFlower,scienceFlower,descriptionFlower;
    ImageView imageFlower;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_flower);
        Intent intent = getIntent();
        flower = intent.getStringArrayExtra("flower");
        Log.i("flowerclass", flower+"" );
        nameFlower = findViewById(R.id.nameFlower);
        introFlower = findViewById(R.id.introFlower);
        scienceFlower = findViewById(R.id.scienceFlower);
        descriptionFlower = findViewById(R.id.descriptionFlower);
        imageFlower = findViewById(R.id.imageFlower);

        nameFlower.setText(flower[0]);
//        imageFlower.setImageDrawable(Drawable.createFromPath(flower[2]));
        int id1 = getResources().getIdentifier(flower[1].toLowerCase(),"drawable",getPackageName());
        Drawable drawable1= getResources().getDrawable(id1);
        imageFlower.setImageDrawable(drawable1);

        introFlower.setText(flower[2]);
        introFlower.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        scienceFlower.setText(flower[3]);
        descriptionFlower.setText(flower[4]);
        descriptionFlower.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
    }
}