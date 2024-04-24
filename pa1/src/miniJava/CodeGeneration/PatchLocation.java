package miniJava.CodeGeneration;

import miniJava.AbstractSyntaxTrees.MethodDecl;

public class PatchLocation {
    public MethodDecl method;
    public int index;
    public int callAddress;

    public PatchLocation(MethodDecl m, int i,int a)
    {
        method = m;
        index = i;
        callAddress=a;
    }
}
