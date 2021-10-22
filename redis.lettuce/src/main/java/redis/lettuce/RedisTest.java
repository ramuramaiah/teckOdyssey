package redis.lettuce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisTest {
	public static StatefulRedisConnection<String, String> getRedisConn() {
		RedisURI redisUri = RedisURI.Builder.redis("localhost", 6379).withDatabase(1).build();
		RedisClient redisClient = RedisClient.create(redisUri);
		StatefulRedisConnection<String, String> connection = redisClient.connect();
		return connection;
	}

	public static void testSyncCommands(StatefulRedisConnection<String, String> connection) {
		RedisCommands<String, String> syncCommands = connection.sync();
		syncCommands.set("key", "Hello, Redis!");
		String value = syncCommands.get("key");
		System.out.println("The value for key: \"key\" is: " + value);
		syncCommands.del("key");

		syncCommands.hset("recordName", "FirstName", "John");
		syncCommands.hset("recordName", "LastName", "Smith");
		Map<String, String> record = syncCommands.hgetall("recordName");
		System.out.println("record: " + record);
		syncCommands.del("recordName");
	}

	public static void testASyncCommands(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();
		asyncCommands.set("key", "Hello, Redis!");
		RedisFuture<String> result = asyncCommands.get("key");
		try {
			System.out.println("The value for key: \"key\" is: " + result.get());
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		asyncCommands.del("key");
	}

	public static void testLists(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();

		asyncCommands.lpush("tasks", "firstTask");
		asyncCommands.lpush("tasks", "secondTask");

		// Note: Lpush pushes values to the head of the list

		try {
			RedisFuture<String> redisFuture = asyncCommands.lpop("tasks");
			String task = redisFuture.get();
			System.out.println("tasks: " + task);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		asyncCommands.del("tasks");
	}

	public static void testSets(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();

		asyncCommands.sadd("pets", "dog");
		asyncCommands.sadd("pets", "cat");
		asyncCommands.sadd("pets", "cat");

		try {
			RedisFuture<Set<String>> pets = asyncCommands.smembers("pets");
			System.out.println("The size of the set \"pets\" is: " + pets.get().size());

			RedisFuture<Boolean> exists = asyncCommands.sismember("pets", "dog");
			System.out.println("Does \"dog\" exists in the set \"pets\" ? :" + exists.get());
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		asyncCommands.del("pets");

	}

	public static void testHashes(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();

		asyncCommands.hset("recordName", "FirstName", "John");
		asyncCommands.hset("recordName", "LastName", "Smith");

		RedisFuture<Map<String, String>> record = asyncCommands.hgetall("recordName");

		try {
			System.out.println("record: " + record.get());
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		asyncCommands.del("recordName");
	}

	public static void testSortedSets(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();

		asyncCommands.zadd("sortedset", 1, "one-1");
		asyncCommands.zadd("sortedset", 1, "one-2");
		asyncCommands.zadd("sortedset", 4, "zero");
		asyncCommands.zadd("sortedset", 2, "two");

		RedisFuture<List<String>> valuesForward = asyncCommands.zrange("sortedset", 0, -1);
		RedisFuture<List<String>> valuesReverse = asyncCommands.zrevrange("sortedset", 0, -1);

		try {
			System.out.println("zrange: " + valuesForward.get());
			System.out.println("zrevrange: " + valuesReverse.get());
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		asyncCommands.del("sortedset");
	}

	public static void testTransactions(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();

		// Starts the txn
		asyncCommands.multi();

		List<RedisFuture<String>> setResults = new ArrayList<>();

		setResults.add(asyncCommands.set("key1", "value1"));
		setResults.add(asyncCommands.set("key2", "value2"));
		setResults.add(asyncCommands.set("key3", "value3"));

		// Executes the txn
		RedisFuture<TransactionResult> execResult = asyncCommands.exec();

		System.out.println("Transaction Queuing Results:");
		setResults.stream().map((RedisFuture<String> f) -> {
			try {
				return f.get();
			} catch (InterruptedException | ExecutionException e1) {
				return Optional.ofNullable(null);
			}
		}).forEach(System.out::println);

		TransactionResult transactionResult;
		try {
			transactionResult = execResult.get();

			System.out.println("Transaction Results:");
			transactionResult.stream().forEach(System.out::println);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Starts the txn
		asyncCommands.multi();

		asyncCommands.del("key1");
		asyncCommands.del("key2");
		asyncCommands.del("key3");

		// Executes the txn
		execResult = asyncCommands.exec();
	}

	public static void testBatching(StatefulRedisConnection<String, String> connection) {
		RedisAsyncCommands<String, String> asyncCommands = connection.async();

		asyncCommands.setAutoFlushCommands(false);

		int iterations = 10;
		List<RedisFuture<String>> futures = new ArrayList<>();
		for (int i = 0; i < iterations; i++) {
			futures.add(asyncCommands.set("key-" + i, "value-" + i));
		}
		asyncCommands.flushCommands();

		boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[0]));

		futures.clear();

		for (int i = 0; i < iterations; i++) {
			futures.add(asyncCommands.get("key-" + i));
		}
		asyncCommands.flushCommands();

		result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[0]));
		futures.stream().map((RedisFuture<String> f) -> {
			try {
				return f.get();
			} catch (InterruptedException | ExecutionException e1) {
				return Optional.ofNullable(null);
			}
		}).forEach(System.out::println);

		// reset the auto flush to true
		asyncCommands.setAutoFlushCommands(true);

		for (int i = 0; i < iterations; i++) {
			asyncCommands.del("key-" + i);
		}
	}

	public static void main(String[] args) {
		StatefulRedisConnection<String, String> redisConn = getRedisConn();
		testSyncCommands(redisConn);
		testASyncCommands(redisConn);
		testLists(redisConn);
		testSets(redisConn);
		testHashes(redisConn);
		testSortedSets(redisConn);
		testTransactions(redisConn);
		testBatching(redisConn);
		redisConn.close();
	}
}
