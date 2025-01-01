package com.example.demo.controller;

import com.example.demo.common.ResultResponse;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @author lenovo
 */
@RestController
@RequestMapping("/repCountry")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 导入
     *
     * @param
     */
    @PostMapping("/importExcel")
    public ResultResponse<Map<String, Object>> importExcelOverwriteExisting(@RequestParam("file") MultipartFile file, boolean overwriteExisting) throws Exception {
        Map<String, Object> result = userService.importExcelOverwriteExisting(file,overwriteExisting);
        return ResultResponse.success(result);
    }
}
