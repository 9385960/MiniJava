package miniJava.ContextualAnalysis;

public class Context {

    private String className;

    private String contextClass;

    private boolean inStaticContext = false;

    private String type;

    public void SetType(String t)
    {
        type = t;
    }

    public String GetType()
    {
        return type;
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


    public Context CopyContext()
    {
        Context toReturn = new Context();
        toReturn.SetClassName(this.className);
        toReturn.SetContextClass(this.contextClass);
        toReturn.SetStaticContext(this.inStaticContext);
        toReturn.SetType(this.type);
        return toReturn;
    }
}
