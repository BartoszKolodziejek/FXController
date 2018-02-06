package com.forex.kolodziejek.fxcontroller.FX.Controller;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;





@RestController

public class RequestController {
	
	private List<String> commands = new ArrayList<>();
	@Autowired
	private WebQuerySender sender;
	
	private final Logger logger = LogManager.getLogger(RequestController.class);
	
	
	
	
	
	@RequestMapping(value="/insertcandle", method = RequestMethod.GET)
	@ResponseBody
	public boolean insertCandle(@RequestParam String interval, @RequestParam String date, @RequestParam String symbol, @RequestParam String high, @RequestParam String low, @RequestParam String open, @RequestParam String close) {
		Map<String, String> params = new HashMap<>();
		try {
			params.put("interval", interval);
			params.put("date", date);
			params.put("symbol", symbol);
			params.put("high", high);
			params.put("low", low);
			params.put("open", open);
			params.put("close", close);
			
			sender.send("http://localhost:8090", params, "insertcandle");
			return true;
	
	} catch (Exception e) {
		logger.error(e.getLocalizedMessage());
		return false;
	}
	
}
	@RequestMapping(value = "/insertactivetrades", method=RequestMethod.GET)
	@ResponseBody
	public boolean insertCurrentTrade(@RequestParam String date_open, @RequestParam String open_price, @RequestParam String type, @RequestParam String strategy, @RequestParam String symbol, @RequestParam String status, @RequestParam String account, @RequestParam String stoploss, @RequestParam String interval, @RequestParam String stoploss_type ) {
		Map<String, String> params = new HashMap<>();
		params.put("date_open", date_open);
		params.put("strategy", strategy);
		params.put("symbol", symbol);
		params.put("status", status);
		params.put("account", account);
		params.put("stoploss", stoploss);
		params.put("interval", interval);
		params.put("stoploss_type", stoploss_type);
		params.put("open_price", open_price);
		params.put("type", type);
		try {
			sender.send("http://localhost:8090", params, "insertactivetrades");
			
			return true;
		}
		catch(Exception e){
			logger.error(e.getLocalizedMessage());
			return false;
		}
		
	}
	@RequestMapping(value="/inserttrades", method=RequestMethod.GET)
	@ResponseBody
	public boolean insertAccount(@RequestParam String date_open, @RequestParam String strategy, @RequestParam String symbol, @RequestParam String effect, @RequestParam String account, @RequestParam String date_close, @RequestParam String interval ) {
		Map<String, String> params = new HashMap<>();
		params.put("date_open", date_open);
		params.put("strategy", strategy);
		params.put("symbol", symbol);
		params.put("effect", effect);
		params.put("account", account);
		params.put("date_close", date_close);
		params.put("interval", interval);
		try {
			sender.send("http://localhost:8090", params, "inserttrades");
			return true;
		}
		catch(Exception e){
			logger.error(e.getLocalizedMessage());
			return false;
		}
	}
	
	@RequestMapping(value="/deletecommand", method=RequestMethod.GET)
	@ResponseBody
	public void deleteCommand(@RequestParam String command) {
		command=command.replace("\"", "");
		command=command+"&";
		
		commands.remove(command);
	}
	
	
	@RequestMapping(value="/getcandles", method=RequestMethod.GET)
	@ResponseBody
	public void getCandles(@RequestParam String symbol, @RequestParam String  interval, @RequestParam String date_start, @RequestParam String date_end ) {
		
		String commandGettingCandles = symbol + "!"+"interval:"+interval+"!"+"getCandles!" + "start:" + date_start + "!" + "end:" + date_end + "&" ;
		commands.add(commandGettingCandles);
	}
	
	@RequestMapping(value="/controller", method=RequestMethod.GET)
	@ResponseBody
	public List<String> controller(){
		return commands;
	}
	
	@RequestMapping(value="/test", method=RequestMethod.GET)
	@ResponseBody
	public String test() {
		return "Connection successed";
	}
	

}
