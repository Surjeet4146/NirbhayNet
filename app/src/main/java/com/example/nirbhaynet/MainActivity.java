package com.example.nirbhaynet;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
    private Interpreter tflite;
    private StandardScaler scaler;
    private EditText typingSpeedInput, swipeSpeedInput, tapPressureInput, deviceAngleInput;
    private TextView resultText, appTitle;
    private Button predictButton;
    private Handler animationHandler;
    private ValueAnimator colorAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            setContentView(createErrorLayout());
            TextView errorText = findViewById(android.R.id.text1);
            errorText.setText("⚠️ Layout Error: Please check activity_main.xml");
            return;
        }

        initializeUI();
        setupScaler();
        loadModel();
        startNeonAnimations();
    }

    private void initializeUI() {
        typingSpeedInput = findViewById(R.id.typing_speed_input);
        swipeSpeedInput = findViewById(R.id.swipe_speed_input);
        tapPressureInput = findViewById(R.id.tap_pressure_input);
        deviceAngleInput = findViewById(R.id.device_angle_input);
        resultText = findViewById(R.id.result_text);
        predictButton = findViewById(R.id.predict_button);
        appTitle = findViewById(R.id.app_title);
        animationHandler = new Handler();

        predictButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            animateButtonClick(v);
            predict();
        });
    }

    private void setupScaler() {
        float[] mean = new float[]{45.65284321f, 321.48546237f, 0.55596076f, 34.71889549f};
        float[] scale = new float[]{9.29479523f, 55.26307357f, 0.11470705f, 10.82035121f};
        scaler = new StandardScaler(mean, scale);
    }

    private void loadModel() {
        try {
            tflite = new Interpreter(loadModelFile());
            resultText.setText("🤖 AI Model Ready • Enter data to analyze");
            animateSuccess();
        } catch (IOException e) {
            resultText.setText("❌ Model Load Error: " + e.getMessage());
            predictButton.setEnabled(false);
            animateError();
        }
    }

    private void startNeonAnimations() {
        // Pulsing title animation
        if (appTitle != null) {
            ValueAnimator titleAnimator = ValueAnimator.ofFloat(0.8f, 1.2f);
            titleAnimator.setDuration(2000);
            titleAnimator.setRepeatMode(ValueAnimator.REVERSE);
            titleAnimator.setRepeatCount(ValueAnimator.INFINITE);
            titleAnimator.addUpdateListener(animation -> {
                float scale = (float) animation.getAnimatedValue();
                appTitle.setScaleX(scale);
                appTitle.setScaleY(scale);
            });
            titleAnimator.start();
        }

        // Color cycling for result text
        startColorCycling();
    }

    private void startColorCycling() {
        if (resultText != null) {
            colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    0xFF00FFFF, 0xFFFF00FF, 0xFF00FF00, 0xFF00FFFF);
            colorAnimator.setDuration(4000);
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
            colorAnimator.addUpdateListener(animation ->
                    resultText.setTextColor((Integer) animation.getAnimatedValue()));
            colorAnimator.start();
        }
    }

    private void animateButtonClick(View button) {
        // Multi-stage button animation (scale + rotation)
        button.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction(() -> {
                button.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        button.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                    })
                    .start();
            })
            .start();

        button.animate()
            .rotation(360f)
            .setDuration(800)
            .start();
    }

    private void animateSuccess() {
        if (resultText != null) {
            resultText.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    resultText.setTextColor(0xFF00FF00);
                    resultText.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
                })
                .start();
        }
    }

    private void animateError() {
        if (resultText != null) {
            resultText.animate()
                .translationX(-10f)
                .setDuration(50)
                .withEndAction(() -> {
                    resultText.animate()
                        .translationX(10f)
                        .setDuration(50)
                        .withEndAction(() -> {
                            resultText.animate()
                                .translationX(0f)
                                .setDuration(50)
                                .start();
                        })
                        .start();
                })
                .start();
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        try (AssetFileDescriptor fileDescriptor = getAssets().openFd("nn_model.tflite");
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    private void predict() {
        try {
            if (!validateInputs()) {
                animateError();
                return;
            }

            float typingSpeed = Float.parseFloat(typingSpeedInput.getText().toString().trim());
            float swipeSpeed = Float.parseFloat(swipeSpeedInput.getText().toString().trim());
            float tapPressure = Float.parseFloat(tapPressureInput.getText().toString().trim());
            float deviceAngle = Float.parseFloat(deviceAngleInput.getText().toString().trim());

            resultText.setText("🔄 Analyzing behavior patterns...");
            animateProcessing();

            animationHandler.postDelayed(() -> {
                performInference(typingSpeed, swipeSpeed, tapPressure, deviceAngle);
            }, 1500);
        } catch (NumberFormatException e) {
            resultText.setText("⚠️ Please enter valid numbers");
            animateError();
        } catch (Exception e) {
            resultText.setText("❌ Analysis Error: " + e.getMessage());
            animateError();
        }
    }

    private void performInference(float typingSpeed, float swipeSpeed, float tapPressure, float deviceAngle) {
        try {
            float[][] input = new float[1][4];
            input[0][0] = typingSpeed;
            input[0][1] = swipeSpeed;
            input[0][2] = tapPressure;
            input[0][3] = deviceAngle;

            input[0] = scaler.transform(input[0]);

            float[][] output = new float[1][1];
            tflite.run(input, output);

            // Play sound effect
            try {
                MediaPlayer.create(this, R.raw.beep_sound).start();
            } catch (Exception e) {
                // Skip sound if file is missing
            }

            float prediction = output[0][0];
            String emoji = prediction > 0.5 ? "🚨" : "✅";
            String status = prediction > 0.5 ? "FRAUD DETECTED" : "NORMAL BEHAVIOR";
            String confidence = String.format("%.1f%%", Math.abs(prediction - 0.5) * 200);

            String result = String.format("%s %s\nConfidence: %s\nRisk Score: %.3f",
                    emoji, status, confidence, prediction);

            resultText.setText(result);

            // Contextual animation and background
            if (prediction > 0.5) {
                animateFraudDetected();
                resultText.getRootView().findViewById(R.id.result_section)
                    .setBackgroundResource(R.drawable.result_background_fraud);
            } else {
                animateNormalBehavior();
                resultText.getRootView().findViewById(R.id.result_section)
                    .setBackgroundResource(R.drawable.result_background_normal);
            }

            // Pulse animation
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
            resultText.startAnimation(pulse);
        } catch (Exception e) {
            resultText.setText("❌ Inference Error: " + e.getMessage());
            animateError();
        }
    }

    private void animateProcessing() {
        if (resultText != null) {
            ValueAnimator processingAnimator = ValueAnimator.ofFloat(0.5f, 1.0f);
            processingAnimator.setDuration(300);
            processingAnimator.setRepeatMode(ValueAnimator.REVERSE);
            processingAnimator.setRepeatCount(4);
            processingAnimator.addUpdateListener(animation -> {
                float alpha = (float) animation.getAnimatedValue();
                resultText.setAlpha(alpha);
            });
            processingAnimator.start();
        }
    }

    private void animateFraudDetected() {
        ValueAnimator fraudAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFFFF0000, 0xFFFF6666, 0xFFFF0000);
        fraudAnimator.setDuration(1000);
        fraudAnimator.setRepeatCount(2);
        fraudAnimator.addUpdateListener(animation ->
                resultText.setTextColor((Integer) animation.getAnimatedValue()));
        fraudAnimator.start();
    }

    private void animateNormalBehavior() {
        ValueAnimator normalAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                0xFF00FF00, 0xFF66FF66, 0xFF00FF00);
        normalAnimator.setDuration(800);
        normalAnimator.setRepeatCount(1);
        normalAnimator.addUpdateListener(animation ->
                resultText.setTextColor((Integer) animation.getAnimatedValue()));
        normalAnimator.start();
    }

    private boolean validateInputs() {
        EditText[] inputs = {typingSpeedInput, swipeSpeedInput, tapPressureInput, deviceAngleInput};
        String[] fieldNames = {"Typing Speed", "Swipe Speed", "Tap Pressure", "Device Angle"};

        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i].getText().toString().trim().isEmpty()) {
                resultText.setText("⚠️ Please fill: " + fieldNames[i]);
                return false;
            }
        }
        return true;
    }

    private View createErrorLayout() {
        TextView errorText = new TextView(this);
        errorText.setId(android.R.id.text1);
        errorText.setText("Layout Error");
        errorText.setTextSize(18);
        errorText.setTextColor(0xFFFF0000);
        errorText.setPadding(16, 16, 16, 16);
        return errorText;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
    }

    static class StandardScaler {
        private final float[] mean;
        private final float[] scale;

        public StandardScaler(float[] mean, float[] scale) {
            if (mean.length != scale.length) {
                throw new IllegalArgumentException("Mean and scale arrays must have the same length");
            }
            this.mean = mean.clone();
            this.scale = scale.clone();
        }

        public float[] transform(float[] input) {
            if (input.length != mean.length) {
                throw new IllegalArgumentException("Input size must match scaler dimensions");
            }

            float[] transformed = new float[input.length];
            for (int i = 0; i < input.length; i++) {
                if (scale[i] == 0) {
                    transformed[i] = 0;
                } else {
                    transformed[i] = (input[i] - mean[i]) / scale[i];
                }
            }
            return transformed;
        }
    }
}