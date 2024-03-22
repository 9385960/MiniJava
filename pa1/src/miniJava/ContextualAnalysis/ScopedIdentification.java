package miniJava.ContextualAnalysis;
import java.util.*;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MemberDecl;

public class ScopedIdentification {
    private static ErrorReporter error;
    private static HashMap<String,HashMap<String,MemberDecl>> test;
    private static Stack<HashMap<String,Declaration>> siStack;

    public static void AddClass()
    {
        
    }

    public static void AddClassField()
    {

    }

    public static void AddClassMethod()
    {

    }

    public static void GetDeclaration(String id)
    {
        
    }

    public static void AddDec(String type, String id)
    {

    }

    public static void openScope()
    {
        HashMap<String,Declaration> toAdd = new HashMap<String,Declaration>();

        siStack.add(toAdd);
    }

    public void closeScope()
    {
        siStack.pop();
    }

    public boolean addDeclaration(String s, Declaration d)
    {
        for(HashMap<String,Declaration> map : siStack)
        {
            if(map.containsKey(s))
            {
                return false;
            }
        }
        HashMap<String,Declaration> map = siStack.peek();
        map.put(s, d);
        return true;
    }

    public Declaration findDeclaration(String s)
    {



        return null;
    }
}
