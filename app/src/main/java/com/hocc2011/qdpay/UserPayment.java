package com.hocc2011.qdpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class UserPayment extends AppCompatActivity {

    // --- Configuration ---
    private static final String SHARED_SECRET = "super_secure_shared_secret_key"; // Must match server
    private static final String SERVER_URL = "http://192.168.50.139:8000";
    private static String AccountID = "";
    ImageView qrImage;
    TextView refresh;
    boolean refreshButtonEnabled = true;
    boolean alreadyDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_payment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences pref = this.getSharedPreferences("AppData", MODE_PRIVATE);
        AccountID = pref.getString("AccountID", "");

        new CountDownTimer(3000, 1000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                refresh.setText("Click to refresh QR code");
            }
        }.start();

        refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(v -> {
            if (refreshButtonEnabled == true) {
                generateUserQR();
                refreshButtonEnabled = false;
                new CountDownTimer(3000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        refresh.setText("Refreshed");
                    }

                    public void onFinish() {
                        refresh.setText("Click to refresh QR code");
                        refreshButtonEnabled = true;
                    }
                }.start();
            }
        });

        findViewById(R.id.back).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        qrImage = findViewById(R.id.img_qr);
        generateUserQR();
    }

    @Override
    protected void onResume() {
        super.onResume();
        generateUserQR();
    }

    private void generateUserQR() {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String rawData = AccountID + ":" + timestamp;

            // Sign the data
            String signature = hmacSha256(rawData, SHARED_SECRET);

            // Final Token: Base64(userId:timestamp:signature)
            String tokenPayload = rawData + ":" + signature;
            String finalToken = Base64.encodeToString(tokenPayload.getBytes(), Base64.NO_WRAP);

            // Convert to Bitmap
            BitMatrix matrix = new MultiFormatWriter().encode(finalToken, BarcodeFormat.QR_CODE, 600, 600);
            Bitmap bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565);
            for (int x = 0; x < 600; x++) {
                for (int y = 0; y < 600; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            qrImage.setImageBitmap(bitmap);

            startStatusPolling(finalToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startStatusPolling(String token) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/transaction/status?token=" + token);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d("Response", response.toString());

                if (response.toString().equals("SUCCESS")) {
                    runOnUiThread(() -> {
                        if (alreadyDisplayed == false) {
                            SuccessView();
                            alreadyDisplayed = true;
                        }
                    });
                } else {
                    Thread.sleep(3500);
                    startStatusPolling(token);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void SuccessView() {
        Log.d("Log", "Payment Successful");
        /*
        new android.app.AlertDialog.Builder(this)
                .setTitle("Payment Successful")
                .setMessage("You have successfully paid the merchant.")
                .setPositiveButton("OK", null)
                .show();
         */
        Intent intent = new Intent(this, SuccessView.class);
        startActivity(intent);
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}