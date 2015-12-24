/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.format.nodes.PackGuards;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.runtime.exceptions.CantConvertException;
import org.jruby.truffle.format.runtime.exceptions.NoImplicitConversionException;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * Convert a value to a {@code long}.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToLongNode extends PackNode {

    @Child private CallDispatchHeadNode toIntNode;

    @CompilationFinal private boolean seenInt;
    @CompilationFinal private boolean seenLong;
    @CompilationFinal private boolean seenBignum;

    public ToLongNode(RubyContext context) {
        super(context);
    }

    public abstract long executeToLong(VirtualFrame frame, Object object);

    @Specialization
    public long toLong(VirtualFrame frame, boolean object) {
        CompilerDirectives.transferToInterpreter();
        throw new NoImplicitConversionException(object, "Integer");
    }

    @Specialization
    public long toLong(VirtualFrame frame, int object) {
        return object;
    }

    @Specialization
    public long toLong(VirtualFrame frame, long object) {
        return object;
    }

    @Specialization(guards = "isRubyBignum(object)")
    public long toLong(VirtualFrame frame, DynamicObject object) {
        // A truncated value is exactly what we want
        return Layouts.BIGNUM.getValue(object).longValue();
    }

    @Specialization(guards = "isNil(nil)")
    public long toLongNil(VirtualFrame frame, Object nil) {
        CompilerDirectives.transferToInterpreter();
        throw new NoImplicitConversionException(nil, "Integer");
    }

    @Specialization(guards = {"!isBoolean(object)", "!isInteger(object)", "!isLong(object)", "!isBigInteger(object)", "!isRubyBignum(object)", "!isNil(object)"})
    public long toLong(VirtualFrame frame, Object object) {
        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true, MissingBehavior.RETURN_MISSING));
        }

        final Object value = toIntNode.call(frame, object, "to_int", null);

        if (seenInt && value instanceof Integer) {
            return toLong(frame, (int) value);
        }

        if (seenLong && value instanceof Long) {
            return toLong(frame, (long) value);
        }

        if (seenBignum && PackGuards.isRubyBignum(value)) {
            return toLong(frame, (DynamicObject) value);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (value == DispatchNode.MISSING) {
            throw new NoImplicitConversionException(object, "Integer");
        }

        if (value instanceof Integer) {
            seenInt = true;
            return toLong(frame, (int) value);
        }

        if (value instanceof Long) {
            seenLong = true;
            return toLong(frame, (long) value);
        }

        if (PackGuards.isRubyBignum(value)) {
            seenBignum = true;
            return toLong(frame, (DynamicObject) value);
        }

        // TODO CS 5-April-15 missing the (Object#to_int gives String) part

        throw new CantConvertException("can't convert Object to Integer");
    }

}
