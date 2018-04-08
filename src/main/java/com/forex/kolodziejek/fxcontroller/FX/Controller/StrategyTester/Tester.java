package com.forex.kolodziejek.fxcontroller.FX.Controller.StrategyTester;

import java.math.BigDecimal;
import java.util.*;

import com.forex.jExpertAdvisor.trades.IStrategy;
import com.forex.jExpertAdvisor.trades.TradeConfig;
import com.forex.kolodziejek.fxcontroller.FX.Controller.WebQuerySender;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import com.forex.jExpertAdvisor.main.MarketMgr;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


public class Tester extends Thread{






	private Date start;
	private Date end;
	private Period period;
	private String symbol;
	private IStrategy strategy;
    private BigDecimal point;
    private BigDecimal size;


	@Override
	public void run() {
		super.run();
		prepareTesting();
		test();
	}

	public Tester(Date start, Date end, Period period, String symbol, IStrategy strategy, BigDecimal point, BigDecimal size) {
	    this.point = point;
		this.start = start;
		this.end = end;
		this.period = period;
		this.symbol = symbol;
		this.strategy = strategy;
		this.size = size;
	}





	public void prepareTesting() {
	    MarketMgr.getInstance(symbol).setHistoricView(CandleMgr.getInstance().getCandles(start,  end,  period,  symbol));
		MarketMgr.getInstance(symbol).setInterval(period.toString());
		MarketMgr.getInstance(symbol).setSymbol(symbol);
		TradeConfig.setMax(new Integer((int) (period.toTime()/(60*1000))).intValue());

		}
	public void test(){
		strategy.setSymbol(symbol);
		strategy.setPoint(point);
		strategy.setSize(size);
		strategy.OnInit();
		do{
			MarketMgr.getInstance(symbol).update();
			if(!MarketMgr.getInstance(symbol).isEnd())
			strategy.OnStart();

		}
		while (!MarketMgr.getInstance(symbol).isEnd());




	}

	

}
