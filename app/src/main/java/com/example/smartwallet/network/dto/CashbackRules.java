package com.example.smartwallet.network.dto;

import com.google.gson.annotations.SerializedName;

public class CashbackRules {
    @SerializedName("additionalProp1")
    public int additionalProp1;
    
    @SerializedName("additionalProp2")
    public int additionalProp2;
    
    @SerializedName("additionalProp3")
    public int additionalProp3;
    
    public CashbackRules() {}
    
    public CashbackRules(int additionalProp1, int additionalProp2, int additionalProp3) {
        this.additionalProp1 = additionalProp1;
        this.additionalProp2 = additionalProp2;
        this.additionalProp3 = additionalProp3;
    }
}
