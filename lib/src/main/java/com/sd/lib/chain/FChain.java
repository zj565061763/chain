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
    private final List<Runnable> mListRunnable = new ArrayList<>();

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

        node.setChain(this);
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
            throw new RuntimeException("head node is finish");
        }

        mCurrentIndex = index;
        mCurrentNode = node;

        notifyCurrentNodeRun();
        return true;
    }

    private synchronized void runNextNode(@NonNull Node node) {
        if (mCurrentNode != node) {
            throw new RuntimeException("current node:" + mCurrentNode + " call node:" + node);
        }

        final int nextIndex = mCurrentIndex + 1;
        if (nextIndex >= mListNode.size()) {
            return;
        }

        mCurrentIndex = nextIndex;
        mCurrentNode = mListNode.get(nextIndex);

        notifyCurrentNodeRun();
    }

    private void notifyCurrentNodeRun() {
        final Node currentNode = mCurrentNode;
        if (currentNode == null) {
            return;
        }

        final Runnable notifyRunnable = new Runnable() {
            @Override
            public void run() {
                mListRunnable.remove(this);
                currentNode.notifyRun();
            }
        };

        mListRunnable.add(notifyRunnable);
        mHandler.post(notifyRunnable);
    }

    /**
     * 取消，并清空所有节点
     */
    public synchronized void cancel() {
        if (mIsDispatchCancel) {
            return;
        }

        mIsDispatchCancel = true;

        for (Runnable item : mListRunnable) {
            mHandler.removeCallbacks(item);
        }
        mListRunnable.clear();

        for (Node item : mListNode) {
            item.notifyCancel();
        }
        mListNode.clear();

        mCurrentIndex = -1;
        mCurrentNode = null;

        mIsDispatchCancel = false;
    }

    public static abstract class Node {
        private FChain _chain;
        private volatile boolean _isFinish = false;

        private synchronized void setChain(@NonNull FChain chain) {
            if (_chain == null) {
                _chain = chain;
            } else {
                throw new IllegalArgumentException("node has been add to " + _chain);
            }
        }

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
            onFinish();
        }

        /**
         * 执行下一个节点
         */
        public final void nextNode() {
            if (_isFinish) {
                return;
            }
            _isFinish = true;

            onFinish();
            _chain.runNextNode(this);
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
