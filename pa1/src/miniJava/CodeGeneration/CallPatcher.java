package miniJava.CodeGeneration;

import java.util.*;

import miniJava.CodeGeneration.x64.InstructionList;
import miniJava.CodeGeneration.x64.ISA.Call;
import miniJava.CodeGeneration.x64.*;

public class CallPatcher {
    HashMap<String,HashMap<String,Integer>> methodstart = new HashMap<>();

    ArrayList<PatchLocation> toPatch = new ArrayList<>();

    public void AddMethod(String className, String methodName, int startAddress)
    {
        if(methodstart.containsKey(className))
        {
            HashMap<String,Integer> methods = methodstart.get(className);
            methods.put(methodName, startAddress);
        }else{
            methodstart.put(className, new HashMap<>());
            HashMap<String,Integer> methods = methodstart.get(className);
            methods.put(methodName, startAddress);
        }
    }

    public void patchCalls(InstructionList ins)
    {
        for (PatchLocation patchLocation : toPatch) {
            int jumpAddress = methodstart.get(patchLocation.contextClass).get(patchLocation.methodName);
            Instruction patch = new Call(patchLocation.callAddress, jumpAddress);
            ins.patch(patchLocation.index, patch);
        }
    }

    public void AddToPatch(PatchLocation p)
    {
        toPatch.add(p);
    }
}
