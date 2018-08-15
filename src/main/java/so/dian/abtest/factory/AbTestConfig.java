package so.dian.abtest.factory;

import lombok.Data;

import java.util.List;

@Data
public class AbTestConfig {

    private String instanceName;

    private String defaultBizName;

    private List<AbTestBucketConfig> buckets;

    @Data
    public static class AbTestBucketConfig {
        private Integer value;
        private String bizName;
    }

}
