package com.example.smartwallet.network;

import com.example.smartwallet.network.dto.LoginRequest;
import com.example.smartwallet.network.dto.RegisterRequest;
import com.example.smartwallet.network.dto.TokenResponse;
import com.example.smartwallet.network.dto.ProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("auth/login")
    Call<TokenResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<TokenResponse> register(@Body RegisterRequest request);

    @GET("auth/profile")
    Call<ProfileResponse> getProfile(@Header("Authorization") String token);
}


