/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.nodes.objects.SingletonClassNodeGen;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

public class GetDefaultDefineeNode extends RubyNode {

    @Child private SingletonClassNode singletonClassNode;

    public GetDefaultDefineeNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
    }

    @Override
    public DynamicObject execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        return RubyArguments.getDeclarationContext(frame.getArguments()).getModuleToDefineMethods(frame, getContext(), singletonClassNode);
    }
}
