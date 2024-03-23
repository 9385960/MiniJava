package miniJava.ContextualAnalysis;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;


public class Identification implements Visitor<Context,Object>{

    private ErrorReporter error;

    public void identify(AST ast,ErrorReporter e)
    {
        error = e;
        ast.visit(this, new Context());
    }


    @Override
    public Object visitPackage(Package prog, Context arg) {
        printIndent(arg);
        //System.out.println("package : "+prog.toString());
        for(ClassDecl dec : prog.classDeclList)
        {
            Context param = new Context();
            param.SetDepth(arg.GetDepth()+1);
            dec.visit(this, param);
        }
        return arg;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Context arg) {
        arg.SetClassName(cd.name);
        arg.SetContextClass(cd.name);
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();

        printIndent(arg);
        //System.out.println("class : "+cd.toString());
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
        
        printIndent(arg);
        //System.out.println("field decl : "+fd.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        fd.type.visit(this,nextArg);
        arg.SetStaticContext(nextArg.GetStaticContext());
        return arg;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Context arg) {
        
        ScopedIdentification.openScope();
        printIndent(arg);
        //System.out.println("Method decl : "+md.toString()+" : is static "+md.isStatic);
        arg.SetStaticContext(md.isStatic);
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        
        for (ParameterDecl pd: md.parameterDeclList) {
            pd.visit(this, nextArg.CopyContext());
        }
        for (Statement s: md.statementList) {
            s.visit(this, nextArg.CopyContext());
        }
        ScopedIdentification.closeScope();
        return arg;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Context arg) {
        ScopedIdentification.addDeclaration(pd.name, pd);
        printIndent(arg);
        //System.out.println("Parameter decl : "+pd.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        pd.type.visit(this,nextArg);
        return arg;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Context arg) {
        printIndent(arg);
        //System.out.println("var decl : "+decl.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        decl.type.visit(this, nextArg);
        ScopedIdentification.addDeclaration(decl.name,decl);
        //System.out.println("added new declaration : " + decl.name);
        return arg;
    }

    @Override
    public Object visitBaseType(BaseType type, Context arg) {
        
        printIndent(arg);
        //System.out.println("found base Type "+type.typeKind.toString());
        //System.out.println("Base Type : " + type.toString());
        //arg.SetType(type.typeKind.toString());
        return arg;
    }

    @Override
    public Object visitClassType(ClassType type, Context arg) {
        printIndent(arg);
        //System.out.println("class type : "+type.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        //arg.SetType(type.className.spelling);
        type.className.visit(this, nextArg);
        arg.SetStaticContext(nextArg.GetStaticContext());
        return arg;
    }

