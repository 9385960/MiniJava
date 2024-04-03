package miniJava.ContextualAnalysis;

import javax.lang.model.type.TypeKind;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.TokenType;

public class TypeCheck implements Visitor<String,String> {

    private ErrorReporter error;

    private String currentClass;

    public void typecheck(AST ast,ErrorReporter e)
    {
        error = e;
        ast.visit(this, null);
    }

    @Override
    public String visitPackage(Package prog, String arg) {
        
        for(ClassDecl dec : prog.classDeclList)
        {
            dec.visit(this, null);
        }
        return "";
    }

    @Override
    public String visitClassDecl(ClassDecl cd, String arg) {
        currentClass = cd.name;
        for (FieldDecl f: cd.fieldDeclList)
        {
            f.visit(this, null);
        }
        for (MethodDecl m: cd.methodDeclList)
        {
            m.visit(this, null);
        }
        return arg;
    }

    @Override
    public String visitFieldDecl(FieldDecl fd, String arg) {
        return fd.type.visit(this,null);
    }

    @Override
    public String visitMethodDecl(MethodDecl md, String arg) {
             
        for (ParameterDecl pd: md.parameterDeclList) {
            pd.visit(this, null);
        }
        for (Statement s: md.statementList) {
            s.visit(this, null);
        }
        
        return "";
    }

    @Override
    public String visitParameterDecl(ParameterDecl pd, String arg) {
        return pd.type.visit(this, null);
    }

    @Override
    public String visitVarDecl(VarDecl decl, String arg) {
        return decl.type.visit(this, null);
    }

    @Override
    public String visitBaseType(BaseType type, String arg) {
        return type.typeKind.toString();
    }

    @Override
    public String visitClassType(ClassType type, String arg) {
        return type.className.spelling;
    }

    @Override
    public String visitArrayType(ArrayType type, String arg) {
        return "Array"+type.eltType.visit(this, null);
    }

    @Override
    public String visitBlockStmt(BlockStmt stmt, String arg) {
        for (Statement statement : stmt.sl) {
            statement.visit(this,null);
        }
        return "";
    }

    @Override
    public String visitVardeclStmt(VarDeclStmt stmt, String arg) {
        String t1 = stmt.varDecl.visit(this,null);
        String t2 = stmt.initExp.visit(this, null);
        if(!t1.equals(t2)&&!t2.equals("null")&&!t2.equals("ERROR"))
        {
            error.reportError("Cannot assign type "+t2+" to type "+t1);
        }
        return "";
    }

    @Override
    public String visitAssignStmt(AssignStmt stmt, String arg) {
        String t1 = stmt.ref.visit(this,null);
        String t2 = stmt.val.visit(this, null);
        if(!t1.equals(t2)&&!t2.equals("null")&&!t2.equals("ERROR"))
        {
            error.reportError("Cannot assign type " + t2 + " to type "+t1);
        }

        return "";
    }

    @Override
    public String visitIxAssignStmt(IxAssignStmt stmt, String arg) {
        String t1 = stmt.ref.visit(this, null).substring(5);
        String t2 = stmt.ix.visit(this, null);
        String t3 = stmt.exp.visit(this, null);
        if(!t2.equals("INT"))
        {
            error.reportError("Cannot index into an array with type "+t2);
        }
        if(!t1.equals(t3)&&!t3.equals("null")&&!t2.equals("ERROR"))
        {
            error.reportError("Cannot assign type " + t3 + " to type "+t1);
        }
        return t1;
    }

    @Override
    public String visitCallStmt(CallStmt stmt, String arg) {
        //TODO 
        return stmt.methodRef.visit(this, null);
    }

    @Override
    public String visitReturnStmt(ReturnStmt stmt, String arg) {
        // TODO Auto-generated method stub
        return stmt.returnExpr.visit(this, null);
    }

