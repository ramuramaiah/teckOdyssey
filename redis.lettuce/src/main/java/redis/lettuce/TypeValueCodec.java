package redis.lettuce;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import io.lettuce.core.codec.RedisCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public class TypeValueCodec<T> implements RedisCodec<String, T> {

	private static final byte[] EMPTY = new byte[0];
	private final Charset charset = Charset.defaultCharset();
	private RedisValuesCodable<T> type;

	public TypeValueCodec(RedisValuesCodable<T> type) {
		this.type = type;
	}
	
	@Override
	public String decodeKey(ByteBuffer bytes) {
		return Unpooled.wrappedBuffer(bytes).toString(charset);
	}

	@Override
	public T decodeValue(ByteBuffer bytes) {
		byte[] ba = getBytes(bytes);
		return type.decode(ba);
	}

	@Override
	public ByteBuffer encodeKey(String key) {
		return encodeAndAllocateBuffer(key);
	}

	@Override
	public ByteBuffer encodeValue(T value) {
		byte[] ba = type.encode(value);
		return ByteBuffer.wrap(ba);
	}

	private byte[] getBytes(ByteBuffer buffer) {
		int remaining = buffer.remaining();

		if (remaining == 0) {
			return EMPTY;
		}

		byte[] b = new byte[remaining];
		buffer.get(b);
		return b;
	}

	private ByteBuffer encodeAndAllocateBuffer(String key) {
		if (key == null) {
			return ByteBuffer.wrap(EMPTY);
		}

		CharsetEncoder encoder = CharsetUtil.encoder(charset);
		ByteBuffer buffer = ByteBuffer.allocate((int) (encoder.maxBytesPerChar() * key.length()));

		ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
		byteBuf.clear();
		encode(key, byteBuf);
		buffer.limit(byteBuf.writerIndex());

		return buffer;
	}

	private void encode(String str, ByteBuf target) {
		if (str == null) {
			return;
		}

		CharsetEncoder encoder = CharsetUtil.encoder(charset);
		int length = (int) ((double) str.length() * encoder.maxBytesPerChar());
		target.ensureWritable(length);
		try {
			final ByteBuffer dstBuf = target.nioBuffer(0, length);
			final int pos = dstBuf.position();
			CoderResult cr = encoder.encode(CharBuffer.wrap(str), dstBuf, true);
			if (!cr.isUnderflow()) {
				cr.throwException();
			}
			cr = encoder.flush(dstBuf);
			if (!cr.isUnderflow()) {
				cr.throwException();
			}
			target.writerIndex(target.writerIndex() + dstBuf.position() - pos);
		} catch (CharacterCodingException x) {
			throw new IllegalStateException(x);
		}
	}
}
