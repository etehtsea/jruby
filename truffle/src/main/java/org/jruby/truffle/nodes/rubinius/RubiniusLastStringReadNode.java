/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.ThreadLocalObject;

public class RubiniusLastStringReadNode extends RubyNode {

    public RubiniusLastStringReadNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // Rubinius expects $_ to be thread-local, rather than frame-local.  If we see it in a method call, we need
        // to look to the caller's frame to get the correct value, otherwise it will be nil.
        final MaterializedFrame callerFrame = RubyCallStack.getCallerFrame(getContext()).getFrame(FrameInstance.FrameAccess.READ_ONLY, true).materialize();

        final FrameSlot slot = callerFrame.getFrameDescriptor().findFrameSlot("$_");
        try {
            final ThreadLocalObject threadLocalObject = (ThreadLocalObject) callerFrame.getObject(slot);
            return getThreadLocal(threadLocalObject);
        } catch (FrameSlotTypeException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @TruffleBoundary
    private static Object getThreadLocal(ThreadLocalObject threadLocalObject) {
        return threadLocalObject.get();
    }
}
