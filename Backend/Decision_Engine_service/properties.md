#spring.application.name=decision-engine-service
#spring.config.import=configserver:http://localhost:8888

#server.port=7002
#eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
#eureka.client.fetch-registry=true
#eureka.client.register-with-eureka=true
#eureka.instance.prefer-ip-address=true


spring.application.name=decision-engine-service
spring.config.import=configserver:http://localhost:8888

server.port=7002

google.api.key=replace-your-api-key


eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.fetch-registry=true
eureka.client.register-with-eureka=true
eureka.instance.prefer-ip-address=true

# MySQL Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/decision_db
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true