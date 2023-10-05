/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.ta.reportportal.core.configs;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Application Entry Point
 *
 * @author Andrei Varabyeu
 */
@SpringBootApplication(scanBasePackages = { "com.epam.ta.reportportal", "com.epam.reportportal" }, exclude = {
		MultipartAutoConfiguration.class, FlywayAutoConfiguration.class })
@Configuration
@Import({ com.epam.ta.reportportal.config.DatabaseConfiguration.class })
public class ReportPortalApp {

	public static void main(String[] args) {
		Logger log = (Logger) LoggerFactory.getLogger(ReportPortalApp.class);
		LoggerContext loggerContext = log.getLoggerContext();
		loggerContext.reset();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%d{yyyy-MM-dd' 'HH:mm:ss.SSS} %5p " + ProcessHandle.current().pid() + " --- [%15.15t] %-40.40logger{39} : %m%n");
		encoder.start();
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		appender.setContext(loggerContext);
		appender.setEncoder(encoder);
		appender.start();
		log.addAppender(appender);
		log.info("service-api branch: {}, commit: {}", "5.7.3-2-add-logging", "fca8f461f5d16d9faf0f3835c86ba6d78dd3c538");

		SpringApplication.run(ReportPortalApp.class, args);
	}

}
