package com.inslab.tool;

import com.inslab.amqp.SubscriptionDeliveryHandler;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

public class ReviewboardHandler implements SubscriptionDeliveryHandler{

	@Override
	public void handleDelivery(Channel channel, Envelope envelope,
			BasicProperties properties, byte[] body) {
		
		createReview();
		
	}

	private void createReview() {
		// TODO Auto-generated method stub
		
	}

}
