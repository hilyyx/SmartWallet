package com.example.smartwallet;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartwallet.utils.Logger;
import com.example.smartwallet.utils.PermissionHelper;

public class MainActivity extends AppCompatActivity implements PermissionHelper.PermissionCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        // Check and request permissions first
        if (PermissionHelper.hasAllRequiredPermissions(this)) {
            launchAuthActivity();
        } else {
            Logger.i("MainActivity", "Requesting required permissions");
            PermissionHelper.requestRequiredPermissions(this);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.handlePermissionResult(requestCode, permissions, grantResults, this);
    }
    
    @Override
    public void onAllPermissionsGranted() {
        Logger.i("MainActivity", "All permissions granted, launching auth activity");
        launchAuthActivity();
    }
    
    @Override
    public void onPermissionsDenied(String[] deniedPermissions) {
        Logger.w("MainActivity", "Some permissions denied, launching auth activity anyway");
        // Launch anyway - the app will handle missing permissions gracefully
        launchAuthActivity();
    }
    
    private void launchAuthActivity() {
        Intent intent = new Intent(this, com.example.smartwallet.ui.AuthActivity.class);
        startActivity(intent);
        finish();
    }
}