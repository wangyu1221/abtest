# a simple abtest util

## 主要类

AbTestConfigHolder 获取abtest配置的接口
BucketBizProcessor bucket业务逻辑
BucketCalculator bucket依据计算器
AbTestConfig 维护abtest实例和bucket流量、bucket依据计算器、bucket业务逻辑的关联关系
AbTestInitConfig bucket依据计算器、bucket业务逻辑的具体实现的关联关系
AbTestFactory 工厂，负责解析AbTestConfig和AbTestInitConfig并组装成AbTestInstance
AbTestInstance 创建好的abtest实例

## Demo

Instantiate ConfigHolder

    @Component
    public class ConfigHolder implements AbTestConfigHolder {
     
        @Resource
        private ConfigManager configManager;
     
        @Override
        public List<AbTestConfig> fetchConfig() {
            // 配置的维护可以随意实现
            return list;
        }
    }
    
create A AbTest beanFactory

    @Configuration
    public class AbTestFactoryBean {
     
        @Resource
        private AbTestConfigManager configHolder;
     
        @Bean
        public AbTestFactory abTestFactory(){
            AbTestFactory abTestFactory = new AbTestFactory();
            abTestFactory.setAbTestConfigHolder(configHolder);
     
            List<AbTestInitConfig> initConfigs = Lists.newArrayList();
     
            AbTestInitConfig initConfig = new AbTestInitConfig();
     
            List<BucketBizProcessor> processors = Lists.newArrayList();
            processors.add(new BucketBizProcessor("processor0"){
                // 分桶的业务逻辑0
            });
            processors.add(new BucketBizProcessor("processor1"){
                // 分桶的业务逻辑1
            });
            initConfig.setProcessors(processors);
            initConfig.setCalculator(new BucketCalculator(){
                // 例如用 用户id mod 100作为分桶依据
                public Integer calculate(Long userId) {
                    return new Long(Math.abs(userId) % 100).intValue() ;
                }
            });
            initConfig.setInstanceName("testInstance");
            initConfigs.add(initConfig);
     
            abTestFactory.setInitConfigs(initConfigs);
     
            return abTestFactory;
        }
    }

get and use AbTestInstance

    @Component
    public class BizManager {
     
        @Resource
        private AbTestFactory<Long, String> abTestFactory;
     
        public void doBiz() throws InstanceNotFountException {
            Random random = new Random();
            // get abtest instance
            try{
                AbTestInstance<Long, String> instance = abTestFactory.getInstance("testInstance");         
                String result = instance.execute(userId);
            }cache(Exception e){
                // default business
            }
        }
    }

## Notice

1. 分桶策略配置的instanceName不能为空，且唯一
2. 每个instanceName必须对应一个getName()为instanceName的BucketCalculator实例，若为空抛出ComponentNotFoundException使容器初始化失败
3. 分桶策略配置的defaultBizName不能为空，且defaultBizName能对应到一个getName()为defaultBizName的BucketBizProcessor实例，若为空抛出ComponentNotFoundException使容器初始化失败
4. 分桶策略配置的buckets若为空，则直接执行defaultBizName的逻辑
5. AbTestFactory.getInstance(String name)若找不到参数name对应的instance，抛InstanceNotFountException，业务方需捕获该异常进行处理
6. bucket的value取值为自然数（0<= value < Integet.MAX）
7. BucketCalculator的计算值尽量落在分桶区间内，否则走defaultBizName逻辑
8. 由于存在泛型，建议使用泛型限定类型防止出现类型转换错误。
9. AbTestConfigHolder的接口实现可以做预处理，只返回当前业务关心的内容
