package miniJava.ContextualAnalysis;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;


public class Identification implements Visitor<Context,Object>{


    @Override
    public Object visitPackage(Package prog, Context arg) {
        for(int i = 0; i < arg.GetDepth(); i++)
        {
            System.out.print(" ");
        }
        System.out.println("package");
        for(ClassDecl dec : prog.classDeclList)
        {
            Context param = new Context();
            param.SetDepth(arg.GetDepth()+1);
            dec.visit(this, param);
        }
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Context arg) {
        arg.SetClassName(cd.name);

        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();

        for(int i = 0; i < arg.GetDepth(); i++)
        {
            System.out.print(" ");
        }
        System.out.println("class");
        for (FieldDecl f: cd.fieldDeclList)
        {
            f.visit(this, nextArg);
        }
        for (MethodDecl m: cd.methodDeclList)
        {
            m.visit(this, nextArg);
        }
        return arg;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Context arg) {
        // TODO Auto-generated method stub
        for(int i = 0; i < arg.GetDepth(); i++)
        {
            System.out.print(" ");
        }
        System.out.println("field decl");
        fd.visit(this,arg.CopyContext());
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Context arg) {
        decl.type.visit(this, arg);
        String className = arg.GetType();
        String id = decl.name;
        ScopedIdentification.AddDec(className,id);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Context arg) {
        // TODO Auto-generated method stub
        arg.SetType(type.typeKind.toString());
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Context arg) {
        // TODO Auto-generated method stub
        type.className.visit(this, arg);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Context arg) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
