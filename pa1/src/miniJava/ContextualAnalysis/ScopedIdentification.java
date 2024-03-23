package miniJava.ContextualAnalysis;
import java.util.*;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class ScopedIdentification {
    private static ErrorReporter error;
    private static HashMap<String,ClassDecl> level0 = new HashMap<>();
    private static HashMap<String,HashMap<String,MemberDecl>> level1 = new HashMap<>();
    private static Stack<HashMap<String,Declaration>> siStack = new Stack<>();
    private static String currentClass;

    public static void init(ErrorReporter e)
    {
        error = e;
        /*AddClass("System");
        Identifier id = new Identifier(new Token(TokenType.ID, "_PrintStream", null));
        TypeDenoter type = new ClassType(id, null);
        MemberDecl toadd = new FieldDecl(false,true,type,"out",null);
        ParameterDeclList parameterList = new ParameterDeclList();
		StatementList statementList = new StatementList();
        toadd = new MethodDecl(toadd, parameterList, statementList, null);
        AddClassMember("System", "out", toadd);
        AddClass("_PrintStream");
        type = new BaseType(TypeKind.VOID, null);
        toadd = new FieldDecl(false,false,type,"println",null);
        parameterList = new ParameterDeclList();
        TypeDenoter parameterType = new BaseType(TypeKind.INT, null);
        parameterList.add(new ParameterDecl(parameterType, "n", null));
		statementList = new StatementList();
        toadd = new MethodDecl(toadd, parameterList, statementList, null);
        AddClassMember("_PrintStream", "println", toadd);*/

    }


    public static void AddClass(String n,ClassDecl decl)
    {
        System.out.println("added "+n+" to the classes declaired");
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
        classMembers.put(m, dec);
    }

    public static void AddCurrentClassMember(String m, MemberDecl dec)
    {
        System.out.println("added "+m+" to the "+currentClass+" class");
        HashMap<String,MemberDecl> classMembers = level1.get(currentClass);
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
        if(level0.containsKey(s))
        {
            return level0.get(s);
        }
        if(level0.containsKey(c.GetContextClass()))
        {
            HashMap<String,MemberDecl> members = level1.get(c.GetContextClass());
            if(members.containsKey(s))
            {
                MemberDecl dec = members.get(s);
                if(dec.isPrivate)
                {
                    if(c.GetClassName().equals(c.GetContextClass()))
                    {
                        return dec;
                    }else{
                        return null;
                    }
                }else{
                    return dec;
                }
            }
        }
        return null;
    }
}
