package miniJava.CodeGeneration;

public class PatchLocation {
    public String contextClass;
    public String methodName;
    public int index;
    public int callAddress;

    public PatchLocation(String c, String m, int i,int a)
    {
        contextClass = c;
        methodName = m;
        index = i;
        callAddress=a;
    }
}
