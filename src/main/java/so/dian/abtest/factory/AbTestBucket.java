package so.dian.abtest.factory;

import lombok.Data;
import so.dian.abtest.bucket.BucketBizProcessor;

@Data
class AbTestBucket<PROCESS_PARAM, PROCESS_RESULT> {

    private AbTestBucketValueInterval intervalPair;
    private BucketBizProcessor<PROCESS_PARAM, PROCESS_RESULT> processor;
}
