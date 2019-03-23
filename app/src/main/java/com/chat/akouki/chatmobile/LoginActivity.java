package com.chat.akouki.chatmobile;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.chat.akouki.chatmobile.helpers.PrefsManager;
import com.chat.akouki.chatmobile.helpers.Utils;

import microsoft.aspnet.signalr.client.http.CookieCredentials;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private String loginUrl = "http://localhost:2325/Account/Login";
    private String registerUrl = "http://localhost:2325/Account/Register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        // Enable This for HttpPost in AsyncTaskLogin
        CookieSyncManager.createInstance(this.getApplicationContext());

        // Listeners
        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        Button btnShowRegister = (Button) findViewById(R.id.btnShowRegister);
        btnLogin.setOnClickListener(this);
        btnShowRegister.setOnClickListener(this);

        // Load Auth cookies/credentials
        CookieCredentials loginCredentials = PrefsManager.loadAuthCookie(this);
        if (loginCredentials != null) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                if (Utils.isOnline(this)) {
                    String username = ((EditText) findViewById(R.id.editUsername)).getText().toString();
                    String password = ((EditText) findViewById(R.id.editPassword)).getText().toString();

                    new AsyncLoginTask(this).execute(loginUrl, username, password);

                } else {
                    Toast.makeText(this, "You are not connected to the Internet!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnShowRegister:
                Uri uri = Uri.parse(registerUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
        }
    }

}
