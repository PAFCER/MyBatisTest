package com.zzu.test;

import java.io.InputStream;
import java.util.List;

import com.zzu.dao.UserDao;
import com.zzu.domain.User;
import com.zzu.io.Resources;
import com.zzu.sqlsession.SqlSession;
import com.zzu.sqlsession.SqlSessionFactory;
import com.zzu.sqlsession.SqlSessionFatcoryBuilder;


/**
 * 测试我的自定义MyBatis
 * @author hotwater
 *
 */
public class TestMyBatis {

	public static void main(String[] args) {
		
		/**
		 * 关于自定义MyBatis的实现---模仿官方的MyBatis
		 */
		//1.加载主配置文件
		InputStream  in=Resources.getResourceAsStream("MyBatisConfig.xml" );
		//2.利用构建者工厂SqlSessionFactoryBuilder的build方法创建SqlSessionFactory
		SqlSessionFactory sqlSessionFactory=new  SqlSessionFatcoryBuilder().build(in);
		//3.利用上述的SqlSessionFactory调用opensession得到SqlSession
		SqlSession sqlSession=sqlSessionFactory.opernSession();
		//4.利用上述的SqlSession进行获取动态代理对象
		UserDao userDao=sqlSession.getMapper(UserDao.class);
		//5.利用动态代理对象调用方法
		List<User> userList = userDao.findAll();
		//遍历数据
		System.out.println("查询数据如下：");
		if(userList!=null&&userList.size()>0) {
			for (User user : userList) {
				System.out.println("\t\t"+user);
			}
		}
		
	}
	
}
