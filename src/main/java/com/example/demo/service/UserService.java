package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @author lenovo
 */
public interface UserService extends IService<User> {
    Map<String, Object> importExcelOverwriteExisting(MultipartFile file, boolean overwriteExisting);
}
