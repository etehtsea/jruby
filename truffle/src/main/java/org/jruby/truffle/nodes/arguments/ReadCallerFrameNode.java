/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

public class ReadCallerFrameNode extends RubyNode {

    private final ConditionProfile hasCallerFrameProfile = ConditionProfile.createBinaryProfile();

    public ReadCallerFrameNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object callerFrame = RubyArguments.getCallerFrame(frame.getArguments());

        if (hasCallerFrameProfile.profile(callerFrame != null)) {
            return callerFrame;
        } else {
            return NotProvided.INSTANCE;
        }
    }

}
