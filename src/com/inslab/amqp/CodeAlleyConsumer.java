package com.inslab.amqp;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.inslab.tool.RedmineHandler;
import com.inslab.tool.ReviewboardHandler;

public class CodeAlleyConsumer {

	public final static String TOOL_REDMINE 			= "redmine";
	public final static String TOOL_REVIEWBOARD 		= "reviewboard";

	public final static String OPTION_RABBITMQ_HOST 	= "rhost";
	public final static String OPTION_RABBITMQ_VHOST	= "rvhost";
	public final static String OPTION_RABBITMQ_USER 	= "user";
	public final static String OPTION_RABBITMQ_PASSWD 	= "passwd";
	public final static String OPTION_RABBITMQ_QUEUE	= "queue";
	
	public final static String OPTION_DBPOOL_HOST		= "dhost";
	
	public final static String OPTION_TOOL 				= "tool";
	public final static String OPTION_TOOL_HOST 		= "thost";
	public final static String OPTION_TOOL_USER			= "tuser";
	public final static String OPTION_TOOL_PASSWD		= "tpasswd";
	public final static String OPTION_TOOL_QUEUE		= "tqueue";
	
	public final static String OPTION_HELP				= "help";
	
	
	public final static String QUEUE_JENKINS_REDMINE 	= "jenkins-result-redmine";
	
	public static void main(String[] args) {

		Options options = new Options();
		options.addOption(CodeAlleyConsumer.OPTION_RABBITMQ_HOST, 		true, "RabbitMq Broker Host");		// RabbitMq 서버
		options.addOption(CodeAlleyConsumer.OPTION_RABBITMQ_VHOST, 		true, "RabbitMq Virtual Host");		// RabbitMq 가상호스트
		options.addOption(CodeAlleyConsumer.OPTION_RABBITMQ_USER, 		true, "RabbitMq User Name");			// RabbitMq 접속 user ID
		options.addOption(CodeAlleyConsumer.OPTION_RABBITMQ_PASSWD, 	true, "RabbitMq User Password");	// RabbitMq 접속 user Password
		options.addOption(CodeAlleyConsumer.OPTION_RABBITMQ_QUEUE, 		true, "RabbitMq Queue Name");		// RabbitMq queue 
		
		options.addOption(CodeAlleyConsumer.OPTION_DBPOOL_HOST, 		true, "DatabasePoolService Host");	 
		
		options.addOption(CodeAlleyConsumer.OPTION_TOOL, 				true, "Tool  (Redmind/Reviewboard/...)");	// 연동할 Tool 명
		options.addOption(CodeAlleyConsumer.OPTION_TOOL_HOST, 			true, "Tool Host");	// 연동할 Tool 명
		options.addOption(CodeAlleyConsumer.OPTION_TOOL_USER, 			true, "Tool User Name");				// 연동 Tool의 user id
		options.addOption(CodeAlleyConsumer.OPTION_TOOL_PASSWD, 		true, "Tool User Password");		// 연동 Tool의 user passwd
		
		options.addOption(CodeAlleyConsumer.OPTION_HELP, 				false, "Description");				// 도움말
		
		String handlerName = null;
		
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
			
			// 도움말
			if(cmd.hasOption(CodeAlleyConsumer.OPTION_HELP)){
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Codealley Consumer", options);
				System.exit(0);
			}
			
			// Tool 정보
			if(cmd.hasOption(CodeAlleyConsumer.OPTION_TOOL)){
				
				handlerName = cmd.getOptionValue(CodeAlleyConsumer.OPTION_TOOL);
				if(handlerName == null){
					System.out.println("Invalid handler name");
					System.exit(0);
				}
				System.out.println(" ### tool | " + cmd.getOptionValue(CodeAlleyConsumer.OPTION_TOOL));
			}
			
			// RabbitMQ HOST정보
			if(cmd.hasOption(CodeAlleyConsumer.OPTION_RABBITMQ_HOST))
				System.out.println(" ### RabbitMQ HOST | " + cmd.getOptionValue(CodeAlleyConsumer.OPTION_RABBITMQ_HOST));
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			System.exit(0);
		}
		
		
		RabbitMqManager rabbitMqManager = RabbitMqManager.connectRabbitMqManager();
		
		
		// Tool 핸들러 선택
		SubscriptionDeliveryHandler handler = null;
		
		if(handlerName.equalsIgnoreCase(CodeAlleyConsumer.TOOL_REDMINE))		
		{
			handler = new RedmineHandler(cmd);
		}
		else if(handlerName.equalsIgnoreCase(CodeAlleyConsumer.TOOL_REVIEWBOARD))
		{
			handler = new ReviewboardHandler();
		}
		else
		{
			System.out.println("Not found tool handler : " + handlerName);
			System.exit(0);
		}
		
		rabbitMqManager.createSubscription(QUEUE_JENKINS_REDMINE, handler);
		rabbitMqManager.start();
		
	}

}
