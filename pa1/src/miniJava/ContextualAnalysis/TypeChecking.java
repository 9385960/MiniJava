package miniJava.ContextualAnalysis;

import java.lang.ProcessBuilder.Redirect.Type;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.SyntacticAnalyzer.TokenType;

public class TypeChecking {
    
    private static ErrorReporter error;


    public static void init(ErrorReporter e)
    {
        error = e;
    }


    public static TypeKind GetType(TypeDenoter t1, TypeDenoter t2, Operator op)
    {
        String opS = op.spelling;
        if(opS.equals("&&")||opS.equals("&&"))
        {
            if(!(t1.typeKind == TypeKind.BOOLEAN)||!(t1.typeKind == TypeKind.BOOLEAN))
            {
                return TypeKind.BOOLEAN;
            }
        }
        
        return TypeKind.ERROR;
    }

}
