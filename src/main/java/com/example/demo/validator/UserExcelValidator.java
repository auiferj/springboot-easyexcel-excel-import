package com.example.demo.validator;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.UserImport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class UserExcelValidator {
    public List<String> validate(UserImport customer, Map<String, Map<String, String>> dictionaryMap) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(customer.getName())) {
            errors.add("个人客户姓名不能为空");
        }


        return errors;
    }

}
