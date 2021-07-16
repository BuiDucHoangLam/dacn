package org.pytorch.demo.vision;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.Constants;
import org.pytorch.demo.InfoFlower;
import org.pytorch.demo.PickCamera;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.demo.vision.view.ResultRowView;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

public class ImageClassificationActivity<textView> extends AbstractCameraXActivity<ImageClassificationActivity.AnalysisResult> {

  public static final String INTENT_MODULE_ASSET_NAME = "INTENT_MODULE_ASSET_NAME";
  public static final String INTENT_INFO_VIEW_TYPE = "INTENT_INFO_VIEW_TYPE";

  private static final int INPUT_TENSOR_WIDTH = 224;
  private static final int INPUT_TENSOR_HEIGHT = 224;
  private static final int TOP_K = 1;
  private static final int MOVING_AVG_PERIOD = 10;
  private static final String FORMAT_MS = "%dms";
  private static final String FORMAT_AVG_MS = "avg:%.0fms";
  public static int a;
  private static final String FORMAT_FPS = "%.1fFPS";
  public static final String SCORES_FORMAT = "%.2f";
  public static int percent;
  Context context;
  String[] extra;
  public ImageClassificationActivity() {
  }

  static class AnalysisResult {

    private final String[] topNClassNames;
    private final float[] topNScores;
    private final long analysisDuration;
    private final long moduleForwardDuration;

    public AnalysisResult(String[] topNClassNames, float[] topNScores,
                          long moduleForwardDuration, long analysisDuration) {
      this.topNClassNames = topNClassNames;
      this.topNScores = topNScores;
      this.moduleForwardDuration = moduleForwardDuration;
      this.analysisDuration = analysisDuration;
    }
  }

  private boolean mAnalyzeImageErrorState;
  private ResultRowView[] mResultRowViews = new ResultRowView[TOP_K];
  private TextView mFpsText;
  private TextView mMsText;
  private TextView mMsAvgText;
  private Module mModule;
  private String mModuleAssetName;
  private FloatBuffer mInputTensorBuffer;
  private Tensor mInputTensor;
  private long mMovingAvgSum = 0;
  private Queue<Long> mMovingAvgQueue = new LinkedList<>();

  @Override
  protected int getContentViewLayoutId() {
    return R.layout.activity_image_classification;
  }

  @Override
  protected TextureView getCameraPreviewTextureView() {
    return ((ViewStub) findViewById(R.id.image_classification_texture_view_stub))
        .inflate()
        .findViewById(R.id.image_classification_texture_view);
  }
  public float[] softMax(float[] arr) {
    int length = arr.length;
//    Arrays.sort(arr);
    float max = arr[length - 1];
    float[] exp_a = new float[arr.length];
    for(int i = 0; i < length; i++) {
      exp_a[i] = (float) Math.pow(Math.E, arr[i] - max);
    }
    float sum = 0;
    for(int i = 0; i < length; i++) {
      sum += exp_a[i];
    }
    float[] result = new float[length];
    for(int i = 0; i < length; i++) {
      result[i] = exp_a[i] / sum;
    }
    return result;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    final ResultRowView headerResultRowView =
//        findViewById(R.id.image_classification_result_header_row);
//    headerResultRowView.nameTextView.setText(R.string.image_classification_results_header_row_name);

//    headerResultRowView.scoreTextView.setText(R.string.image_classification_results_header_row_score);


    mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row);


//    mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row);
//    mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row);

//    mFpsText = findViewById(R.id.image_classification_fps_text);
//    mMsText = findViewById(R.id.image_classification_ms_text);
//    mMsAvgText = findViewById(R.id.image_classification_ms_avg_text);

  }

  @Override
  protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
    mMovingAvgSum += result.moduleForwardDuration;
    mMovingAvgQueue.add(result.moduleForwardDuration);
    if (mMovingAvgQueue.size() > MOVING_AVG_PERIOD) {
      mMovingAvgSum -= mMovingAvgQueue.remove();
    }

    Log.i("RS", String.valueOf(result.topNClassNames.length));

    for (int i = 0; i < TOP_K; i++) {
      final ResultRowView rowView = mResultRowViews[i];
      rowView.nameTextView.setText(result.topNClassNames[i]);
//      rowView.scoreTextView.setText(String.format(Locale.US, SCORES_FORMAT,
//          result.topNScores[i]));
      Log.i("topscore", result.topNScores[i]+"");

      if(extra != null && result.topNScores[i] > 0.7) {
        Intent intent = new Intent(ImageClassificationActivity.this, InfoFlower.class);
        intent.putExtra("flower",extra);
        Log.i("doan hoa", extra+"");
        int finalI = i;
        rowView.setOnClickListener(v -> {startActivity(intent);result.topNScores[finalI] = 0;});
      }
      rowView.setProgressState(false);
    }

