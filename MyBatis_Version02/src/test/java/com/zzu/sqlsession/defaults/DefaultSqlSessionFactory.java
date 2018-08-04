package com.zzu.sqlsession.defaults;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.zzu.sqlsession.SqlSession;
import com.zzu.sqlsession.SqlSessionFactory;
import com.zzu.sqlsession.config.Configuration;

public class DefaultSqlSessionFactory implements SqlSessionFactory {
//定义一个参数接收流数据--主配置文件信息
	private InputStream config ;
	 

	public void setConfig(InputStream config) {
		this.config = config;
	}


	@Override
	public SqlSession opernSession() {
		DefaultSqlSession session = new DefaultSqlSession();
		ParseXMLToSession(session);
		return session;
	}

	/**
	 * 解析config中的数据进行数据初始化-----主配置文件
	 * ---定义一个实体进行存储主配置信息的数据
	 * @param session
	 */
	private void ParseXMLToSession(DefaultSqlSession session) {
			//承载主配置文件的信息
		Configuration  configuration = new Configuration();
		try {
			//1.利用Dom4j进行解析
			SAXReader reader= new SAXReader();
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//			Document doc = new SAXReader().read(this.config);
			Document doc = reader.read(this.config);
			//2.拿到根节点--configuration
			Element root =doc.getRootElement();
			//3.拿到连接数据库的四要素信息
			List<Element> props = root.selectNodes("//property");
			//4.遍历集合将数据注入到Configuration
			if(props!=null&&props.size()>0) {
				for (Element prop : props) {
					String name =prop.attributeValue("name");
					String value =prop.attributeValue("value");
					name =name.toUpperCase();//转换为大写
					switch(name) {
					case "DRIVER"://驱动
						configuration.setDriver(value);
						break;
					case "URL"://路径
						configuration.setUrl(value);
						break;
					case "USERNAME"://用户名
						configuration.setUsername(value);
						break;
					case "PASSWORD"://密码
						configuration.setPassword(value);
						break;
					default:
						System.out.println("无用的数据");
						break;
					}
				}
			}
			//5.将连接池配置数据注入到configuration
			String type = root.selectSingleNode("//dataSource").valueOf("@type");
			configuration.setType(type);
			//6.将mappers中的映射文件信息存储到configuration中
			List<Element> mapperElements = root.selectSingleNode("//mappers").selectNodes("//mapper");
			//定义两个集合存储对应的映射文件信息   遍历其
			List<String>resourcesMappers=new ArrayList<String>();
			
			
			List<String>anntationsMappers=new ArrayList<String>();
			if(mapperElements!=null&&mapperElements.size()>0) {
				for (Element mapper : mapperElements) {
					if(mapper.attribute("resource")!=null) {//XML方式
						String resourceValue = mapper.attributeValue("resource");
						resourcesMappers.add(resourceValue);
						
						
						
					}else 	if(mapper.attribute("class")!=null) {//注解方式
						String annotationValue=mapper.attributeValue("class");
						anntationsMappers.add(annotationValue);
					}else {//其他方式不做处理
						System.out.println("Do    Nothing  ");
					}
					
				}
			}
			//将数据注入到集合中
			configuration.setResourcesMappers(resourcesMappers);
			configuration.setAnntationsMappers(anntationsMappers);
			//将承载主配置文件信息的configuration注入到DefaultSqlSession中
			session.setConfiguration(configuration);
			//此处与老师讲解的不同，在这里的很多数据信息都封装到对应的Configuration中
			//在DefaultSqlSession中进行解析该configuration拿到对应的数据进行解析即可。
			
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	}

}
