package com.example.nirbhaynet;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    private EditText typingSpeedInput, swipeSpeedInput, tapPressureInput, deviceAngleInput;
    private TextView resultText, titleText;
    private Button predictButton;
    private Interpreter tflite;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        typingSpeedInput = findViewById(R.id.typing_speed_input);
        swipeSpeedInput = findViewById(R.id.swipe_speed_input);
        tapPressureInput = findViewById(R.id.tap_pressure_input);
        deviceAngleInput = findViewById(R.id.device_angle_input);
        resultText = findViewById(R.id.result_text);
        predictButton = findViewById(R.id.predict_button);
        titleText = findViewById(R.id.title_text);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            resultText.setText("⚠️ Error loading model: " + e.getMessage());
            return;
        }

        startTitleAnimation();
        startResultColorCycling();

        predictButton.setOnClickListener(v -> {
            vibrator.vibrate(50);
            try {
                MediaPlayer.create(this, R.raw.beep_sound).start();
            } catch (Exception e) {
                // Skip sound if file is missing
            }
            animateButton();
            predict();
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(getAssets().openFd("nn_model.tflite").getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = getAssets().openFd("nn_model.tflite").getStartOffset();
        long declaredLength = getAssets().openFd("nn_model.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void startTitleAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(titleText, "scaleX", 0.8f, 1.2f);
        scaleX.setDuration(2000);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(titleText, "scaleY", 0.8f, 1.2f);
        scaleY.setDuration(2000);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        scaleX.start();
        scaleY.start();
    }

    private void startResultColorCycling() {
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFF00FFFF, 0xFFFF00FF, 0xFF00FF00, 0xFF00FFFF);
        colorAnim.setDuration(4000);
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.addUpdateListener(animator ->
                resultText.setTextColor((int) animator.getAnimatedValue()));
        colorAnim.start();
    }

    private void animateButton() {
        predictButton.setStateListAnimator(null);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(predictButton, "scaleX", 0.9f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(predictButton, "scaleY", 0.9f, 1.1f, 1.0f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(predictButton, "rotation", 0f, 360f);

        scaleX.setDuration(800);
        scaleY.setDuration(800);
        rotation.setDuration(800);

        scaleX.start();
        scaleY.start();
        rotation.start();
    }

    private void animateResult(boolean isFraud) {
        int backgroundColor = isFraud ? 0xFFFF0000 : 0xFF00FF00;
        int textColor = isFraud ? 0xFFFF0000 : 0xFF00FF00;

        ValueAnimator bgAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFF1A1A1A, backgroundColor);
        bgAnim.setDuration(1500);
        bgAnim.addUpdateListener(animator ->
                resultText.setBackgroundColor((int) animator.getAnimatedValue()));
        bgAnim.start();

        ValueAnimator textAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFF00FFFF, textColor);
        textAnim.setDuration(1500);
        textAnim.addUpdateListener(animator ->
                resultText.setTextColor((int) animator.getAnimatedValue()));
        textAnim.start();

        ObjectAnimator pulse = ObjectAnimator.ofFloat(resultText, "scaleX", 1.0f, 1.05f);
        pulse.setDuration(500);
        pulse.setRepeatCount(2);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.start();

        ObjectAnimator pulseY = ObjectAnimator.ofFloat(resultText, "scaleY", 1.0f, 1.05f);
        pulseY.setDuration(500);
        pulseY.setRepeatCount(2);
        pulseY.setRepeatMode(ValueAnimator.REVERSE);
        pulseY.start();
    }

    private void animateError() {
        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(resultText, "alpha", 0.5f, 1.0f);
        fadeAnim.setDuration(1500);
        fadeAnim.start();
    }

    private void predict() {
        try {
            float typingSpeed = Float.parseFloat(typingSpeedInput.getText().toString().trim());
            float swipeSpeed = Float.parseFloat(swipeSpeedInput.getText().toString().trim());
            float tapPressure = Float.parseFloat(tapPressureInput.getText().toString().trim());
            float deviceAngle = Float.parseFloat(deviceAngleInput.getText().toString().trim());

            resultText.setText("🔄 Analyzing behavior patterns...");
            animateError();

            float[] input = new float[]{typingSpeed / 1000, swipeSpeed / 1000, tapPressure, deviceAngle / 360};
            float[][] output = new float[1][1];
            tflite.run(input, output);

            float prediction = output[0][0];
            boolean isFraud = prediction > 0.5;
            float confidence = isFraud ? prediction * 100 : (1 - prediction) * 100;
            float riskScore = prediction;

            String result = isFraud
                    ? "🚨 FRAUD DETECTED\nConfidence: " + String.format("%.1f%%", confidence) + "\nRisk Score: " + String.format("%.3f", riskScore)
                    : "✅ NORMAL BEHAVIOR\nConfidence: " + String.format("%.1f%%", confidence) + "\nRisk Score: " + String.format("%.3f", riskScore);

            resultText.setText(result);
            animateResult(isFraud);
        } catch (NumberFormatException e) {
            resultText.setText("⚠️ Please enter valid numbers");
            animateError();
        } catch (Exception e) {
            resultText.setText("⚠️ Error: " + e.getMessage());
            animateError();
        }
    }
}