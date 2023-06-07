package com.sd.demo.chain

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.chain.databinding.ActivityMainBinding
import com.sd.lib.chain.FChain
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val _scope = MainScope()

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
                _chain = null
            }
        }.apply {
            // node 1
            this.add(
                object : FChain.Node() {
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
            )

            // node 2
            this.add(
                object : FChain.Node() {
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
            )

            // node 3
            this.add(
                object : FChain.Node() {
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
            )
        }.also { chain ->
            _chain = chain
            chain.start()
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