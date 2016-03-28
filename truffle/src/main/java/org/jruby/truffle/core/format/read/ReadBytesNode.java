/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.read;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.SourceNode;
import org.jruby.truffle.core.format.MissingValue;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadBytesNode extends FormatNode {

    private final int count;
    private final boolean consumePartial;

    public ReadBytesNode(RubyContext context, int count, boolean consumePartial) {
        super(context);
        this.count = count;
        this.consumePartial = consumePartial;
    }

    @Specialization(guards = "isNull(source)")
    public void read(VirtualFrame frame, Object source) {
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame, count);

        throw new IllegalStateException();
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        int index = advanceSourcePositionNoThrow(frame, count, consumePartial);

        if (index == -1) {
            if (consumePartial) {
                return MissingValue.INSTANCE;
            } else {
                return getContext().getCoreLibrary().getNilObject();
            }
        }

        return Arrays.copyOfRange(source, index, index + count);
    }

}