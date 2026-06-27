package org.example.model.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult implements Serializable {

    /**
     * 文件访问 URL
     */
    private String url;

    /**
     * COS 中的对象 Key
     */
    private String objectKey;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    private static final long serialVersionUID = 1L;

}
