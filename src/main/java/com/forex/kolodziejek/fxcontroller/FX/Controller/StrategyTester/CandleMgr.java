package com.forex.kolodziejek.fxcontroller.FX.Controller.StrategyTester;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.forex.jExpertAdvisor.candles.Candle;
import com.forex.kolodziejek.fxcontroller.FX.Controller.WebQuerySender;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


public class CandleMgr {

    private static CandleMgr instance = null;

    private CandleMgr() {

    }

    public static CandleMgr getInstance() {
        if (instance != null)
            return instance;
        return new CandleMgr();
    }

    private WebQuerySender sender = new WebQuerySender();
    private final static Logger logger = LogManager.getLogger(CandleMgr.class);


    private Candle parse(String str) throws ParseException, IOException {
        Map<String, String> candleAsMap = new ObjectMapper().readValue((str.replaceAll("/", "")), HashMap.class);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        if (candleAsMap.get("interval").contains("PERIOD_M1"))
            return new Candle(new BigDecimal(candleAsMap.get("high").replaceAll("[^\\d.]", "")),
                    new BigDecimal(candleAsMap.get("low").replaceAll("[^\\d.]", "")),
                    new BigDecimal(candleAsMap.get("open").replaceAll("[^\\d.]", "")),
                    new BigDecimal(candleAsMap.get("close").replaceAll("[^\\d.]", "")),
                    dateFormat.parse(candleAsMap.get("date")), null);
        else {
            return new Candle(new BigDecimal(candleAsMap.get("high").replaceAll("[^\\d.]", "")),
                    new BigDecimal(candleAsMap.get("low").replaceAll("[^\\d.]", "")),
                    new BigDecimal(candleAsMap.get("open").replaceAll("[^\\d.]", "")),
                    new BigDecimal(candleAsMap.get("close").replaceAll("[^\\d.]", "")),
                    dateFormat.parse(candleAsMap.get("date")), getCandles((new Date(dateFormat.parse(candleAsMap.get("date")).getTime() - Period.valueOf(candleAsMap.get("interval")).toTime())),
                    dateFormat.parse(candleAsMap.get("date")), Period.PERIOD_M1, candleAsMap.get("symol")));
        }

    }

    public List<Candle> getCandles(Date start, Date end, Period period, String symbol) {

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy-HH:mm");
        Map<String, String> params = new HashMap<>();
        params.put("start", df.format(start));
        params.put("end", df.format(end));
        params.put("interval", period.toString());
        params.put("symbol", symbol);
        try {
            Map<Integer, String> results = new ObjectMapper().readValue(sender.getJson("http://localhost:8090", params, "getcandles").toString(), HashMap.class);

            results = results.entrySet().stream().sorted(Map.Entry.<Integer, String>comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> k, LinkedMap::new));
            List<Candle> candles = new ArrayList<>();
            for (String candle : results.values()) {
                try {
                    candles.add(parse(candle));
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return candles;


        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
