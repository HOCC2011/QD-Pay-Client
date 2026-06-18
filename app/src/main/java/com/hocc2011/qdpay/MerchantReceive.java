package com.hocc2011.qdpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MerchantReceive extends AppCompatActivity {

    private static String SERVER_URL;
    private static String AccountID = "";
    private EditText amountInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_merchant_receive);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SERVER_URL = this.getSharedPreferences("NetworkData", MODE_PRIVATE).getString("URL", "1.1.1.1");

        SharedPreferences pref = this.getSharedPreferences("AppData", MODE_PRIVATE);
        AccountID = pref.getString("AccountID", "");

        // Merchant Scan Button
        findViewById(R.id.btn_scan).setOnClickListener(v -> scanCode());

        amountInput = findViewById(R.id.input_amount);

        findViewById(R.id.back).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

    // ==========================================
    // MERCHANT ROLE: Scan & Process
    // ==========================================
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    String scannedToken = result.getContents();
                    String amount;
                    if (!amountInput.getText().toString().equals("")) {
                        amount = amountInput.getText().toString();
                    } else {
                        amount = "0";
                    }

                    if(amount.isEmpty()) {
                        Toast.makeText(this, "Enter amount first!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Send to Server (Network OP on separate thread)
                    new Thread(() -> sendTransactionToServer(scannedToken, amount)).start();
                }
            });

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Customer QR");
        options.setBeepEnabled(true);

        // CRITICAL: Set this to false to allow the scanner to rotate
        options.setOrientationLocked(false);

        // Optional: use the wide-angle camera if available
        options.setBarcodeImageEnabled(true);

        barcodeLauncher.launch(options);
    }

    private void sendTransactionToServer(String token, String amount) {
        try {
            URL url = new URL(SERVER_URL + "/transaction/process");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Format: token|merchantId|amount
            String body = token + "|" + AccountID + "|" + amount;

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            runOnUiThread(() -> {
                if (code == 200) {
                    //Toast.makeText(this, "Payment Success!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, SuccessView.class);
                    startActivity(intent);
                }
                else Toast.makeText(this, "Payment Failed: " + code, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show());
        }
    }
}