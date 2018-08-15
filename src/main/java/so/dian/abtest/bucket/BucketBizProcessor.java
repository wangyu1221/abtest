package so.dian.abtest.bucket;

public abstract class BucketBizProcessor<PROCESS_PARAM, PROCESS_RESULT> {

    private String name;

    public BucketBizProcessor(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract PROCESS_RESULT process(PROCESS_PARAM param);
}
