package com.example.demo.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.User;
import com.example.demo.entity.UserImport;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import com.example.demo.validator.UserExcelValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lenovo
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserExcelValidator userExcelValidator;
    public UserServiceImpl(UserExcelValidator userExcelValidator) {
        this.userExcelValidator = userExcelValidator;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcelOverwriteExisting(MultipartFile file, boolean overwriteExisting) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<User> validData = new ArrayList<>();
        Set<String> branchNamesInExcel = new HashSet<>();
        Set<String> branchesToDelete = new HashSet<>();

        try (InputStream inputStream = file.getInputStream()){

            List<UserImport> customerInfoList = new ArrayList<>();
            // 使用 EasyExcel 读取 Excel 文件
            EasyExcel.read(inputStream, UserImport.class, new AnalysisEventListener<UserImport>() {
                @Override
                public void invoke(UserImport customer, AnalysisContext context) {
                    branchNamesInExcel.add(customer.getOrgName());
                    customerInfoList.add(customer);  // 收集数据
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {

                }
            }).sheet().headRowNumber(2).doRead();

            // 检查数据库中是否存在对应的“分支机构”数据
            List<String> existingBranchNames = this.findExistingBranchNames(this,branchNamesInExcel);
            if (!existingBranchNames.isEmpty()) {
                if(overwriteExisting){
                    // 标记为待删除
                    branchesToDelete.addAll(existingBranchNames);
                }else{
                    result.put("status", "exists");
                    result.put("message", "以下分支机构已经存在：" + String.join(", ", existingBranchNames));
                    result.put("existingBranches", existingBranchNames);
                    return result;
                }
            }

            // 获取所有字典数据
            Map<String, Map<String, String>> dictionaryMap = new HashMap<>();
//            dictionaryMap.put("corporate-type", repCountryService.getDictMap("corporate-type", 36));
//            dictionaryMap.put("subject-code-country", repCountryService.getDictMapGuarantee("subject-code-country", 36));
//            dictionaryMap.put("sex-code", repCountryService.getDictMapGuarantee("sex-code", 36));
//            dictionaryMap.put("marital-code", repCountryService.getDictMapGuarantee("marital-code", 36));
//            dictionaryMap.put("loan-subject-code", repCountryService.getDictMapCodeToGuarantee("loan-subject-code", 36));
//            dictionaryMap.put("depart", repCountryService.getDepartMap());

            // 校验数据
            LocalDateTime date = LocalDateTime.now();
            for (int i = 0; i < customerInfoList.size(); i++) {
                UserImport customer = customerInfoList.get(i);
                List<String> rowErrors = userExcelValidator.validate(customer,dictionaryMap);

                // 判断是否重复（根据唯一字段）
//                if (customerRepository.existsByCertNo(customer.getCertNo())) {
//                    rowErrors.add("证件号码已存在，重复数据");
//                }

                if (!rowErrors.isEmpty()) {
                    errors.add("第 " + (i + 3) + " 行: " + String.join("; ", rowErrors));
                } else {
                    User user = new User();
                    BeanUtils.copyProperties(customer, user);
                    user.setCreateTime(date);
                    validData.add(user);
                }
            }

            // 如果有校验错误，返回错误信息
            if (!errors.isEmpty()) {
                result.put("status", "fail");
                result.put("errors", errors);
                return result;
            }

            // 如果没有校验错误，删除已存在的分支机构数据
            if (overwriteExisting && !branchesToDelete.isEmpty()) {
                for (String branchName : branchesToDelete) {
                    this.remove(new QueryWrapper<User>().eq("org_name", branchName));
                }
            }

            // 存入数据库
            this.saveBatch(validData,1000);

            result.put("status", "success");
            result.put("data", validData);
        } catch (Exception e) {
            log.error("导入失败：", e);
            result.put("status", "error");
            result.put("message", "导入失败：" + e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return result;
    }

    public <T> List<String> findExistingBranchNames(IService<T> mapper, Set<String> branchNamesInExcel) {
        if (branchNamesInExcel == null || branchNamesInExcel.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT org_name");
        queryWrapper.in("org_name", branchNamesInExcel);

        return mapper.listObjs(queryWrapper)
                .stream()
                .map(obj -> (String) obj)
                .collect(Collectors.toList());
    }
}
