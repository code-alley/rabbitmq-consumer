package com.inslab.tool;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.json.JSONObject;

import com.inslab.amqp.CodeAlleyConsumer;
import com.inslab.amqp.SubscriptionDeliveryHandler;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.LongString;

public class RedmineHandler implements SubscriptionDeliveryHandler{
	
	protected final Logger LOGGER = Logger.getLogger(getClass().getName());
	
	private final String DBPOOL_AUTH_USER 	= "USERNAME";
	private final String DBPOOL_AUTH_PASSWD = "PASSWORD";
	
	//private final String CODEALLEY_HOST		= "codealley.co/redmine";
	private final String CODEALLEY_HOST		= "cloudapp.net/redmine";
	
	private final String KEY_PROJECT_ID 	= "project_id";
	private final String KEY_NOTES 			= "notes";
	private final String KEY_ISSUE_ID 		= "issue_id";
	private final String KEY_SUB_DOMAIN 	= "subdomain";
	
	private CommandLine m_command;
	
	private String m_host;		// Redmine host
	private String m_user;		// Redmine API auth user
	private String m_password;  // Redmine API auth password
	
	private String m_dbpHost;	// Databasepool service host
	
	public RedmineHandler(CommandLine cmd) {
		this.m_command = cmd;
		
		// ### 커맨드 옵션 체크
	    if(cmd.hasOption(CodeAlleyConsumer.OPTION_TOOL_HOST))
	    {	m_host = cmd.getOptionValue(CodeAlleyConsumer.OPTION_TOOL_HOST);
	        //m_host = String.format("%s/issues.json", m_host); 이슈등록
	    
	        
	        if(!m_host.contains("http://"))
	        	m_host = "http://" + m_host;
	    }
	    
	    if(cmd.hasOption(CodeAlleyConsumer.OPTION_TOOL_USER))
	    	m_user = cmd.getOptionValue(CodeAlleyConsumer.OPTION_TOOL_USER);
	    
	    if(cmd.hasOption(CodeAlleyConsumer.OPTION_TOOL_PASSWD))
	    	m_password = cmd.getOptionValue(CodeAlleyConsumer.OPTION_TOOL_PASSWD);
	    
	    if(cmd.hasOption(CodeAlleyConsumer.OPTION_DBPOOL_HOST))
	    	m_dbpHost = cmd.getOptionValue(CodeAlleyConsumer.OPTION_DBPOOL_HOST);
	    
	    
	}

	@Override
	public void handleDelivery(Channel channel, Envelope envelope,
			BasicProperties properties, byte[] body) {
		
		/**
		 * Exchange	jenkins-result
			Routing Key	 : org.jenkinsci.plugins.rabbitmqbuildtrigger
			Redelivered	 : ○
			Properties	
				app_id:	remote-build
				headers:	
					jenkins-url:	http://catool-jenkins.cloudapp.net:8080/
					content_type:	application/json
				Payload	52 bytes	Encoding: string
					{"project":"test_job","number":4,"status":"SUCCESS"}
		 */
		
		LOGGER.info("handleDelivery > message body : " + new String(body));
		
		JSONObject obj = new JSONObject(new String(body));
		LongString jenkinsUrl = (LongString) properties.getHeaders().get("jenkins-url");
		
		JSONObject flow = checkRedmineFlow(obj, jenkinsUrl);
		if(flow == null || flow.getJSONArray("result").length() == 0)
		{
			LOGGER.info("not found redmine flow. " + obj.getString("project"));
			return;
		}
		// RabbitMQ 메시지를 파싱하여 Redmine에 이슈를 등록한다.
		// 저장소명, 커밋로그
		
		// Redmine REST API사용시 id / passwd 필요(basicAuth) or API Key
		addComment(flow, jenkinsUrl, obj.getInt("number"), obj.getString("status"));
	}

