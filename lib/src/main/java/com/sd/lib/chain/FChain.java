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
    private volatile boolean mIsDispatchCancel = false;

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
        if (mIsDispatchCancel) {
            throw new RuntimeException("can not add node when cancelling");
        }

        node.setChain(this);
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
        mHandler.post(mNodeRunnable);
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

        mHandler.removeCallbacks(mNodeRunnable);
        for (Node item : mListNode) {
            item.notifyCancel();
        }
        mListNode.clear();
        mCurrentNode = null;
        mCurrentIndex = -1;

        mIsDispatchCancel = false;
    }

    private synchronized void runNextNode(@NonNull Node node) {
        if (mCurrentNode != node) {
            throw new RuntimeException("current node:" + mCurrentNode + " call node:" + node);
        }

        if (node.getState() != NodeState.Finish) {
            throw new RuntimeException("Illegal node state " + node.getState());
        }

        final int nextIndex = mCurrentIndex + 1;
        if (nextIndex >= mListNode.size()) {
            return;
        }

        mCurrentIndex = nextIndex;
        mCurrentNode = mListNode.get(nextIndex);
        mHandler.post(mNodeRunnable);
    }

    private final Runnable mNodeRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (FChain.this) {
                final Node currentNode = mCurrentNode;
                if (currentNode != null) {
                    currentNode.notifyRun();
                }
            }
        }
    };

    public enum NodeState {
        None,
        Run,
        Finish,
    }

    public static abstract class Node {
        private FChain _chain;
        private volatile NodeState _state = NodeState.None;

        /**
         * 节点状态
         */
        public final NodeState getState() {
            return _state;
        }

        private synchronized void setChain(@NonNull FChain chain) {
            if (_chain == null) {
                _chain = chain;
            } else {
                throw new IllegalArgumentException("node has been add to " + _chain);
            }
        }

        private void notifyRun() {
            if (_state == NodeState.None) {
                _state = NodeState.Run;
                onRun();
            }
        }

        private void notifyCancel() {
            if (_state != NodeState.Finish) {
                _state = NodeState.Finish;
                onCancel();
                onFinish();
            }
        }

        /**
         * 执行下一个节点
         */
        protected final void nextNode() {
            final FChain chain = _chain;
            if (chain == null) {
                throw new RuntimeException("Current node has not been added to the chain.");
            }

            synchronized (chain) {
                if (_state == NodeState.Finish) {
                    return;
                }
                if (_state != NodeState.Run) {
                    throw new RuntimeException("nextNode() should be called when state Run " + this);
                }

                _state = NodeState.Finish;

                /**
                 * 由于{@link FChain#runNextNode(Node)}里面是通过{@link Handler}通知下一个节点，
                 * 所以这里在{@link #onFinish()}之前触发
                 */
                chain.runNextNode(this);
                onFinish();
            }
        }

        /**
         * 节点执行回调，UI线程触发
         */
        protected abstract void onRun();

        /**
         * 节点取消回调，{@link FChain#cancel()}所在线程触发
         */
        protected void onCancel() {
        }

        /**
         * 节点结束回调，{@link FChain#cancel()}或者{@link #nextNode()}所在线程触发
         */
        protected void onFinish() {
        }
    }
}
