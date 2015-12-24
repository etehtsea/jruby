# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def plus_int(a, b)
  a + b
end

Truffle::Interop.export_method(:plus_int)

def fourty_two
  42
end

Truffle::Interop.export_method(:fourty_two)

def ret_nil
  nil
end

Truffle::Interop.export_method(:ret_nil)

$invocations = 0

def count_invocations
  $invocations += 1
end

Truffle::Interop.export_method(:count_invocations)

def apply_numbers(f)
  Truffle::Interop.execute(f, 18, 32) + 10
end

Truffle::Interop.export_method(:apply_numbers)

def compound_object
  obj = Object.new

  def obj.fourtyTwo
    42
  end

  def obj.plus(a, b)
    a + b
  end

  def obj.returnsNull
    nil
  end

  def obj.returnsThis
    self
  end

  obj
end

Truffle::Interop.export_method(:compound_object)

def identity(value)
  value
end

Truffle::Interop.export_method(:identity)

def evaluate_source(mime, source)
  # TODO CS-21-Dec-15 java_string_to_ruby shouldn't be needed - we need to convert j.l.String to Ruby's String automatically

  Truffle::Interop.eval(
      Truffle::Interop.java_string_to_ruby(mime),
      Truffle::Interop.java_string_to_ruby(source))
end

Truffle::Interop.export_method(:evaluate_source)

def complex_add(a, b)
  Truffle::Interop.write_property a, :imaginary, Truffle::Interop.read_property(a, :imaginary) + Truffle::Interop.read_property(b, :imaginary)
  Truffle::Interop.write_property a, :real, Truffle::Interop.read_property(a, :real) + Truffle::Interop.read_property(b, :real)
end

Truffle::Interop.export_method(:complex_add)

def complex_sum_real(complexes)
  complexes = Truffle::Interop.enumerable(complexes)

  complexes.map{ |c| Truffle::Interop.read_property(c, :real) }.inject(&:+)
end

Truffle::Interop.export_method(:complex_sum_real)

def complex_copy(a, b)
  a = Truffle::Interop.enumerable(a)
  b = Truffle::Interop.enumerable(b)

  # TODO CS 21-Dec-15
  # If we don't force b to an array here, the zip below will try to iterate both a and b at the same time. It can't do
  # that with Ruby blocks, so it creates a Fiber (a Java thread) to do it using two separate call stacks. That causes
  # com.oracle.truffle.api.interop.ForeignAccess.checkThread(ForeignAccess.java:133) to fail. What do we do about this?
  b = b.to_a

  a.zip(b).each do |x, y|
    Truffle::Interop.write_property x, :imaginary, Truffle::Interop.read_property(y, :imaginary)
    Truffle::Interop.write_property x, :real, Truffle::Interop.read_property(y, :real)
  end
end

Truffle::Interop.export_method(:complex_copy)
