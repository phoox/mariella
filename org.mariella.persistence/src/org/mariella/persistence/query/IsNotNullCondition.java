package org.mariella.persistence.query;

public class IsNotNullCondition implements Expression {
	private final Expression expression;
	
public IsNotNullCondition(Expression expression) {
	super();
	this.expression = expression;
}

public Expression getExpression() {
	return expression;
}
	
@Override
public void printSql(StringBuilder b) {
	expression.printSql(b);
	b.append(" IS NOT NULL");
}

}
