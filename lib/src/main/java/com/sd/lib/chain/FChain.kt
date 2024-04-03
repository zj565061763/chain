package com.sd.lib.chain

import android.os.Handler
import android.os.Looper

open class FChain {
    /** 所有节点  */
    private val _nodes: MutableList<Node> = mutableListOf()

    /** 当前执行的节点位置  */
    private var _currentIndex = -1
    /** 当前执行的节点  */
    private var _currentNode: Node? = null

    private val _handler = Handler(Looper.getMainLooper())

    /**
     * 是否正在执行中
     */
    @Synchronized
    fun isRunning(): Boolean = _currentNode != null

    /**
     * 节点数量
     */
    @Synchronized
    fun size(): Int = _nodes.size

    /**
     * 添加节点，一个节点对象只能被添加一次
     */
    @Synchronized
    fun add(node: Node) {
        node.init(this@FChain, _handler)
        _nodes.add(node)
    }

    /**
     * 开始
     * @return true-本次调用触发了开始
     */
    @Synchronized
    fun start(): Boolean {
        if (_currentNode != null) return false
        if (_nodes.isEmpty()) return false

        _nodes.first().also {
            _currentIndex = 0
            _currentNode = it
        }.notifyRun()
        return true
    }

    /**
     * 取消，并清空所有节点。
     */
    @Synchronized
    fun cancel() {
        val node = _currentNode ?: return
        node.notifyCancel()
        finish()
    }

    @Synchronized
    private fun runNextNode() {
        val node = _currentNode ?: return
        node.checkFinish()

        val nextIndex = _currentIndex + 1
        if (nextIndex >= _nodes.size) {
            finish()
            return
        }

        _nodes[nextIndex].also {
            _currentIndex = nextIndex
            _currentNode = it
        }.notifyRun()
    }

    private fun finish() {
        _currentIndex = -1
        _currentNode = null
        _nodes.clear()
        _handler.post { onFinish() }
    }

    /**
     * 结束回调，在主线程触发
     */
    protected open fun onFinish() = Unit

    private enum class NodeState {
        None,
        Run,
        Finish
    }

    abstract class Node {
        private lateinit var _chain: FChain
        private lateinit var _handler: Handler

        private var _state = NodeState.None
        private var _hasRun = false

        internal fun init(chain: FChain, handler: Handler) {
            if (this::_chain.isInitialized) error("Node has been added to $_chain")
            _chain = chain
            _handler = handler
        }

        internal fun checkFinish() {
            check(_state == NodeState.Finish) { "Node has not finished with state:${_state}" }
        }

        internal fun notifyRun() {
            check(_state == NodeState.None)
            _state = NodeState.Run
            _handler.post {
                synchronized(_chain) {
                    (_state == NodeState.Run).also {
                        if (it) _hasRun = true
                    }
                }.let { notify ->
                    if (notify) {
                        onRun()
                    }
                }
            }
        }

        internal fun notifyCancel() {
            if (_state != NodeState.Finish) {
                _state = NodeState.Finish
                if (_hasRun) {
                    _handler.post { onCancel() }
                    _handler.post { onFinish() }
                }
            }
        }

        /**
         * 执行下一个节点
         */
        protected fun nextNode() {
            check(this::_chain.isInitialized) { "Node has not been initialized." }
            synchronized(_chain) {
                when (_state) {
                    NodeState.None -> error("Can not call nextNode() before onRun().")
                    NodeState.Finish -> return
                    NodeState.Run -> {
                        _state = NodeState.Finish
                        if (_hasRun) {
                            _handler.post { onFinish() }
                        }
                        _chain.runNextNode()
                    }
                }
            }
        }

        /**
         * 节点执行回调，主线程触发
         */
        protected abstract fun onRun()

        /**
         * 节点取消回调，主线程触发，
         * 如果[onRun]未触发过，则此方法不会被触发
         */
        protected open fun onCancel() = Unit

        /**
         * 节点结束回调，主线程触发，
         * 如果[onRun]未触发过，则此方法不会被触发
         */
        protected open fun onFinish() = Unit
    }
}