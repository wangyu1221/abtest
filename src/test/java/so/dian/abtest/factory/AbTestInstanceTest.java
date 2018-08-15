package so.dian.abtest.factory;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import so.dian.abtest.bucket.BucketBizProcessor;
import so.dian.abtest.bucket.BucketCalculator;

import java.util.List;

public class AbTestInstanceTest {

    private BucketBizProcessor defaultProcessor(){
        return new BucketBizProcessor<String, String>("default"){

            @Override
            public String process(String o) {
                return getName() + "-" + o;
            }
        };
    }
    private BucketBizProcessor bizProcessor(String biaName){
        return new BucketBizProcessor<String, String>(biaName){

            @Override
            public String process(String o) {
                return getName() + "-" + o;
            }
        };
    }

    private AbTestBucket createBucket(Integer left, Integer right, BucketBizProcessor processor){
        AbTestBucket bucket = new AbTestBucket();
        AbTestBucketValueInterval valueInterval = new AbTestBucketValueInterval();
        valueInterval.setLeft(left);
        valueInterval.setRight(right);
        bucket.setIntervalPair(valueInterval);
        bucket.setProcessor(processor);
        return bucket;
    }

    @Test
    public void noCalculator(){
        AbTestInstance<Integer, String, String> abTestInstance = new AbTestInstance();
        abTestInstance.setDefaultProcessor(defaultProcessor());
        Assert.assertEquals("default-fuck", abTestInstance.execute(0, "fuck"));
    }

    @Test
    public void emptyBuckets(){
        AbTestInstance<Integer, String, String> abTestInstance = new AbTestInstance();
        abTestInstance.setDefaultProcessor(defaultProcessor());
        abTestInstance.setCalculator(new BucketCalculator<Integer>() {
            @Override
            public Integer calculate(Integer integer) {
                return integer % 3;
            }
        });
        Assert.assertEquals("default-Sylvanas", abTestInstance.execute(0, "Sylvanas"));
        Assert.assertEquals("default-Thrall", abTestInstance.execute(1, "Thrall"));
        Assert.assertEquals("default-VolJin", abTestInstance.execute(2, "VolJin"));
    }

    @Test
    public void hitBucket(){
        AbTestInstance<Integer, String, String> abTestInstance = new AbTestInstance();
        abTestInstance.setDefaultProcessor(defaultProcessor());
        abTestInstance.setCalculator(new BucketCalculator<Integer>() {
            @Override
            public Integer calculate(Integer integer) {
                return integer % 10;
            }
        });
        List<AbTestBucket<String, String>> buckets = Lists.newArrayList();
        buckets.add(createBucket(0, 3, bizProcessor("biz1")));
        buckets.add(createBucket(3, 6, bizProcessor("biz2")));
        buckets.add(createBucket(6, 9, bizProcessor("biz3")));
        abTestInstance.setBuckets(buckets);

        Assert.assertEquals("biz1-Garrosh", abTestInstance.execute(10, "Garrosh"));
        Assert.assertEquals("biz1-Garrosh", abTestInstance.execute(11, "Garrosh"));
        Assert.assertEquals("biz1-Garrosh", abTestInstance.execute(12, "Garrosh"));
        Assert.assertEquals("biz2-Cairne", abTestInstance.execute(13, "Cairne"));
        Assert.assertEquals("biz2-Cairne", abTestInstance.execute(14, "Cairne"));
        Assert.assertEquals("biz2-Cairne", abTestInstance.execute(15, "Cairne"));
        Assert.assertEquals("biz3-Saurfang", abTestInstance.execute(16, "Saurfang"));
        Assert.assertEquals("biz3-Saurfang", abTestInstance.execute(17, "Saurfang"));
        Assert.assertEquals("biz3-Saurfang", abTestInstance.execute(18, "Saurfang"));
        Assert.assertEquals("default-Ogrim", abTestInstance.execute(19, "Ogrim"));
    }
}
