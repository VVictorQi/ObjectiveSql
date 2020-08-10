package com.github.braisdom.funcsql.apt;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import java.util.Arrays;

public class MethodBuilder {

    private final TreeMaker treeMaker;
    private final APTUtils aptUtils;
    private final ListBuffer<JCVariableDecl> parameters;
    private final ListBuffer<JCStatement> statements;

    private JCExpression returnType;
    private ListBuffer<JCExpression> throwsClauses;
    private JCExpression returnStatement;

    MethodBuilder(APTUtils aptUtils) {
        this.treeMaker = aptUtils.getTreeMaker();
        this.aptUtils = aptUtils;

        this.parameters = new ListBuffer<>();
        this.statements = new ListBuffer<>();
        this.throwsClauses = new ListBuffer<>();
    }

    public MethodBuilder setThrowsClauses(Class<? extends Throwable>... throwsClauseArray) {
        for (Class<? extends Throwable> throwsClause : throwsClauseArray)
            throwsClauses.append(aptUtils.typeRef(throwsClause.getName()));
        return this;
    }

    public MethodBuilder setReturnType(Class<?> typeClass, Class<?>... genTypeClass) {
        JCExpression[] genTypes = Arrays.stream(genTypeClass).map(exceptionClass ->
                treeMaker.Throw(aptUtils.typeRef(exceptionClass.getName())).getExpression())
                .toArray(JCExpression[]::new);
        returnType = treeMaker.TypeApply(aptUtils.typeRef(typeClass), List.from(genTypes));
        return this;
    }

    public MethodBuilder setReturnType(Class<?> typeClass, JCExpression... genTypeClass) {
        returnType = treeMaker.TypeApply(aptUtils.typeRef(typeClass), List.from(genTypeClass));
        return this;
    }

    public MethodBuilder setReturnType(JCExpression type, JCExpression... genType) {
        returnType = treeMaker.TypeApply(type, List.from(genType));
        return this;
    }

    public MethodBuilder setReturnType(JCExpression type) {
        returnType = type;
        return this;
    }


    public MethodBuilder setReturnStatement(String varName, String methodName, JCExpression... params) {
        JCTree.JCExpression methodRef = treeMaker.Select(aptUtils.varRef(varName),
                aptUtils.toName(methodName));
        this.returnStatement = treeMaker.Apply(List.nil(), methodRef, List.from(params));
        return this;
    }

    public MethodBuilder setReturnStatement(Class clazz, String methodName, JCExpression... params) {
        JCTree.JCExpression methodRef = treeMaker.Select(aptUtils.typeRef(clazz),
                aptUtils.toName(methodName));
        this.returnStatement = treeMaker.Apply(List.nil(), methodRef, List.from(params));
        return this;
    }


    public MethodBuilder setReturnStatement(JCExpression returnStatement) {
        this.returnStatement = returnStatement;
        return this;
    }

    public MethodBuilder addStatements(List<JCStatement> newStatement) {
        statements.appendList(newStatement);
        return this;
    }

    public MethodBuilder addStatement(JCStatement statement) {
        statements.append(statement);
        return this;
    }

    public MethodBuilder addParameter(String name, JCExpression type, JCExpression... genTypes) {
        addParameter(name, type, List.from(genTypes));
        return this;
    }

    public MethodBuilder addParameter(String name, JCExpression type, List<JCExpression> genTypes) {
        if (genTypes.size() > 0) {
            JCTypeApply typeApply = treeMaker.TypeApply(type, genTypes);
            addParameter(name, typeApply);
        } else addParameter(name, type);
        return this;
    }

    public MethodBuilder addParameter(String name, Class<?> clazz) {
        addParameter(aptUtils.toName(name), clazz);
        return this;
    }

    public MethodBuilder addParameter(Name name, Class<?> clazz) {
        addParameter(name, aptUtils.typeRef(clazz));
        return this;
    }

    public MethodBuilder addParameter(String name, JCExpression type) {
        addParameter(aptUtils.toName(name), type);
        return this;
    }

    public MethodBuilder addParameter(Name name, JCExpression type) {
        treeMaker.at(aptUtils.get().pos);
        parameters.add(treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER), name, type, null));
        return this;
    }

    public MethodBuilder addArrayParameter(String name, Class<?> clazz) {
        addParameter(aptUtils.toName(name), aptUtils.newArrayType(clazz));
        return this;
    }

    public MethodBuilder addVarargsParameter(String name, Class typeClass) {
        addVarargsParameter(aptUtils.toName(name), aptUtils.typeRef(typeClass));
        return this;
    }

    public MethodBuilder addVarargsParameter(String name, JCExpression type) {
        addVarargsParameter(aptUtils.toName(name), type);
        return this;
    }

    public MethodBuilder addVarargsParameter(Name name, JCExpression type) {
        treeMaker.at(aptUtils.get().pos);
        parameters.add(treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER | Flags.VARARGS),
                name, treeMaker.TypeArray(type), null));
        return this;
    }

    public JCTree.JCMethodDecl build(String name, int modifiers) {
        if (returnType == null)
            returnType = treeMaker.TypeIdent(TypeTag.VOID);

        if (returnStatement != null)
            statements.append(treeMaker.Return(returnStatement));

        return treeMaker.MethodDef(treeMaker.Modifiers(modifiers),
                aptUtils.toName(name),
                returnType,
                List.<JCTree.JCTypeParameter>nil(),
                parameters.toList(),
                throwsClauses.toList(),
                treeMaker.Block(0, statements.toList()), null);
    }
}
