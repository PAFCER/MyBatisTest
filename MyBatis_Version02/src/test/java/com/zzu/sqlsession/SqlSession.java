package com.zzu.sqlsession;

public interface SqlSession {

	<T>T getMapper(Class<T> T);

}
