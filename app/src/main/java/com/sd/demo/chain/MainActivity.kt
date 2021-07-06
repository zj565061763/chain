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

    private val _chain = FChain()

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
            Log.i(TAG, "node1 onRun")
            nextNode()
        }

        override fun onCancel() {
            super.onCancel()
            Log.i(TAG, "node1 onCancel")
        }

        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "node1 onFinish")
        }
    }

    private val _node2 = object : FChain.Node() {
        private var _count = 0
        private var _job: Job? = null

        override fun onRun() {
            Log.i(TAG, "node2 onRun")
            _job = GlobalScope.launch {
                while (_count < 5) {
                    delay(1000)
                    _count++
                    Log.i(TAG, "node2 _count ${_count}")
                }
                nextNode()
            }
        }

        override fun onCancel() {
            super.onCancel()
            Log.i(TAG, "node2 onCancel")
            _job?.cancel()
        }

        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "node2 onFinish")
        }
    }

    private val _node3 = object : FChain.Node() {
        override fun onRun() {
            Log.i(TAG, "node3 onRun")
            nextNode()
        }

        override fun onCancel() {
            super.onCancel()
            Log.i(TAG, "node3 onCancel")
        }

        override fun onFinish() {
            super.onFinish()
            Log.i(TAG, "node3 onFinish")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _chain.cancel()
    }
}