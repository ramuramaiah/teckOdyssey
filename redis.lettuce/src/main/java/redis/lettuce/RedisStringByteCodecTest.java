package redis.lettuce;

import java.util.Map;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisStringByteCodecTest {
	public static StatefulRedisConnection<String, byte[]> getRedisConn() {
		RedisURI redisUri = RedisURI.Builder.redis("localhost", 6379).withDatabase(1).build();
		RedisClient redisClient = RedisClient.create(redisUri);
		StatefulRedisConnection<String, byte[]> connection = redisClient.connect(ByteArrayValueCodec.INSTANCE);
		return connection;
	}
	
	public static void testSyncCommands(StatefulRedisConnection<String, byte[]> connection) {
		RedisCommands<String, byte[]> syncCommands = connection.sync();
		syncCommands.set("key", "Hello, Redis!".getBytes());
		String value = new String(syncCommands.get("key"));
		System.out.println("The value for key: \"key\" is: " + value);
		syncCommands.del("key");

		syncCommands.hset("recordName", "FirstName", "John".getBytes());
		syncCommands.hset("recordName", "LastName", "Smith".getBytes());
		Map<String, byte[]> record = syncCommands.hgetall("recordName");
		System.out.println("record: ");
		record.forEach((String b1, byte[] b2) -> {
			System.out.println(b1 + ": " + new String(b2));
		});
		syncCommands.del("recordName");
	}
	
	public static void main(String[] args) {
		StatefulRedisConnection<String, byte[]> redisConn = getRedisConn();
		testSyncCommands(redisConn);
		redisConn.close();
	}
}
