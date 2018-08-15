package so.dian.abtest.factory;

import lombok.Data;
import so.dian.abtest.bucket.BucketBizProcessor;
import so.dian.abtest.bucket.BucketCalculator;

import java.util.List;

@Data
public class AbTestInitConfig<BUCKET_CALC_PARAM, PROCESS_PARAM, PROCESS_RESULT> {

    private String instanceName;
    private BucketCalculator<BUCKET_CALC_PARAM> calculator;
    private List<BucketBizProcessor<PROCESS_PARAM, PROCESS_RESULT>> processors;
}
