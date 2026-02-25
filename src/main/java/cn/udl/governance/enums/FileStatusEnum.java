package cn.udl.governance.enums;

import lombok.Getter;

@Getter
public enum FileStatusEnum {
    UPLOADED(1, "Uploaded"),
    ACTIVE(2, "Active"),
    EXPIRED(3, "Expired"),
    RECYCLED(4, "Recycled"),
    DESTROYED(5, "Destroyed");

    private final Integer code;
    private final String name;

    FileStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileStatusEnum fromCode(Integer code) {
        for (FileStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid file status code: " + code);
    }
}