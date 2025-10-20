package com.example.smartwallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartwallet.R;
import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.AuthApi;
import com.example.smartwallet.network.dto.ProfileResponse;
import com.example.smartwallet.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView textName;
    private TextView textPhone;
    private TextView textEmail;
    private ProgressBar progress;
    private Button buttonAiChat;
    private LinearLayout buttonLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        textName = view.findViewById(R.id.textName);
        textPhone = view.findViewById(R.id.textPhone);
        textEmail = view.findViewById(R.id.textEmail);
        progress = view.findViewById(R.id.progress);
        buttonAiChat = view.findViewById(R.id.buttonAiChat);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        buttonAiChat.setOnClickListener(v -> openAssistant());
        buttonLogout.setOnClickListener(v -> logout());

        loadProfile();
        return view;
    }

    private void loadProfile() {
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        String token = tokenManager.getToken();
        
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден. Войдите в систему.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        AuthApi api = ApiClient.getAuthApi();
        String authToken = "Bearer " + token;
        api.getProfile(authToken).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call, @NonNull Response<ProfileResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();
                    textName.setText(profile.name);
                    textPhone.setText(profile.phone);
                    textEmail.setText(profile.email);
                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Сессия истекла. Войдите в систему.", Toast.LENGTH_SHORT).show();
                    tokenManager.clearToken();
                    logout();
                } else {
                    Toast.makeText(requireContext(), "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void openAssistant() {
        Intent intent = new Intent(requireContext(), AssistantActivity.class);
        startActivity(intent);
    }

    private void logout() {
        TokenManager.getInstance(requireContext()).clearToken();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}


