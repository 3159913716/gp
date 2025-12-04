# Big Event Spring Boot Project

## 项目介绍
这是一个基于Spring Boot的大型事件管理系统，包含用户管理、文章管理、评论管理、分类管理等功能。

## 技术栈
- Spring Boot 3.4.4
- MyBatis 3.0.3
- MySQL 8.0
- Redis
- JWT
- Lombok
- Spring Validation
- PageHelper
- Aliyun OSS
- Spring Mail

## 项目结构
```
src/
├── main/
│   ├── java/com/zhao/
│   │   ├── anno/          # 自定义注解
│   │   ├── config/        # 配置类
│   │   ├── controller/    # 控制器
│   │   ├── exception/     # 异常处理
│   │   ├── interceptors/  # 拦截器
│   │   ├── mapper/        # Mapper接口
│   │   ├── pojo/          # 实体类
│   │   ├── service/       # 业务层
│   │   ├── utils/         # 工具类
│   │   └── validation/    # 验证器
│   └── resources/
│       ├── com/zhao/mapper/  # MyBatis映射文件
│       └── application.yml   # 配置文件
└── test/                  # 测试类
```

## 运行项目

### 方式一：使用Maven命令（推荐）
1. 确保安装了JDK 17+和Maven 3.6+
2. 配置application.yml文件，修改数据库、Redis等配置
3. 运行以下命令：
   ```
   mvn spring-boot:run
   ```

### 方式二：使用IDE运行
1. 使用IntelliJ IDEA或Eclipse导入Maven项目
2. 配置application.yml文件
3. 找到BigEventApplication类，右键运行main方法

### 方式三：打包成jar运行
1. 执行打包命令：
   ```
   mvn clean package
   ```
2. 运行生成的jar文件：
   ```
   java -jar target/big-event-1.0-SNAPSHOT.jar
   ```

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/big_event?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
```

### JWT配置
```yaml
jwt:
  secret: your-jwt-secret-key
  expire: 86400000
```

## API接口
项目包含以下主要接口：
- 用户管理：注册、登录、忘记密码等
- 文章管理：发布、修改、删除、查询等
- 评论管理：发表评论、点赞评论等
- 分类管理：创建、修改、删除分类等
- 搜索功能：全文搜索文章
- 邮件服务：发送验证码
- 文件上传：图片上传到阿里云OSS

## 开发说明

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 开发流程
1. 克隆项目
2. 配置application.yml
3. 创建数据库并导入SQL脚本
4. 运行项目
5. 开发新功能
6. 编写测试用例
7. 提交代码

## 注意事项
1. 首次运行需要创建数据库并导入SQL脚本
2. 阿里云OSS配置需要替换为真实的AccessKey和Bucket信息
3. 邮件服务需要配置真实的SMTP服务器和账号
4. JWT密钥需要替换为安全的随机字符串

## 许可证
MIT License