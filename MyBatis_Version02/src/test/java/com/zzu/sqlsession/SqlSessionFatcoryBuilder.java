package com.zzu.sqlsession;

import java.io.InputStream;

import com.zzu.sqlsession.defaults.DefaultSqlSessionFactory;

public class SqlSessionFatcoryBuilder {

	public SqlSessionFactory build(InputStream in) {
		DefaultSqlSessionFactory factory = new DefaultSqlSessionFactory();
		factory.setConfig(in);
		return factory;
	}

}
