package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.dataflow.analyses.BindingStorePlacementProblem;
import org.jruby.compiler.ir.dataflow.analyses.BindingLoadPlacementProblem;
import org.jruby.compiler.ir.representations.CFGData;

public class AddBindingInstructions implements CompilerPass {
    public boolean isPreOrder() {
        return false;
    }

    public void run(IRScope s) {
        if (!(s instanceof IRMethod)) return;

        IRMethod m = (IRMethod) s;
        //        if (m.requiresBinding()) {
        CFGData cfgData = m.getCFGData();
        BindingStorePlacementProblem fsp = new BindingStorePlacementProblem();
        fsp.setup(cfgData);
        fsp.compute_MOP_Solution();
        fsp.addStoreAndBindingAllocInstructions();

        BindingLoadPlacementProblem frp = new BindingLoadPlacementProblem();
        frp.setup(cfgData);
        frp.compute_MOP_Solution();
        frp.addLoads();
        //       }
    }
}
