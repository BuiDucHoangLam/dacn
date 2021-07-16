package org.pytorch.demo.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.Constants;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.demo.vision.view.ResultRowView;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

public class ImageClassificationActivity extends AbstractCameraXActivity<ImageClassificationActivity.AnalysisResult> {

  public static final String INTENT_MODULE_ASSET_NAME = "INTENT_MODULE_ASSET_NAME";
  public static final String INTENT_INFO_VIEW_TYPE = "INTENT_INFO_VIEW_TYPE";

  private static final int INPUT_TENSOR_WIDTH = 224;
  private static final int INPUT_TENSOR_HEIGHT = 224;
  private static final int TOP_K = 3;
  private static final int MOVING_AVG_PERIOD = 10;
  private static final String FORMAT_MS = "%dms";
  private static final String FORMAT_AVG_MS = "avg:%.0fms";

  private static final String FORMAT_FPS = "%.1fFPS";
  public static final String SCORES_FORMAT = "%.2f";
  Context context;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final ResultRowView headerResultRowView =
        findViewById(R.id.image_classification_result_header_row);
    headerResultRowView.nameTextView.setText(R.string.image_classification_results_header_row_name);
    headerResultRowView.scoreTextView.setText(R.string.image_classification_results_header_row_score);

    mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row);
    mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row);
    mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row);

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
      rowView.scoreTextView.setText(String.format(Locale.US, SCORES_FORMAT,
          result.topNScores[i]));

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
        : "model_ten_flowers.pt";

    return mModuleAssetName;
  }

  @Override
  protected String getInfoViewAdditionalText() {
    return getModuleAssetName();
  }

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
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1, 3, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH});
      }

      final long startTime = SystemClock.elapsedRealtime();
      TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
          image.getImage(), rotationDegrees,
          INPUT_TENSOR_WIDTH, INPUT_TENSOR_HEIGHT,
          TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
          TensorImageUtils.TORCHVISION_NORM_STD_RGB,
          mInputTensorBuffer, 0);

      final long moduleForwardStartTime = SystemClock.elapsedRealtime();
      final Tensor outputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
      final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

      final float[] scores = outputTensor.getDataAsFloatArray();
      final int[] ixs = Utils.topK(scores, TOP_K);

      Log.i("IXS", ixs[0]+"");

      final String[] topKClassNames = new String[TOP_K];
      final float[] topKScores = new float[TOP_K];

      float[] scoresPer;
      for (int i = 0; i < scores.length; i++) {

      }

      for (int i = 0; i < TOP_K; i++) {
        final int ix = ixs[i];
        Log.i("IX", ix+"");
        // topKClassNames[i] = Constants.IMAGENET_CLASSES[ix];
        topKClassNames[i] = Constants.IMAGENET_LABEL[ix];
        Log.i("HOA", topKClassNames[i]+"");
        topKScores[i] = scores[ix];
        Log.i("DiemHOA", topKScores[i]+"");
      }
      final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
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

   public AnalysisResult predict( Uri uri){
//    Bitmap bitmap = null;
    Module module = null;

    if (mAnalyzeImageErrorState) {
      return null;
    }

    try {
      if (mModule == null) {
//        final String moduleFileAbsoluteFilePath = new File(
//                Utils.assetFilePath(this, getModuleAssetName())).getAbsolutePath();
//        mModule = Module.load(moduleFileAbsoluteFilePath);
        mModule = Module.load("/data/user/0/org.pytorch.demo/files/model_ten_flowers.pt");


        mInputTensorBuffer =
                Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT);
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1, 3, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH});
      }

      InputStream inputStream = this.getContentResolver().openInputStream(uri);
      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
//      final long startTime = SystemClock.elapsedRealtime();
//      TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
//              .getImage(), rotationDegrees,
//              INPUT_TENSOR_WIDTH, INPUT_TENSOR_HEIGHT,
//              TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
//              TensorImageUtils.TORCHVISION_NORM_STD_RGB,
//              mInputTensorBuffer, 0);

//      final long moduleForwardStartTime = SystemClock.elapsedRealtime();
//      final Tensor outputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
//      final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

//      final float[] scores = outputTensor.getDataAsFloatArray();
      // preparing input tensor
      final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
              TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

      // running the model
      final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

      // getting tensor content as java array of floats
      final float[] scores = outputTensor.getDataAsFloatArray();


      final int[] ixs = Utils.topK(scores, TOP_K);

      Log.i("IXS", ixs[0]+"");

      final String[] topKClassNames = new String[TOP_K];
      final float[] topKScores = new float[TOP_K];

      float[] scoresPer;
      for (int i = 0; i < scores.length; i++) {

      }

      for (int i = 0; i < TOP_K; i++) {
        final int ix = ixs[i];
        Log.i("IX", ix+"");
        // topKClassNames[i] = Constants.IMAGENET_CLASSES[ix];
        topKClassNames[i] = Constants.IMAGENET_LABEL[ix];
        Log.i("HOA", topKClassNames[i]+"");
        topKScores[i] = scores[ix];
        Log.i("DiemHOA", topKScores[i]+"");
      }
//      final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
      return new AnalysisResult(topKClassNames, topKScores, 0, 0);
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

}


