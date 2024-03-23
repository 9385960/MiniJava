package miniJava.ContextualAnalysis;

public class Context {
    private int depth = 0;

    private String type;

    private String className;

    private String contextClass;

    private boolean inStaticContext = false;

    public void IncrementDepth()
    {
        depth += 1;
    }

    public void SetContextClass(String c)
    {
        contextClass = c;
    }

    public void SetStaticContext(boolean s)
    {
        inStaticContext = s;
    }

    public boolean GetStaticContext()
    {
        return inStaticContext;
    }

    public String GetContextClass()
    {
        return contextClass;
    }

    public void SetClassName(String c)
    {
        className = c;
    }

    public String GetClassName()
    {
        return className;
    }

    public int GetDepth()
    {
        return depth;
    }

    public void SetDepth(int d)
    {
        depth = d;
    }

    public void SetType(String s)
    {
        type = s;
    }

    public String GetType()
    {
        return type;
    }

    public Context CopyContext()
    {
        Context toReturn = new Context();
        toReturn.SetClassName(this.className);
        toReturn.SetDepth(this.depth);
        toReturn.SetType(this.type);
        toReturn.SetContextClass(this.contextClass);
        return toReturn;
    }
}
