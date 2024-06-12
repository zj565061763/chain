package com.sd.demo.chain

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sd.lib.chain.FChain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChainTest {

    @Test
    fun testSize() {
        val chain = FChain()
        chain.add(newTestNode())
        chain.add(newTestNode())
        assertEquals(2, chain.size())
    }

    @Test
    fun testErrorAddNode() {
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
    fun testErrorNextNode() {
        val chain = FChain()

        val node = TestErrorNextNode()

        runCatching {
            node.testRunNextNode()
        }.let { result ->
            assertEquals("Node has not been initialized.", result.exceptionOrNull()!!.message)
        }

        // init node
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

        val chain = object : FChain() {
            override fun onStart() {
                events.add("onStart")
            }

            override fun onFinish() {
                events.add("onFinish")
            }
        }

        chain.add(newTestNode(prefix = "1", events = events))
        chain.add(newTestNode(prefix = "2", events = events))
        chain.add(newTestNode(prefix = "3", events = events))

        chain.start()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        listOf(
            "onStart",
            "1onRun", "1onFinish",
            "2onRun", "2onFinish",
            "3onRun", "3onFinish",
            "onFinish",
        ).let { expectedEvents ->
            assertEquals(expectedEvents, events)
        }
    }

    @Test
    fun testCancelAfterChainStart() {
        val events = mutableListOf<String>()

        val chain = object : FChain() {
            override fun onStart() {
                events.add("onStart")
            }

            override fun onFinish() {
                events.add("onFinish")
            }
        }

        chain.add(newTestNode(prefix = "1", events = events))
        chain.add(newTestNode(prefix = "2", events = events))
        chain.add(newTestNode(prefix = "3", events = events))

        chain.start()
        chain.cancel()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        listOf(
            "onStart",
            "onFinish",
        ).let { expectedEvents ->
            assertEquals(expectedEvents, events)
        }
    }

    @Test
    fun testCancelChainOnStart() {
        val events = mutableListOf<String>()

        val chain = object : FChain() {
            override fun onStart() {
                events.add("onStart")
                cancel()
            }

            override fun onFinish() {
                events.add("onFinish")
            }
        }

        chain.add(newTestNode(prefix = "1", events = events))
        chain.add(newTestNode(prefix = "2", events = events))
        chain.add(newTestNode(prefix = "3", events = events))

        chain.start()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        listOf(
            "onStart",
            "onFinish",
        ).let { expectedEvents ->
            assertEquals(expectedEvents, events)
        }
    }

    @Test
    fun testCancelOnRun() {
        val events = mutableListOf<String>()

        val chain = object : FChain() {
            override fun onStart() {
                events.add("onStart")
            }

            override fun onFinish() {
                events.add("onFinish")
            }
        }

        chain.add(newTestNode(prefix = "1", events = events))
        chain.add(newTestNode(prefix = "2", events = events, onRun = {
            // cancel
            chain.cancel()
        }))
        chain.add(newTestNode(prefix = "3", events = events))

        chain.start()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        listOf(
            "onStart",
            "1onRun", "1onFinish",
            "2onRun", "2onCancel", "2onFinish",
            "onFinish",
        ).let { expectedEvents ->
            assertEquals(expectedEvents, events)
        }
    }

    @Test
    fun testCancelOnFinish() {
        val events = mutableListOf<String>()

        val chain = object : FChain() {
            override fun onStart() {
                events.add("onStart")
            }

            override fun onFinish() {
                events.add("onFinish")
            }
        }

        chain.add(newTestNode(prefix = "1", events = events))
        chain.add(newTestNode(prefix = "2", events = events, onFinish = {
            // cancel
            chain.cancel()
        }))
        chain.add(newTestNode(prefix = "3", events = events))

        chain.start()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        listOf(
            "onStart",
            "1onRun", "1onFinish",
            "2onRun", "2onFinish",
            "onFinish",
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
            checkMainLooper()
            events.add("${prefix}onRun")
            onRun.invoke(this)
            nextNode()
        }

        override fun onCancel() {
            checkMainLooper()
            events.add("${prefix}onCancel")
            onCancel.invoke(this)
        }

        override fun onFinish() {
            checkMainLooper()
            events.add("${prefix}onFinish")
            onFinish.invoke(this)
        }
    }
}

private fun checkMainLooper() {
    check(Looper.myLooper() === Looper.getMainLooper())
}

private class TestErrorNextNode : FChain.Node() {

    fun testRunNextNode() {
        nextNode()
    }

    override fun onRun() {

    }
}