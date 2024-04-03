package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.TypeKind;

public class TypeChecking {
    
    private static ErrorReporter error;


    public static void init(ErrorReporter e)
    {
        error = e;
    }


    public static String GetTypeBinop(String t1, String t2, Operator op)
    {
        if(t1.equals(TypeKind.ERROR.toString())||t2.equals(TypeKind.ERROR.toString()))
        {
            return TypeKind.ERROR.toString();
        }
        String opS = op.spelling;
        if(opS.equals("&&")||opS.equals("||"))
        {
            if(t1.equals(TypeKind.BOOLEAN.toString())&&t2.equals(TypeKind.BOOLEAN.toString()))
            {
                return TypeKind.BOOLEAN.toString();
            }
        }else if(opS.equals(">")||opS.equals(">=")||opS.equals("<")||opS.equals("<="))
        {
            if(t1.equals(TypeKind.INT.toString())&&t2.equals(TypeKind.INT.toString()))
            {
                return TypeKind.BOOLEAN.toString();
            }
        }else if(opS.equals("+")||opS.equals("-")||opS.equals("*")||opS.equals("/"))
        {
            if(t1.equals(TypeKind.INT.toString())&&t2.equals(TypeKind.INT.toString()))
            {
                return TypeKind.INT.toString();
            }
        }else if(opS.equals("==")||opS.equals("!="))
        {
            if((t1.equals(t2)))
            {
                return TypeKind.BOOLEAN.toString();
            }
            if(t1.equals("null")||t2.equals("null"))
            {
                Boolean t1NotObject = t1.equals(TypeKind.INT.toString())||t1.equals(TypeKind.BOOLEAN.toString())||t1.equals("Array"+TypeKind.INT.toString());
                Boolean t2NotObject = t2.equals(TypeKind.INT.toString())||t2.equals(TypeKind.BOOLEAN.toString())||t2.equals("Array"+TypeKind.INT.toString());
                if(!(t1NotObject||t2NotObject))
                {
                    return TypeKind.BOOLEAN.toString();
                }
            }
        }
        error.reportError("Unsuported operation "+op.spelling+" between "+t1+" and "+t2);
        return TypeKind.ERROR.toString();
    }

    public static String GetTypeUnop(String t1, Operator op)
    {
        if(t1.equals(TypeKind.ERROR.toString()))
        {
            return TypeKind.ERROR.toString();
        }
        String opS = op.spelling;
        if(opS.equals("-")||opS.equals("&&"))
        {
            if(t1.equals(TypeKind.INT.toString()))
            {
                return TypeKind.INT.toString();
            }
        }else if(opS.equals("!"))
        {
            if(t1.equals(TypeKind.BOOLEAN.toString()))
            {
                return TypeKind.BOOLEAN.toString();
            }
        }
        error.reportError("Unsuported operation "+op.spelling+" on "+t1);
        return TypeKind.ERROR.toString();
    }
}
