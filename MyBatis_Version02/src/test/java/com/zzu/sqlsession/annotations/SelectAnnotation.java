package com.zzu.sqlsession.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 自定义注解-----配合注解方式实现自定义的MyBatis
 * @author hotwater
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SelectAnnotation {
		//属性值
		String value();
}