	private JSONObject checkRedmineFlow(JSONObject obj, LongString jenkinsUrl) {
		
		HttpResponse<JsonNode> response = null;
		JSONObject result = null;
		try {
			
			String jobName = obj.getString("project");
			//jobName = "test_job";
			//jenkinsUrl 끝에 '/' 제거?
			String jenkins = jenkinsUrl.toString();
			if(jenkins.endsWith("/")){
				jenkins = jenkins.substring(0, jenkins.length()-1);
			}
			
			/**
			 * test
			 */
			jenkins = "http://tester.codealley.co/jenkins";	
			//m_dbpHost = "http://localhost:8080/databasepool/v1/query";

			
			String query = String.format("select * from v_workflow_pm where ci_url = '%s' and ci_job_name = '%s';", jenkins, jobName);
			response = Unirest.post(m_dbpHost)
					.basicAuth(DBPOOL_AUTH_USER, DBPOOL_AUTH_PASSWD)
					.body(query)
					.asJson();
			
			result = response.getBody().getObject();
			
			LOGGER.info(result.toString());
			
			if(result.has("error"))
			{	
				LOGGER.info(result.getString("error"));
				result = null;
			}
				
			


		} catch (UnirestException e) {
			e.printStackTrace();
			result = null;
		} 
		
		return result;
	}

	private void addComment(JSONObject obj, LongString jenkinsUrl, int buildNumber, String status) {
		
		
	
		/**
		 ### Redmine 이슈등록 샘플		
		{
			  "issue": {
			    "project_id": 1, 
			    "subject": "새로운 이슈", 
			    "priority_id": 4 
			  }
		}
		  tracker_id ==>  1 (Bug) / 2 (Feature) /  
		 
		 ### comment 등록 샘플
		 http://catool-redmine.cloudapp.net/redmine/issues/20.json
		 	{
			  "issue": {
			    "notes": "The subject was changed" 
			  }
			}
		 */
		
		JSONObject body = new JSONObject();
		JSONObject issue = new JSONObject();
		
		/* 이슈 등록
		issue.put("project_id", project_id);
		issue.put("tracker_id", tracker_id);	
		issue.put("subject", String.format("from codealley consumer (project_id:%d traker_id:%d)",project_id, tracker_id));
		*/
		
		/** 
		 * {"result":[{"id":2,"project":"test-project","ci_job_name":"test_job",
		 * "ci_url":"http://tester.codealley.co/jenkins","project_id":"1",
		 * "domain":"tester","issue_id":"1","url":"http://tester.codealley.co/redmine"}]}
		 */
		
		JSONObject redmineObj = (JSONObject) obj.getJSONArray("result").get(0);
		String redmineHost = redmineObj.getString("url");
		if(!redmineHost.startsWith("http://"))
			redmineHost = "http://" + redmineHost;
		String apiKey = redmineObj.getString("api_key");
		String notes = String.format("jenkins : %s\n project : %s\n build : #%d %s", jenkinsUrl, redmineObj.getString("project"), buildNumber, status); 
		int issueId = redmineObj.getInt("issue_id");
		/**
		 * test
		 */
		redmineHost = "http://catool-redmine.cloudapp.net/redmine";
		apiKey 		= "API_KEY";
		
		/* Comment 등록 */
		m_host = String.format("%s/issues/%d.json", redmineHost, issueId);
		issue.put("notes", notes);
		
		body.put("issue", issue);
		
		HttpResponse<JsonNode> response = null;
		try {
			//response = Unirest.get(host).basicAuth(authUserId, authPassWd).asJson();
			response = Unirest.put(m_host)
					.header("X-Redmine-API-Key", apiKey)
					//.basicAuth(m_user, m_password)
					.header("Content-Type", "application/json")
					.body(body.toString())
					.asJson();
			
			JSONObject result = response.getBody().getObject();
			
			if(response.getStatus() == 201)
				LOGGER.log(Level.INFO, " ## Result : " + result.toString());
			else
				LOGGER.log(Level.SEVERE, " ## Status : " + response.getStatusText());

		} catch (UnirestException e) {
			e.printStackTrace();
		}
		
		
	}

}
