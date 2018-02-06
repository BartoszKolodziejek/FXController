package com.forex.kolodziejek.fxcontroller.FX.Controller;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;



import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.LogManager;


@Component
public class WebQuerySender {
	
	private final Logger logger = LogManager.getLogger(WebQuerySender.class);
	
	
	
	private String getPametriziedUrl(Map<String, String> params, String url) {
		StringBuilder sb = new StringBuilder();
		sb.append(url+"?");
params.forEach((k,v) -> sb.append(k+"="+v+"&"));
sb.deleteCharAt(sb.length()-1);
return sb.toString();
	}
		
		 
 
	public String send(String url, Map<String, String> params, String target) throws Exception {
	 
		
	 HttpClient client = HttpClientBuilder.create().build();
	 
	    HttpGet request = new HttpGet(getPametriziedUrl(params, url + "/" + target));
	    System.out.println(getPametriziedUrl(params,  url + "/" + target));
	    HttpResponse response = client.execute(request);
	    logger.info(response.toString());
	    return response.toString();
			
				
		
	}

}
