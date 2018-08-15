package so.dian.abtest.factory;

import lombok.Setter;
import org.springframework.util.CollectionUtils;
import so.dian.abtest.bucket.BucketCalculator;
import so.dian.abtest.bucket.BucketBizProcessor;

import java.util.List;

public class AbTestInstance<BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> {

    @Setter
    private BucketBizProcessor<PROCESS_PARAM, PROCESS_RESULT> defaultProcessor;
    @Setter
    private BucketCalculator<BUCKET_CALC_PARAM> calculator;
    @Setter
    private List<AbTestBucket<PROCESS_PARAM, PROCESS_RESULT>> buckets;

    public PROCESS_RESULT execute(BUCKET_CALC_PARAM bucketCalcParam, PROCESS_PARAM processParam) {
        Integer calcResult = 0;
        if (calculator != null){
            calcResult = calculator.calculate(bucketCalcParam);
        }
        if (CollectionUtils.isEmpty(buckets)){
            return defaultProcessor == null ? null : defaultProcessor.process(processParam);
        }
        for (AbTestBucket<PROCESS_PARAM, PROCESS_RESULT> bucket : buckets) {
            if (calcResult.compareTo(bucket.getIntervalPair().getLeft()) >= 0 && calcResult.compareTo(bucket.getIntervalPair().getRight()) < 0) {
                return bucket.getProcessor().process(processParam);
            }
        }
        return defaultProcessor == null ? null : defaultProcessor.process(processParam);
    }
}
