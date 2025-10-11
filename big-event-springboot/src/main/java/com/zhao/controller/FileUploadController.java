package com.zhao.controller;


import com.zhao.pojo.Result;
import com.zhao.utils.AliOssUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.util.UUID;

@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        //把文件的内容存储到本地磁盘上
        String originalFilename = file.getOriginalFilename();
        //保证文件的名字是唯一的，从而防止文件覆盖
        // 安全地处理文件扩展名，确保即使没有扩展名也不会出错
        String ext = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String fileName = UUID.randomUUID().toString() + ext;
//        file.transferTo(new File("C:\\Code\\file\\" + fileName));
        String url = AliOssUtil.uploadFile(fileName,file.getInputStream());
        return Result.success(url);
    }


}
