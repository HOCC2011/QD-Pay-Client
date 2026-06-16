package com.hocc2011.qdpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static String configIpAndPort = "192.168.50.82:4800";
    private static String SERVER_URL;
    private static String AccountID = "";
    TextView balance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.getSharedPreferences("NetworkData", MODE_PRIVATE).edit().putString("URL", "http://" + configIpAndPort).apply();

        SERVER_URL = this.getSharedPreferences("NetworkData", MODE_PRIVATE).getString("URL", "1.1.1.1");

        SharedPreferences pref = this.getSharedPreferences("AppData", MODE_PRIVATE);
        AccountID = pref.getString("AccountID", "");

        findViewById(R.id.btn_mode_user).setOnClickListener(v -> {
            Intent intent = new Intent(this, UserPayment.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_mode_merchant).setOnClickListener(v -> {
            Intent intent = new Intent(this, MerchantReceive.class);
            startActivity(intent);
        });

        findViewById(R.id.switch_account).setOnClickListener(v -> {
            Intent intent = new Intent(this, DataConfig.class);
            startActivity(intent);
        });

        balance = findViewById(R.id.balance);
        fetchBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchBalance();
    }

    private void fetchBalance() {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/balance?userId=" + AccountID);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    var response = new String(conn.getInputStream().readAllBytes());
                    runOnUiThread(() -> balance.setText("$" + response));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}