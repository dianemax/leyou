package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.domain.ThumbImageConfig;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UploadService {

    @Autowired
    private FastFileStorageClient storageClient;//文件上传的最终地址


    private static final List<String> ALLOWTYPES = Arrays.asList("image/jpeg","image/png");

    public String uploadImage(MultipartFile file) {
        try {
            //校验格式
            String contentType = file.getContentType();
            if(!ALLOWTYPES.contains(contentType)){
                throw new LyException(ExceptionEnum.CONTENT_TYPE_ERROE);
            }
            //校验内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image == null){
                throw new LyException(ExceptionEnum.INVALID_IMAGE_ERROE);
            }
            /*//准备目标路径
            File dest = new File("D:/javacode/idea/upload/", file.getOriginalFilename());
            //保存到本地
            file.transferTo(dest);*/

            //上传到FastDFS
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);

            //System.out.println(storePath.getFullPath());

            //返回路径
            return "http://image.leyou.com/" + storePath.getFullPath();

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new LyException(ExceptionEnum.UPLOAD_FAILED_ERROE);

        }
    }
}
