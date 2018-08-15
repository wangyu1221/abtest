# a simple abtest util

## Usege

1 Instantiate ConfigHolder

    @Component
    public class ConfigHolder implements AbTestConfigHolder {
     
        @Resource
        private ConfigManager configManager;
     
        @Override
        public List<AbTestConfig> fetchConfig() {
            String configValue = configManager.getValue(ConfigConstant.AB_TEST_CONFIG);
            List<AbTestConfig> configList = JSON.parseObject(configValue, new TypeReference<List<AbTestConfig>>(){});
            return configList;
        }
    }

2 Instantiate BucketBizProcessor

    public class ProcessorA extends BucketBizProcessor<String> {
     
        public ProcessorA(String name) {
            super(name);
        }
     
        @Override
        public String process() {
            System.out.println("this is processor A");
            return "A";
        }
    }
    
3 Instantiate BucketCalculator

    BucketCalculator calculator = new BucketCalculator<Long>(){
     
        @Override
        // return bucket calc result
        public Integer calculate(Long userId) {
            Integer result = new Long(Math.abs(userId) % 100).intValue() ;
            System.out.println(String.format("param=%s, result=%s", o, result));
            return result;
        }
    });
    
4 create A AbTest beanFactory

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
            processors.add(new ProcessorA("processor0"));
            processors.add(new ProcessorA("processor1"));
            initConfig.setProcessors(processors);
            initConfig.setCalculator(new BucketCalculator(){
                // see 3
            });
            initConfig.setInstanceName("couponAfterPay");
            initConfigs.add(initConfig);
     
            abTestFactory.setInitConfigs(initConfigs);
     
            return abTestFactory;
        }
    }

5 get and use AbTestInstance

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
