package redis.lettuce;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.ZoneId;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisTypeValueCodecTest {

	@DefaultSerializer(PersonSerializer.class)
	public static class Person implements RedisValuesCodable<Person> {

		public static final Person INSTANCE = new Person();

		private String name;
		private long timeSinceEpoch;

		private Kryo kryo = new Kryo();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getTimeSinceEpoch() {
			return timeSinceEpoch;
		}

		public void setTimeSinceEpoch(long timeSinceEpoch) {
			this.timeSinceEpoch = timeSinceEpoch;
		}

		@Override
		public byte[] encode(Person codable) {
			Output output = new Output(4096, Integer.MAX_VALUE - 8);
			kryo.writeObject(output, codable);
			output.close();
			return output.toBytes();
		}

		public Person decode(byte[] codedValue) {
			ByteArrayInputStream bais = new ByteArrayInputStream(codedValue);
			Input input = new Input(bais);
			Person readObject = kryo.readObject(input, Person.class);
			return readObject;
		}
	}

	public static class PersonSerializer extends Serializer<Person> {

		@Override
		public void write(Kryo kryo, Output output, Person object) {
			output.writeString(object.getName());
			output.writeLong(object.getTimeSinceEpoch());
		}

		@Override
		public Person read(Kryo kryo, Input input, Class<Person> type) {
			Person person = new Person();
			person.setName(input.readString());
			person.setTimeSinceEpoch(input.readLong());
			return person;
		}
	}

	public static StatefulRedisConnection<String, Person> getRedisConn() {
		RedisURI redisUri = RedisURI.Builder.redis("localhost", 6379).withDatabase(1).build();
		RedisClient redisClient = RedisClient.create(redisUri);
		StatefulRedisConnection<String, Person> connection = redisClient
				.connect(new TypeValueCodec<Person>(Person.INSTANCE));
		return connection;
	}

	public static void testSyncCommands(StatefulRedisConnection<String, Person> connection) {
		RedisCommands<String, Person> syncCommands = connection.sync();
		Person person = new Person();
		person.setName("Ramu");
		LocalDate date = LocalDate.of(1980, 11, 15);
		ZoneId zoneId = ZoneId.systemDefault();
		long epoch = date.atStartOfDay(zoneId).toEpochSecond();
		person.setTimeSinceEpoch(epoch);
		syncCommands.set("key", person);
		Person value = syncCommands.get("key");
		System.out.println("The value for key: \"key\" is: ");
		System.out.println(value.getName());
		System.out.println(value.getTimeSinceEpoch());
		syncCommands.del("key");
	}

	public static void main(String[] args) {
		StatefulRedisConnection<String, Person> redisConn = getRedisConn();
		testSyncCommands(redisConn);
		redisConn.close();
	}
}
