/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.callgraph;

public class CallsMethod implements Calls {

    private final MethodVersion methodVersion;

    public CallsMethod(MethodVersion methodVersion) {
        this.methodVersion = methodVersion;
    }

    public MethodVersion getMethodVersion() {
        return methodVersion;
    }
}
