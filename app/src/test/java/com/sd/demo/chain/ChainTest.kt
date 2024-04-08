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

    @Test
    fun `test node size`() {
        val chain = FChain()
        chain.add(newChainNode())
        chain.add(newChainNode())
        chain.add(newChainNode())
        assertEquals(3, chain.size())
    }
}

private fun newChainNode(
    onCancel: () -> Unit = {},
    onFinish: () -> Unit = {},
    onRun: () -> Unit = {},
): FChain.Node {
    return object : FChain.Node() {
        override fun onRun() {
            onRun.invoke()
        }

        override fun onCancel() {
            onCancel.invoke()
        }

        override fun onFinish() {
            onFinish.invoke()
        }
    }
}