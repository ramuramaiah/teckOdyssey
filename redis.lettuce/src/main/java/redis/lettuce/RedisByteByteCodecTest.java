package redis.lettuce;

import java.util.Map;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;

public class RedisByteByteCodecTest {
	public static StatefulRedisConnection<byte[], byte[]> getRedisConn() {
		RedisURI redisUri = RedisURI.Builder.redis("localhost", 6379).withDatabase(1).build();
		RedisClient redisClient = RedisClient.create(redisUri);
		StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE);
		return connection;
	}
	
	public static void testSyncCommands(StatefulRedisConnection<byte[], byte[]> connection) {
		RedisCommands<byte[], byte[]> syncCommands = connection.sync();
		syncCommands.set("key".getBytes(), "Hello, Redis!".getBytes());
		String value = new String(syncCommands.get("key".getBytes()));
		System.out.println("The value for key: \"key\" is: " + value);
		syncCommands.del("key".getBytes());

		syncCommands.hset("recordName".getBytes(), "FirstName".getBytes(), "John".getBytes());
		syncCommands.hset("recordName".getBytes(), "LastName".getBytes(), "Smith".getBytes());
		Map<byte[], byte[]> record = syncCommands.hgetall("recordName".getBytes());
		System.out.println("record: ");
		record.forEach((byte[] b1, byte[] b2) -> {
			System.out.println(new String(b1) + ": " + new String(b2));
		});
		syncCommands.del("recordName".getBytes());
	}
	
	public static void main(String[] args) {
		StatefulRedisConnection<byte[], byte[]> redisConn = getRedisConn();
		testSyncCommands(redisConn);
		redisConn.close();
	}
}