    @Override
    public String visitIfStmt(IfStmt stmt, String arg) {
        //TODO
        String t1 = stmt.cond.visit(this, null);
        if(!t1.equals(TokenType.BOOLEAN.toString()))
        {
            error.reportError("Expression inside if statement must be a boolean");
        }
        return "";
    }

    @Override
    public String visitWhileStmt(WhileStmt stmt, String arg) {
        //TODO
        String t1 = stmt.cond.visit(this, null);
        if(!t1.equals(TokenType.BOOLEAN.toString()))
        {
            error.reportError("Expression inside while statement must be a boolean");
        }
        return "";
    }

    @Override
    public String visitUnaryExpr(UnaryExpr expr, String arg) {
        String t = expr.expr.visit(this, null);
        return TypeChecking.GetTypeUnop(t, expr.operator);
    }

    @Override
    public String visitBinaryExpr(BinaryExpr expr, String arg) {
        String t1 = expr.left.visit(this, null);
        String t2 = expr.right.visit(this, null);
        return TypeChecking.GetTypeBinop(t1, t2, expr.operator);
    }

    @Override
    public String visitRefExpr(RefExpr expr, String arg) {
        return expr.ref.visit(this,null);
    }

    @Override
    public String visitIxExpr(IxExpr expr, String arg) {
        String t1 = expr.ixExpr.visit(this,null);
        if(!t1.equals("INT"))
        {
            error.reportError("Cannot index into array with non-integer");
        }
        return expr.ref.visit(this,null).substring(5);
    }

    @Override
    public String visitCallExpr(CallExpr expr, String arg) {
        return expr.functionRef.visit(this, null);
    }

    @Override
    public String visitLiteralExpr(LiteralExpr expr, String arg) {
        return expr.lit.visit(this, null);
    }

    @Override
    public String visitNewObjectExpr(NewObjectExpr expr, String arg) {
        //ToDo return new object type
        return expr.classtype.visit(this, null);
    }

    @Override
    public String visitNewArrayExpr(NewArrayExpr expr, String arg) {
        // TODO return new array type
        return "Array"+expr.eltType.visit(this, null);
    }

    @Override
    public String visitThisRef(ThisRef ref, String arg) {
        // TODO return type of this
        return currentClass;
    }

    @Override
    public String visitIdRef(IdRef ref, String arg) {
        return ref.id.visit(this, "");
    }

    @Override
    public String visitQRef(QualRef ref, String arg) {
        //TODO return q ref type
        return "";
    }

    @Override
    public String visitIdentifier(Identifier id, String arg) {
        //TODO return identifier type
        if(id.decl == null)
        {
            return "";
        }
        return GetTypeFromId(id);
    }

    @Override
    public String visitOperator(Operator op, String arg) {
        return "";
    }

    @Override
    public String visitIntLiteral(IntLiteral num, String arg) {
        return TypeKind.INT.toString();
    }

    @Override
    public String visitBooleanLiteral(BooleanLiteral bool, String arg) {
        return TypeKind.BOOLEAN.toString();
    }

    @Override
    public String visitNullLiteral(NullLiteral nl, String arg) {
        return "null";
    }
    
    private String GetTypeFromId(Identifier id)
    {
        Declaration decl = id.decl;
        if(decl == null)
        {
            return null;
        }
        if(decl.type instanceof BaseType)
        {
            return ((BaseType)decl.type).typeKind.toString();
        }else if(decl.type instanceof ClassType)
        {
            return ((ClassType)decl.type).className.spelling;
        }else if(decl.type instanceof ArrayType)
        {
            TypeDenoter type = ((ArrayType)decl.type).eltType;
            String cName = "";
            if(type instanceof BaseType)
            {
                cName = type.typeKind.toString();
            }else if(type instanceof ClassType){
                cName = ((ClassType)type).className.spelling;
            }
            String prefix = "Array";
            return(prefix+cName);          
        }else{
           return(decl.name);
        }
    }
}
