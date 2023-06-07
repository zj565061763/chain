package com.sd.demo.chain

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.chain.databinding.ActivityMainBinding
import com.sd.lib.chain.FChain
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val _scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)

        _chain.apply {
            this.add(_node1)
            this.add(_node2)
            this.add(_node3)
        }.start()
    }

    private val _chain = object : FChain() {
        override fun onFinish() {
            super.onFinish()
            logMsg { "chain onFinish" }
        }
    }

    private val _node1 = object : FChain.Node() {
        override fun onRun() {
            logMsg { "node1 onRun ----->" }
            nextNode()
        }

        override fun onCancel() {
            super.onCancel()
            logMsg { "node1 onCancel" }
        }

        override fun onFinish() {
            super.onFinish()
            logMsg { "node1 onFinish" }
        }
    }

    private val _node2 = object : FChain.Node() {
        private var _job: Job? = null

        override fun onRun() {
            logMsg { "node2 onRun ----->" }
            _job = _scope.launch {
                repeat(5) {
                    delay(1000)
                    logMsg { "node2 count ${it + 1}" }
                }
                nextNode()
            }
        }

        override fun onCancel() {
            super.onCancel()
            logMsg { "node2 onCancel" }
            _job?.cancel()
        }

        override fun onFinish() {
            super.onFinish()
            logMsg { "node2 onFinish" }
        }
    }

    private val _node3 = object : FChain.Node() {
        override fun onRun() {
            logMsg { "node3 onRun ----->" }
            nextNode()
        }

        override fun onCancel() {
            super.onCancel()
            logMsg { "node3 onCancel" }
        }

        override fun onFinish() {
            super.onFinish()
            logMsg { "node3 onFinish" }
        }
    }

    override fun onStop() {
        super.onStop()
        _scope.cancel()
        _chain.cancel()
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("chain-demo", block())
}