package com.hocc2011.qdpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class DataConfig extends AppCompatActivity {

    EditText AccountID;
    Button apply;

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

        AccountID = findViewById(R.id.AccountID);
        apply = findViewById(R.id.apply);
        apply.setOnClickListener(v -> {
            SharedPreferences pref = this.getSharedPreferences("AppData", MODE_PRIVATE);
            pref.edit().putString("AccountID", AccountID.getText().toString()).apply();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }
}