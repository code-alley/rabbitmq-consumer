
package com.inslab.amqp;

import static java.util.Collections.synchronizedSet;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;

public class RabbitMqManager extends RabbitMqManagerBase 
{
	private static String QUEUE_NAME = "jenkins-result-redmine";
	private static String HOST = "catool-rabbitmq.cloudapp.net";
	public final static String VIRTUAL_HOST = "codealley";
	
    private final Set<Subscription> subscriptions;

    public static RabbitMqManager connectRabbitMqManager()
    {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("USERNAME");
        factory.setPassword("PASSWORD");
        factory.setVirtualHost("codealley");
        factory.setHost(HOST);
        //factory.setPort(5672);
        factory.setRequestedHeartbeat(3); //seconds
        
        // simulate dependency management creation and wiring
        final RabbitMqManager rabbitMqManager = new RabbitMqManager(factory);
        
//        rabbitMqManager.createSubscription(QUEUE_NAME, rabbitMqManager);
//        
//        rabbitMqManager.start();
        return rabbitMqManager;
    }
    
    public RabbitMqManager(final ConnectionFactory factory)
    {
        super(factory);
        subscriptions = synchronizedSet(new HashSet<Subscription>());
    }

    @Override
    public void start()
    {
        try
        {
            connection = factory.newConnection();
            
            connection.addShutdownListener(this);
            LOGGER.info("Connected to " + factory.getHost() + ":" + factory.getPort());
            
            restartSubscriptions();
        }
        catch (final Exception e)
        {
            LOGGER.log(Level.SEVERE, "Failed to connect to " + factory.getHost() + ":" + factory.getPort(), e);
            asyncWaitAndReconnect();
        }
    }

    protected void restartSubscriptions()
    {
        LOGGER.info("Restarting " + subscriptions.size() + " subscriptions");

        for (final Subscription subscription : subscriptions)
        {
            startSubscription(subscription);
        }
    }

    public Subscription createSubscription(final String queue, final SubscriptionDeliveryHandler handler)
    {
        final Subscription subscription = new Subscription(queue, handler);
        subscriptions.add(subscription);
        startSubscription(subscription);
        return subscription;
    }

    private void startSubscription(final Subscription subscription)
    {
        final Channel channel = createChannel();
        
        if (channel != null)
        {
            try
            {
                subscription.start(channel);
            }
            catch (final Exception e)
            {
                LOGGER.log(Level.SEVERE, "Failed to start subscription: " + subscription + " on channel: "
                                         + channel, e);
            }
        }
    }


}
