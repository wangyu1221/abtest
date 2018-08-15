package so.dian.abtest.factory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import so.dian.abtest.bucket.BucketBizProcessor;
import so.dian.abtest.bucket.BucketCalculator;
import so.dian.abtest.exception.ComponentNotFoundException;
import so.dian.abtest.exception.InstanceNotFountException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbTestFactory.class})
public class AbTestFactoryTest {

    @Before
    public void before() throws IllegalAccessException {
        AbTestFactory abTestFactory = new AbTestFactory();
        PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").set(abTestFactory, Maps.newConcurrentMap());
        PowerMockito.field(AbTestFactory.class, "CALCULATOR_MAP").set(abTestFactory, Maps.newConcurrentMap());
        PowerMockito.field(AbTestFactory.class, "PROCESSOR_MAP").set(abTestFactory, Maps.newConcurrentMap());
    }

    @Test
    public void getInstance$noInstance(){
        AbTestFactory abTestFactory = new AbTestFactory();
        try {
            abTestFactory.getInstance("test");
            Assert.assertEquals(true, false);
        }catch (InstanceNotFountException e){
            Assert.assertEquals("cannot find instance with name 'test'", e.getMessage());
        }
    }

    @Test
    public void getInstance$hasInstance() throws IllegalAccessException {
        AbTestFactory abTestFactory = new AbTestFactory();
        Map<String, AbTestInstance> map = (Map<String, AbTestInstance>) PowerMockito.field(AbTestFactory.class, "INSTANCE_MAP").get(abTestFactory);
        AbTestInstance abTestInstance = new AbTestInstance();
        map.put("test", abTestInstance);
        try {
            Assert.assertEquals(abTestInstance, abTestFactory.getInstance("test"));
        } catch (InstanceNotFountException e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void afterPropertiesSet() throws Exception {
        AbTestFactory abTestFactory = PowerMockito.spy(new AbTestFactory());
        PowerMockito.doNothing().when(abTestFactory, "processInitConfig");
        PowerMockito.doNothing().when(abTestFactory, "rebuildInstance");
        abTestFactory.afterPropertiesSet();
        PowerMockito.verifyPrivate(abTestFactory, Mockito.times(1)).invoke("processInitConfig");
        PowerMockito.verifyPrivate(abTestFactory, Mockito.times(1)).invoke("rebuildInstance");
    }

    @Test
    public void updateConfig$fetchEmpty() throws InvocationTargetException, IllegalAccessException {
        AbTestConfigHolder abTestConfigHolder = new AbTestConfigHolder() {
            @Override
            public List<AbTestConfig> fetchConfig() {
                return null;
            }
        };
        AbTestFactory abTestFactory = new AbTestFactory();
        abTestFactory.setAbTestConfigHolder(abTestConfigHolder);
        PowerMockito.method(AbTestFactory.class, "updateConfig").invoke(abTestFactory);
        Map<String, AbTestConfig> map = (Map<String, AbTestConfig>) PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").get(abTestFactory);
        Assert.assertEquals(0, map.size());

        abTestConfigHolder = new AbTestConfigHolder() {
            @Override
            public List<AbTestConfig> fetchConfig() {
                return Lists.newArrayList();
            }
        };
        PowerMockito.method(AbTestFactory.class, "updateConfig").invoke(abTestFactory);
        map = (Map<String, AbTestConfig>) PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").get(abTestFactory);
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void updateConfig$fetch() throws InvocationTargetException, IllegalAccessException {
        AbTestConfigHolder abTestConfigHolder = new AbTestConfigHolder() {
            @Override
            public List<AbTestConfig> fetchConfig() {
                AbTestConfig abTestConfig = new AbTestConfig();
                abTestConfig.setInstanceName("Varian");
                return Lists.newArrayList(abTestConfig);
            }
        };
        AbTestFactory abTestFactory = new AbTestFactory();
        abTestFactory.setAbTestConfigHolder(abTestConfigHolder);
        PowerMockito.method(AbTestFactory.class, "updateConfig").invoke(abTestFactory);
        Map<String, AbTestConfig> map = (Map<String, AbTestConfig>) PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").get(abTestFactory);
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("Varian", map.get("Varian").getInstanceName());
    }

    private BucketCalculator createCalculator(Integer i){
        return new BucketCalculator<Integer>() {
            @Override
            public Integer calculate(Integer integer) {
                return integer % i;
            }
        };
    }

    private BucketBizProcessor createProcessor(String name){
        BucketBizProcessor<String, String> processor = new BucketBizProcessor<String, String>(name){

            @Override
            public String process(String o) {
                return String.format("%s-%s", getName(), o);
            }
        };
        return processor;
    }

    private List<AbTestInitConfig> initConfigs(){
        List<AbTestInitConfig> initConfigs = Lists.newArrayList();
        for (int i = 0; i < 3; i++){
            final AbTestInitConfig<Integer, String, String> abTestInitConfig = new AbTestInitConfig<>();
            abTestInitConfig.setInstanceName(String.format("instance-%s", i));
            abTestInitConfig.setCalculator(createCalculator(i + 1));
            List<BucketBizProcessor<String, String>> processors = Lists.newArrayList();
            for (int j = 0; j < 3; j++){
                processors.add(createProcessor(String.format("%s-%s", abTestInitConfig.getInstanceName(), j)));
            }
            abTestInitConfig.setProcessors(processors);
            initConfigs.add(abTestInitConfig);
        }
        return initConfigs;
    }

    @Test
    public void processInitConfig() throws InvocationTargetException, IllegalAccessException {

        AbTestFactory abTestFactory = new AbTestFactory();
        abTestFactory.setInitConfigs(initConfigs());
        PowerMockito.method(AbTestFactory.class, "processInitConfig").invoke(abTestFactory);
        Map<String, BucketBizProcessor> PROCESSOR_MAP = (Map<String, BucketBizProcessor>) PowerMockito.field(AbTestFactory.class, "PROCESSOR_MAP").get(abTestFactory);
        Map<String, BucketCalculator<Integer>> CALCULATOR_MAP = (Map<String, BucketCalculator<Integer>>) PowerMockito.field(AbTestFactory.class, "CALCULATOR_MAP").get(abTestFactory);

        Assert.assertEquals(3, CALCULATOR_MAP.size());

        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-0").calculate(0));
        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-0").calculate(1));
        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-0").calculate(2));

        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-1").calculate(0));
        Assert.assertEquals(Integer.valueOf(1), CALCULATOR_MAP.get("instance-1").calculate(1));
        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-1").calculate(2));
        Assert.assertEquals(Integer.valueOf(1), CALCULATOR_MAP.get("instance-1").calculate(3));

        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-2").calculate(0));
        Assert.assertEquals(Integer.valueOf(1), CALCULATOR_MAP.get("instance-2").calculate(1));
        Assert.assertEquals(Integer.valueOf(2), CALCULATOR_MAP.get("instance-2").calculate(2));
        Assert.assertEquals(Integer.valueOf(0), CALCULATOR_MAP.get("instance-2").calculate(3));

        Assert.assertNull(CALCULATOR_MAP.get("instance-3"));

        Assert.assertEquals(9, PROCESSOR_MAP.size());

        Assert.assertEquals("instance-0-0-Anduin", PROCESSOR_MAP.get("instance-0-0").process("Anduin"));
        Assert.assertEquals("instance-0-2-Anduin", PROCESSOR_MAP.get("instance-0-2").process("Anduin"));
        Assert.assertEquals("instance-1-1-Anduin", PROCESSOR_MAP.get("instance-1-1").process("Anduin"));
        Assert.assertEquals("instance-2-2-Anduin", PROCESSOR_MAP.get("instance-2-2").process("Anduin"));

    }

    @Test
    public void rebuildInstance$noInitConfig() throws Exception {
        AbTestFactory abTestFactory = PowerMockito.spy(new AbTestFactory());
        PowerMockito.doNothing().when(abTestFactory, "updateConfig");
        PowerMockito.method(AbTestFactory.class, "rebuildInstance").invoke(abTestFactory);
        Map<String, AbTestInstance> INSTANCE_MAP = (Map<String, AbTestInstance>) PowerMockito.field(AbTestFactory.class, "PROCESSOR_MAP").get(abTestFactory);
        Assert.assertEquals(0, INSTANCE_MAP.size());
    }

    @Test
    public void rebuildInstance$noConfigMap() throws Exception {
        AbTestFactory abTestFactory = PowerMockito.spy(new AbTestFactory());
        abTestFactory.setInitConfigs(initConfigs());
        PowerMockito.doNothing().when(abTestFactory, "updateConfig");
        Map<String, AbTestConfig> CONFIG_MAP = Maps.newConcurrentMap();
        PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").set(abTestFactory, CONFIG_MAP);

        PowerMockito.method(AbTestFactory.class, "rebuildInstance").invoke(abTestFactory);
        Map<String, AbTestInstance> INSTANCE_MAP = (Map<String, AbTestInstance>) PowerMockito.field(AbTestFactory.class, "PROCESSOR_MAP").get(abTestFactory);
        Assert.assertEquals(0, INSTANCE_MAP.size());
    }

    @Test
    public void rebuildInstance$noCalculator() throws Exception {
        AbTestFactory abTestFactory = PowerMockito.spy(new AbTestFactory());
        abTestFactory.setInitConfigs(initConfigs());
        PowerMockito.doNothing().when(abTestFactory, "updateConfig");
        Map<String, AbTestConfig> CONFIG_MAP = Maps.newConcurrentMap();
        AbTestConfig abTestConfig = new AbTestConfig();
        abTestConfig.setInstanceName("instance-0");
        abTestConfig.setDefaultBizName("instance-0-0");
        abTestConfig.setBuckets(Lists.newArrayList());

        AbTestConfig.AbTestBucketConfig bucketConfig0 = new AbTestConfig.AbTestBucketConfig();
        bucketConfig0.setBizName("instance-0-0");
        bucketConfig0.setValue(1);
        abTestConfig.getBuckets().add(bucketConfig0);

        AbTestConfig.AbTestBucketConfig bucketConfig1 = new AbTestConfig.AbTestBucketConfig();
        bucketConfig1.setBizName("instance-0-1");
        bucketConfig1.setValue(1);
        abTestConfig.getBuckets().add(bucketConfig1);

        CONFIG_MAP.put("instance-0", abTestConfig);
        PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").set(abTestFactory, CONFIG_MAP);

        PowerMockito.method(AbTestFactory.class, "processInitConfig").invoke(abTestFactory);

        PowerMockito.field(AbTestFactory.class, "CALCULATOR_MAP").set(abTestFactory, Maps.newHashMap());

        try {
            PowerMockito.method(AbTestFactory.class, "rebuildInstance").invoke(abTestFactory);
            Assert.assertTrue(false);
        }catch (Exception e){
            if (e.getCause() instanceof ComponentNotFoundException){
                ComponentNotFoundException componentNotFoundException = (ComponentNotFoundException)e.getCause();
                Assert.assertEquals("cannot find bucket calculator of instance 'instance-0'.", componentNotFoundException.getMessage());
                Assert.assertTrue(true);
            }else {
                Assert.assertTrue(false);
            }
        }
    }

    @Test
    public void rebuildInstance$noDefaultProcessor() throws Exception {
        AbTestFactory abTestFactory = PowerMockito.spy(new AbTestFactory());
        abTestFactory.setInitConfigs(initConfigs());
        PowerMockito.doNothing().when(abTestFactory, "updateConfig");
        Map<String, AbTestConfig> CONFIG_MAP = Maps.newConcurrentMap();
        AbTestConfig abTestConfig = new AbTestConfig();
        abTestConfig.setInstanceName("instance-0");
        abTestConfig.setDefaultBizName("instance-3-0");
        abTestConfig.setBuckets(Lists.newArrayList());

        AbTestConfig.AbTestBucketConfig bucketConfig0 = new AbTestConfig.AbTestBucketConfig();
        bucketConfig0.setBizName("instance-0-0");
        bucketConfig0.setValue(1);
        abTestConfig.getBuckets().add(bucketConfig0);

        AbTestConfig.AbTestBucketConfig bucketConfig1 = new AbTestConfig.AbTestBucketConfig();
        bucketConfig1.setBizName("instance-0-1");
        bucketConfig1.setValue(1);
        abTestConfig.getBuckets().add(bucketConfig1);

        CONFIG_MAP.put("instance-0", abTestConfig);
        PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").set(abTestFactory, CONFIG_MAP);

        PowerMockito.method(AbTestFactory.class, "processInitConfig").invoke(abTestFactory);

        try {
            PowerMockito.method(AbTestFactory.class, "rebuildInstance").invoke(abTestFactory);
            Assert.assertTrue(false);
        }catch (Exception e){
            if (e.getCause() instanceof ComponentNotFoundException){
                ComponentNotFoundException componentNotFoundException = (ComponentNotFoundException)e.getCause();
                Assert.assertEquals("cannot find default processor of instance 'instance-0'.", componentNotFoundException.getMessage());
                Assert.assertTrue(true);
            }else {
                Assert.assertTrue(false);
            }
        }
    }

    @Test
    public void rebuildInstance() throws Exception {
        AbTestFactory abTestFactory = PowerMockito.spy(new AbTestFactory());
        abTestFactory.setInitConfigs(initConfigs());
        PowerMockito.doNothing().when(abTestFactory, "updateConfig");
        Map<String, AbTestConfig> CONFIG_MAP = Maps.newConcurrentMap();
        AbTestConfig abTestConfig = new AbTestConfig();
        abTestConfig.setInstanceName("instance-0");
        abTestConfig.setDefaultBizName("instance-0-0");
        abTestConfig.setBuckets(Lists.newArrayList());

        AbTestConfig.AbTestBucketConfig bucketConfig0 = new AbTestConfig.AbTestBucketConfig();
        bucketConfig0.setBizName("instance-0-0");
        bucketConfig0.setValue(1);
        abTestConfig.getBuckets().add(bucketConfig0);

        AbTestConfig.AbTestBucketConfig bucketConfig1 = new AbTestConfig.AbTestBucketConfig();
        bucketConfig1.setBizName("instance-0-1");
        bucketConfig1.setValue(1);
        abTestConfig.getBuckets().add(bucketConfig1);

        CONFIG_MAP.put("instance-0", abTestConfig);
        PowerMockito.field(AbTestFactory.class, "CONFIG_MAP").set(abTestFactory, CONFIG_MAP);

        PowerMockito.method(AbTestFactory.class, "processInitConfig").invoke(abTestFactory);
        PowerMockito.method(AbTestFactory.class, "rebuildInstance").invoke(abTestFactory);
        Map<String, AbTestInstance<Integer, String, String>> INSTANCE_MAP = (Map<String, AbTestInstance<Integer, String, String>>) PowerMockito.field(AbTestFactory.class, "INSTANCE_MAP").get(abTestFactory);

        Assert.assertEquals(1, INSTANCE_MAP.size());
        AbTestInstance instance = INSTANCE_MAP.get("instance-0");

        List<AbTestBucket> buckets = (List<AbTestBucket>)PowerMockito.field(AbTestInstance.class, "buckets").get(instance);

        Assert.assertEquals(Integer.valueOf(0), buckets.get(0).getIntervalPair().getLeft());
        Assert.assertEquals(Integer.valueOf(1), buckets.get(0).getIntervalPair().getRight());
        Assert.assertEquals("instance-0-0", buckets.get(0).getProcessor().getName());

        Assert.assertEquals(Integer.valueOf(1), buckets.get(1).getIntervalPair().getLeft());
        Assert.assertEquals(Integer.valueOf(2), buckets.get(1).getIntervalPair().getRight());
        Assert.assertEquals("instance-0-1", buckets.get(1).getProcessor().getName());

        // FIXME
        Assert.assertEquals("instance-0-0-qwerty", INSTANCE_MAP.get("instance-0").execute(0, "qwerty"));
        Assert.assertEquals("instance-0-0-qwerty", INSTANCE_MAP.get("instance-0").execute(1, "qwerty"));
        Assert.assertEquals("instance-0-0-qwerty", INSTANCE_MAP.get("instance-0").execute(2, "qwerty"));
        Assert.assertEquals("instance-0-0-qwerty", INSTANCE_MAP.get("instance-0").execute(3, "qwerty"));
    }

}
