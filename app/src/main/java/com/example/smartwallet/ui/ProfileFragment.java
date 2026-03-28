package com.example.smartwallet.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.smartwallet.BuildConfig;
import com.example.smartwallet.R;
import com.example.smartwallet.network.ApiClient;
import com.example.smartwallet.network.AuthApi;
import com.example.smartwallet.network.DemoApi;
import com.example.smartwallet.network.dto.DemoSeedResponse;
import com.example.smartwallet.network.dto.ProfileResponse;
import com.example.smartwallet.utils.TokenManager;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ImageView imageAvatar;
    private View avatarButton;
    private TextView textName;
    private TextView textPhone;
    private TextView textEmail;
    private ProgressBar progress;
    private MaterialButton buttonAiChat;
    private View buttonDemoSeed;
    private MaterialButton buttonLogout;

    private final ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onAvatarUriPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imageAvatar = view.findViewById(R.id.imageAvatar);
        avatarButton = view.findViewById(R.id.avatarButton);
        textName = view.findViewById(R.id.textName);
        textPhone = view.findViewById(R.id.textPhone);
        textEmail = view.findViewById(R.id.textEmail);
        progress = view.findViewById(R.id.progress);
        buttonAiChat = view.findViewById(R.id.buttonAiChat);
        buttonDemoSeed = view.findViewById(R.id.buttonDemoSeed);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        buttonAiChat.setOnClickListener(v -> openAssistant());
        if (buttonDemoSeed != null) {
            buttonDemoSeed.setOnClickListener(v -> confirmAndRunDemoSeed());
        }
        buttonLogout.setOnClickListener(v -> logout());
        if (avatarButton != null) {
            avatarButton.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        }

        bindAvatar(null);
        loadProfile();
        return view;
    }

    private void onAvatarUriPicked(@Nullable Uri uri) {
        if (uri != null) {
            uploadAvatar(uri);
        }
    }

    private void confirmAndRunDemoSeed() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.demo_confirm_title)
                .setMessage(R.string.demo_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.demo_confirm_yes, (d, w) -> runDemoSeed(true))
                .show();
    }

    private void runDemoSeed(boolean reset) {
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден. Войдите в систему.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        if (buttonDemoSeed != null) buttonDemoSeed.setEnabled(false);

        DemoApi demoApi = ApiClient.getDemoApi();
        String authToken = "Bearer " + token;
        demoApi.seed(authToken, reset).enqueue(new Callback<DemoSeedResponse>() {
            @Override
            public void onResponse(@NonNull Call<DemoSeedResponse> call, @NonNull Response<DemoSeedResponse> response) {
                setLoading(false);
                if (buttonDemoSeed != null) buttonDemoSeed.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    DemoSeedResponse body = response.body();
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.demo_success, body.cardsCreated, body.transactionsCreated),
                            Toast.LENGTH_LONG
                    ).show();
                    if (getActivity() instanceof DashboardActivity) {
                        ((DashboardActivity) getActivity()).notifyDemoSeeded();
                    }
                    return;
                }

                if (response.code() == 409) {
                    String detail = readErrorBody(response);
                    if (detail == null || detail.isEmpty()) {
                        detail = getString(R.string.demo_presentation_hint);
                    }
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.demo_error_409, detail),
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Сессия истекла. Войдите в систему.", Toast.LENGTH_SHORT).show();
                    tokenManager.clearToken();
                    logout();
                    return;
                }

                Toast.makeText(requireContext(), "Ошибка демо: код " + response.code(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<DemoSeedResponse> call, @NonNull Throwable t) {
                setLoading(false);
                if (buttonDemoSeed != null) buttonDemoSeed.setEnabled(true);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    private static String readErrorBody(@NonNull Response<?> response) {
        if (response.errorBody() == null) return null;
        try {
            return response.errorBody().string();
        } catch (IOException e) {
            return null;
        }
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
                    bindProfile(response.body());
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

    private void bindProfile(@NonNull ProfileResponse profile) {
        textName.setText(profile.name);
        textPhone.setText(profile.phone);
        textEmail.setText(profile.email);
        bindAvatar(profile.avatarUrl);
    }

    private void bindAvatar(@Nullable String avatarUrl) {
        if (imageAvatar == null) return;
        String url = resolveAvatarUrl(avatarUrl);
        if (url == null) {
            imageAvatar.setImageResource(R.drawable.ic_person_24);
            imageAvatar.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimaryContainer)));
            return;
        }
        imageAvatar.setImageTintList(null);
        Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_person_24)
                .into(imageAvatar);
    }

    @Nullable
    private static String resolveAvatarUrl(@Nullable String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) return null;
        String t = pathOrUrl.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) return t;
        String base = BuildConfig.API_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (t.startsWith("/")) return base + t;
        return base + "/" + t;
    }

    private void uploadAvatar(@NonNull Uri uri) {
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        String token = tokenManager.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Токен не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        File file;
        try {
            file = copyUriToCacheFile(uri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Не удалось прочитать файл", Toast.LENGTH_SHORT).show();
            return;
        }

        String mime = requireContext().getContentResolver().getType(uri);
        if (mime == null || mime.isEmpty()) {
            mime = "image/jpeg";
        }
        RequestBody body = RequestBody.create(MediaType.parse(mime), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), body);

        setLoading(true);
        String authToken = "Bearer " + token;
        ApiClient.getAuthApi().uploadAvatar(authToken, part).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call, @NonNull Response<ProfileResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    bindProfile(response.body());
                    Toast.makeText(requireContext(), R.string.profile_avatar_upload_ok, Toast.LENGTH_SHORT).show();
                } else if (response.code() == 401) {
                    tokenManager.clearToken();
                    logout();
                } else {
                    Toast.makeText(requireContext(), R.string.profile_avatar_no_endpoint, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @NonNull
    private File copyUriToCacheFile(@NonNull Uri uri) throws IOException {
        String mime = requireContext().getContentResolver().getType(uri);
        String ext = ".jpg";
        if (mime != null) {
            if (mime.contains("png")) ext = ".png";
            else if (mime.contains("webp")) ext = ".webp";
        }
        File out = new File(requireContext().getCacheDir(), "avatar_upload_" + System.currentTimeMillis() + ext);
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("openInputStream null");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        return out;
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
