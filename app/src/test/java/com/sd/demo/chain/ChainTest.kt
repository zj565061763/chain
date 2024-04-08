package com.sd.demo.chain

import android.os.Looper
import com.sd.lib.chain.FChain
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChainTest {

    @Before
    fun setUp() {
        val looper = mockkClass(Looper::class)
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns looper
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test add node`() {
        val chain = FChain()

        val node = newChainNode()
        chain.add(node)

        runCatching {
            chain.add(node)
        }.let { result ->
            assertEquals("Node has been initialized.", result.exceptionOrNull()!!.message)
        }
    }
}

private fun newChainNode(
    onCancel: FChain.Node.() -> Unit = {},
    onFinish: FChain.Node.() -> Unit = {},
    onRun: FChain.Node.() -> Unit = {},
): FChain.Node {
    return object : FChain.Node() {
        override fun onRun() {
            onRun.invoke(this)
            nextNode()
        }

        override fun onCancel() {
            onCancel.invoke(this)
        }

        override fun onFinish() {
            onFinish.invoke(this)
        }
    }
}