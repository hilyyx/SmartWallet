package com.example.smartwallet.network;

import com.example.smartwallet.network.dto.TransactionRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface TransactionsApi {
    
    @POST("transactions")
    Call<Void> createTransaction(@Header("Authorization") String token, 
                                @Body TransactionRequest request);
}
