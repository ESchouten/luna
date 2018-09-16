package io.luna.net.codec.login;

import io.luna.LunaContext;
import io.luna.net.codec.ByteMessage;
import io.luna.net.codec.IsaacCipher;
import io.luna.net.codec.ProgressiveMessageDecoder;
import io.luna.net.msg.GameMessageRepository;
import io.luna.net.client.Client;
import io.luna.net.client.LoginClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;
import static io.luna.LunaConstants.RSA_EXPONENT;
import static io.luna.LunaConstants.RSA_MODULUS;

/**
 * A {@link ByteToMessageDecoder} implementation that decodes a {@link LoginCredentialsMessage}.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class LoginDecoder extends ProgressiveMessageDecoder<LoginDecoder.DecodeState> {

    // TODO Protocol documentation.

    /**
     * An enumerated type representing login decoder states.
     */
    enum DecodeState {
        HANDSHAKE,
        LOGIN_TYPE,
        RSA_BLOCK
    }

    /**
     * A cryptographically secure RNG.
     */
    private static final Random RANDOM = new SecureRandom();

    /**
     * The size of the RSA block.
     */
    private int rsaBlockSize;

    /**
     * The context instance.
     */
    private final LunaContext context;

    /**
     * The message repository.
     */
    private final GameMessageRepository repository;

    /**
     * Creates a new {@link LoginDecoder}.
     *
     * @param context The context instance.
     * @param repository The message repository.
     */
    public LoginDecoder(LunaContext context, GameMessageRepository repository) {
        super(DecodeState.HANDSHAKE);
        this.context = context;
        this.repository = repository;
    }

    @Override
    protected Object decodeMsg(ChannelHandlerContext ctx, ByteBuf in, DecodeState state) {
        switch (state) {
            case HANDSHAKE:
                Attribute<Client<?>> attribute = ctx.channel().attr(Client.KEY);
                attribute.set(new LoginClient(ctx.channel(), context, repository));

                decodeHandshake(ctx, in);
                break;
            case LOGIN_TYPE:
                decodeLoginType(ctx, in);
                break;
            case RSA_BLOCK:
                return decodeRsaBlock(ctx, in);

        }
        return null;
    }

    @Override
    protected void resetState() {
        rsaBlockSize = 0;
    }

    /**
     * Decodes the handshake.
     *
     * @param ctx The channel handler context.
     * @param in The buffer to read data from.
     */
    private void decodeHandshake(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() >= 2) {
            int opcode = in.readUnsignedByte(); // TODO Ondemand?

            @SuppressWarnings("unused") int nameHash = in.readUnsignedByte();

            checkState(opcode == 14, "opcode != 14");

            ByteBuf buf = ctx.alloc().buffer(17);
            buf.writeLong(0);
            buf.writeByte(0);
            buf.writeLong(RANDOM.nextLong());
            ctx.writeAndFlush(buf);

            checkpoint(DecodeState.LOGIN_TYPE);
        }
    }

    /**
     * Decodes the login type and RSA block size.
     *
     * @param ctx The channel handler context.
     * @param in The buffer to read data from.
     */
    private void decodeLoginType(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() >= 2) {
            int loginType = in.readUnsignedByte();
            checkState(loginType == 16 || loginType == 18, "loginType != 16 or 18");

            rsaBlockSize = in.readUnsignedByte();
            checkState((rsaBlockSize - 40) > 0, "(rsaBlockSize - 40) <= 0");

            checkpoint(DecodeState.RSA_BLOCK);
        }
    }

    /**
     * Decodes the RSA block.
     *
     * @param ctx The channel handler context.
     * @param in The buffer to read data from.
     * @return The decoded login response message.
     */
    private Object decodeRsaBlock(ChannelHandlerContext ctx, ByteBuf in) {
        if (in.readableBytes() >= rsaBlockSize) {

            int magicId = in.readUnsignedByte();
            checkState(magicId == 255, "magicId != 255");

            int clientVersion = in.readUnsignedShort();
            checkState(clientVersion == 317, "clientVersion != 317");

            @SuppressWarnings("unused") int memoryVersion = in.readUnsignedByte();

            for (int i = 0; i < 9; i++) {
                in.readInt();
            }

            int expectedSize = in.readUnsignedByte();
            rsaBlockSize -= 41;
            checkState(expectedSize == rsaBlockSize, "expectedSize != rsaBlockSize");

            byte[] rsaBytes = new byte[rsaBlockSize];
            in.readBytes(rsaBytes);

            ByteBuf rsaBuffer = ctx.alloc().buffer();
            try {
                rsaBuffer.writeBytes(new BigInteger(rsaBytes).modPow(RSA_EXPONENT, RSA_MODULUS).toByteArray());

                int rsaOpcode = rsaBuffer.readUnsignedByte();
                checkState(rsaOpcode == 10, "rsaOpcode != 10");

                long clientHalf = rsaBuffer.readLong();
                long serverHalf = rsaBuffer.readLong();

                int[] isaacSeed = {(int) (clientHalf >> 32), (int) clientHalf, (int) (serverHalf >> 32),
                        (int) serverHalf};

                IsaacCipher decryptor = new IsaacCipher(isaacSeed);
                for (int i = 0; i < isaacSeed.length; i++) {
                    isaacSeed[i] += 50;
                }
                IsaacCipher encryptor = new IsaacCipher(isaacSeed);

                @SuppressWarnings("unused") int uid = rsaBuffer.readInt();

                ByteMessage msg = ByteMessage.wrap(rsaBuffer);
                String username = msg.getString().toLowerCase();
                String password = msg.getString().toLowerCase();

                return new LoginCredentialsMessage(username,
                        password, encryptor, decryptor, ctx.channel().pipeline());
            } finally {
                rsaBuffer.release();
            }
        }
        return null;
    }

}
