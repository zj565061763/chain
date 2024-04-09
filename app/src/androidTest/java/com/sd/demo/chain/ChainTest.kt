package com.sd.demo.chain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sd.lib.chain.FChain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChainTest {

    @Test
    fun testAddNode() {
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
    fun testStart() {
        val chain = FChain()

        val node = newChainNode()
        chain.add(node)

        assertEquals(true, chain.start())
        assertEquals(false, chain.start())

        assertEquals(true, chain.isStarted())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(false, chain.isStarted())
    }

    @Test
    fun testOnFinish() {
        val events = mutableListOf<String>()

        val chain = object : FChain() {
            override fun onFinish() {
                events.add("onFinish")
            }
        }

        val node = newChainNode()
        chain.add(node)
        chain.start()

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(listOf("onFinish"), events)
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