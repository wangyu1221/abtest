package so.dian.abtest.factory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import so.dian.abtest.bucket.BucketBizProcessor;
import so.dian.abtest.bucket.BucketCalculator;
import so.dian.abtest.exception.ComponentNotFoundException;
import so.dian.abtest.exception.InstanceNotFountException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AbTestFactory implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbTestFactory.class);

    @Setter
    private AbTestConfigHolder abTestConfigHolder;
    @Setter
    private List<AbTestInitConfig> initConfigs;

    private static Map<String, AbTestConfig> CONFIG_MAP = Maps.newConcurrentMap();

    private static Map<String, BucketCalculator> CALCULATOR_MAP = Maps.newConcurrentMap();
    private static Map<String, BucketBizProcessor> PROCESSOR_MAP = Maps.newConcurrentMap();
    private Map<String, AbTestInstance> INSTANCE_MAP = Maps.newConcurrentMap();

    private final Comparator comparator = (Comparator<AbTestConfig.AbTestBucketConfig>) (o1, o2) -> {
        int compareResult = o1.getValue().compareTo(o2.getValue());
        if (compareResult == 0){
            return o1.getBizName().compareTo(o2.getBizName());
        }else {
            return compareResult;
        }
    };

    private void updateConfig(){
        List<AbTestConfig> abTestConfigList = abTestConfigHolder.fetchConfig();
        if (CollectionUtils.isEmpty(abTestConfigList)){
            return;
        }
        for (AbTestConfig abTestConfig : abTestConfigHolder.fetchConfig()){
            CONFIG_MAP.put(abTestConfig.getInstanceName(), abTestConfig);
        }
    }

    private <BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> void processInitConfig(){
        for (AbTestInitConfig<BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> abTestInitConfig : initConfigs){
            for (BucketBizProcessor<PROCESS_PARAM, PROCESS_RESULT> processor : abTestInitConfig.getProcessors()){
                PROCESSOR_MAP.put(processor.getName(), processor);
            }
            CALCULATOR_MAP.put(abTestInitConfig.getInstanceName(), abTestInitConfig.getCalculator());
        }
    }

    @Scheduled(cron = "0 */10 * * * ?")
    private void rebuildInstance() throws ComponentNotFoundException {
        updateConfig();
        if (CollectionUtils.isEmpty(initConfigs)){
            return;
        }
        Map<String, AbTestInstance> instanceMap = Maps.newHashMap();
        for (AbTestInitConfig initConfig : initConfigs){
            String instanceName = initConfig.getInstanceName();
            AbTestConfig abTestConfig = CONFIG_MAP.get(instanceName);
            if (abTestConfig == null){
                continue;
            }
            List<AbTestConfig.AbTestBucketConfig> bucketConfigs = abTestConfig.getBuckets();
            Collections.sort(bucketConfigs, comparator);
            abTestConfig.setBuckets(bucketConfigs);

            AbTestInstance instance = new AbTestInstance();
            BucketCalculator calculator = CALCULATOR_MAP.get(instanceName);
            if (calculator == null){
                String errMsg = String.format("cannot find bucket calculator of instance '%s'.", instanceName);
                LOGGER.error(errMsg);
                throw new ComponentNotFoundException(errMsg);
            }
            instance.setCalculator(calculator);
            BucketBizProcessor defaultProcessor = PROCESSOR_MAP.get(abTestConfig.getDefaultBizName());
            if (defaultProcessor == null){
                String errMsg = String.format("cannot find default processor of instance '%s'.", instanceName);
                LOGGER.error(errMsg);
                throw new ComponentNotFoundException(errMsg);
            }
            instance.setDefaultProcessor(defaultProcessor);
            List<AbTestBucket> buckets = Lists.newArrayList();
            int startIndex = 0;
            for (AbTestConfig.AbTestBucketConfig bucketConfig : bucketConfigs){
                AbTestBucket abTestBucket = new AbTestBucket();
                AbTestBucketValueInterval intervalPair = new AbTestBucketValueInterval();
                intervalPair.setLeft(startIndex);
                intervalPair.setRight(startIndex + bucketConfig.getValue());
                abTestBucket.setIntervalPair(intervalPair);
                BucketBizProcessor processor = PROCESSOR_MAP.get(bucketConfig.getBizName());
                if (processor == null){
                    String errMsg = String.format("cannot find bucket processor named '%s' in instance '%s'.", bucketConfig.getBizName(), instanceName);
                    LOGGER.warn(errMsg);
                    continue;
                }
                abTestBucket.setProcessor(processor);
                buckets.add(abTestBucket);
                startIndex = intervalPair.getRight();
            }
            instance.setBuckets(ImmutableList.copyOf(buckets));
            instanceMap.put(instanceName, instance);
        }
        INSTANCE_MAP = instanceMap;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        processInitConfig();
        rebuildInstance();
    }

    public <BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> AbTestInstance<BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> getInstance(String name) throws InstanceNotFountException {
        AbTestInstance<BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> instance = INSTANCE_MAP.get(name);
        if (instance == null){
            throw new InstanceNotFountException(String.format("cannot find instance with name '%s'", name));
        }
        return instance;
    }
}
