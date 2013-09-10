package org.mariella.persistence.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;;

@Target({ElementType.TYPE}) @Retention(RetentionPolicy.RUNTIME)
public @interface DomainDefinitions {
DomainDefinition[] value();
}
