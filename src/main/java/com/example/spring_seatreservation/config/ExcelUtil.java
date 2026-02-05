package com.example.spring_seatreservation.config;

import com.example.spring_seatreservation.common.MyUser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    public static List<MyUser> parseStudentExcel(
            MultipartFile file,
            String password
    ) throws Exception {

        List<MyUser> list = new ArrayList<>();

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // 学号
            Cell numberCell = row.getCell(0);
            if (numberCell == null) continue;

            numberCell.setCellType(CellType.STRING);
            long number = Long.parseLong(numberCell.getStringCellValue().trim());

            // 姓名
            Cell nameCell = row.getCell(1);
            if (nameCell == null) continue;

            String username = nameCell.getStringCellValue().trim();

            MyUser user = new MyUser();
            user.setNumber(number);
            user.setUsername(username);
            user.setPassword(password);
            user.setType(0);

            list.add(user);
        }

        workbook.close();
        return list;
    }
}
