package kr.co.openeg.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


@Configuration
@ComponentScan(basePackages = { "kr.co.openeg.service", "kr.co.openeg.aop", "kr.co.openeg.task" })
@EnableAspectJAutoProxy
@EnableTransactionManagement

@MapperScan(basePackages = { "kr.co.openeg.mapper" })

public class RootConfig {

	@Bean
	public DataSource dataSource() {
		HikariConfig dbconf = new HikariConfig();
		dbconf.setDriverClassName("net.sf.log4jdbc.sql.jdbcapi.DriverSpy");
		dbconf.setJdbcUrl("jdbc:log4jdbc:mysql://127.0.0.1:3306/kshield");

		dbconf.setUsername("kisa");
		dbconf.setPassword("kisa");

		dbconf.setMinimumIdle(5);
		dbconf.setPoolName("springHikariCP");

		dbconf.addDataSourceProperty("dataSource.cachePrepStmts"       , "true");
		dbconf.addDataSourceProperty("dataSource.prepStmtCacheSize"    , "200" );
		dbconf.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048");
		dbconf.addDataSourceProperty("dataSource.useServerPrepStmts"   , "true");

		HikariDataSource hd = new HikariDataSource(dbconf);

		return hd;
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
		sqlSessionFactory.setDataSource(dataSource());
		return (SqlSessionFactory) sqlSessionFactory.getObject();
	}
	
	@Bean
	public DataSourceTransactionManager txManager() {
		return new DataSourceTransactionManager(dataSource());
	}

}
