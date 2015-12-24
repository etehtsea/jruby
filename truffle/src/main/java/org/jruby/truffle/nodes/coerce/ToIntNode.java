/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */


package org.jruby.truffle.nodes.coerce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.FloatNodes;
import org.jruby.truffle.nodes.core.FloatNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.CoreLibrary;
import org.jruby.truffle.runtime.layouts.Layouts;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToIntNode extends RubyNode {

    @Child private CallDispatchHeadNode toIntNode;
    @Child private FloatNodes.ToINode floatToIntNode;

    private final ConditionProfile wasInteger = ConditionProfile.createBinaryProfile();
    private final ConditionProfile wasLong = ConditionProfile.createBinaryProfile();
    private final ConditionProfile wasLongInRange = ConditionProfile.createBinaryProfile();

    public ToIntNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public int doInt(VirtualFrame frame, Object object) {
        // TODO CS 14-Nov-15 this code is crazy - should have separate nodes for ToRubyInteger and ToJavaInt

        final Object integerObject = executeIntOrLong(frame, object);

        if (wasInteger.profile(integerObject instanceof Integer)) {
            return (int) integerObject;
        }

        if (wasLong.profile(integerObject instanceof Long)) {
            final long longValue = (long) integerObject;

            if (wasLongInRange.profile(CoreLibrary.fitsIntoInteger(longValue))) {
                return (int) longValue;
            }
        }

        CompilerDirectives.transferToInterpreter();
        if (RubyGuards.isRubyBignum(object)) {
            throw new RaiseException(getContext().getCoreLibrary().rangeError("bignum too big to convert into `long'", this));
        } else {
            throw new UnsupportedOperationException(object.getClass().toString());
        }
    }

    public abstract Object executeIntOrLong(VirtualFrame frame, Object object);

    @Specialization
    public int coerceInt(int value) {
        return value;
    }

    @Specialization
    public long coerceLong(long value) {
        return value;
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject coerceRubyBignum(DynamicObject value) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().rangeError("bignum too big to convert into `long'", this));
    }

    @Specialization
    public Object coerceDouble(VirtualFrame frame, double value) {
        if (floatToIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            floatToIntNode = insert(FloatNodesFactory.ToINodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null }));
        }
        return floatToIntNode.executeToI(frame, value);
    }

    @Specialization
    public Object coerceBoolean(VirtualFrame frame, boolean value) {
        return coerceObject(frame, value);
    }

    @Specialization(guards = "!isRubyBignum(object)")
    public Object coerceBasicObject(VirtualFrame frame, DynamicObject object) {
        return coerceObject(frame, object);
    }

    private Object coerceObject(VirtualFrame frame, Object object) {
        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        final Object coerced;
        try {
            coerced = toIntNode.call(frame, object, "to_int", null);
        } catch (RaiseException e) {
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getRubyException()) == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "Integer", this));
            } else {
                throw e;
            }
        }

        if (getContext().getCoreLibrary().getLogicalClass(coerced) == getContext().getCoreLibrary().getFixnumClass()) {
            return coerced;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorBadCoercion(object, "Integer", "to_int", coerced, this));
        }
    }

}
