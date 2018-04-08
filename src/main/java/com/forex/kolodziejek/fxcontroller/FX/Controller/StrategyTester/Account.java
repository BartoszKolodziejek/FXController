package com.forex.kolodziejek.fxcontroller.FX.Controller.StrategyTester;

import com.forex.kolodziejek.fxcontroller.FX.Controller.WebQuerySender;
import org.json.JSONException;
import org.json.JSONObject;

import javax.json.JsonObject;
import java.math.BigDecimal;

public class Account {



    private String name;
    private String currency;
    private  BigDecimal deposit;
    private BigDecimal lavarage;

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public BigDecimal getLavarage() {
        return lavarage;
    }

    public Account(JSONObject jsonObject) {


        try {
            this.name = jsonObject.getString("name");
            this.currency = jsonObject.getString("currency");
            this.deposit = new BigDecimal(jsonObject.getString("deposit"));
            this.lavarage = new BigDecimal(jsonObject.getString("lavarage"));
        } catch (JSONException e) {
            this.name = null;
            this.currency = null;
            this.deposit = null;
            this.lavarage = null;
            e.printStackTrace();
        }

    }

}
