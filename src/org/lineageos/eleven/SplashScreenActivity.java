package org.lineageos.eleven;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getApplicationContext(),
                org.lineageos.eleven.ui.activities.HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
