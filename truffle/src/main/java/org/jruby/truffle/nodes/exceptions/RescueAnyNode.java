/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.IsANode;
import org.jruby.truffle.nodes.objects.IsANodeGen;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Rescues any exception where {@code $!.is_a?(StandardError)}.
 */
public class RescueAnyNode extends RescueNode {

    @Child private IsANode isANode;

    public RescueAnyNode(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection, body);
        isANode = IsANodeGen.create(context, sourceSection, null, null);
    }

    @Override
    public boolean canHandle(VirtualFrame frame, DynamicObject exception) {
        return isANode.executeIsA(exception, getContext().getCoreLibrary().getStandardErrorClass());
    }

}
