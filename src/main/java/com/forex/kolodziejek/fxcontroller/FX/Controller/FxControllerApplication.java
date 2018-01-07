package com.forex.kolodziejek.fxcontroller.FX.Controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;



@SpringBootApplication
public class FxControllerApplication extends SpringBootServletInitializer {
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(FxControllerApplication.class);
    }
	
	
	public static void main(String[] args) {
		SpringApplication.run(FxControllerApplication.class, args);
	}
}
