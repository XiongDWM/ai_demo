package com.xiongdwm.ai_demo;

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
			out.println("||-----------------------------------||");
			out.println("||         AI Demo Application       ||");
			out.println("||         Version: 0.1              ||");
			out.println("||         Author: dawei xiong       ||");
			out.println("||         Date: 2025-04-04          ||");
			out.println("||-----------------------------------||");
			out.println("||         Welcome to AI Demo!       ||");
		}
	}
}
