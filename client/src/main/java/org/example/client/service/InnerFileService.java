package org.example.client.service;

/**
 * 文件服务 Dubbo 接口
 * 提供文件删除能力
 */
public interface InnerFileService {

    /**
     * 根据文件 URL 删除文件
     *
     * @param fileUrl 文件访问 URL
     * @return 是否删除成功
     */
    boolean deleteByUrl(String fileUrl);

    /**
     * 根据 COS 对象 Key 删除文件
     *
     * @param objectKey COS 对象 Key
     * @return 是否删除成功
     */
    boolean deleteByObjectKey(String objectKey);
}
