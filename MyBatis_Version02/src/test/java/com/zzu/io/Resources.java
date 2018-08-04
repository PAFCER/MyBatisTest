package com.zzu.io;

import java.io.InputStream;

/**
 * 关于加载配置文件为二进制流
 * @author hotwater
 *
 */
public class Resources {

	public  static  InputStream getResourceAsStream(String resource) {
		InputStream in =null;
		if(resource==null||"".equals(resource.trim())) {
			throw new IllegalArgumentException("参数有误");
		}
		in=Resources.class.getClassLoader().getResourceAsStream(resource);
		if(in==null) {
			throw new RuntimeException("加载文件异常");
		}
		return in;
	}
	
}
