package com.sd.lib.chain;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FChain {
    /** 节点链 */
    private final List<Node> mListNode = new ArrayList<>();

    /** 当前执行的节点位置 */
    private int mCurrentIndex = -1;
    /** 当前执行的节点 */
    private Node mCurrentNode = null;

    /** 是否正在分发取消事件 */
    private boolean mIsDispatchCancel = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * 节点数量
     */
    public int size() {
        return mListNode.size();
    }

    /**
     * 添加节点，一个节点对象只能添加到一个链上
     */
    public void add(@NonNull Node node) {
        synchronized (this) {
            if (mIsDispatchCancel) {
                throw new RuntimeException("Cannot add node when canceling.");
            }
        }

        node.init(this, mHandler);
        synchronized (this) {
            mListNode.add(node);
        }
    }

    /**
     * 开始
     *
     * @return true-本次调用触发了开始
     */
    public synchronized boolean start() {
        if (mListNode.isEmpty()) {
            return false;
        }
        if (mCurrentNode != null) {
            // 已经开始了，不处理
            return false;
        }

        final int index = 0;
        final Node node = mListNode.get(index);
        if (node.getState() != NodeState.None) {
            throw new RuntimeException("Illegal node state " + node.getState());
        }

        mCurrentIndex = index;
        mCurrentNode = node;
        mCurrentNode.notifyRun();
        return true;
    }

    /**
     * 取消，并清空所有节点
     */
    public synchronized void cancel() {
        if (mIsDispatchCancel) {
            return;
        }

        mIsDispatchCancel = true;

        for (Node item : mListNode) {
            item.notifyCancel();
        }
        mListNode.clear();
        mCurrentNode = null;
        mCurrentIndex = -1;

        mIsDispatchCancel = false;
    }

    private synchronized void runNextNode() {
        final Node currentNode = mCurrentNode;
        if (currentNode == null) {
            // 还未开始，不允许调用此方法
            return;
        }

        if (currentNode.getState() != NodeState.Finish) {
            throw new RuntimeException("Current node has not finished " + currentNode.getState());
        }

        final int nextIndex = mCurrentIndex + 1;
        if (nextIndex >= mListNode.size()) {
            return;
        }

        mCurrentIndex = nextIndex;
        mCurrentNode = mListNode.get(nextIndex);
        mCurrentNode.notifyRun();
    }

    public enum NodeState {
        None,
        Run,
        Finish,
    }

    public static abstract class Node {
        private FChain _chain;
        private Handler _handler;
        private volatile NodeState _state = NodeState.None;
        private volatile boolean _hasRun;

        /**
         * 节点状态
         */
        public final NodeState getState() {
            return _state;
        }

        private synchronized void init(@NonNull FChain chain, @NonNull Handler handler) {
            if (_chain == null) {
                _chain = chain;
                _handler = handler;
            } else {
                throw new IllegalArgumentException("Node has been added to " + _chain);
            }
        }

        private synchronized void checkInit() {
            if (_chain == null) {
                throw new RuntimeException("Node has not been added to any chain.");
            }
            if (_handler == null) {
                throw new RuntimeException("Node's handler is null.");
            }
        }

        private void notifyRun() {
            checkInit();

            boolean notify = false;
            synchronized (Node.this) {
                if (_state == NodeState.None) {
                    _state = NodeState.Run;
                    notify = true;
                }
            }

            if (notify) {
                _handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (_state == NodeState.Run) {
                            _hasRun = true;
                            onRun();
                        }
                    }
                });
            }
        }

        private void notifyCancel() {
            checkInit();

            boolean notify = false;
            synchronized (Node.this) {
                if (_state != NodeState.Finish) {
                    _state = NodeState.Finish;
                    notify = _hasRun;
                }
            }

            if (notify) {
                _handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            onCancel();
                        } finally {
                            onFinish();
                        }
                    }
                });
            }
        }

        /**
         * 执行下一个节点
         */
        protected final void nextNode() {
            checkInit();

            boolean notify = false;
            synchronized (Node.this) {
                if (_state == NodeState.Run) {
                    _state = NodeState.Finish;
                    notify = true;
                }
            }

            if (notify) {
                _handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onFinish();
                    }
                });
                _chain.runNextNode();
            }
        }

        /**
         * 节点执行回调，主线程触发
         */
        protected abstract void onRun();

        /**
         * 节点取消回调，主线程触发
         */
        protected void onCancel() {
        }

        /**
         * 节点结束回调，主线程触发
         */
        protected void onFinish() {
        }
    }
}
