package com.sd.demo.chain

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.lib.chain.FChain
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    private val _chain = object : FChain() {
        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "chain onFinish ${Thread.currentThread().name}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _chain.apply {
            this.add(_node1)
            this.add(_node2)
            this.add(_node3)
        }
        _chain.start()
    }

    private val _node1 = object : FChain.Node() {
        override fun onRun() {
            Log.i(TAG, "node1 onRun -----> ${Thread.currentThread().name}")
            nextNode()
        }

        override fun onCancel() {
            super.onCancel()
            Log.i(TAG, "node1 onCancel ${Thread.currentThread().name}")
        }

        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "node1 onFinish ${Thread.currentThread().name}")
        }
    }

    private val _node2 = object : FChain.Node() {
        private var _job: Job? = null

        override fun onRun() {
            Log.i(TAG, "node2 onRun -----> ${Thread.currentThread().name}")
            _job = GlobalScope.launch {
                repeat(5) {
                    delay(1000)
                    Log.i(TAG, "node2 _count ${it + 1} ${Thread.currentThread().name}")
                }
                nextNode()
            }
        }

        override fun onCancel() {
            super.onCancel()
            Log.i(TAG, "node2 onCancel ${Thread.currentThread().name}")
            _job?.cancel()
        }

        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "node2 onFinish ${Thread.currentThread().name}")
        }
    }

    private val _node3 = object : FChain.Node() {
        override fun onRun() {
            Log.i(TAG, "node3 onRun -----> ${Thread.currentThread().name}")
            nextNode()
        }

        override fun onCancel() {
            super.onCancel()
            Log.i(TAG, "node3 onCancel ${Thread.currentThread().name}")
        }

        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "node3 onFinish ${Thread.currentThread().name}")
        }
    }

    override fun onStop() {
        super.onStop()
        _chain.cancel()
    }
}