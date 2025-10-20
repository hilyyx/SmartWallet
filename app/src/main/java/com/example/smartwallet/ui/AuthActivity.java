package com.example.smartwallet.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smartwallet.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AuthActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        ViewPager2 viewPager = findViewById(R.id.authViewPager);
        TabLayout tabLayout = findViewById(R.id.authTabs);

        AuthPagerAdapter adapter = new AuthPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText(R.string.login_tab);
            else tab.setText(R.string.register_tab);
        }).attach();
    }
    
    public void switchToLogin() {
        ViewPager2 viewPager = findViewById(R.id.authViewPager);
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true); // Switch to login tab (position 0)
        }
    }
}





