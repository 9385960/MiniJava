package miniJava.ContextualAnalysis;
import java.util.*;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class ScopedIdentification {
    private static ErrorReporter error;
    private static HashMap<String,Declaration> level0 = new HashMap<>();
    private static HashMap<String,HashMap<String,MemberDecl>> level1 = new HashMap<>();
    private static Stack<HashMap<String,Declaration>> siStack = new Stack<>();
    private static String currentClass;

    public static void init(ErrorReporter e)
    {
        error = e;
        AddClassList("System");
        Identifier id = new Identifier(new Token(TokenType.ID, "_PrintStream", null));
        FieldDeclList fields = new FieldDeclList();
		MethodDeclList methods = new MethodDeclList();
        TypeDenoter type = new ClassType(id, null);
        MemberDecl toadd = new FieldDecl(false,true,type,"out",null);
        ParameterDeclList parameterList = new ParameterDeclList();
		StatementList statementList = new StatementList();
        MethodDecl methodtoadd = new MethodDecl(toadd, parameterList, statementList, null);
        AddClassMember("System", "out", methodtoadd);
        methods.add(methodtoadd);
        ClassDecl classDecl = new ClassDecl("System", fields, methods, null);

        AddClass("System", classDecl);


        AddClassList("_PrintStream");

        fields = new FieldDeclList();
		methods = new MethodDeclList();

        type = new BaseType(TypeKind.VOID, null);
        toadd = new FieldDecl(false,false,type,"println",null);
        parameterList = new ParameterDeclList();
        TypeDenoter parameterType = new BaseType(TypeKind.INT, null);
        parameterList.add(new ParameterDecl(parameterType, "n", null));
		statementList = new StatementList();
        methodtoadd = new MethodDecl(toadd, parameterList, statementList, null);
        AddClassMember("_PrintStream", "println", methodtoadd);
        methods.add(methodtoadd);
        classDecl = new ClassDecl("_PrintStream", fields, methods, null);
        AddClass("_PrintStream", classDecl);

        //TypeDenoter string = new BaseType(TypeKind.UNSUPPORTED, null);
        fields = new FieldDeclList();
		methods = new MethodDeclList();
        AddClassList("String");
        classDecl = new ClassDecl("String", fields, methods, null);
        AddClass("String", classDecl);
        //Declaration dec = new VarDecl(string, "String", null);
        TypeChecking.init(e);
    }


    public static void AddClass(String n,Declaration decl)
    {
        //System.out.println("added "+n+" to the classes declaired");
        if(level0.containsKey(n))
        {
            error.reportError("Class " + n + " has already been defined.");
        }
        level0.put(n,decl);
    }

    public static void SetCurrentClass(String c)
    {
        currentClass = c;
    }

    public static void AddClassList(String n)
    {
        level1.put(n,new HashMap<String,MemberDecl>());
    }

    public static void AddClassMember(String c, String m, MemberDecl dec)
    {
        HashMap<String,MemberDecl> classMembers = level1.get(c);
        if(classMembers.containsKey(m))
        {
            error.reportError("Identification Error: " + m + " already exists");
            return ;
        }
        classMembers.put(m, dec);
    }

    public static void AddCurrentClassMember(String m, MemberDecl dec)
    {
        //System.out.println("added "+m+" to the "+currentClass+" class");
        HashMap<String,MemberDecl> classMembers = level1.get(currentClass);
        if(classMembers.containsKey(m))
        {
            error.reportError("Identification Error: " + m + " already exists");
            return ;
        }
        classMembers.put(m,dec);
    }

    public static void openScope()
    {
        HashMap<String,Declaration> toAdd = new HashMap<String,Declaration>();

        siStack.add(toAdd);
    }

    public static void closeScope()
    {
        siStack.pop();
    }

    public static boolean addDeclaration(String s, Declaration d)
    {
        for(HashMap<String,Declaration> map : siStack)
        {
            if(map.containsKey(s))
            {
                error.reportError("Identification Error: " + s + " already exists");
                return false;
            }
        }
        HashMap<String,Declaration> map = siStack.peek();
        map.put(s, d);
        if(d.type instanceof ClassType)
        {
            String s1 = ((ClassType)d.type).className.spelling;
            if(!level0.containsKey(s1))
            {
                error.reportError("Class "+s1+" does not exist");
            }
        }
        //System.out.println("ID "+s+" declaired with type "+d.type.toString());
        return true;
    }

    public static Declaration findDeclaration(String s, Context c)
    {
        for(HashMap<String,Declaration> map : siStack)
        {
            if(map.containsKey(s))
            {
                return map.get(s);
            }
        }
        if(level0.containsKey(c.GetContextClass()))
        {
            HashMap<String,MemberDecl> members = level1.get(c.GetContextClass());
            if(members.containsKey(s))
            {
                MemberDecl dec = members.get(s);
                if(dec.isStatic)
                {
                    if(c.GetStaticContext())
                    {
                        if(dec.isPrivate)
                        {
                            if(c.GetClassName().equals(c.GetContextClass()))
                            {
                                return dec;
                            }else{
                                error.reportError("Cannot acces private member "+s+" of "+c.GetContextClass()+" in "+c.GetClassName());
                                return null;
                            }
                        }
                    }else{
                        error.reportError("Cannot acces static member "+s+" in a non static context");
                    }      
                }else{
                    if(!c.GetStaticContext())
                    {
                        if(dec.isPrivate)
                        {
                            if(c.GetClassName().equals(c.GetContextClass()))
                            {
                                return dec;
                            }else{
                                error.reportError("Cannot acces private member "+s+" of "+c.GetContextClass()+" in "+c.GetClassName());
                                return null;
                            }
                        }
                    }else{
                        error.reportError("Cannot acces non static member "+s+" in a static context");
                    }
                }
                if(dec.isPrivate)
                {
                    if(c.GetClassName().equals(c.GetContextClass()))
                    {
                        return dec;
                    }else{
                        error.reportError("Cannot acces private member "+s+" of "+c.GetContextClass()+" in "+c.GetClassName());
                        return null;
                    }
                }else{
                    return dec;
                }
            }
        }
        if(level0.containsKey(s))
        {
            return level0.get(s);
        }
        return null;
    }
    public static boolean IsClass(String s)
    {
        return level0.containsKey(s);
    }
    public static boolean IsScopeVariable(String s)
    {
        for(HashMap<String,Declaration> map : siStack)
        {
            if(map.containsKey(s))
            {
                return true;
            }
        }

        return false;
    }
}
