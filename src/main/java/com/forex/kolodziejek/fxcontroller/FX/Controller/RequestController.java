package com.forex.kolodziejek.fxcontroller.FX.Controller;




import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forex.jExpertAdvisor.main.MarketMgr;
import com.forex.jExpertAdvisor.trades.IStrategy;
import com.forex.jExpertAdvisor.trades.TradeConfig;
import com.forex.jExpertAdvisor.trades.TradeMgr;
import ognl.enhance.ContextClassLoader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.forex.kolodziejek.fxcontroller.FX.Controller.StrategyTester.Period;
import com.forex.kolodziejek.fxcontroller.FX.Controller.StrategyTester.Tester;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;


@RestController
@EnableAsync
public class RequestController {
	
	private List<String> commands = new ArrayList<>();
	@Autowired
	private WebQuerySender sender;
	
	private final Logger logger = LogManager.getLogger(RequestController.class);
	

    @RequestMapping(value = "/mainTest", method = RequestMethod.GET)
    @ResponseBody
    @Async
    public CompletableFuture<Boolean> mainTest(@RequestParam String start, @RequestParam String end, @RequestParam String interval, @RequestParam String sl){
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Map<String, String> params = new HashMap<>();
		params.put("username", "test");
		params.put("pass", "test");

		try {
			Date endDay = df.parse(end);
			Date startDay = df.parse(start);



		do{
			Date endWeek =new  Date(startDay.getTime() + 604800000);


		try {
			JSONObject jsonStrategies = sender.getJson("http://localhost:8090", new HashMap<>(), "get_all_strategies");
			JSONObject jsonSymbols = sender.getJson("http://localhost:8090", new HashMap<>(), "get_all_symbols");
			Map<String, Map<String,String>> strategies = new ObjectMapper().readValue(jsonStrategies.toString(), HashMap.class);
			List<String> symbols = new ArrayList(new ObjectMapper().readValue(jsonSymbols.toString(), HashMap.class).values());
			Map<Map<String,String>, Map<String, String>> variances_and_payoff = new HashMap<>();
			for (String symbol : symbols){
				for (Map<String, String> strategy : strategies.values()){
					params.clear();
					params.put("symbol", symbol);
					params.put("strategy", strategy.get("name"));
					Map<String,String> variance_and_pay_off = new ObjectMapper().readValue(sender.getJson("http://localhost:2137", params, "prognose_variance_and_expected_payoff").toString(), HashMap.class);
					if(Double.parseDouble(variance_and_pay_off.get("e_payoff"))>0)
					variances_and_payoff.put(params, variance_and_pay_off);

				}

			}
			Map<String, String> matchedStrategyAndSymbol = new HashMap<>();
			for (String symbol : symbols){
				Map<String, Double> variances = new HashMap<>();
				for (Map<String, String> strategy : strategies.values()) {
					Map<String, String> key = new HashMap<>();
					key.put("symbol", symbol);
					key.put("strategy", strategy.get("name"));
					variances.put(strategy.get("name"), new  Double(variances_and_payoff.get(key).get("var")));
				}
				Double min = Double.MAX_VALUE;
				String bestStrategy = "";
				for (Map.Entry<String, Double> variance : variances.entrySet()){
					if (variance.getValue().compareTo(min)<0){
						min = variance.getValue();
						bestStrategy = variance.getKey();
					}
				}
				matchedStrategyAndSymbol.put(symbol, bestStrategy);
				}
				final ExpressionsBasedModel model = new ExpressionsBasedModel();
			Expression expression = model.addExpression("Safe Level expression").lower(1/(Double.parseDouble(sl)+1));
				Map<Map.Entry<String, String>, Double> matchesWithParts = new HashMap<>();
				for (Map.Entry<String, String> match : matchedStrategyAndSymbol.entrySet()){
					Map<String, String> key = new HashMap<>();
					key.put("symbol", match.getKey());
					key.put("strategy", match.getValue());
					Variable v = new Variable(match.getKey()+"_"+match.getValue()).upper(0).lower(1).weight(Math.exp(Double.parseDouble(variances_and_payoff.get(key).get("e_payoff"))));
					model.addVariable(v);
					expression.set(v, 1);
				}
				Optimisation.Result result = model.maximise();

				List<Tester> testers = new ArrayList<>();
				int i =0;
			for (Map.Entry<String, String> match : matchedStrategyAndSymbol.entrySet()){
				JarClassLoader jcl = new JarClassLoader();
				jcl.add(this.getLocation(match.getValue(), strategies));
				IStrategy iStrategy = (IStrategy) JclObjectFactory.getInstance().create(jcl, match.getValue());
				params.clear();
				params.put("symbol", match.getKey());
				JSONObject jsonObject = sender.getJson("http://localhost:8090",params, "getpoint" );
				testers.add(new Tester(startDay,endWeek,Period.valueOf(interval),match.getKey(),iStrategy,new BigDecimal(jsonObject.get("point").toString()), result.get(i) ));
				i++;
			}
			logger.info("MATCHING RESULTS: ");
			logger.info(matchedStrategyAndSymbol);
			logger.info(result.toString());

			for (Tester tester: testers ){
				tester.start();
			}
			for (Tester tester: testers ){
				tester.join();
			}


			startDay = endWeek;

		} catch (Exception e) {
			e.printStackTrace();
		}}
		while (startDay.before(endDay));
			return CompletableFuture.completedFuture(new Boolean(true));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return CompletableFuture.completedFuture(new Boolean(false));
    }



	private String getLocation(String name, Map<String, Map<String,String>> strategies){
		for (Map<String, String> strategy : strategies.values()) {
			if(strategy.get("name").equals(name))
				return strategy.get("location");
		}
		return null;
	}

	
	
	@RequestMapping(value="/tester", method= RequestMethod.GET)
	@ResponseBody
	@Async
	public boolean testStrategy(@RequestParam String start, @RequestParam String end, @RequestParam String interval, @RequestParam String strategy, @RequestParam String symbol, @RequestParam String location) {
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Map<String, String> params = new HashMap<>();
		params.put("username", "test");
		params.put("pass", "test");



		try {
			sender.send("http://localhost:8090", params, "login");

            JarClassLoader jcl = new JarClassLoader();
            jcl.add("c:\\" + location);
            IStrategy iStrategy = (IStrategy) JclObjectFactory.getInstance().create(jcl, strategy);
            params.clear();
            params.put("symbol", symbol);
            JSONObject jsonObject = sender.getJson("http://localhost:8090",params, "getpoint" );
			Tester tester = new Tester(df.parse(start),df.parse(end),Period.valueOf(interval), symbol, iStrategy, new BigDecimal((String) jsonObject.get("point")), new BigDecimal(1));
			tester.prepareTesting();
			tester.test();
			return true;
		} catch (ParseException e) {

			logger.error(e.getMessage());

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	return false;}
	
	@RequestMapping(value="/insertcandle", method = RequestMethod.GET)
	@ResponseBody
	@Async
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
	@Async
	public boolean insertCurrentTrade(@RequestParam String date_open, @RequestParam String open_price, @RequestParam String type, @RequestParam String strategy, @RequestParam String symbol, @RequestParam String status, @RequestParam String account, @RequestParam String stoploss, @RequestParam String interval, @RequestParam String stoploss_type, @RequestParam String size ) {
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
		params.put("size", size);
		try {
			sender.send("http://localhost:8090", params, "insertactivetrades");
			
			return true;
		}
		catch(Exception e){
			logger.error(e.getLocalizedMessage());
			return false;
		}



		
	}

    @RequestMapping(value = "/insertrate", method = RequestMethod.GET)
    @ResponseBody
	@Async
    public boolean insertRate(@RequestParam String base, @RequestParam String target, @RequestParam String rate, @RequestParam String date){
        Map<String, String> params = new HashMap<>();
        params.put("base", base);
        params.put("target", target);
        params.put("rate", rate);
        params.put("date", date);
        params.put("account", TradeConfig.getAccount() );
        try {
            sender.send("http://localhost:8090", params, "insertrate");
            return true;
        } catch (Exception e) {
			e.printStackTrace();
            return false;

        }


    }
	@RequestMapping(value="/inserttrades", method=RequestMethod.GET)
	@ResponseBody
	@Async
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
	@Async
	public void deleteCommand(@RequestParam String command) {
		command=command.replace("\"", "");
		command=command+"&";
		
		commands.remove(command);
	}




    @RequestMapping(value = "/getrate", method = RequestMethod.GET)
    @ResponseBody
	@Async
    public void getRate( @RequestParam String date, @RequestParam String symbol){

	    Map<String, String> params = new HashMap<>();
	    params.put("name", TradeConfig.getInstance().getAccount());

        JSONObject jsonObject = sender.getJson("http://localhost:8090", params, "get_account");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm");




        String commandGettingRate = null;
        try {
            Date dateOfRate = dateFormat.parse(date);
            dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            commandGettingRate = MarketMgr.getInstance(symbol).getSymbol().substring(3) + jsonObject.getString("currency")   + "!"+"interval:"+"PERIOD_M1"+"!"+"getRate!" + "date:" + dateFormat.format(dateOfRate) +  "&";
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        commands.add(commandGettingRate);

    }


	
	@RequestMapping(value="/getcandles", method=RequestMethod.GET)
	@ResponseBody
	@Async
	public void getCandles(@RequestParam String symbol, @RequestParam String  interval, @RequestParam String date_start, @RequestParam String date_end ) {
		
		String commandGettingCandles = symbol + "!"+"interval:"+interval+"!"+"getCandles!" + "start:" + date_start + "!" + "end:" + date_end + "&" ;
		commands.add(commandGettingCandles);
	}
	
	@RequestMapping(value="/controller", method=RequestMethod.GET)
	@ResponseBody
	@Async
	public CompletableFuture<List<String>> controller(){
		return CompletableFuture.completedFuture(commands);
	}
	
	@RequestMapping(value="/test", method=RequestMethod.GET)
	@ResponseBody
	public String test() {
		return "Connection successed";
	}
	

}
