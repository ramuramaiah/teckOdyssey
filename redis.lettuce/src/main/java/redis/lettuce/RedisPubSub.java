package redis.lettuce;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class RedisPubSub {
	RedisClient redisClient;
	BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
	private static final String DONE = "done";
	public static StatefulRedisPubSubConnection<String, String> subConnection;

	public class Listener implements RedisPubSubListener<String, String>, Runnable {

		@Override
		public void message(String channel, String message) {
			System.out.println("Got message: " + message + " in channel: " + channel);
			try {
				queue.put(message);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void message(String pattern, String channel, String message) {
			// TODO Auto-generated method stub

		}

		@Override
		public void subscribed(String channel, long count) {
			// TODO Auto-generated method stub

		}

		@Override
		public void psubscribed(String pattern, long count) {
			// TODO Auto-generated method stub

		}

		@Override
		public void unsubscribed(String channel, long count) {
			// TODO Auto-generated method stub

		}

		@Override
		public void punsubscribed(String pattern, long count) {
			// TODO Auto-generated method stub

		}

		@Override
		public void run() {
			while (true) {
				try {
					String message = queue.take();
					if (DONE.equals(message)) {
						System.out.println("Shutting down the pub-sub listener, received " + message);
						subConnection.close();
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void startListener() {
		Thread listener = new Thread(new Listener());
		listener.start();
	}
	
	public void setupClient() {
		RedisURI redisUri = RedisURI.Builder.redis("localhost", 6379).withDatabase(1).build();
		redisClient = RedisClient.create(redisUri);
	}

	public void setupSubsciber() {
		subConnection = redisClient.connectPubSub();
		subConnection.addListener(new Listener());

		RedisPubSubAsyncCommands<String, String> async = subConnection.async();
		async.subscribe("channel");
	}

	public void Publish(String message) {
		StatefulRedisPubSubConnection<String, String> pubConnection = redisClient.connectPubSub();

		RedisPubSubAsyncCommands<String, String> async = pubConnection.async();
		async.publish("channel", message);
		pubConnection.close();
	}

	public static void main(String[] args) {
		RedisPubSub redisPubSub = new RedisPubSub();
		redisPubSub.setupClient();
		redisPubSub.setupSubsciber();
		redisPubSub.startListener();
		redisPubSub.Publish("Hello Redis!");
		for (int i=0; i<10; i++) {
			redisPubSub.Publish(String.valueOf(i));
		}
		redisPubSub.Publish(DONE);
	}
}
