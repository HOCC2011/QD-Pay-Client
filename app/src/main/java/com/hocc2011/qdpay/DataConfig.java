package com.hocc2011.qdpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DataConfig extends AppCompatActivity {

    EditText AccountID;
    Button apply;
    private static String SERVER_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_data_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SERVER_URL = this.getSharedPreferences("NetworkData", MODE_PRIVATE).getString("URL", "1.1.1.1");

        AccountID = findViewById(R.id.AccountID);
        apply = findViewById(R.id.apply);
        apply.setOnClickListener(v -> {
            fetchSessionToken(AccountID.getText().toString());
        });
    }

    private void fetchSessionToken(String userID) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/getSessionToken?userId=" + userID);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d("ResponseToken", response.toString());

                if (conn.getResponseCode() == 200) {
                    runOnUiThread(() -> {
                       fetchSharedSecret(response.toString());
                    });
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchSharedSecret(String SessionToken) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/getKey");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.addRequestProperty("Authorization", SessionToken);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d("ResponseKey", response.toString());

                if (conn.getResponseCode() == 200) {
                    conn.disconnect();
                    runOnUiThread(() -> {
                        SharedPreferences pref = this.getSharedPreferences("AppData", MODE_PRIVATE);
                        pref.edit().putString("AccountID", AccountID.getText().toString()).apply();
                        pref.edit().putString("sharedSecret", response.toString()).apply();
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}