    @Override
    public Object visitArrayType(ArrayType type, Context arg) {
        printIndent(arg);
        //System.out.println("Array Type : "+type.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        type.eltType.visit(this, nextArg);
        return arg;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Context arg) {
        ScopedIdentification.openScope();
        printIndent(arg);
        //System.out.println("Block stmt : "+stmt.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        for (Statement s: stmt.sl) {
        	s.visit(this, nextArg.CopyContext());
        }
        ScopedIdentification.closeScope();
        return arg;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("VarDeclStmt : "+stmt.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        stmt.varDecl.visit(this, nextArg.CopyContext());	
        stmt.initExp.visit(this, nextArg.CopyContext());
        return arg;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("Assign stmt : "+stmt.toString());
        Context firstArg = arg.CopyContext();
        firstArg.IncrementDepth();
        Context secondArg = arg.CopyContext();
        firstArg.IncrementDepth();
        stmt.ref.visit(this, firstArg);
        String t1 = firstArg.GetType();
        //System.out.println("First type : "+t1);
        stmt.val.visit(this, secondArg);
        String t2 = secondArg.GetType();
        //System.out.println("Second  type : "+t2);
        arg.SetType(t1);
        if(t1 != t2)
        {
            error.reportError("Cannot assign type " + t2 + " to type "+t1);
        }
        return arg;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("Indexed Assign stmt : ");
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        stmt.ref.visit(this, nextArg.CopyContext());
        
        stmt.ix.visit(this, nextArg.CopyContext());
        stmt.exp.visit(this, nextArg.CopyContext());
        return arg;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("Call stmt : "+stmt.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();


        stmt.methodRef.visit(this, nextArg.CopyContext());
        for (Expression e: stmt.argList) {
            e.visit(this, nextArg.CopyContext());
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("Return stmt : "+stmt.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        if (stmt.returnExpr != null)
            stmt.returnExpr.visit(this, nextArg);
        return arg;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("If stmt : "+stmt.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        stmt.cond.visit(this, nextArg);
        if(!nextArg.GetType().equals(TypeKind.BOOLEAN.toString()))
        {
            error.reportError("If statement expression needs to evaluate to a boolean");
        }
        stmt.thenStmt.visit(this, nextArg.CopyContext());
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, nextArg.CopyContext());
        return arg;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Context arg) {
        printIndent(arg);
        //System.out.println("while stmt : "+stmt.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        stmt.cond.visit(this, nextArg.CopyContext());
        stmt.body.visit(this, nextArg.CopyContext());
        return arg;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("Unary Expr : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        expr.operator.visit(this, nextArg.CopyContext());
        expr.expr.visit(this, nextArg);
        arg.SetType(TypeChecking.GetTypeUnop(nextArg.GetType(), expr.operator));
        return arg;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("Binary Expr : ");
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        expr.operator.visit(this, nextArg);

        expr.left.visit(this, nextArg);
        String t1 = nextArg.GetType();
        expr.right.visit(this, nextArg);
        String t2 = nextArg.GetType();
        //System.out.println(t1 + expr.operator.spelling+t2);
        arg.SetType(TypeChecking.GetTypeBinop(t1, t2, expr.operator));
        //System.out.println("Result Type : "+arg.GetType());
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("Reference Expression : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        expr.ref.visit(this, nextArg);
        arg.SetType(nextArg.GetType());
        return arg;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("Index expr : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        expr.ref.visit(this, nextArg.CopyContext());
        expr.ixExpr.visit(this, nextArg.CopyContext());
        return arg;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("Call Expression : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();

        expr.functionRef.visit(this, nextArg.CopyContext());

        for (Expression e: expr.argList) {
            e.visit(this, nextArg.CopyContext());
        }

        return arg;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("Literal Expr : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();

        expr.lit.visit(this, nextArg);
        arg.SetType(nextArg.GetType());
        return arg;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("New Object expr : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();

        expr.classtype.visit(this, nextArg);
        arg.SetType(nextArg.GetType());
        return arg;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Context arg) {
        printIndent(arg);
        //System.out.println("new array expr : "+expr.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();

        expr.eltType.visit(this, nextArg.CopyContext());
        expr.sizeExpr.visit(this, nextArg.CopyContext());

        return arg;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Context arg) {
        printIndent(arg);
        //System.out.println("this ref : "+ref.toString());
        arg.SetType(arg.GetClassName());
        if(arg.GetStaticContext())
        {
            error.reportError("This keyword cannot be used in a static context.");
        }
        return arg;
    }

    @Override
    public Object visitIdRef(IdRef ref, Context arg) {
        printIndent(arg);
        //System.out.println("id ref : "+ref.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        ref.id.visit(this, nextArg);
        arg.SetType(nextArg.GetType());
        arg.SetStaticContext(nextArg.GetStaticContext());
        return arg;
    }

    @Override
    public Object visitQRef(QualRef ref, Context arg) {
        printIndent(arg);
        //System.out.println("q ref : "+ref.toString());
        Context nextArg = arg.CopyContext();
        nextArg.IncrementDepth();
        //System.out.println("Resolving q ref in class " + arg.GetClassName()+" in the context of " + arg.GetContextClass());
        ref.ref.visit(this, nextArg);
        nextArg.SetContextClass(nextArg.GetType());
        arg.SetStaticContext(nextArg.GetStaticContext());
        ref.id.visit(this, nextArg);
        arg.SetStaticContext(nextArg.GetStaticContext());
        //ref.id.decl.visit(this, nextArg);
        arg.SetType(nextArg.GetType());
    	
        return arg;
    }

    @Override
    public Object visitIdentifier(Identifier id, Context arg) {
        printIndent(arg);
        //System.out.println("identifier in "+arg.GetClassName()+" with context "+arg.GetContextClass()+" : "+id.spelling+" and is static " + arg.GetStaticContext());
        if(id.decl == null)
        {
            Declaration decl = ScopedIdentification.findDeclaration(id.spelling, arg);
            if(decl != null)
            {
                //System.out.println("Declaration found");
                id.decl = decl;
                SetTypeDecl(arg,decl);
                if(ScopedIdentification.IsClass(id.spelling))
                {
                    arg.SetStaticContext(true);
                }
            }else {
                //System.out.println("Declaration not found");
                error.reportError("Identifier for "+id.spelling+" does not exist.");
            }
        }else{
            SetTypeDecl(arg, id.decl);
        }
        
        return arg;
    }


    @Override
    public Object visitOperator(Operator op, Context arg) {
        printIndent(arg);
        //System.out.println("operator : "+op.toString());
        
        return arg;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Context arg) {
        printIndent(arg);
        //System.out.println("Int literal : "+num.toString());
        
        arg.SetType(TypeKind.INT.toString());
        return arg;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Context arg) {
        printIndent(arg);
        //System.out.println("Block stmt : "+bool.toString());
        arg.SetType(TypeKind.BOOLEAN.toString());
        return arg;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nl, Context arg)
    {
        printIndent(arg);
        //System.out.println("Block stmt : "+nl.toString());
        arg.SetType("null");
        return arg;
    }

    private void printIndent(Context arg)
    {
        for(int i = 0; i < arg.GetDepth(); i++)
        {
            //System.out.print("  ");
        }
    }

    private void SetTypeDecl(Context arg, Declaration decl) {
       
        if(decl.type instanceof BaseType)
        {
            arg.SetType(((BaseType)decl.type).typeKind.toString());
        }else if(decl.type instanceof ClassType)
        {
            arg.SetType(((ClassType)decl.type).className.spelling);
        }else{
            arg.SetType(decl.name);
        }
        //System.out.println("Type decl "+arg.GetType());
    }
    
}
