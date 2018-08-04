package com.zzu.sqlsession.config;

import java.util.ArrayList;
import java.util.List;

//承载主配置文件信息的数据
public class Configuration {
	
	//存储连接数据库的四要素
	private  String driver;
	private String  url;
	private String  username;
	private String password;
	//存储是否使用连接池
	private String type;
	//存储映射配置文件信息---分为XML和注解两种
	//XML方式
	List<String>resourcesMappers= new ArrayList<String>();
	//注解方式
	List<String>AnntationsMappers=new ArrayList<String>();
	
	
	
	
	public String getDriver() {
		return driver;
	}
	public void setDriver(String driver) {
		this.driver = driver;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List<String> getResourcesMappers() {
		return resourcesMappers;
	}
	public void setResourcesMappers(List<String> resourcesMappers) {
		this.resourcesMappers = resourcesMappers;
	}
	public List<String> getAnntationsMappers() {
		return AnntationsMappers;
	}
	public void setAnntationsMappers(List<String> anntationsMappers) {
		AnntationsMappers = anntationsMappers;
	}
	
	
	
	
	
	

}
