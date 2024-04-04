package miniJava.ContextualAnalysis;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;


public class SinglePassTypeCheck implements Visitor<Context,Context>{

    private ErrorReporter error;

    private boolean insideVoid = false;

    private String varname = "";

    public void identify(AST ast,ErrorReporter e)
    {
        error = e;
        ast.visit(this, new Context());
    }


    @Override
    public Context visitPackage(Package prog, Context arg) {
        for(ClassDecl dec : prog.classDeclList)
        {
            dec.visit(this, arg);
        }
        return arg;
    }




    @Override
    public Context visitClassDecl(ClassDecl cd, Context arg) {
        arg.SetClassName(cd.name);
        arg.SetContextClass(cd.name);
        for(FieldDecl f: cd.fieldDeclList)
        {
            f.visit(this, arg);
        }
        for(MethodDecl m: cd.methodDeclList)
        {
            m.visit(this,arg);
        }
        return arg;
    }




    @Override
    public Context visitFieldDecl(FieldDecl fd, Context arg) {
        return fd.type.visit(this, arg);
    }




    @Override
    public Context visitMethodDecl(MethodDecl md, Context arg) {
        ScopedIdentification.openScope();
        arg.SetStaticContext(md.isStatic);
        if(md.type.typeKind == TypeKind.VOID)
        {
            insideVoid = true;
        }else{
            insideVoid = false;
        }

        for (ParameterDecl pd: md.parameterDeclList) {
            pd.visit(this, null);
        }
        for (Statement s: md.statementList) {
            s.visit(this, null);
        }
        ScopedIdentification.closeScope();
        return arg;
    }




    @Override
    public Context visitParameterDecl(ParameterDecl pd, Context arg) {
        ScopedIdentification.addDeclaration(pd.name, pd);
        pd.type.visit(this,arg);
        return arg;
    }




    @Override
    public Context visitVarDecl(VarDecl decl, Context arg) {
        ScopedIdentification.addDeclaration(decl.name, decl);
        varname=decl.name;
        return arg;
    }




    @Override
    public Context visitBaseType(BaseType type, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBaseType'");
    }




    @Override
    public Context visitClassType(ClassType type, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitClassType'");
    }




    @Override
    public Context visitArrayType(ArrayType type, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitArrayType'");
    }




    @Override
    public Context visitBlockStmt(BlockStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBlockStmt'");
    }




    @Override
    public Context visitVardeclStmt(VarDeclStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitVardeclStmt'");
    }




    @Override
    public Context visitAssignStmt(AssignStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitAssignStmt'");
    }




    @Override
    public Context visitIxAssignStmt(IxAssignStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIxAssignStmt'");
    }




    @Override
    public Context visitCallStmt(CallStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallStmt'");
    }




    @Override
    public Context visitReturnStmt(ReturnStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
    }




    @Override
    public Context visitIfStmt(IfStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
    }




    @Override
    public Context visitWhileStmt(WhileStmt stmt, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
    }




    @Override
    public Context visitUnaryExpr(UnaryExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitUnaryExpr'");
    }




    @Override
    public Context visitBinaryExpr(BinaryExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
    }




    @Override
    public Context visitRefExpr(RefExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitRefExpr'");
    }




    @Override
    public Context visitIxExpr(IxExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIxExpr'");
    }




    @Override
    public Context visitCallExpr(CallExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
    }




    @Override
    public Context visitLiteralExpr(LiteralExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitLiteralExpr'");
    }




    @Override
    public Context visitNewObjectExpr(NewObjectExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNewObjectExpr'");
    }




    @Override
    public Context visitNewArrayExpr(NewArrayExpr expr, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNewArrayExpr'");
    }




    @Override
    public Context visitThisRef(ThisRef ref, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitThisRef'");
    }




    @Override
    public Context visitIdRef(IdRef ref, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdRef'");
    }




    @Override
    public Context visitQRef(QualRef ref, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitQRef'");
    }




    @Override
    public Context visitIdentifier(Identifier id, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
    }




    @Override
    public Context visitOperator(Operator op, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
    }




    @Override
    public Context visitIntLiteral(IntLiteral num, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitIntLiteral'");
    }




    @Override
    public Context visitBooleanLiteral(BooleanLiteral bool, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitBooleanLiteral'");
    }




    @Override
    public Context visitNullLiteral(NullLiteral nl, Context arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visitNullLiteral'");
    }
    
}
