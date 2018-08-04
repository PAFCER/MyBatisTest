package com.zzu.sqlsession.defaults;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zzu.domain.User;
import com.zzu.io.Resources;
import com.zzu.sqlsession.SqlSession;
import com.zzu.sqlsession.annotations.SelectAnnotation;
import com.zzu.sqlsession.config.Configuration;
import com.zzu.sqlsession.domain.Mapper;
/**
 * 自定义MyBatis核心类
 * @author hotwater
 *
 */
public class DefaultSqlSession implements SqlSession {
	//承载主配置文件信息
private  Configuration configuration;
	public Configuration getConfiguration() {
	return configuration;
}
public void setConfiguration(Configuration configuration) {
	this.configuration = configuration;
}
//承载映射关系数据信息
private Map<String,Mapper>mappersMap=new HashMap<String,Mapper>();
	public Map<String, Mapper> getMappersMap() {
	return mappersMap;
}
public void setMappersMap(Map<String, Mapper> mappersMap) {
	this.mappersMap.putAll(mappersMap);
}
//连接池信息
private DataSource dataSource;
	public DataSource getDataSource() {
	return dataSource;
}
public void setDataSource(DataSource dataSource) {
	this.dataSource = dataSource;
}
//数据库连接信息connection
private  Connection connection;
	public Connection getConnection() {
	return connection;
}
public void setConnection(Connection connection) {
	this.connection = connection;
}
	@Override
	/**
	 * 动态代理对象
	 */
	@SuppressWarnings("all")
	public <T> T getMapper(Class<T> T) {
		//生成动态代理之前需要进行数据的信息的格式化-----此步骤很关键，如果不进行解析，那么SqlSession将一无是处
		ParseConfiguration(configuration);
		//此种方式需要多写一个类，过于麻烦因此舍弃，采用下面的匿名内部类的方式：感慨张阳大神的功力深厚
//		return (T)Proxy.newProxyInstance(T.getClassLoader(), new Class[] {T}, new MapperHandler());
		
		return (T)Proxy.newProxyInstance(T.getClassLoader(), new Class[] {T}, new InvocationHandler() {
		/**此处对于InvocationHandler进行使用匿名内部类的方式会减少不必要的传递参数的麻烦，直接在这里调用即可
		*	如connection，如mappersMap
		 */
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				
				//1.判定是否在配置文件中存在了已经配置的映射方法---此处利用method进行
				String className = method.getDeclaringClass().getName();
				String methodName = method.getName();
				//2.组装映射中的key
				String  key =className+"."+methodName;
				//3.根据组装的key获取是否存在对应的Mapper
				Mapper mapper = getMappersMap().get(key);
				if(mapper==null) {
					throw new RuntimeException("没有对应的映射Mapper,请检查映射配置文件");
				}
				//4.如果存在，那么QueryString ,resultType就存在了
				//执行处理SQL 
				Connection connection0 =null;
				PreparedStatement prepareStatement =null;
				ResultSet resultSet =null;
				//4.1拿到数据库连接
				connection0 = getConnection0();
				//4.2拿到SQL执行器
				prepareStatement = connection0.prepareStatement(mapper.getQueryString());
				//4.3拿到结果集
				resultSet = prepareStatement.executeQuery();
				//4.4处理结果集
				List<User>userList=HandlerResultSet(resultSet,mapper.getResultType());
				return userList;
			}
			
		/**	
		 * 处理结果集对象
		 * @param resultSet
		 * @param resultType
		 * @return
		 */
		private <T>List<T> HandlerResultSet(ResultSet resultSet, String resultType) {
			//定义一个List集合承载结果集数据
			List userList= new ArrayList ();
			if(resultSet==null||resultType==null||"".equalsIgnoreCase(resultType.trim())) {
				throw new IllegalArgumentException("解析结果集参数传入有误");
			}
			try {
				// 利用总记录数进行迭代出来结果集的数据
				while(resultSet.next()) {//存在记录数
					try {
						//0.每一条记录就是一个对应的实体---根据反射获取一个对象
						Class<?> resultTypeClass = Class.forName(resultType);
						Object resultTypeModel=resultTypeClass.newInstance();
						//1.迭代一次拿到每条记录的元素据
						ResultSetMetaData metaData = resultSet.getMetaData();
						//2.拿到每条记录的列数
						int count = metaData.getColumnCount();
						//3.通过列数进行遍历获取其中的每一个字段的数据
						for (int i = 1	; i <=count; i++) {
							//3.1拿到列字段名称
							String name = metaData.getColumnName(i);
							//3.2拿到列字段对应的数据-----根据上述元素据中获取的字段名称
							Object value =resultSet.getObject(name);
							//3.3将数据注入到对象中---如何通过反射注入呢？需要利用属性描述器
							PropertyDescriptor  descriptor = new PropertyDescriptor(name, resultTypeClass);
							Method writeMethod = descriptor.getWriteMethod();
							writeMethod.invoke(resultTypeModel, value);
							//错误示范----每次将一个字段注入model中就将数据注入集合中会造成数据的重复
							//userList.add(resultTypeModel);
						}
						//此处应该是将每一条记录数据完全注入model之后再将该model注入到集合中
						userList.add(resultTypeModel);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IntrospectionException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
				
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return userList;
		}});
	}
	/**
	 * 解析configuration对象
	 * @param configuration
	 */
	private void ParseConfiguration(Configuration configuration) {
		if(configuration==null) {
			throw new  IllegalArgumentException("configuration参数传递有误");
		}
		//1.判定是否使用连接池---依据type
		if("POOLED".equalsIgnoreCase(configuration.getType())) {//使用连接池
			//1.1.创建连接池
		this.dataSource=	createDataSource();
		}else if("UNPOOLED".equalsIgnoreCase(configuration.getType())){//不使用连接池
			//1.2.创建connection连接对象
			this.connection = createConnection();
		}
		//2.解析映射配置文件信息
		//2.1注解方式---暂时不处理------实现注解方式的代码实现----2018年4月9日13点57分
		List<String> annotationsMappers = configuration.getAnntationsMappers();
		/**
		 *思路：
		 *				不论哪种方式都是要实现对sqlsession中mappersMap数据信息的注入
		 *				就是组装mappersMap数据，因为在XML方式中是组装，在注解方式中同样是组装
		 *				那么就简单了，因为在XML中我们需要解析配置文件，在配置文件中进行组装数据
		 *				在注解方式中，我们可以在在注解上解析数据并成功的将数据注入到mappersMap中
		 *				即可。比如注解的value值就是sql语句，比如
		 *				：此处的我们拿到的是权限定接口名称，那么我们需要组装
		 *				Map<String,Mapper>
		 *				其中String是className+"."+methodName
		 *				注解结合中放的元素就是权限定的接口名称---然后通过反射获取方法即可
		 *				关于Mapper我们知道其中是两个属性：其一queryString其二resultType
		 *				通过反射拿到注解的值即可即为queryString
		 *				其二resultType即为对应的方法的参数化的类型的实际类型参数
		 *				通过方法我们同样可以拿到，然后将其注入到Mapper中即可
		 *				然后将其注入到mappersMap即可。
		 *				
		 */
			if(annotationsMappers!=null&&annotationsMappers.size()>0) {
				
				for (String className : annotationsMappers) {
					//解析注解配置中的接口类
				    Map<String ,Mapper>oneClassMapper=paseAnnotation(className);
				    this.setMappersMap(oneClassMapper);
				}
			}
			//走到这里说明解析注解配置的数据注入完毕
			
		
		//2.2XML方式
		List<String> resourcesMappers = configuration.getResourcesMappers();
		if(resourcesMappers!=null&&resourcesMappers.size()>0) {
		//遍历XML方式数据----映射配置文件的路径
			//此处剩余解析映射配置文件的代码23点15分
			for (String mapper : resourcesMappers) {
				//解析配置文件
				 Map<String,Mapper> oneMapperMap = parseMapperXML(mapper);
				 //每次解析都将数据注入到本地的this.mappersMap中
				 this.setMappersMap(oneMapperMap);
			}
			//走到这里就已经将所有的映射配置文件注入到本地的mappersMap中了----
			//也就意味着所有的配置文件已经全部解析完毕
			
		}
		
		
		
		
	}
	/**
	 * 解析单独的映射配置接口类-----注解
	 * @param className
	 * @return
	 */
	private Map<String, Mapper> paseAnnotation(String className) {
		//定义一个承载一个注解类的数据容器Map<String,Mapper>
		Map<String,Mapper>oneClassMapper=new HashMap<String,Mapper>();
		
		if(className==null||"".equalsIgnoreCase(className.trim())) {
			throw new IllegalArgumentException("注解方式解析接口类异常");
		}
		try {
			//1.将接口类权限定名称加载到内存中，拿到对应的字节码
			Class classOne =Class.forName(className);
			//拿到对应的类名称----组装key值做准备
			String key_className=classOne.getName();
			//2.通过反射拿到上述字节码对应的public 方法
			Method [] methods=classOne.getMethods();
			if(methods!=null&&methods.length>0) {
				//2.1遍历方法数组获取
				for (Method method : methods) {
					//2.2验证该方法上是否存在我们对应的注解
					if(method.isAnnotationPresent(SelectAnnotation.class)) {
						//2.3如果存在，那么就是我们要找的方法，进行提取数据
						//key值的部分-----方法名称
						String key_methodName=method.getName();
						//2.4组装key值
						String  key =key_className+"."+key_methodName;
						//2.5获取注解上的值-----queryString
						//2.5.1首先拿到方法上的注解	
						SelectAnnotation annotation = method.getDeclaredAnnotation(SelectAnnotation.class);
						//2.5.2根据注解拿到注解上的值---查询语句
						String queryString = annotation.value();
						//2.6获取返回值类型resultType
						//首先拿到对应的带泛型的返回值类型
						Type type = method.getGenericReturnType();
						//判定看是不是参数化的类型
						if(type instanceof ParameterizedType) {
							//强转为子类型
							ParameterizedType  parameterizedType =(ParameterizedType)type;
							//拿到实际的参数类型数组
							Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
							//我们只需要拿到其中之一即可
							String resultType = actualTypeArguments[0].getTypeName();
							//此时我们拿到了queryString 和 resultType  那么就可以进行组装mapper
							Mapper mapper = new Mapper ();
							mapper.setQueryString(queryString);
							mapper.setResultType(resultType);
							//如此便有了key mapper 就可以组装一个Map<String,mapper>中的一条记录
							oneClassMapper.put(key, mapper);
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return oneClassMapper;
	}
	/**
	 * 解析单独的映射配置文件----XML
	 * @param mapper
	 * @return
	 */
	private Map<String, Mapper> parseMapperXML(String mapper) {
		//定义承载一个映射配置文件的容器Map<String,Mapper>
		Map<String,Mapper>oneMapperMap=new HashMap<String,Mapper>();
		//流对象定义外部便于关闭
		InputStream inputStream = null;
		//1.加载映射配置文件
		 inputStream = Resources.getResourceAsStream(mapper);
		try {
			//2.利用dom4j将其解析为文档对象
			SAXReader reader= new SAXReader();
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			Document  doc = reader.read(inputStream);
			//3.获取根节点----mapper
			Element root =doc.getRootElement();
			//4.拿到namespace----根节点属性
			String namespace =root.attributeValue("namespace");
			//5.获取select元素集合
			List<Element> selects = root.selectNodes("//select");
			//6.遍历select元素集合解析数据
			if(selects!=null&&selects.size()>0) {
				for (Element select : selects) {
					//6.1拿到id值
					String id = select.attributeValue("id");
					//6.2拿到返回值类型
					String resultType=select.attributeValue("resultType");
					//6.3拿到查询语句
					String queryString =select.getText();
					//6.4组装Map集合中的key和value
					String key =namespace+"."+id;
					Mapper value = new Mapper();
					value.setQueryString(queryString);
					value.setResultType(resultType);
					//6.5将每次解析的一个select作为oneMapperMap的一条记录
					oneMapperMap.put(key, value);
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(inputStream!=null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return oneMapperMap;
	}
	/**
	 * 获取数据库连接对象：getConnection0
	 * @return
	 */
	private Connection getConnection0() {
		if(this.dataSource!=null) {
			try {
				return this.dataSource.getConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return this.connection;
	}
	
	
	
	
	/**
	 * 创建connection
	 * @return
	 */
	private Connection createConnection() {
		
		if(this.configuration==null) {
			throw new NullPointerException("configuration异常");
		}
		try {
			Class.forName(configuration.getDriver());
	this.connection=DriverManager.getConnection(configuration.getUrl(), configuration.getUsername(), configuration.getPassword());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return this.connection;
	}
	/**
	 * 创建连接池
	 * @return
	 */
	private DataSource createDataSource() {
		ComboPooledDataSource  dataSource = new ComboPooledDataSource();
		try {
			if(this.configuration==null) {
				throw new NullPointerException("configuration异常");
			}
			dataSource.setDriverClass(configuration.getDriver());
			dataSource.setJdbcUrl(configuration.getUrl());
			dataSource.setUser(configuration.getUsername());
			dataSource.setPassword(configuration.getPassword());
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		return dataSource;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
