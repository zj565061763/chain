package com.sd.lib.chain;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FChain {
    /** 节点链 */
    private final List<Node> mListNode = new CopyOnWriteArrayList<>();

    /** 当前执行的节点 */
    private Node mCurrentNode = null;
    /** 当前执行的节点位置 */
    private int mCurrentNodeIndex = -1;

    /** 是否正在分发取消事件 */
    private volatile boolean mIsDispatchCancel = false;

    /**
     * 节点数量
     */
    public int size() {
        return mListNode.size();
    }

    /**
     * 添加节点
     */
    public synchronized void add(@NonNull Node node) {
        if (mIsDispatchCancel) {
            throw new RuntimeException("can not add node when cancelling");
        }

        if (node._chain == null) {
            node._chain = this;
        } else {
            // 同一个node，只允许被添加一次
            throw new IllegalArgumentException("node has been add to " + node._chain);
        }

        mListNode.add(node);
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
            return false;
        }

        final int index = 0;
        final Node node = mListNode.get(index);
        if (node._isFinish) {
            return false;
        }

        mCurrentNode = node;
        mCurrentNodeIndex = index;
        node.notifyRun();
        return true;
    }

    private synchronized void runNext(Node node) {
        if (mCurrentNode != node) {
            throw new RuntimeException("current node:" + mCurrentNode + " call node:" + node);
        }

        final int nextIndex = mCurrentNodeIndex + 1;
        if (nextIndex >= mListNode.size()) {
            return;
        }

        final Node nextNode = mListNode.get(nextIndex);
        mCurrentNode = nextNode;
        mCurrentNodeIndex = nextIndex;
        nextNode.notifyRun();
    }

    /**
     * 取消，并清空所有节点
     */
    public synchronized void cancel() {
        if (mIsDispatchCancel) {
            return;
        }

        mIsDispatchCancel = true;
        for (Node node : mListNode) {
            node.notifyCancel();
        }

        mCurrentNode = null;
        mCurrentNodeIndex = -1;
        mListNode.clear();
        mIsDispatchCancel = false;
    }

    public static abstract class Node {
        volatile FChain _chain;
        private volatile boolean _isFinish = false;

        private void notifyRun() {
            if (_isFinish) {
                return;
            }
            onRun();
        }

        private void notifyCancel() {
            if (_isFinish) {
                return;
            }
            _isFinish = true;
            onCancel();
        }

        /**
         * 执行下一个节点
         */
        public final void next() {
            if (_isFinish) {
                return;
            }
            _isFinish = true;
            _chain.runNext(this);
        }

        protected abstract void onRun();

        protected void onCancel() {
        }
    }
}
