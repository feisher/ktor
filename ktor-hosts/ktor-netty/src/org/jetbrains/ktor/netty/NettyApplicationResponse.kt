package org.jetbrains.ktor.netty

import io.netty.channel.*
import io.netty.handler.codec.http.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import org.jetbrains.ktor.cio.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.response.*
import kotlin.coroutines.experimental.*

internal abstract class NettyApplicationResponse(call: NettyApplicationCall,
                                        protected val context: ChannelHandlerContext,
                                        protected val hostCoroutineContext: CoroutineContext,
                                        protected val userCoroutineContext: CoroutineContext) : BaseApplicationResponse(call) {

    internal val responseMessage = CompletableDeferred<Any>()

    @Volatile
    protected var responseMessageSent = false

    internal var responseChannel: ByteReadChannel = EmptyByteReadChannel

    init {
        pipeline.intercept(ApplicationSendPipeline.Host) {
            call.finish()
        }
    }

    suspend override fun respondFinalContent(content: FinalContent) {
        try {
            super.respondFinalContent(content)
        } catch (t: Throwable) {
            val out = responseChannel as? ByteWriteChannel
            if (out != null) out.close(t)
        } finally {
            val out = responseChannel as? ByteWriteChannel
            if (out != null) out.close()
        }
    }

    suspend override fun respondFromBytes(bytes: ByteArray) {
        // Note that it shouldn't set HttpHeaders.ContentLength even if we know it here,
        // because it should've been set by commitHeaders earlier
        sendResponse(chunked = false, content = ByteReadChannel(bytes))
    }

    override suspend fun responseChannel(): WriteChannel {
        val channel = ByteChannel()
        sendResponse(content = channel)
        return CIOWriteChannelAdapter(channel)
    }

    protected abstract fun responseMessage(chunked: Boolean, last: Boolean): Any

    protected final suspend fun sendResponse(chunked: Boolean = true, content: ByteReadChannel) {
        if (!responseMessageSent) {
            responseChannel = content
            responseMessage.complete(responseMessage(chunked, content.isClosedForRead))
            responseMessageSent = true
        }
    }

    suspend final fun close() {
        sendResponse(content = EmptyByteReadChannel) // we don't need to suspendAwait() here as it handled in NettyApplicationCall
         // while close only does flush() and doesn't terminate connection
    }

    companion object {
        val responseStatusCache = HttpStatusCode.allStatusCodes.associateBy({ it.value }, { HttpResponseStatus.valueOf(it.value) })
    }
}