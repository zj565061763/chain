package com.sd.demo.chain

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.chain.databinding.ActivityMainBinding
import com.sd.lib.chain.FChain
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var _chain: FChain? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStart.setOnClickListener {
            startChain()
        }
        _binding.btnCancel.setOnClickListener {
            cancelChain()
        }
    }

    private fun startChain() {
        if (_chain != null) return
        object : FChain() {
            override fun onFinish() {
                super.onFinish()
                logMsg { "chain onFinish" }
            }
        }.let { chain ->
            _chain = chain
            chain.add(newNode("node1"))
            chain.add(newNode("node2"))
            chain.start()
        }
    }

    private fun newNode(tag: String): FChain.Node {
        return object : FChain.Node() {
            private val _scope = MainScope()

            override fun onRun() {
                logMsg { "$tag onRun ----->" }
                _scope.launch {
                    repeat(5) {
                        delay(1000)
                        logMsg { "$tag count ${it + 1}" }
                    }
                    nextNode()
                }
            }

            override fun onCancel() {
                super.onCancel()
                logMsg { "$tag onCancel" }
                _scope.cancel()
            }

            override fun onFinish() {
                super.onFinish()
                logMsg { "$tag onFinish" }
            }
        }
    }

    private fun cancelChain() {
        _chain?.cancel()
        _chain = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelChain()
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("chain-demo", block())
}