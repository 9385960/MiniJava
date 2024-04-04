package miniJava.ContextualAnalysis;

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
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
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

public class Identification implements Visitor<Context,Context> {
    ErrorReporter error;

    private boolean insideQref = false;

    private String varname = "";
    private boolean insideDecl = false;
    private boolean insideCall = false;

    public void identify(AST ast, ErrorReporter e)
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
        fd.type.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitMethodDecl(MethodDecl md, Context arg) {
        ScopedIdentification.openScope();
        arg.SetStaticContext(md.isStatic);
        for(ParameterDecl pd: md.parameterDeclList)
        {
            pd.visit(this, arg);
        }
        for (Statement s: md.statementList) {
            s.visit(this, arg.CopyContext());
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
        //decl.type.visit(this,arg);
        ScopedIdentification.addDeclaration(decl.name, decl);
        varname=decl.name;
        return arg;
    }

    @Override
    public Context visitBaseType(BaseType type, Context arg) {
        return arg;
    }

    @Override
    public Context visitClassType(ClassType type, Context arg) {
        if(!ScopedIdentification.IsClass(type.className.spelling))
        {
            error.reportError("Cannot find class "+ type.className.spelling);
        }
        return arg;
    }

    @Override
    public Context visitArrayType(ArrayType type, Context arg) {
        type.eltType.visit(this,arg);
        return arg;
    }

    @Override
    public Context visitBlockStmt(BlockStmt stmt, Context arg) {
        ScopedIdentification.openScope();
        for (Statement s: stmt.sl) {
        	s.visit(this, arg.CopyContext());
        }
        ScopedIdentification.closeScope();
        return arg;
    }

    @Override
    public Context visitVardeclStmt(VarDeclStmt stmt, Context arg) {
        insideDecl = true;
        stmt.varDecl.visit(this,arg);
        stmt.initExp.visit(this, arg);
        insideDecl = false;
        if(stmt.initExp instanceof RefExpr)
        {
            RefExpr exp = (RefExpr)stmt.initExp;
            if(exp.ref instanceof IdRef)
            {
                IdRef id = (IdRef)exp.ref;
                if(ScopedIdentification.IsClass(id.id.spelling)&&!ScopedIdentification.IsScopeVariable(id.id.spelling))
                {
                    error.reportError(id.id.spelling+" cannot be resolved to a variable.");
                }
            }
        }   
        return arg;
    }

    @Override
    public Context visitAssignStmt(AssignStmt stmt, Context arg) {
        stmt.ref.visit(this, arg.CopyContext());
        stmt.val.visit(this, arg);
        if(stmt.val instanceof RefExpr)
        {
            RefExpr exp = (RefExpr)stmt.val;
            if(exp.ref instanceof IdRef)
            {
                IdRef id = (IdRef)exp.ref;
                if(ScopedIdentification.IsClass(id.id.spelling)&&!ScopedIdentification.IsScopeVariable(id.id.spelling))
                {
                    error.reportError(id.id.spelling+" cannot be resolved to a variable.");
                }
            }
        }   
        return arg;
    }

    @Override
    public Context visitIxAssignStmt(IxAssignStmt stmt, Context arg) {
        stmt.ref.visit(this, arg.CopyContext());
        stmt.ix.visit(this, arg);
        stmt.exp.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitCallStmt(CallStmt stmt, Context arg) {
        insideCall = true;
        if(stmt.methodRef instanceof ThisRef)
        {
            error.reportError("Keyword this is not callable");
        }
        stmt.methodRef.visit(this,arg.CopyContext());

        for (Expression e: stmt.argList) {
            e.visit(this, arg);
        }
        insideCall = false;
        return arg;
    }

    @Override
    public Context visitReturnStmt(ReturnStmt stmt, Context arg) {
        if (stmt.returnExpr != null)
            stmt.returnExpr.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitIfStmt(IfStmt stmt, Context arg) {
        stmt.cond.visit(this,arg);
        if(stmt.thenStmt instanceof VarDeclStmt)
        {
            error.reportError("Single variable declaration not permitted after if statement");
        } 
        if(stmt.elseStmt instanceof VarDeclStmt)
        {
            error.reportError("Single variable declaration not permitted after if statement");
        }
        stmt.thenStmt.visit(this,arg);
        if(stmt.elseStmt != null)
        {
            stmt.elseStmt.visit(this, arg);
        }
        return arg;
    }

    @Override
    public Context visitWhileStmt(WhileStmt stmt, Context arg) {
        if(stmt.body instanceof VarDeclStmt)
        {
            error.reportError("Single variable declaration not permitted after while statement");
        }
        stmt.cond.visit(this, arg.CopyContext());
        stmt.body.visit(this, arg.CopyContext());
        return arg;
    }

    @Override
    public Context visitUnaryExpr(UnaryExpr expr, Context arg) {
        expr.operator.visit(this, arg);
        expr.operator.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitBinaryExpr(BinaryExpr expr, Context arg) {
        expr.operator.visit(this, arg);
        expr.left.visit(this, arg);
        expr.right.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitRefExpr(RefExpr expr, Context arg) {
        expr.ref.visit(this, arg.CopyContext());
        return arg;
    }

    @Override
    public Context visitIxExpr(IxExpr expr, Context arg) {
        expr.ref.visit(this, arg.CopyContext());
        expr.ixExpr.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitCallExpr(CallExpr expr, Context arg) {
        insideCall = true;
        expr.functionRef.visit(this, arg.CopyContext());
        for (Expression e: expr.argList) {
            e.visit(this, arg);
        }
        insideCall = false;
        return arg;
    }

    @Override
    public Context visitLiteralExpr(LiteralExpr expr, Context arg) {
        expr.lit.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitNewObjectExpr(NewObjectExpr expr, Context arg) {
        expr.classtype.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitNewArrayExpr(NewArrayExpr expr, Context arg) {
        expr.eltType.visit(this, arg);
        expr.sizeExpr.visit(this, arg);
        return arg;
    }

    @Override
    public Context visitThisRef(ThisRef ref, Context arg) {
        if(arg.GetStaticContext())
        {
            error.reportError("This keyword cannot be used in a static context.");
        }
        return arg;
    }

    @Override
    public Context visitIdRef(IdRef ref, Context arg) {
        ref.id.visit(this, arg);
        arg.SetContextClass(GetTypeFromId(ref.id));
        Boolean isStaticRef = GetStaticFromId(ref.id);
        if((isStaticRef != arg.GetStaticContext())&&!ScopedIdentification.IsScopeVariable(ref.id.spelling)&&!ScopedIdentification.IsClass(ref.id.spelling)&&!insideQref)
        {
            error.reportError("Cannot access member "+ref.id.spelling);
        }
        //arg.SetStaticContext(GetStaticFromId(ref.id));
        return arg;
    }

    @Override
    public Context visitQRef(QualRef ref, Context arg) {
        ref.ref.visit(this, arg);
        insideQref = true;
        ref.id.visit(this, arg);
        insideQref = false;
        if(ref.ref instanceof IdRef)
        {
            IdRef idref = (IdRef)ref.ref;
            if(ScopedIdentification.IsClass(idref.id.spelling))
            {
                if(!GetStaticFromId(ref.id))
                {
                    error.reportError("Cannot access non static member "+ref.id.spelling+" from static context.");
                }
            }
        }
        arg.SetContextClass(GetTypeFromId(ref.id));
        //arg.SetStaticContext(GetStaticFromId(ref.id));
        return arg;
    }

    @Override
    public Context visitIdentifier(Identifier id, Context arg) {
        if(insideDecl&&!insideQref)
        {
            if(id.spelling.equals(varname))
            {
                error.reportError("Cannot reference "+varname+" during assignment.");
            }
        }
        if(id.decl == null)
        {
            Declaration decl = ScopedIdentification.findDeclaration(id.spelling, arg);
            if(!insideCall&&(decl instanceof MethodDecl))
            {
                error.reportError("Must call "+id.spelling+" cannot use as variable.");
            }
            if(decl != null)
            {
                id.decl = decl;
            }else {
                error.reportError("Identifier for "+id.spelling+" does not exist.");
            }
        }        
        return arg;
    }

    @Override
    public Context visitOperator(Operator op, Context arg) {
        return arg;
    }

    @Override
    public Context visitIntLiteral(IntLiteral num, Context arg) {
        return arg;
    }

    @Override
    public Context visitBooleanLiteral(BooleanLiteral bool, Context arg) {
        return arg;
    }

    @Override
    public Context visitNullLiteral(NullLiteral nl, Context arg) {
        return arg;
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
            String postfix = "Array";
            return(cName+postfix);          
        }else{
           return(decl.name);
        }
    }

    private Boolean GetStaticFromId(Identifier id)
    {
        Declaration decl = id.decl;
        if(decl == null)
        {
            return false;
        }
        if(decl instanceof MemberDecl)
        {
            return ((MemberDecl)decl).isStatic;
        }else if(decl instanceof ClassDecl){
            return true;
        }else{
            return false;
        }
    }

}
