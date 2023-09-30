# fyc-yx-parent
该仓库只包含项目的后端代码

•项目系统描述: 社区团购是社区居民团体的一种互联网线上线下购物消费行为,是依托社区的一种区域化、网
络化的团购形式。总之,依托社区和团长社交关系实现商品流通模式。
•该系统包含平台管理端和微信小程序端
1. 平台管理端:权限管理、区域管理、商品信息管理、营销活动管理
2. 微信小程序:登录管理、首页信息、商品分类、购物车管理、商品管理、订单管理
•技术选型:SpringBoot、SpringCloudAlibaba、MybatisPlus、MySQL、ElasticSearch、Redis、RabbitM
Q、Redisson、OSS、Knife4j、SpringData

该仓库包含common、model、service-client、service-gateway、service模块。
common模块：包含该项目中的一些工具类，比如关于rabbitMq的常量、JWT框架、MD5编码等等工具类
model模块：只要是定义了一系列模型数据类
service-client:定义了一系列远程调用的方法
service-gateway:定义了微服务的网关
service:包含了响应http请求、处理业务逻辑、访问数据库的类和方法。
