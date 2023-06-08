package com.sd.lib.chain

import android.os.Handler
import android.os.Looper

open class FChain {
    /** 节点链  */
    private val _nodeHolder: MutableList<Node> = mutableListOf()
    private val _handler = Handler(Looper.getMainLooper())

    /** 当前执行的节点位置  */
    private var _currentIndex = -1
    /** 当前执行的节点  */
    private var _currentNode: Node? = null

    /** 是否已经通知了[onFinish]  */
    private var _hasNotifyFinish = false

    /**
     * 节点数量
     */
    @Synchronized
    fun size(): Int {
        return _nodeHolder.size
    }

    /**
     * 添加节点，一个节点对象只能添加到一个链上。
     */
    @Synchronized
    fun add(node: Node) {
        node.init(this@FChain, _handler)
        _nodeHolder.add(node)
    }

    /**
     * 开始
     * @return true-本次调用触发了开始
     */
    @Synchronized
    fun start(): Boolean {
        if (_nodeHolder.isEmpty()) return false
        if (_currentNode != null) return false

        // 重置为false
        _hasNotifyFinish = false

        _nodeHolder[0].also {
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
        _currentNode = null
        _currentIndex = -1
        _nodeHolder.clear()
        notifyOnFinishLocked()
    }

    @Synchronized
    private fun runNextNode() {
        val node = _currentNode ?: return
        if (node.state != NodeState.Finish) {
            error("Current node has not finished " + node.state)
        }

        val nextIndex = _currentIndex + 1
        if (nextIndex >= _nodeHolder.size) {
            notifyOnFinishLocked()
            return
        }

        _nodeHolder[nextIndex].also {
            _currentIndex = nextIndex
            _currentNode = it
        }.notifyRun()
    }

    private fun notifyOnFinishLocked() {
        if (!_hasNotifyFinish) {
            _hasNotifyFinish = true
            _handler.post { onFinish() }
        }
    }

    /**
     * 结束回调，在主线程触发。
     */
    protected open fun onFinish() {}

    enum class NodeState {
        None,
        Run,
        Finish
    }

    abstract class Node {
        private lateinit var _chain: FChain
        private lateinit var _handler: Handler

        private var _state = NodeState.None

        private var _hasRun = false
            set(value) {
                require(value) { "Require true value." }
                field = value
            }

        val state: NodeState
            get() = synchronized(this@Node) { _state }

        internal fun init(chain: FChain, handler: Handler) {
            synchronized(this@Node) {
                if (this::_chain.isInitialized) error("Node has been added to $_chain")
                _chain = chain
                _handler = handler
            }
        }

        internal fun notifyRun() {
            synchronized(this@Node) {
                check(_state == NodeState.None) { "Illegal node state $_state" }
                _state = NodeState.Run
            }

            _handler.post {
                synchronized(this@Node) {
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
            synchronized(this@Node) {
                if (_state != NodeState.Finish) {
                    _state = NodeState.Finish
                    _hasRun
                } else {
                    false
                }
            }.let { notify ->
                if (notify) {
                    _handler.post { onCancel() }
                    _handler.post { onFinish() }
                }
            }
        }

        /**
         * 执行下一个节点
         */
        protected fun nextNode() {
            synchronized(this@Node) {
                when (_state) {
                    NodeState.None -> error("Can not call nextNode() before onRun() invoked.")
                    NodeState.Finish -> return
                    NodeState.Run -> {
                        _state = NodeState.Finish
                        _hasRun
                    }
                }
            }.let { notify ->
                if (notify) {
                    _handler.post { onFinish() }
                }
                _chain.runNextNode()
            }
        }

        /**
         * 节点执行回调，主线程触发
         */
        protected abstract fun onRun()

        /**
         * 节点取消回调，主线程触发。
         * 如果[onRun]未触发过，则此方法不会被触发。
         */
        protected open fun onCancel() {}

        /**
         * 节点结束回调，主线程触发。
         * 如果[onRun]未触发过，则此方法不会被触发。
         */
        protected open fun onFinish() {}
    }
}