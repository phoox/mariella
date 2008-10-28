package org.mariella.glue.service;

import java.util.List;

import org.mariella.persistence.mapping.ClassMapping;
import org.mariella.persistence.query.QueryBuilder;
import org.mariella.persistence.schema.ClassDescription;


public class QueryResult<E> {
	private final List<E> result;
	private final List<String> resultPropertyNames;
	private final QueryBuilder queryBuilder;

public QueryResult(QueryBuilder queryBuilder, List<String> resultPropertyNames, List<E> result) {
	super();
	this.queryBuilder = queryBuilder;
	this.resultPropertyNames = resultPropertyNames;
	this.result = result;
}

public List<E> getResult() {
	return result;
}

public QueryBuilder getQueryBuilder() {
	return queryBuilder;
}

public List<String> getResultPropertyNames() {
	return resultPropertyNames;
}

public ClassDescription getRootClassDescription() {
	ClassMapping classMapping = queryBuilder.getClassMapping("root");
	return classMapping == null ? null : classMapping.getClassDescription();
}

}
