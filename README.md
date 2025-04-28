public ResponseEntity<?> parseExcel(@RequestBody Map<String, Object> body) {
    final int MAX_ROWS_PER_SHEET = 500000;
    
    try {
        List<?> headers = (List<?>) body.get("headers");
        List<?> data = (List<?>) body.get("data");
        
        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().body("No data provided");
        }

        // For small datasets (<500K), return single Excel file as before
        if (data.size() <= MAX_ROWS_PER_SHEET) {
            try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
                wb.setCompressTempFile(false);
                createSheet(wb, "data", headers, data);
                
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    wb.write(out);
                    return ResponseEntity.ok()
                        .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "attachment; filename=data.xlsx")
                        .body(out.toByteArray());
                }
            }
        } 
        // For large datasets (>500K), split into multiple sheets and zip
        else {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                
                int sheetCount = (int) Math.ceil((double) data.size() / MAX_ROWS_PER_SHEET);
                
                for (int i = 0; i < sheetCount; i++) {
                    int fromIndex = i * MAX_ROWS_PER_SHEET;
                    int toIndex = Math.min((i + 1) * MAX_ROWS_PER_SHEET, data.size());
                    List<?> subData = data.subList(fromIndex, toIndex);
                    
                    try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
                        wb.setCompressTempFile(false);
                        createSheet(wb, "data_" + (i + 1), headers, subData);
                        
                        // Add workbook to zip
                        zos.putNextEntry(new ZipEntry("data_part_" + (i + 1) + ".xlsx"));
                        try (ByteArrayOutputStream sheetOut = new ByteArrayOutputStream()) {
                            wb.write(sheetOut);
                            zos.write(sheetOut.toByteArray());
                        }
                        zos.closeEntry();
                    }
                }
                
                zos.finish();
                return ResponseEntity.ok()
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", "attachment; filename=data_export.zip")
                    .body(baos.toByteArray());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError()
            .body("Error while creating excel: " + e.getMessage());
    }
}

private void createSheet(SXSSFWorkbook wb, String sheetName, List<?> headers, List<?> data) {
    SXSSFSheet sh = wb.createSheet(sheetName);
    
    // Create header row
    Row headerRow = sh.createRow(0);
    MutableInt headerIndex = new MutableInt();
    headers.forEach(key -> 
        headerRow.createCell(headerIndex.getAndIncrement()).setCellValue(String.valueOf(key)));
    
    // Create data rows
    for (int i = 0; i < data.size(); i++) {
        Row row = sh.createRow(i + 1);
        List<?> rowData = (List<?>) data.get(i);
        MutableInt cellIndex = new MutableInt();
        
        rowData.forEach(val -> {
            String value = String.valueOf(val);
            row.createCell(cellIndex.getAndIncrement())
               .setCellValue("null".equals(value) ? "" : value);
        });
    }
}
