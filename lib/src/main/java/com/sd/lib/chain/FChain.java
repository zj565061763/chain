package com.sd.lib.chain;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FChain {
    /** 节点链 */
    private final List<Node> mListNode = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** 当前执行的节点位置 */
    private int mCurrentIndex = -1;
    /** 当前执行的节点 */
    private Node mCurrentNode = null;
    /** 是否已经通知了{@link #onFinish()} */
    private boolean mHasNotifyFinish = false;

    /**
     * 节点数量
     */
    public final synchronized int size() {
        return mListNode.size();
    }

    /**
     * 返回当前节点
     */
    @Nullable
    public final synchronized Node getCurrentNode() {
        return mCurrentNode;
    }

    /**
     * 添加节点，一个节点对象只能添加到一个链上。
     */
    public void add(@NonNull Node node) {
        synchronized (FChain.this) {
            node.init(FChain.this, mHandler);
            mListNode.add(node);
        }
    }

    /**
     * 开始
     *
     * @return true-本次调用触发了开始
     */
    public boolean start() {
        synchronized (FChain.this) {
            if (mListNode.isEmpty()) return false;
            if (mCurrentNode != null) return false;

            final int index = 0;
            final Node node = mListNode.get(index);
            if (node.getState() != NodeState.None) {
                throw new RuntimeException("Illegal node state " + node.getState());
            }

            // 重置为false
            mHasNotifyFinish = false;

            mCurrentIndex = index;
            mCurrentNode = node;
            mCurrentNode.notifyRun();
            return true;
        }
    }

    /**
     * 取消当前节点，并清空所有节点。
     */
    public void cancel() {
        synchronized (FChain.this) {
            if (mCurrentNode == null) return;

            mCurrentNode.notifyCancel();
            mCurrentNode = null;
            mCurrentIndex = -1;
            mListNode.clear();

            notifyOnFinishLocked();
        }
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
            notifyOnFinishLocked();
            return;
        }

        mCurrentIndex = nextIndex;
        mCurrentNode = mListNode.get(nextIndex);
        mCurrentNode.notifyRun();
    }

    private void notifyOnFinishLocked() {
        if (mHasNotifyFinish) return;
        mHasNotifyFinish = true;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onFinish();
            }
        });
    }

    /**
     * 结束回调，在主线程触发。
     */
    protected void onFinish() {
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
        @NonNull
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

            synchronized (Node.this) {
                if (_state == NodeState.None) {
                    _state = NodeState.Run;
                } else {
                    throw new RuntimeException("Illegal node state " + _state);
                }
            }

            _handler.post(new Runnable() {
                @Override
                public void run() {
                    // 检查状态，如果未被取消，则通知回调
                    if (_state == NodeState.Run) {
                        _hasRun = true;
                        onRun();
                    }
                }
            });
        }

        private void notifyCancel() {
            checkInit();

            boolean stateChanged = false;
            synchronized (Node.this) {
                if (_state != NodeState.Finish) {
                    _state = NodeState.Finish;
                    stateChanged = true;
                }
            }

            if (!stateChanged) {
                // 状态未改变，不处理
                return;
            }

            if (_hasRun) {
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

            boolean stateChanged = false;
            synchronized (Node.this) {
                if (_state == NodeState.Run) {
                    _state = NodeState.Finish;
                    stateChanged = true;
                }
            }

            if (stateChanged) {
                if (_hasRun) {
                    _handler.post(new Runnable() {
                        @Override
                        public void run() {
                            onFinish();
                        }
                    });
                }

                // 通知下一个节点
                _chain.runNextNode();
            }
        }

        /**
         * 节点执行回调，主线程触发
         */
        protected abstract void onRun();

        /**
         * 节点取消回调，主线程触发。
         * 如果{@link #onRun()}未触发过，则此方法不会被触发。
         */
        protected void onCancel() {
        }

        /**
         * 节点结束回调，主线程触发。
         * 如果{@link #onRun()}未触发过，则此方法不会被触发。
         */
        protected void onFinish() {
        }
    }
}
