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

        val node = newTestNode()
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
        assertEquals(false, chain.isStarted())

        chain.add(newTestNode())

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

        chain.add(newTestNode())
        chain.start()

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(listOf("onFinish"), events)
    }

    @Test
    fun testErrorNextNode() {
        val chain = FChain()

        val node = TestErrorNextNode()

        runCatching {
            node.testRunNextNode()
        }.let { result ->
            assertEquals("Node has not been initialized.", result.exceptionOrNull()!!.message)
        }

        chain.add(node)
        runCatching {
            node.testRunNextNode()
        }.let { result ->
            assertEquals("Can not call nextNode() before onRun().", result.exceptionOrNull()!!.message)
        }
    }

    @Test
    fun testRunNodes() {
        val events = mutableListOf<String>()
        val chain = FChain()

        chain.add(newTestNode(prefix = "1", events = events))
        chain.add(newTestNode(prefix = "2", events = events))
        chain.add(newTestNode(prefix = "3", events = events))
        chain.start()

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        listOf(
            "1onRun", "1onFinish",
            "2onRun", "2onFinish",
            "3onRun", "3onFinish",
        ).let { expectedEvents ->
            assertEquals(expectedEvents, events)
        }
    }
}

private fun newTestNode(
    prefix: String = "",
    events: MutableList<String> = mutableListOf(),
    onCancel: FChain.Node .() -> Unit = {},
    onFinish: FChain.Node.() -> Unit = {},
    onRun: FChain.Node.() -> Unit = {},
): FChain.Node {
    return object : FChain.Node() {
        override fun onRun() {
            events.add("${prefix}onRun")
            onRun.invoke(this)
            nextNode()
        }

        override fun onCancel() {
            events.add("${prefix}onCancel")
            onCancel.invoke(this)
        }

        override fun onFinish() {
            events.add("${prefix}onFinish")
            onFinish.invoke(this)
        }
    }
}

private class TestErrorNextNode : FChain.Node() {

    fun testRunNextNode() {
        nextNode()
    }

    override fun onRun() {

    }
}