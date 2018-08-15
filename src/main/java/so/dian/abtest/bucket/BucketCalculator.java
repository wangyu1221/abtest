package so.dian.abtest.bucket;

public interface BucketCalculator<BUCKET_CALC_PARAM> {

    Integer calculate(BUCKET_CALC_PARAM param);
}
