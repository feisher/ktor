package org.jetbrains.ktor.netty

import io.netty.channel.*
import io.netty.util.*
import kotlinx.coroutines.experimental.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.host.*

internal abstract class NettyApplicationCall(application: Application,
                                    val context: ChannelHandlerContext,
                                    private val requestMessage: Any) : BaseApplicationCall(application) {

    override val bufferPool = NettyByteBufferPool(context)

    override abstract val request: NettyApplicationRequest
    override abstract val response: NettyApplicationResponse

    internal val responseWriteJob = Job()

    internal suspend fun finish() {
        try {
            response.close()
            responseWriteJob.join()
        } finally {
            request.close()
            ReferenceCountUtil.release(requestMessage)
        }
    }
}