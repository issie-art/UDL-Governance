package cn.udl.governance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * MinioEvent 类用于表示MinIO事件的结构
 * 包含一个记录列表，每个记录包含S3事件信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinioEvent {
    @JsonProperty("Records")
    private List<EventRecord> records;

    @Data
    public static class EventRecord {
        @JsonProperty("eventName")
        private String eventName;

        @JsonProperty("s3")
        private S3Info s3;
    }

    @Data
    public static class S3Info {
        @JsonProperty("object")
        private ObjectInfo object;
    }

    @Data
    public static class ObjectInfo {
        @JsonProperty("key")
        private String key;
    }
}
