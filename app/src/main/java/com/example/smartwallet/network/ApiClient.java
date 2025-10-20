package com.example.smartwallet.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE_URL = "http://192.168.0.110:8000/"; // for Android emulator talking to localhost
    private static Retrofit retrofitInstance;

    private ApiClient() {}

    private static Retrofit getRetrofit() {
        if (retrofitInstance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder().create();

            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofitInstance;
    }

    public static AuthApi getAuthApi() {
        return getRetrofit().create(AuthApi.class);
    }
    
    public static CardsApi getCardsApi() {
        return getRetrofit().create(CardsApi.class);
    }
    
    public static CashbackApi getCashbackApi() {
        return getRetrofit().create(CashbackApi.class);
    }
    
    public static TransactionsApi getTransactionsApi() {
        return getRetrofit().create(TransactionsApi.class);
    }
    
    public static AssistantApi getAssistantApi() {
        return getRetrofit().create(AssistantApi.class);
    }
}




