package com.zzu.dao;

import java.util.List;

import com.zzu.sqlsession.annotations.SelectAnnotation;

public interface UserDao {
	/**
	 * 添加注解
	 * @return
	 */
@SelectAnnotation("select * from user")
	<T>List<T>findAll();
}
