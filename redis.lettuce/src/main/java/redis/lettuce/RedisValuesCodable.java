package redis.lettuce;

public interface RedisValuesCodable<T> {

	public byte[] encode(T codable);

	public T decode(byte[] codedValue);
}
