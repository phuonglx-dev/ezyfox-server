package com.tvd12.ezyfoxserver.nio.handler;

import static com.tvd12.ezyfoxserver.util.EzyProcessor.processWithLogException;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import com.tvd12.ezyfoxserver.builder.EzyBuilder;
import com.tvd12.ezyfoxserver.constant.EzyDisconnectReason;
import com.tvd12.ezyfoxserver.context.EzyServerContext;
import com.tvd12.ezyfoxserver.nio.delegate.EzySocketChannelDelegate;
import com.tvd12.ezyfoxserver.nio.entity.EzyChannel;
import com.tvd12.ezyfoxserver.nio.entity.EzyNioSession;
import com.tvd12.ezyfoxserver.nio.socket.EzySessionTicketsQueue;
import com.tvd12.ezyfoxserver.util.EzyDestroyable;
import com.tvd12.ezyfoxserver.util.EzyLoggable;

public abstract class EzyAbstractHandlerGroup
		<
			D extends EzyDestroyable,
			E extends EzyNioDataEncoder
		>
		extends EzyLoggable
		implements EzySocketChannelDelegate {

	protected final EzyChannel channel;
	
	protected D decoder;
	protected E encoder;
	protected final EzyNioDataHandler handler;
	
	protected final ExecutorService statsThreadPool;
	protected final ExecutorService codecThreadPool;
	protected final ExecutorService handlerThreadPool;
	
	protected final AtomicReference<EzyNioSession> session;
	protected final EzySocketChannelDelegate channelDelegate;
	protected final EzySessionTicketsQueue sessionTicketsQueue;
	
	public EzyAbstractHandlerGroup(Builder builder) {
		this.session = new AtomicReference<EzyNioSession>();
		this.channel = builder.channel;
		this.statsThreadPool = builder.statsThreadPool;
		this.codecThreadPool = builder.codecThreadPool;
		this.handlerThreadPool = builder.handlerThreadPool;
		this.channelDelegate = builder.channelDelegate;
		this.sessionTicketsQueue = builder.sessionTicketsQueue;
		
		this.decoder = newDecoder(builder.decoder);
		this.encoder = newEncoder(builder.encoder);
		this.handler = newDataHandler(builder.serverContext);
	}
	
	protected abstract D newDecoder(Object decoder);
	
	protected abstract E newEncoder(Object encoder);
	
	private EzyNioDataHandler newDataHandler(EzyServerContext context) {
		EzySimpleNioDataHandler handler = new EzySimpleNioDataHandler(channel);
		handler.setContext(context);
		handler.setChannelDelegate(this);
		return handler;
	}
	
	public final void fireChannelInactive() {
		try {
			decoder.destroy();
			handler.channelInactive();
		}
		catch(Exception e) {
			getLogger().error("handler inactive error", e);
		}
	}
	
	public final void fireChannelInactive(EzyDisconnectReason reason) {
		try {
			decoder.destroy();
			handler.channelInactive(reason);
		}
		catch(Exception e) {
			getLogger().error("handler inactive error", e);
		}
	}
	
	public final void fireExceptionCaught(Throwable throwable) {
		try {
			handler.exceptionCaught(throwable);
		}
		catch(Exception e) {
			fireChannelInactive();
		}
	}
	
	public final void fireDataSend(Object data) throws Exception {
		executeHandleSendData(data);
	}
	
	public final EzyNioSession fireChannelActive() throws Exception {
		EzyNioSession ss = handler.channelActive();
		ss.setSessionTicketsQueue(sessionTicketsQueue);
		session.set(ss);
		return ss;
	}
	
	@Override
	public final void onChannelInactivated(EzyChannel channel) {
		channelDelegate.onChannelInactivated(channel);
	}
	
	private void executeHandleSendData(Object data) {
		CompletableFuture<Object> encodeFuture =
				supplyAsync(() -> encodeData0(data), codecThreadPool);
		CompletableFuture<Void> sendBytesFuture = encodeFuture
				.thenAcceptAsync(bytes -> sendBytesToClient(bytes), handlerThreadPool);
		processWithLogException(sendBytesFuture::get);
	}
	
	private Object encodeData0(Object data) {
		try {
			return encodeData(data);
		}
		catch(Exception e) {
			getLogger().error("decode data error on session: " + getSession().getClientAddress(), e);
			return null;
		}
	}
	
	protected abstract Object encodeData(Object data) throws Exception;
	
	private void sendBytesToClient(Object bytes) {
		try {
			EzyChannel channel = getSession().getChannel();
			if(bytes != null && channel != null && channel.isConnected()) {
				int writeBytes = channel.write(bytes);
				executeAddWrittenBytes(writeBytes);
			}
		}
		catch(Exception e) {
			getLogger().error("can't send bytes: " + bytes + " to session: " + getSession().getClientAddress(), e);
		}
	}
	
	protected final void executeAddReadBytes(int bytes) {
		statsThreadPool.execute(() -> addReadBytes(bytes));
	}
	
	private void executeAddWrittenBytes(int bytes) {
		statsThreadPool.execute(() -> addWrittenBytes(bytes));
	}
	
	private synchronized void addReadBytes(int count) {
		getSession().addReadBytes(count);
	}
	
	private synchronized void addWrittenBytes(int count) {
		getSession().addWrittenBytes(count);
	}
	
	protected final EzyNioSession getSession() {
		return session.get();
	}
	
	public static abstract class Builder implements EzyBuilder<EzyHandlerGroup> {

		private EzyChannel channel;
		
		private ExecutorService statsThreadPool;
		private ExecutorService codecThreadPool;
		private ExecutorService handlerThreadPool;

		private Object decoder;
		private EzyNioObjectToByteEncoder encoder;
		private EzyServerContext serverContext;
		private EzySocketChannelDelegate channelDelegate;
		private EzySessionTicketsQueue sessionTicketsQueue;
		
		public Builder channel(EzyChannel channel) {
			this.channel = channel;
			return this;
		}
		
		public Builder decoder(Object decoder) {
			this.decoder = decoder;
			return this;
		}
		
		public Builder encoder(Object encoder) {
			this.encoder = (EzyNioObjectToByteEncoder) encoder;
			return this;
		}
		
		public Builder codecThreadPool(ExecutorService codecThreadPool) {
			this.codecThreadPool = codecThreadPool;
			return this;
		}
		
		public Builder handlerThreadPool(ExecutorService handlerThreadPool) {
			this.handlerThreadPool = handlerThreadPool;
			return this;
		}
		
		public Builder statsThreadPool(ExecutorService statsThreadPool) {
			this.statsThreadPool = statsThreadPool;
			return this;
		}
		
		public Builder serverContext(EzyServerContext serverContext) {
			this.serverContext = serverContext;
			return this;
		}
		
		public Builder channelDelegate(EzySocketChannelDelegate delegate) {
			this.channelDelegate = delegate;
			return this;
		}
		
		public Builder sessionTicketsQueue(EzySessionTicketsQueue queue) {
			this.sessionTicketsQueue = queue;
			return this;
		}
	}
	
}