//    mMsText.setText(String.format(Locale.US, FORMAT_MS, result.moduleForwardDuration));
//    if (mMsText.getVisibility() != View.VISIBLE) {
//      mMsText.setVisibility(View.VISIBLE);
//    }
//    mFpsText.setText(String.format(Locale.US, FORMAT_FPS, (1000.f / result.analysisDuration)));
//    if (mFpsText.getVisibility() != View.VISIBLE) {
//      mFpsText.setVisibility(View.VISIBLE);
//    }
//
//    if (mMovingAvgQueue.size() == MOVING_AVG_PERIOD) {
//      float avgMs = (float) mMovingAvgSum / MOVING_AVG_PERIOD;
//      mMsAvgText.setText(String.format(Locale.US, FORMAT_AVG_MS, avgMs));
//      if (mMsAvgText.getVisibility() != View.VISIBLE) {
//        mMsAvgText.setVisibility(View.VISIBLE);
//      }
//    }
  }

  protected String getModuleAssetName() {
    if (!TextUtils.isEmpty(mModuleAssetName)) {
      return mModuleAssetName;
    }
    final String moduleAssetNameFromIntent = getIntent().getStringExtra(INTENT_MODULE_ASSET_NAME);
    mModuleAssetName = !TextUtils.isEmpty(moduleAssetNameFromIntent)
        ? moduleAssetNameFromIntent
        : "model_21flowers_resnet18_2.pt";

    return mModuleAssetName;
  }

//  public double softmax(double input, float[] neuronValues) {
//    double total = 0;
//    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//      total = Arrays.stream(neuronValues).map(Math::exp).sum();
//    }
//
//    return Math.exp(input) / total;
//  }

  @Override
  protected String getInfoViewAdditionalText() {
    return getModuleAssetName();
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  @Override
  @WorkerThread
  @Nullable
  protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {


    if (mAnalyzeImageErrorState) {
      return null;
    }

    try {
      if (mModule == null) {
        final String moduleFileAbsoluteFilePath = new File(
            Utils.assetFilePath(this, getModuleAssetName())).getAbsolutePath();
        mModule = Module.load(moduleFileAbsoluteFilePath);

        mInputTensorBuffer =
            Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT);
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer,
                new long[]{1, 3, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH});
      }

      final long startTime = SystemClock.elapsedRealtime();
      TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
          image.getImage(), rotationDegrees,
          INPUT_TENSOR_WIDTH, INPUT_TENSOR_HEIGHT,
          TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
          TensorImageUtils.TORCHVISION_NORM_STD_RGB,
          mInputTensorBuffer, 0);

      final long moduleForwardStartTime = SystemClock.elapsedRealtime();
      final Tensor outputTensor =
              mModule.forward(IValue.from(mInputTensor)).toTensor();
      final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

      float[] scores = outputTensor.getDataAsFloatArray();
      scores = softMax(outputTensor.getDataAsFloatArray());
      Log.i("scores", scores+"");

      final int[] ixs = Utils.topK(scores, TOP_K);

      Log.i("IXS", ixs[0]+"");

      final String[] topKClassNames = new String[TOP_K];
      final float[] topKScores = new float[TOP_K];

      for (int i = 0; i < TOP_K; i++) {
        final int ix = ixs[i];
        a = ixs[i];
        topKScores[i] = scores[ix];
        Log.i("DiemHOA", topKScores[i]+"");

        if(topKScores[0] < 0.7){
          topKClassNames[0] = "Chưa thể nhận diện";
        }
        else if(topKScores[0] >= .7){
//          topKClassNames[i] = Constants.IMAGENET_LABEL[ix] + " (" + percent+")%" ;
          topKClassNames[i] = Constants.IMAGENET_LABEL[ix][0] + " - " + String.format("%.4g%n",topKScores[0]*100)+"%";
          Log.i("TAG", Constants.IMAGENET_LABEL[ix]+"");
          extra = Constants.IMAGENET_LABEL[ix];
          Log.i("haiz", extra+"");

          // close this activity
//          finish();
//          textView.setOnClickListener(v ->
//                    startActivities(new Intent[]{new Intent(ImageClassificationActivity.this,InfoFlower.class)}));


//          double a = softmax(topKScores[i],topKScores);
//          Log.i("total score", a+"");
        }
//        else if(topKScores[i]>=averageScore){
//          Log.i("IX", ix+"");        // topKClassNames[i] = Constants.IMAGENET_CLASSES[ix];
//
//          topKClassNames[i] = Constants.IMAGENET_LABEL[ix][0] + "(" + percent+")%";
//
//          Log.i("HOA", topKClassNames[i]+"");
////        topKScores[i] = scores[ix];
//        }
//        final int ix = ixs[i];
//        Log.i("IX", ix+"");
//        // topKClassNames[i] = Constants.IMAGENET_CLASSES[ix];
//        topKClassNames[i] = Constants.IMAGENET_LABEL[ix];
//        Log.i("HOA", topKClassNames[i]+"");
//        topKScores[i] = scores[ix];
//        Log.i("DiemHOA", topKScores[i]+"");
      }

      final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
      Log.i("time analyst", analysisDuration+"");
      return new AnalysisResult(topKClassNames, topKScores, moduleForwardDuration, analysisDuration);
    } catch (Exception e) {
      Log.e(Constants.TAG, "Error during image analysis", e);
      mAnalyzeImageErrorState = true;
//      runOnUiThread(() -> {
//        if (!isFinishing()) {
//          showErrorDialog(v -> ImageClassificationActivity.this.finish());
//        }
//      });
      return null;
    }
  }

  @Override
  protected int getInfoViewCode() {
    return getIntent().getIntExtra(INTENT_INFO_VIEW_TYPE, -1);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mModule != null) {
      mModule.destroy();
    }
  }

}


