package com.example.smartwallet.network.dto;

import com.google.gson.annotations.SerializedName;

public class CardRequest {
    @SerializedName("bank_name")
    public String bankName;
    
    @SerializedName("card_name")
    public String cardName;
    
    public String last4;
    
    @SerializedName("cashback_rules")
    public CashbackRules cashbackRules;
    
    @SerializedName("limit_monthly")
    public double limitMonthly;
    
    public CardRequest() {}
    
    public CardRequest(String bankName, String cardName, String last4, CashbackRules cashbackRules, double limitMonthly) {
        this.bankName = bankName;
        this.cardName = cardName;
        this.last4 = last4;
        this.cashbackRules = cashbackRules;
        this.limitMonthly = limitMonthly;
    }
}
