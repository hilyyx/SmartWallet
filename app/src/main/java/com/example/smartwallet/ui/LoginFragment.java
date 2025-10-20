package com.example.smartwallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartwallet.R;
import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.AuthApi;
import com.example.smartwallet.network.dto.LoginRequest;
import com.example.smartwallet.network.dto.TokenResponse;
import com.example.smartwallet.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private EditText phoneInput;
    private EditText passwordInput;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        phoneInput = view.findViewById(R.id.inputPhone);
        passwordInput = view.findViewById(R.id.inputPassword);
        progressBar = view.findViewById(R.id.progress);
        Button loginButton = view.findViewById(R.id.buttonLogin);

        loginButton.setOnClickListener(v -> attemptLogin());
        return view;
    }

    private void attemptLogin() {
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        AuthApi api = ApiClient.getAuthApi();
        api.login(new LoginRequest(phone, password)).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Save token
                    TokenManager.getInstance(requireContext()).saveToken(response.body().accessToken);
                    openWelcome();
                } else {
                    Toast.makeText(requireContext(), R.string.auth_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void openWelcome() {
        Intent intent = new Intent(requireContext(), DashboardActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}


