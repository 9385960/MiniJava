package miniJava.CodeGeneration;

import java.util.*;

import miniJava.CodeGeneration.x64.InstructionList;
import miniJava.CodeGeneration.x64.ISA.Call;
import miniJava.CodeGeneration.x64.*;
import miniJava.AbstractSyntaxTrees.MethodDecl;

public class CallPatcher {
    HashMap<MethodDecl,Integer> methodstart = new HashMap<>();

    ArrayList<PatchLocation> toPatch = new ArrayList<>();

    public void AddMethod(MethodDecl method, int startAddress)
    {
        methodstart.put(method,startAddress);
    }

    public void patchCalls(InstructionList ins)
    {
        for (PatchLocation patchLocation : toPatch) {
            int jumpAddress = methodstart.get(patchLocation.method);
            Instruction patch = new Call(patchLocation.callAddress, jumpAddress);
            ins.patch(patchLocation.index, patch);
        }
    }

    public void AddToPatch(PatchLocation p)
    {
        toPatch.add(p);
    }
}
