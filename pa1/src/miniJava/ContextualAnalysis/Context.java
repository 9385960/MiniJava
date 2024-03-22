package miniJava.ContextualAnalysis;

public class Context {
    private int depth = 0;

    private String type;

    private String className;

    public void IncrementDepth()
    {
        depth += 1;
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
}
