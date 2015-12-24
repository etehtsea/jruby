/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingAction;
import org.jruby.truffle.runtime.util.MethodHandleUtils;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "Queue")
public abstract class QueueNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, new LinkedBlockingQueue<Object>());
        }

    }

    @CoreMethod(names = { "push", "<<", "enq" }, required = 1)
    public abstract static class PushNode extends CoreMethodArrayArgumentsNode {

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject push(DynamicObject self, final Object value) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            queue.add(value);
            return self;
        }

    }

    @CoreMethod(names = { "pop", "shift", "deq" }, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "nonBlocking")
    })
    public abstract static class PopNode extends CoreMethodNode {

        public PopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("nonBlocking")
        public RubyNode coerceToBoolean(RubyNode nonBlocking) {
            return BooleanCastWithDefaultNodeGen.create(getContext(), getSourceSection(), false, nonBlocking);
        }

        @TruffleBoundary
        @Specialization(guards = "!nonBlocking")
        public Object popBlocking(DynamicObject self, boolean nonBlocking) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            return getContext().getThreadManager().runUntilResult(this, new BlockingAction<Object>() {
                @Override
                public Object block() throws InterruptedException {
                    return queue.take();
                }
            });
        }

        @TruffleBoundary
        @Specialization(guards = "nonBlocking")
        public Object popNonBlock(DynamicObject self, boolean nonBlocking) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            final Object value = queue.poll();
            if (value == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("queue empty", this));
            }

            return value;
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "receive_timeout", required = 1, visibility = Visibility.PRIVATE)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "queue"),
            @NodeChild(type = RubyNode.class, value = "duration")
    })
    public abstract static class ReceiveTimeoutNode extends CoreMethodNode {

        public ReceiveTimeoutNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object receiveTimeout(DynamicObject self, int duration) {
            return receiveTimeout(self, (double) duration);
        }

        @Specialization
        public Object receiveTimeout(DynamicObject self, double duration) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            final long durationInMillis = (long) (duration * 1000.0);
            final long start = System.currentTimeMillis();

            return getContext().getThreadManager().runUntilResult(this, new BlockingAction<Object>() {
                @Override
                public Object block() throws InterruptedException {
                    long now = System.currentTimeMillis();
                    long waited = now - start;
                    if (waited >= durationInMillis) {
                        // Try again to make sure we at least tried once
                        final Object result = queue.poll();
                        if (result == null) {
                            return false;
                        } else {
                            return result;
                        }
                    }

                    final Object result = queue.poll(durationInMillis, TimeUnit.MILLISECONDS);
                    if (result == null) {
                        return false;
                    } else {
                        return result;
                    }
                }
            });
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean empty(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);
            return queue.isEmpty();
        }

    }

    @CoreMethod(names = { "size", "length" })
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int size(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);
            return queue.size();
        }

    }

    @CoreMethod(names = "clear")
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject clear(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);
            queue.clear();
            return self;
        }

    }

    @CoreMethod(names = "marshal_dump")
    public abstract static class MarshalDumpNode extends CoreMethodArrayArgumentsNode {

        public MarshalDumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        @TruffleBoundary
        public Object marshal_dump(DynamicObject self) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorCantDump(self, this));
        }

    }

    @CoreMethod(names = "num_waiting")
    public abstract static class NumWaitingNode extends CoreMethodArrayArgumentsNode {

        private static final MethodHandle TAKE_LOCK_FIELD_GETTER = MethodHandleUtils.getPrivateGetter(LinkedBlockingQueue.class, "takeLock");
        private static final MethodHandle NOT_EMPTY_CONDITION_FIELD_GETTER = MethodHandleUtils.getPrivateGetter(LinkedBlockingQueue.class, "notEmpty");

        public NumWaitingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int num_waiting(DynamicObject self) {
            final BlockingQueue<Object> queue = Layouts.QUEUE.getQueue(self);

            final LinkedBlockingQueue<Object> linkedBlockingQueue = (LinkedBlockingQueue<Object>) queue;

            final ReentrantLock lock;
            final Condition notEmptyCondition;
            try {
                lock = (ReentrantLock) TAKE_LOCK_FIELD_GETTER.invokeExact(linkedBlockingQueue);
                notEmptyCondition = (Condition) NOT_EMPTY_CONDITION_FIELD_GETTER.invokeExact(linkedBlockingQueue);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            getContext().getThreadManager().runUntilResult(this, new BlockingAction<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    return SUCCESS;
                }
            });
            try {
                return lock.getWaitQueueLength(notEmptyCondition);
            } finally {
                lock.unlock();
            }
        }

    }

}
