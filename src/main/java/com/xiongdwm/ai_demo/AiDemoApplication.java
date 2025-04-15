package com.xiongdwm.ai_demo;

import java.text.SimpleDateFormat;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiDemoApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(AiDemoApplication.class);
		app.setBanner(new CustomBanner());
		app.setBannerMode(Banner.Mode.CONSOLE);
		app.run(args);
	}

	static class CustomBanner implements Banner {
		@Override
		public void printBanner(org.springframework.core.env.Environment environment, Class<?> sourceClass, java.io.PrintStream out) {
			SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String currentTime = sdf.format(new java.util.Date());
			out.println("||-----------------------------------||");
			out.println("||         AI Demo Application       ||");
			out.println("||         Version: 0.1              ||");
			out.println("||         Author: dawei xiong       ||");
			out.println("||       Date: " + currentTime + "   ||");
			out.println("||-----------------------------------||");
			out.println("||         Welcome to AI Demo!       ||");
		}
	}
}
