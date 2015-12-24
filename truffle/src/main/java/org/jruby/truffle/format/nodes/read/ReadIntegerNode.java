/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.nodes.read;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.nodes.SourceNode;
import org.jruby.truffle.format.nodes.type.ToIntegerNode;
import org.jruby.truffle.format.nodes.type.ToIntegerNodeGen;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Read a {@code int} value from the source.
 */
@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadIntegerNode extends PackNode {

    @Child private ToIntegerNode toIntegerNode;

    public ReadIntegerNode(RubyContext context) {
        super(context);
    }

    @Specialization(guards = "isNull(source)")
    public double read(VirtualFrame frame, Object source) {
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame);

        throw new IllegalStateException();
    }

    @Specialization
    public int read(VirtualFrame frame, int[] source) {
        return source[advanceSourcePosition(frame)];
    }

    @Specialization
    public int read(VirtualFrame frame, long[] source) {
        return (int) source[advanceSourcePosition(frame)];
    }

    @Specialization
    public int read(VirtualFrame frame, double[] source) {
        return (int) source[advanceSourcePosition(frame)];
    }

    @Specialization
    public int read(VirtualFrame frame, Object[] source) {
        if (toIntegerNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntegerNode = insert(ToIntegerNodeGen.create(getContext(), null));
        }

        final Object value = toIntegerNode.executeToInteger(frame, source[advanceSourcePosition(frame)]);
        if (value instanceof Long) {
            return (int) (long) value;
        } else {
            return (int) value;
        }
    }

}
