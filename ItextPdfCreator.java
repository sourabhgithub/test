package com.experian.incomeandemployment.pdf;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.experian.incomeandemployment.response.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.experian.incomeandemployment.constants.IncomeandEmploymentConstants;
import com.experian.incomeandemployment.exception.IncomeAndEmploymentException;
import com.experian.incomeandemployment.exception.NotFitException;
import com.experian.incomeandemployment.request.dto.ReportCustomLabels;
import com.experian.incomeandemployment.util.CommonUtils;
import com.experian.incomeandemployment.util.IncomeUtil;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;

/**
 * Render a IHIS (aka Diamond project, aka Experian Verify) PDF
 *
 * @author C11538A
 * @version 1.0
 * @since Mar. 5, 2021
 */
public class ItextPdfCreator {
    public static final String NOT_AVAILABLE = JsonUtil.NULL_VALUE;
    // Colors
    protected static final BaseColor COLOR_EXPERIAN_COOL_GREY_11 = new BaseColor(83, 86, 90, 255);
    protected static final BaseColor COLOR_EXPERIAN_GREY_10 = new BaseColor(239, 240, 240, 255);
    protected static final BaseColor COLOR_EXPERIAN_GREY_40 = new BaseColor(193, 194, 195, 255);
    protected static final BaseColor COLOR_EXPERIAN_PRIMARY_BLUE_2 = new BaseColor(66, 109, 169, 255);
    protected static final BaseColor COLOR_EXPERIAN_WHITE = new BaseColor(255, 255, 255, 255);

    protected static final int FIELD_SEPARATOR = 10;
    protected static final int FIELD_SPACE = 3;

    // Fonts
    protected static final Font FONT_10_BOLD = FontFactory.getFont("Roboto", 10, Font.BOLD,
            COLOR_EXPERIAN_COOL_GREY_11);
    protected static final Font FONT_10_NORMAL = FontFactory.getFont("Roboto", 10, Font.NORMAL,
            COLOR_EXPERIAN_COOL_GREY_11);
    protected static final Font FONT_15_NORMAL = FontFactory.getFont("Roboto", 15, Font.NORMAL,
            COLOR_EXPERIAN_COOL_GREY_11);
    protected static final Font FONT_8_BLUE = FontFactory.getFont("Roboto", 8, Font.NORMAL,
            COLOR_EXPERIAN_PRIMARY_BLUE_2);
    protected static final Font FONT_8_BLUE_BOLD = FontFactory.getFont("Roboto", 8, Font.BOLD,
            COLOR_EXPERIAN_PRIMARY_BLUE_2);
    protected static final Font FONT_8_BOLD = FontFactory.getFont("Roboto", 8, Font.BOLD, COLOR_EXPERIAN_COOL_GREY_11);
    protected static final Font FONT_8_NORMAL = FontFactory.getFont("Roboto", 8, Font.NORMAL,
            COLOR_EXPERIAN_COOL_GREY_11);
    protected static final Font FONT_8_UNDERLINE = FontFactory.getFont("Roboto", 8, Font.UNDERLINE,
            COLOR_EXPERIAN_COOL_GREY_11);
    protected static final int MARGIN_INSIDE_L_R = 10;
    protected static final int MARGIN_OUTSIDE_L_R = 20;
    protected static final int MARGIN_OUTSIDE_U_D = 15;
    protected static final int PAGE_CUT = MARGIN_OUTSIDE_U_D + 35;

    protected PdfContentByte cb = null;

    protected Document document = null;
    protected List<PdfTemplate> pageNumber = new ArrayList<>();
    protected float y = 0f;
    protected boolean isVOE = false;
    protected boolean isCORE = false;
    protected boolean isPlus = false;
    protected boolean isHR = false;
    protected boolean isPremium = false;
    private List<String> executedYear = new ArrayList<>();


    /**
     * Creates the Experian Verify Premium report.
     *
     * @param expVerifyReport
     * @throws DocumentException
     */
    private void createPremiumReport(IncomeReport expVerifyReport) throws DocumentException {
        boolean finished = false;
        boolean hasData = true;
        boolean footerOnly = false;
        int total = 0;
        int current = 0;
        if (expVerifyReport.getConsumerPii() != null && expVerifyReport.getConsumerPii().getEmploymentHistory() != null) {
            List<EmploymentHistory> employmentHistory = expVerifyReport.getConsumerPii().getEmploymentHistory();
            total = employmentHistory.size();
        }
        int page = 1;
        while (true) {
            if (isVOE || isCORE || isHR) {
                writePageHeader("VERIFICATION OF EMPLOYMENT");
            } else if (isPremium || isPlus){
                writePageHeader("VERIFICATION OF EMPLOYMENT AND INCOME");
            }
            try {
                if (expVerifyReport == null) {
                    writeEmployerTitle(null);
                    finished = true;
                }
                if (page == 1) {
                    writeReportHeader(expVerifyReport);

                }
                if (hasData) {
                    if (isVOE || isCORE || isHR) {
                        writeEmployerTitle("Employment Details");
                    } else if (isPremium || isPlus){
                        writeEmployerTitle("Employment and Income Details");
                    }
                }
                for (int i = current; i < total; i++) {
                    EmploymentHistory eh = expVerifyReport.getConsumerPii().getEmploymentHistory().get(i);
                    writePremuimEmployer(eh, (i + 1), total);
                    executedYear = new ArrayList<>();
                }
                hasData = false;

            } catch (NotFitException nfe) {
                // Ignored, we just need to add a new page
            }
            writePageFooter(footerOnly, false);
            if (!hasData) {
                try {
                    footerOnly = true;
                    // Report footer
                    writeReportFooter(expVerifyReport);
                    finished = true;
                } catch (NotFitException nfe) {
                    // Ignored, we just need to add a new page
                }
            }
            // template let us to set the text after document is closed.
            PdfTemplate template = cb.createTemplate(300, 20);
            cb.addTemplate(template, 280, 12);
            pageNumber.add(template);
            if (finished) {
                break;
            }
            document.newPage();
            page++;
        }
        // Update page numbers
        total = pageNumber.size();
        page = 1;
        Font font = FontFactory.getFont("Roboto-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED, 8f,
                Font.NORMAL, COLOR_EXPERIAN_COOL_GREY_11);
        BaseFont bf = font.getBaseFont();
        for (PdfTemplate t : pageNumber) {
            t.beginText();
            t.setFontAndSize(bf, 8f);
            t.setColorFill(COLOR_EXPERIAN_COOL_GREY_11);
            t.setTextMatrix(0, 5);
            t.showText(String.format("Page %d of %d", page, total));
            t.endText();
            page++;
        }
    }


    /**
     * Compound all 4 address lines into a single string
     *
     * @param address Address values
     * @return Address as string
     */
    private String getFullAddress(RespCurrentAddress address) {
        String fa = address.getLine1();
        if (address.getLine2() != null) {
            fa += ", " + address.getLine2();
        }
        if (address.getLine3() != null) {
            fa += " " + address.getLine3();
        }
        if (address.getLine3() != null) {
            fa += " " + address.getLine3();
        }
        if (address.getCity() != null) {
            fa += ", " + address.getCity();
        }
        if (address.getState() != null) {
            fa += ", " + address.getState();
        }
        if (address.getZipCode() != null) {
            fa += " " + address.getZipCode();
        }

        return fa;
    }

    /**
     * Test for the available space before render a text. If there is no available
     * space, this create a new line and put the text
     *
     * @param font1      Font for text1
     * @param text1      First text (label)
     * @param font2      Font for text 2
     * @param text2      Second text (value)
     * @param x          Left coordinate
     * @param y          Bottom coordinate
     * @param lineHeight Heigt for next line
     * @param maxX       Right limit
     * @param left       Left margin for new line
     * @return An array with the {@code total_width}, {@code x} and {@code y}
     * @throws DocumentException
     */
    private float[] testAndPut(Font font1, String text1, Font font2, String text2, float x, float y, float lineHeight,
                               float maxX, float left, boolean background) throws DocumentException {
        float w1 = writeText(font1, text1, x, y, x + 300, y + lineHeight, true);
        float w2 = writeText(font2, text2, x, y, x + 300, y + lineHeight, true);
        if ((x + w1 + FIELD_SPACE + w2) > maxX) { // label + value does not fit in this line
            y -= lineHeight;
            x = left;
            if (background) {
                float pw = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
                cb.saveState();
                cb.setColorFill(COLOR_EXPERIAN_GREY_10);
                cb.rectangle(MARGIN_OUTSIDE_L_R, y - 5, pw, lineHeight);
                cb.fill();
                cb.restoreState();
            }
        }
        float llx = x;
        w1 = writeText(font1, text1, llx, y, llx + 300, y + lineHeight);
        llx += w1 + FIELD_SPACE;
        w2 = writeText(font2, text2, llx, y, llx + 300, y + lineHeight);
        return new float[]{w1 + FIELD_SPACE + w2, x, y};
    }


    private String getFullAddress(EmployerAddress address) {
        String fa = address.getLineOne();
        if (address.getLineTwo() != null) {
            fa += ", " + address.getLineTwo();
        }
        if (address.getLineThree() != null) {
            fa += " " + address.getLineThree();
        }
        return fa;
    }

    /**
     * Write the "Payroll - Direct Deposit Information" line
     *
     * @throws NotFitException
     * @throws DocumentException
     */
    private void writeEmployerPayrollSubTitle() throws NotFitException, DocumentException {
        float x = MARGIN_OUTSIDE_L_R;
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 20;
        if ((y - h) < PAGE_CUT) {
            throw new NotFitException();
        }
        y -= h;
        // Gray
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(x, y, w, h);
        cb.fill();
        // White
        w -= 2 * MARGIN_INSIDE_L_R;
        x += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.rectangle(x, y, w, h);
        cb.fill();
        cb.restoreState();
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        writeText(FONT_8_NORMAL, "Payroll - Direct Deposit Information", x, y + 5, x + 500, y + 17);
    }

    /**
     * Write the "Employer N of M" sub title
     *
     * @param i     Current index
     * @param total Total count
     * @throws DocumentException
     * @throws NotFitException
     */
    private void writeEmployerSubTitle(int i, int total) throws DocumentException, NotFitException {
        float x = MARGIN_OUTSIDE_L_R;
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 20;
        if ((y - h) < PAGE_CUT) {
            throw new NotFitException();
        }
        y -= h;
        // Gray
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(x, y, w, h);
        cb.fill();
        // White
        w -= 2 * MARGIN_INSIDE_L_R;
        x += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.rectangle(x, y, w, h);
        cb.fill();
        cb.restoreState();
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        writeText(FONT_10_NORMAL, String.format("Employer %d of %d", i, total), x, y + 5, x + 500, y + 20);
    }

    /**
     * Create the rounded rectangle to start the employer section (the white inner
     * rectangle). If a title is provided, render it.
     *
     * @param title Title to add (optional)
     * @throws DocumentException
     * @throws NotFitException
     */
    private void writeEmployerTitle(String title) throws DocumentException, NotFitException {
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 50;
        float x = MARGIN_OUTSIDE_L_R;
        if ((y - h) < PAGE_CUT) {
            throw new NotFitException();
        }
        y = y - h;
        // Gray background
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(x, y, w, h + 10);
        cb.fill();
        // White rounded inside
        w -= 2 * MARGIN_INSIDE_L_R;
        x += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.roundRectangle(x, y - 10, w, h + 10, 10);
        cb.fill();
        cb.restoreState();
        if (title != null && !title.isEmpty()) {
            x += FIELD_SEPARATOR;
            writeText(FONT_10_BOLD, title, x, y + 20, x + 200, y + 35);
        }
    }


    private void writeError(IncomeAndEmploymentException error) throws DocumentException {
        float x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        float maxw = document.getPageSize().getWidth() - x;
        y += 20;
        String code = String.valueOf(error.errorCode());
        if (code == null || code.isEmpty()) {
            code = "(no code)";
        }
        float w = writeText(FONT_8_BOLD, "Error code:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, code, x, y, x + 50, y + 12);
        x += w + FIELD_SEPARATOR;
        String msg = error.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = "Null";
        }
        w = writeText(FONT_8_BOLD, "Message:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        writeText(FONT_8_NORMAL, msg, x, y, (maxw - x), y + 12);
        y -= 15;
    }

    /**
     * Render the page footer, using currently vertical location.
     *
     * @param footerOnly  {@code true} when all data is done
     * @param showMessage {@code true} if we need to render the default foot note
     * @throws DocumentException
     */
    private void writePageFooter(boolean footerOnly, boolean showMessage) throws DocumentException {
        float h = y - MARGIN_OUTSIDE_U_D;
        float x = MARGIN_OUTSIDE_L_R;
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        // Gray background
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        if (footerOnly) {
            cb.rectangle(x, y - 10, w, 20); // Upper side
        } else {
            cb.rectangle(x, y - 10, w, 10); // Upper side
        }
        cb.roundRectangle(x, MARGIN_OUTSIDE_U_D, w, h, 10); // Lower side
        cb.fill();
        if (!footerOnly) {
            // White rectangle
            h = 20;
            w -= 2 * MARGIN_INSIDE_L_R;
            x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R;
            y -= h;
            cb.setColorFill(COLOR_EXPERIAN_WHITE);
            cb.rectangle(x, y + 10, w, h - 10);
            cb.roundRectangle(x, y, w, h, 10);
            cb.fill();
        }
        cb.restoreState();
        if (showMessage) {
            x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R;
            y -= 15;
            writeText(FONT_8_NORMAL, "* N/A is returned when information is not available in the source data", x, y,
                    x + 500, y + 12);
        }
    }

    /**
     * Add Header to page. The header is defined as top images and (optional) the
     * title
     *
     * @param title If present, will be render on the top of the page
     * @throws DocumentException
     */
    private void writePageHeader(String title) throws DocumentException {
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 86;
        float x = MARGIN_OUTSIDE_L_R;
        float y = document.getPageSize().getHeight() - MARGIN_OUTSIDE_U_D - h;
        // Gray background
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.roundRectangle(x, y, w, h, 10);
        cb.fill();
        cb.restoreState();
        // Images
        try {
            URL imgUrl = Thread.currentThread().getContextClassLoader().getResource("dataart5.png");
            Image png = Image.getInstance(imgUrl);
            png.setAbsolutePosition(x, y + 10);
            cb.addImage(png);
            imgUrl = Thread.currentThread().getContextClassLoader().getResource("logo_204x80.png");
            png = Image.getInstance(imgUrl);
            png.scalePercent(27f);
            png.setAbsolutePosition(x + w - 70, y + 47);
            cb.addImage(png);
        } catch (Exception e) {
            throw new DocumentException(e);
        }
        if (title != null && !title.isEmpty()) {
            float tw = writeText(FONT_10_BOLD, title, x, y, x + 500, y + 20, true);
            w = document.getPageSize().getWidth() - MARGIN_OUTSIDE_L_R - FIELD_SEPARATOR;
            writeText(FONT_10_BOLD, title, w - tw - 1, y + 27, w, y + 42);
        }
        this.y = y;
    }


    public void writePDF(IncomeAndEmploymentResponse incomeAndEmploymentResponse, Document pdf, PdfContentByte cb)
            throws Exception {
        this.document = pdf;
        String reportType = incomeAndEmploymentResponse.getExpVerifyReport().getReportType();
        this.cb = cb;

        isVOE = IncomeandEmploymentConstants.REPORT_TYPE_REVERIFY_VOE.equalsIgnoreCase(reportType)
                || IncomeandEmploymentConstants.REPORT_TYPE_PREMIUM_REVERIFICATION_VOE.equalsIgnoreCase(reportType);
        isCORE = IncomeandEmploymentConstants.REPORT_TYPE_ENHANCED_CORE.equalsIgnoreCase(reportType);
        isPlus = IncomeandEmploymentConstants.REPORT_TYPE_PLUS.equalsIgnoreCase(reportType);
        isPremium = IncomeUtil.isPremiumOrVOIE(reportType);
        isHR = IncomeandEmploymentConstants.REPORT_TYPE_VERIFY_EMPLOYMENT.equalsIgnoreCase(reportType);

        if (IncomeUtil.isEnhancedReportType(reportType) || isPlus || isHR) {
            createPremiumReport(incomeAndEmploymentResponse.getExpVerifyReport());
        }

    }

    private void writePlusEmployerDetails(EmploymentHistoryPlus eh, PaymentHistoryStandard ph)
            throws NotFitException, DocumentException {
        float h = 42;
        if ((y - h) < PAGE_CUT) {
            throw new NotFitException();
        }
        float x = MARGIN_OUTSIDE_L_R;
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        // Gray
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(x, y - h, w, h);
        cb.fill();
        // White
        w -= 2 * MARGIN_INSIDE_L_R;
        x += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.rectangle(x, y - h, w, h);
        cb.fill();
        cb.restoreState();
        // First line
        y -= 12;
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "As Of Date:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, eh.getAsOfDate() != null ? CommonUtils.getFormattedDate(eh.getAsOfDate()) : JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Pay Frequency:", x, y, x + 100, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, ph.getPayCycle() != null ? ph.getPayCycle() : JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Reporting Period:", x, y, x + 100, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, String.format("%s - %s", CommonUtils.getFormattedDate(ph.getPayPeriod().getStartDate()), CommonUtils.getFormattedDate(ph.getPayPeriod().getEndDate())), x, y,
                x + 200, y + 12);
        y -= 15;
    }

    /**
     * Writes The 2 lines with details before Payment history tables
     *
     * @param eh Current Employment history record
     * @param ph Current Payment History record
     * @throws NotFitException
     * @throws DocumentException
     */
    private void writePremiumEmployerDetails(EmploymentHistoryEnhanced eh, PaymentHistoryEnhanced ph)
            throws NotFitException, DocumentException {
        float h = 42;
        if ((y - h) < PAGE_CUT) {
            throw new NotFitException();
        }
        float x = MARGIN_OUTSIDE_L_R;
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        // Gray
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(x, y - h, w, h);
        cb.fill();
        // White
        w -= 2 * MARGIN_INSIDE_L_R;
        x += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.rectangle(x, y - h, w, h);
        cb.fill();
        cb.restoreState();
        // First line
        y -= 12;
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "As Of Date:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, eh.getAsOfDate() != null ? CommonUtils.getFormattedDate(eh.getAsOfDate()) : JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Pay Date:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, ph.getPayDate() != null ? CommonUtils.getFormattedDate(ph.getPayDate()) : JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Pay Frequency:", x, y, x + 100, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, ph.getPayCycle() != null ? ph.getPayCycle() : JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Reporting Period:", x, y, x + 100, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, String.format("%s - %s", CommonUtils.getFormattedDate(ph.getPayPeriod().getStartDate()), CommonUtils.getFormattedDate(ph.getPayPeriod().getEndDate())), x, y,
                x + 200, y + 12);
        // Second line
        y -= 15;
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Pay Description:", x, y, x + 100, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, ph.getPayDescription() != null ? ph.getPayDescription() : NOT_AVAILABLE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Rate:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, ph.getBasePayRate() != null ? Optional.ofNullable("$" + getRoundOffValue(ph.getBasePayRate().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE, x, y, x + 100, y + 12);
        x += w + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "Hours:", x, y, x + 50, y + 12);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, ph.getPayPeriodHours() != null ? String.valueOf(ph.getPayPeriodHours()) : NOT_AVAILABLE, x, y, x + 100, y + 12);
        y -= 15;
    }

    /**
     * Writes the Employer name and address
     *
     * @param eh Employer history object
     * @throws DocumentException If an error occurs when the text is rendered
     * @throws NotFitException   If the contents does not fit in the page
     */
    private void writePremiumEmployerName(EmploymentHistory eh) throws DocumentException, NotFitException {
        float x = MARGIN_OUTSIDE_L_R;
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 32;
        if ((y - h) < PAGE_CUT) {
            throw new NotFitException();
        }
        y -= h;
        // Gray
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(x, y, w, h);
        cb.fill();
        // White
        w -= 2 * MARGIN_INSIDE_L_R;
        x += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.rectangle(x, y, w, h);
        cb.fill();
        cb.restoreState();
        y += 15;
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        w = writeText(FONT_10_NORMAL, returnValueOrNA(eh.getEmployerName()), x, y + 5, x + 500, y + 20);
        x += w + FIELD_SEPARATOR;
        String fa = getFullAddress(eh.getEmployerAddress());
        if (eh.getEmployerAddress().getCityName() != null) {
            fa += ", " + eh.getEmployerAddress().getCityName();
        }
        if (eh.getEmployerAddress().getState() != null) {
            fa += ", " + eh.getEmployerAddress().getState();
        }
        if (eh.getEmployerAddress().getPostalCode() != null) {
            fa += " " + eh.getEmployerAddress().getPostalCode();
        }
        writeText(FONT_10_NORMAL, fa, x, y + 5, x + 500, y + 20);
        y -= 12;
        x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        w = writeText(FONT_8_BLUE_BOLD, "FEIN:", x, y, x + 50, y + 15);
        x += w + FIELD_SPACE;
        w = writeText(FONT_8_NORMAL, eh.getEmployerIdentificationNumber() != null ? eh.getEmployerIdentificationNumber() : JsonUtil.NULL_VALUE, x, y, x + 100, y + 15);
        x += w + FIELD_SEPARATOR;
    }

    /**
     * Render first Premium employer table
     *
     * @param eh Employer history object
     * @throws DocumentException
     * @throws NotFitException   If table does not fit in the current page
     */
    private void writePremiumEmployerTable1(EmploymentHistory eh) throws DocumentException, NotFitException {

        String[] header1 = null;
        String[] header2 = null;
        List<String[]> values = new ArrayList<>();
        List<String[]> values1 = new ArrayList<>();
        String[] row1 = null;

        float[] weight = null;
        if (isVOE || isCORE) {
            EmploymentHistoryVOE employmentHistoryVOE = (EmploymentHistoryVOE) eh;

            header1 = new String[]{"Title", "As Of Date", "Original Hire Date", "Most Recent Hire Date",
                    "Employment Status", "Work Status", "Tenure"};
            weight = new float[]{0.13f, 0.13f, 0.17f, 0.2f, 0.15f, 0.1f, 0.15f};
            row1 = new String[]{returnValueOrNA(employmentHistoryVOE.getPositionTitle())
                    , returnValueOrNA(CommonUtils.getFormattedDate(employmentHistoryVOE.getAsOfDate()))
                    , Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryVOE.getOriginalHireDate())).orElse(NOT_AVAILABLE),

                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryVOE.getMostRecentHireDate())).orElse(NOT_AVAILABLE),
                    employmentHistoryVOE.getEmploymentStatus() != null ? Optional.ofNullable(employmentHistoryVOE.getEmploymentStatus().getName()).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    employmentHistoryVOE.getWorkStatus() != null ? Optional.ofNullable(employmentHistoryVOE.getWorkStatus().getName()).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    Optional.ofNullable(CommonUtils.formatTenure(employmentHistoryVOE.getPositionTenure())).orElse(NOT_AVAILABLE)};
        } else if(isPremium) {
            EmploymentHistoryEnhanced employmentHistoryEnhanced = (EmploymentHistoryEnhanced) eh;

            header1 = new String[]{"Title", "Original Hire Date", "Most Recent Hire Date", "Employment Status",
                    "Work Status", "Tenure"};
            row1 = new String[]{employmentHistoryEnhanced.getPositionTitle() != null ? employmentHistoryEnhanced.getPositionTitle() : NOT_AVAILABLE,
                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryEnhanced.getOriginalHireDate())).orElse(NOT_AVAILABLE),
                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryEnhanced.getMostRecentHireDate())).orElse(NOT_AVAILABLE),
                    employmentHistoryEnhanced.getEmploymentStatus() != null ? Optional.ofNullable(employmentHistoryEnhanced.getEmploymentStatus().getName()).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    employmentHistoryEnhanced.getWorkStatus() != null ? Optional.ofNullable(employmentHistoryEnhanced.getWorkStatus().getName()).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    Optional.ofNullable(CommonUtils.formatTenure(employmentHistoryEnhanced.getPositionTenure())).orElse(NOT_AVAILABLE)};
        } else if(isHR){

            EmploymentHistoryEmploymentScreening employmentHistoryEmploymentScreening = (EmploymentHistoryEmploymentScreening) eh;
            List<EmploymentInformation> employmentInformationList = employmentHistoryEmploymentScreening.getEmploymentInformation();
            EmploymentInformation employmentInformation = getEmploymentInformation(employmentInformationList, values1);
            header1 = new String[]{"As Of Date", "Original Hire Date", "Most Recent Hire Date",
                    "Position End Date", "Tenure"};
            row1 = new String[]{Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryEmploymentScreening.getAsOfDate())).orElse(NOT_AVAILABLE),
                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentInformation.getOriginalHireDate())).orElse(NOT_AVAILABLE),
                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentInformation.getMostRecentHireDate())).orElse(NOT_AVAILABLE),
                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentInformation.getMostRecentSeparationDate())).orElse(NOT_AVAILABLE),
                    Optional.ofNullable(CommonUtils.formatTenure(employmentInformation.getPositionTenure())).orElse(NOT_AVAILABLE)
            };

            header2 = new String[]{"Employment Status", "Work Status", "Title"};


        } else if(isPlus){
            EmploymentHistoryPlus employmentHistoryPlus = (EmploymentHistoryPlus) eh;
            PaymentHistoryStandard paymentHistoryStandard = null;
            TotalAnnualRenumerationStandard totalAnnualRenumerationStandard = null;
            List<PaymentHistory> PaymentHistoryList = employmentHistoryPlus.getPaymentHistory();
            List<TotalAnnualRenumeration> totalAnnualRenumerationList = employmentHistoryPlus.getTotalAnnualRemuneration();
            if(!CollectionUtils.isEmpty(PaymentHistoryList)) {
                paymentHistoryStandard = (PaymentHistoryStandard)PaymentHistoryList.get(0);
            }
            if(!CollectionUtils.isEmpty(totalAnnualRenumerationList)) {
                totalAnnualRenumerationStandard = (TotalAnnualRenumerationStandard)totalAnnualRenumerationList.get(0);
            }
            writePlusEmployerDetails(employmentHistoryPlus,paymentHistoryStandard);
            header1 = new String[]{"Original Hire Date", "Most Recent Hire Date", "Employment Status", "Work Status", "Gross Pay", "YTD Gross Pay"};
            row1 = new String[]{Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryPlus.getOriginalHireDate())).orElse(NOT_AVAILABLE),
                    Optional.ofNullable(CommonUtils.getFormattedDate(employmentHistoryPlus.getMostRecentHireDate())).orElse(NOT_AVAILABLE),
                    employmentHistoryPlus.getEmploymentStatus() != null ? Optional.ofNullable(employmentHistoryPlus.getEmploymentStatus().getName()).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    employmentHistoryPlus.getWorkStatus() != null ? Optional.ofNullable(employmentHistoryPlus.getWorkStatus().getName()).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    paymentHistoryStandard.getGrossPayAmount() != null ? Optional.ofNullable("$" + getRoundOffValue(paymentHistoryStandard.getGrossPayAmount().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                    totalAnnualRenumerationStandard != null ? Optional.ofNullable("$" + getRoundOffValue(totalAnnualRenumerationStandard.getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE};
        }
        values.add(row1);
        float x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
        float w = document.getPageSize().getWidth() - 2 * x;
        writeTable(x, w, header1, weight, values);

        if(header2 !=null && isHR) {

            weight= new float[]{0.20f,0.20f,0.17f};
            writeTable(x,w,header2,weight,values1);
        }
    }

    private EmploymentInformation getEmploymentInformation(List<EmploymentInformation> employmentInformationList, List<String[]> values1) {
        EmploymentInformation distinctEmploymentInformation = new EmploymentInformation();

        String[] row2 = null;
        int count = 0;
        for (EmploymentInformation employmentInformation : employmentInformationList) {
            distinctEmploymentInformation=employmentInformation;
            row2 = new String[]{
                    Optional.ofNullable(count ==0 ? employmentInformation.getEmploymentStatus().getName() : "").orElse(NOT_AVAILABLE),
                    Optional.ofNullable(count ==0 ? employmentInformation.getWorkStatus().getName() : "").orElse(NOT_AVAILABLE),
                    Optional.ofNullable(employmentInformation.getPositionTitle()).orElse(NOT_AVAILABLE)
            };
            values1.add(row2);
            count++;
        }
        return distinctEmploymentInformation;
    }

    /**
     * Render second Premium employer table
     *
     * @param eh    Employer history object
     * @param index Current employer index
     * @throws DocumentException
     * @throws NotFitException   If table does not fit in the current page
     */
    private void writePremiumEmployerTable2(EmploymentHistory eh, int index) throws DocumentException, NotFitException {
        if (isVOE == true || isCORE == true || isPlus == true || isHR == true) {
            return;
        }
        EmploymentHistoryEnhanced employmentHistoryEnhanced = (EmploymentHistoryEnhanced) eh;
        List<PaymentHistory> paymentHistory = employmentHistoryEnhanced.getPaymentHistory();


        if (paymentHistory == null) {
            return;
        }
        for (PaymentHistory ph : paymentHistory) {

            PaymentHistoryEnhanced paymentHistoryEnhanced = (PaymentHistoryEnhanced) ph;

            // Details paragraph
            List<String[]> values = new ArrayList<>();
            String[] header;
            float x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R + FIELD_SEPARATOR;
            float w = document.getPageSize().getWidth() - 2 * x;
            if (ph.getStepDone() < 0) {
                writePremiumEmployerDetails(employmentHistoryEnhanced, paymentHistoryEnhanced);
                ph.setStepDone(0);
            }
            // First table
            if (ph.getStepDone() < 1) {
                header = new String[]{"Gross Pay", "Base Pay", "Overtime", "Bonus", "Other", ""};
                values = new ArrayList<>();
                RespPayAmount payAmount = paymentHistoryEnhanced.getPayAmount();

                if (payAmount == null) {
                    values.add(new String[]{JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE,
                            JsonUtil.NULL_VALUE});
                } else {
                    values.add(new String[]{payAmount.getGrossPay() != null ? Optional.ofNullable("$" + getRoundOffValue(payAmount.getGrossPay().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                            payAmount.getBasePay() != null ? Optional.ofNullable("$" + getRoundOffValue(payAmount.getBasePay().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                            payAmount.getOverTimePay() != null ? Optional.ofNullable("$" + getRoundOffValue(payAmount.getOverTimePay().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                            payAmount.getBonusPay() != null ? Optional.ofNullable("$" + getRoundOffValue(payAmount.getBonusPay().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE,
                            payAmount.getOtherPay() != null ? Optional.ofNullable("$" + getRoundOffValue(payAmount.getOtherPay().getAmount())).orElse(NOT_AVAILABLE) : NOT_AVAILABLE});

                }
                writeTable(x, w, header, values);
                ph.setStepDone(1);
            }
            // Third table 1 (inner)
            if (ph.getStepDone() < 2) {
                // its render for the latest employer only
                if (index == 1) {
                    float gap = w / 6;
                    header = new String[]{"Deductions", "Current", "", "", ""};
                    values = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(paymentHistoryEnhanced.getDeductions())) {
                        for (RespDeduction respDeduction : paymentHistoryEnhanced.getDeductions()) {
                            values.add(new String[]{respDeduction.getDeductionCode() != null ? Optional.ofNullable(respDeduction.getDeductionCode().getName()).orElse(NOT_AVAILABLE) : JsonUtil.NULL_VALUE, "$" + getRoundOffValue(respDeduction.getDeductionAmount().getAmount())});
                        }
                    } else {
                        values.add(new String[]{JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE});

                    }
                    writeTable(x + gap, w - gap, header, values);
                }
                ph.setStepDone(2);
            }

            // Fourth table
            if (ph.getStepDone() < 3) {
                getYTDInformation(employmentHistoryEnhanced, ph, x, w);
                ph.setStepDone(3);
            }
            // Payroll title
            if (ph.getStepDone() < 4) {
                writeEmployerPayrollSubTitle();
                ph.setStepDone(4);
            }
            // Fifth table
            if (ph.getStepDone() < 5) {
                header = new String[]{"Account Type", "Account Number", "Routing Number", "Amount", "", ""};
                values = new ArrayList<>();
                List<RespPayDistributions> payDistributions = paymentHistoryEnhanced.getPayDistributions();
                if (!CollectionUtils.isEmpty(payDistributions)) {
                    for (RespPayDistributions payDistribution : payDistributions) {
                        values.add(new String[]{payDistribution.getDepositAccount() != null ? Optional.ofNullable(payDistribution.getDepositAccount().getAccountTypeCode()).orElse(NOT_AVAILABLE) : JsonUtil.NULL_VALUE, payDistribution.getDepositAccount() != null ? Optional.ofNullable(payDistribution.getDepositAccount().getAccountNumber().toUpperCase(Locale.ROOT)).orElse(NOT_AVAILABLE) : JsonUtil.NULL_VALUE, payDistribution.getDepositAccount() != null ? Optional.ofNullable(payDistribution.getDepositAccount().getRoutingTransitID()).orElse(NOT_AVAILABLE) : JsonUtil.NULL_VALUE,
                                payDistribution.getDepositAmount() != null ? "$" + getRoundOffValue(payDistribution.getDepositAmount().getAmount()) : JsonUtil.NULL_VALUE});
                    }
                } else {
                    values.add(new String[]{JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE});

                }

                writeTable(x, w, header, values);
                ph.setStepDone(5);
            }
        }
    }

    private void getYTDInformation(EmploymentHistoryEnhanced employmentHistoryEnhanced, PaymentHistory ph, float x, float w) throws DocumentException, NotFitException {
        String[] header;

        List<String[]> values = new ArrayList<>();

        if (CollectionUtils.isEmpty(employmentHistoryEnhanced.getTotalAnnualRemuneration())) {
            header = new String[]{"YTD Gross Pay", "YTD Base Pay", "YTD Overtime", "YTD Bonus", "YTD Other",
                    ""};
            values.add(new String[]{JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE,
                    JsonUtil.NULL_VALUE, JsonUtil.NULL_VALUE});
            writeTable(x, w, header, values);
        } else {
            String year = null;
            String payDate = ph.getPayDate();
            if(payDate !=null){
                year = StringUtils.right(payDate, 4);
            } else {
                year = StringUtils.right( ph.getPayPeriod().getEndDate(), 4);
            }

            if (!executedYear.contains(year)) {
                header = new String[]{"YTD Gross Pay", "YTD Base Pay", "YTD Overtime", "YTD Bonus", "YTD Other",
                        ""};

                for (TotalAnnualRenumeration totalAnnualRenumeration : employmentHistoryEnhanced.getTotalAnnualRemuneration()) {
                    TotalAnnualRenumerationEnhanced totalAnnualRenumerationEnhanced = (TotalAnnualRenumerationEnhanced) totalAnnualRenumeration;

                    if (year.equals(String.valueOf(totalAnnualRenumeration.getYear()))) {
                        PayAmountYTD payAmountYTD = totalAnnualRenumerationEnhanced.getPayAmountYTD();

                        values.add(new String[]{payAmountYTD.getGrossPayYTD() != null ? "$" + getRoundOffValue(payAmountYTD.getGrossPayYTD().getAmount()) : NOT_AVAILABLE, payAmountYTD.getGrossPayYTD() != null ? "$" + getRoundOffValue(payAmountYTD.getBasePayYTD().getAmount()) : NOT_AVAILABLE, payAmountYTD.getOvertimePayYTD() != null ? "$" + getRoundOffValue(payAmountYTD.getOvertimePayYTD().getAmount()) : NOT_AVAILABLE,
                                payAmountYTD.getBonusPayYTD() != null ? "$" + getRoundOffValue(payAmountYTD.getBonusPayYTD().getAmount()) : NOT_AVAILABLE, payAmountYTD.getOtherPayYTD() != null ? "$" + getRoundOffValue(payAmountYTD.getOtherPayYTD().getAmount()) : NOT_AVAILABLE});
                        executedYear.add(year);
                        writeTable(x, w, header, values);
                    }
                }
            }
        }


    }

    public static String getRoundOffValue(double value) {

        DecimalFormat df = new DecimalFormat("##,##,##,##,##,##,##0.00");
        return df.format(value);
    }

    /**
     * Writes a Premium employer detail
     *
     * @param eh    Employment history object
     * @param i     Current index
     * @param total Total count
     * @throws DocumentException
     * @throws NotFitException
     */
    private void writePremuimEmployer(EmploymentHistory eh, int i, int total)
            throws DocumentException, NotFitException {
        if (eh.getStepDone() < 2) {
            // write subtitle if the employer is not done
            writeEmployerSubTitle(i, total);
        }
        if (eh.getStepDone() < 0) {
            writePremiumEmployerName(eh);
            eh.setStepDone(0);
        }
        if (eh.getStepDone() < 1) {
            writePremiumEmployerTable1(eh);
            eh.setStepDone(1);
        }
        if (eh.getStepDone() < 2) {
            writePremiumEmployerTable2(eh, i);
            eh.setStepDone(2);
        }
    }

    /**
     * Writes the report footer
     *
     * @param expVerifyReport
     * @throws DocumentException
     * @throws NotFitException   If text does not fit in the page
     */
    private void writeReportFooter(IncomeReport expVerifyReport) throws DocumentException, NotFitException {
        if (expVerifyReport.getConsumerPii().getEmploymentHistory() == null) {
            return;
        }
        float x = MARGIN_OUTSIDE_L_R + MARGIN_INSIDE_L_R;
        float urx = document.getPageSize().getWidth() - x;
        if (!expVerifyReport.isFooter()) {
            y -= 15;
            if (y < PAGE_CUT) {
                throw new NotFitException();
            }
            writeText(FONT_8_NORMAL, "* N/A is returned when information is not available in the source data", x, y,
                    x + 500, y + 12);
            expVerifyReport.setFooter(true);
        }
        for (EmploymentHistory eh : expVerifyReport.getConsumerPii().getEmploymentHistory()) {

            if (isVOE == true || isCORE == true) {
                voeFooter(x, urx, eh);
            } else if (isPremium){
                premiumFooter(x, urx, eh);
            }
        }

    }

    private void premiumFooter(float x, float urx, EmploymentHistory eh) throws DocumentException, NotFitException {
        EmploymentHistoryEnhanced employmentHistoryEnhanced = (EmploymentHistoryEnhanced) eh;
        if (employmentHistoryEnhanced.getEmployerDisclaimers() != null && !employmentHistoryEnhanced.getEmployerDisclaimers().isEmpty()) {
            if (!eh.isTitleDisplayed()) {
                y -= 25;
                if (y < PAGE_CUT) {
                    throw new NotFitException();
                }
                writeText(FONT_8_UNDERLINE, returnValueOrNA(eh.getEmployerName()), x, y, x + 300, y + 15, false);
                eh.setTitleDisplayed(true);
            }
            // 1 CT to test, the other to write
            ColumnText ct = new ColumnText(cb);
            ct.setSimpleColumn(x, PAGE_CUT, urx, y, 15, Element.ALIGN_JUSTIFIED);
            ColumnText ct2 = new ColumnText(cb);
            ct2.setSimpleColumn(x, PAGE_CUT, urx, y, 15, Element.ALIGN_JUSTIFIED);
            String text = employmentHistoryEnhanced.getEmployerDisclaimers();
            for (String p : text.split("\n")) {
                ct.addText(new Paragraph(p, FONT_8_NORMAL));
                int r = ct.go(true);
                if (r == ColumnText.NO_MORE_TEXT) {
                    // Add paragraph only if it fit in the page
                    ct2.addText(new Paragraph(p, FONT_8_NORMAL));
                    ct2.go();
                    // Remove paragraph from text
                    String oldText = employmentHistoryEnhanced.getEmployerDisclaimers();
                    String newText = null;
                    if (oldText.length() > p.length()) {
                        newText = oldText.substring(p.length() + 1);
                    } else {
                        newText = null;
                    }
                    if (newText == null) {
                        y = ct.getYLine();
                        break;
                    }
                } else if (r == ColumnText.NO_MORE_COLUMN) {
                    // Paragraph does not fit in this page
                    throw new NotFitException();
                }
            }
        }
    }

    private void voeFooter(float x, float urx, EmploymentHistory eh) throws DocumentException, NotFitException {
        EmploymentHistoryVOE employmentHistoryVOE = (EmploymentHistoryVOE) eh;
        if (employmentHistoryVOE.getEmployerDisclaimers() != null && !employmentHistoryVOE.getEmployerDisclaimers().isEmpty()) {
            if (!eh.isTitleDisplayed()) {
                y -= 25;
                writeText(FONT_8_UNDERLINE, eh.getEmployerName(), x, y, x + 300, y + 12, false);
                eh.setTitleDisplayed(true);
            }
            // 1 CT to test, the other to write
            ColumnText ct = new ColumnText(cb);
            ct.setSimpleColumn(x, PAGE_CUT, urx, y, 15, Element.ALIGN_JUSTIFIED);
            ColumnText ct2 = new ColumnText(cb);
            ct2.setSimpleColumn(x, PAGE_CUT, urx, y, 15, Element.ALIGN_JUSTIFIED);
            String text = employmentHistoryVOE.getEmployerDisclaimers();
            for (String p : text.split("\n")) {
                ct.addText(new Paragraph(p, FONT_8_NORMAL));
                int r = ct.go(true);
                if (r == ColumnText.NO_MORE_TEXT) {
                    // Add paragraph only if it fit in the page
                    ct2.addText(new Paragraph(p, FONT_8_NORMAL));
                    ct2.go();
                    // Remove paragraph from text
                    String oldText = employmentHistoryVOE.getEmployerDisclaimers();
                    String newText = null;
                    if (oldText.length() > p.length()) {
                        newText = oldText.substring(p.length() + 1);
                    } else {
                        newText = null;
                    }
                    employmentHistoryVOE.setEmployerDisclaimers(newText);
                    if (newText == null) {
                        y = ct.getYLine();
                        break;
                    }
                } else if (r == ColumnText.NO_MORE_COLUMN) {
                    // Paragraph does not fit in this page
                    throw new NotFitException();
                }
            }
        }
    }

    /**
     * Write the report header. This include the consumer identity, along with some
     * general report information
     *
     * @param expVerifyReport Data container
     * @throws DocumentException
     */
    private void writeReportHeader(IncomeReport expVerifyReport) throws DocumentException {
        float w = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 0f;
        float x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
        if (expVerifyReport != null) {
            h = 40;
            // Gray background
            cb.saveState();
            cb.setColorFill(COLOR_EXPERIAN_GREY_10);
            cb.rectangle(MARGIN_OUTSIDE_L_R, y - h, document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R, h + 10);
            cb.fill();
            cb.restoreState();
            y -= 15;
            // Text
            if (expVerifyReport.getRequestor() != null && expVerifyReport.getRequestor().getVerifierName() != null) {
                // First line
                y = y + h - 25;
                String dob = null;
                ApplicantInformation applicantInformation1 = expVerifyReport.getConsumerPii().getApplicantInformation();

                ApplicantInformationEnhanced applicantInformation = mapToApplicationInformationEnhanced(applicantInformation1);

                if (applicantInformation != null) {

                    RespName name = applicantInformation.getName();
                    String firstName = StringUtils.capitalize(applicantInformation.getName().getFirstName());
                    String middleName = !StringUtils.isEmpty(name.getMiddleName()) ? " " + name.getMiddleName() + "." : "";
                    String lastName = name.getLastName() != null ? " " + name.getLastName() : "";
                    String generationCodeFormat = name.getGenerationCode() != null ? " " + name.getGenerationCode() : "";

                    w = writeText(FONT_15_NORMAL, firstName + middleName + lastName + generationCodeFormat
                            , x, y, x + 300, y + 23);
                    if (applicantInformation.getDob() != null) {
                        dob = applicantInformation.getDob().getDob();
                    } else {
                        dob = JsonUtil.NULL_VALUE;
                    }
                }
                x += w + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Address:", x, y, x + 50, y + 12);
                x += w + FIELD_SPACE;
                if (expVerifyReport.getConsumerPii().getApplicantInformation().getCurrentAddress() == null) {
                    w = writeText(FONT_8_NORMAL, JsonUtil.NULL_VALUE, x, y, x + 200, y + 12);
                } else {
                    w = writeText(FONT_8_NORMAL, getFullAddress(expVerifyReport.getConsumerPii().getApplicantInformation().getCurrentAddress()), x, y, x + 250,
                            y + 12);
                }
                // Second line
                y -= 20;
                x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Phone:", x, y, x + 50, y + 12);
                x += w + FIELD_SPACE;
                if (expVerifyReport.getConsumerPii().getApplicantInformation().getPhone() == null) {
                    w = writeText(FONT_8_NORMAL, JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
                } else {
                    if (!CollectionUtils.isEmpty(expVerifyReport.getConsumerPii().getApplicantInformation().getPhone())) {
                        String phoneNumber = expVerifyReport.getConsumerPii().getApplicantInformation().getPhone().get(0).getNumber();
                        String phoneFormattedString = !StringUtils.isEmpty(phoneNumber) ? phoneNumber.substring(0, 3) + '-' + phoneNumber.substring(3, 6) + '-' + phoneNumber.substring(6, 10) : JsonUtil.NULL_VALUE;
                        w = writeText(FONT_8_NORMAL, phoneFormattedString, x,
                                y, x + 100, y + 12);
                    } else {
                        w = writeText(FONT_8_NORMAL, JsonUtil.NULL_VALUE, x,
                                y, x + 100, y + 12);
                    }
                }
                x += w + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Social Security Number:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;
                if (expVerifyReport.getConsumerPii().getApplicantInformation().getSsn() == null) {
                    w = writeText(FONT_8_NORMAL, JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
                } else {
                    RespSsn ssn = expVerifyReport.getConsumerPii().getApplicantInformation().getSsn();
                    String ssnFormatted = expVerifyReport.getConsumerPii().getApplicantInformation().getSsn().getSsn();
                    w = writeText(FONT_8_NORMAL, ssnFormatted, x, y, x + 100,
                            y + 12);
                }
                x += w + FIELD_SEPARATOR;
                if(!isHR) {
                    w = writeText(FONT_8_BOLD, "Date of Birth:", x, y, x + 100, y + 12);
                    x += w + FIELD_SPACE;
                    if (dob == null) {
                        w = writeText(FONT_8_NORMAL, JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
                    } else {
                        w = writeText(FONT_8_NORMAL, CommonUtils.getFormattedDate(dob), x, y, x + 100, y + 12);
                    }
                }
            }
            h = 15;
            cb.saveState();
            cb.setColorFill(COLOR_EXPERIAN_GREY_10);
            cb.rectangle(MARGIN_OUTSIDE_L_R, y - h, document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R, h - 5);
            cb.fill();
            cb.restoreState();
            x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
            y -= 10;
            cb.saveState();
            cb.setColorStroke(COLOR_EXPERIAN_GREY_40);
            cb.moveTo(x, y);
            cb.lineTo(document.getPageSize().getWidth() - x, y);
            cb.stroke();
            cb.restoreState();
            // General info
            if (expVerifyReport != null) {
                // First line
                h = 30;
                cb.saveState();
                cb.setColorFill(COLOR_EXPERIAN_GREY_10);
                cb.rectangle(MARGIN_OUTSIDE_L_R, y - h - 5, document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R, h);
                cb.fill();
                cb.restoreState();
                w = 0f;
                x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
                y -= 15;
                w = writeText(FONT_8_BOLD, "Requestor:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;
                w = writeText(FONT_8_NORMAL, expVerifyReport.getRequestor() != null ? returnValueOrNA(expVerifyReport.getRequestor().getVerifierName()) : JsonUtil.NULL_VALUE, x, y, x + 200, y + 12);
                x += w + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Requestor ID:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;
                w = writeText(FONT_8_NORMAL, expVerifyReport.getRequestor() != null ? returnValueOrNA(expVerifyReport.getRequestor().getSubscriberCode()) : JsonUtil.NULL_VALUE, x, y, x + 100, y + 12);
                x += w + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Report ID:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;
                w = writeText(FONT_8_NORMAL, returnValueOrNA(expVerifyReport.getReportId()), x, y, x + 200, y + 12);
                x += w + FIELD_SEPARATOR;
                // Second line
                y -= 15;
                x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Report Type:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;
                w = writeText(FONT_8_NORMAL, returnValueOrNA(expVerifyReport.getReportType()), x, y, x + 200, y + 12);
                x += w + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "Date Requested:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;

                if (expVerifyReport.getReportGeneratedDate() == null) {
                    w = writeText(FONT_8_NORMAL, JsonUtil.NULL_VALUE, x, y, x + 200, y + 12);

                } else {
                    String reportGenerationDate = expVerifyReport.getReportGeneratedDate();
                    if (reportGenerationDate != null) {
                        java.util.Date date = new java.util.Date(Long.parseLong(expVerifyReport.getReportGeneratedDate()) * 1000);
                        SimpleDateFormat df2 = new SimpleDateFormat("MM/dd/YYYY");
                        reportGenerationDate = df2.format(date);
                    } else {
                        reportGenerationDate = JsonUtil.NULL_VALUE;
                    }

                    w = writeText(FONT_8_NORMAL, reportGenerationDate, x, y, x + 200, y + 12);

                }

                x += w + FIELD_SEPARATOR;
                w = writeText(FONT_8_BOLD, "At The Request Of:", x, y, x + 100, y + 12);
                x += w + FIELD_SPACE;
                w = writeText(FONT_8_NORMAL, expVerifyReport.getResellerInfo() != null ? returnValueOrNA(expVerifyReport.getResellerInfo().getEndUserName()) : JsonUtil.NULL_VALUE, x, y, x + 200, y + 12);
                ReportCustomLabels reportCustomLabels = null;
                if (isVOE || isCORE) {
                    IncomeReportVOE expVerifyReport1 = (IncomeReportVOE) expVerifyReport;
                    reportCustomLabels = expVerifyReport1.getReportCustomLabels();

                } else if(isPremium){
                    IncomeReportEnhanced expVerifyReport1 = (IncomeReportEnhanced) expVerifyReport;
                    reportCustomLabels = expVerifyReport1.getReportCustomLabels();

                }
                if (reportCustomLabels == null) {
                    y -= 15;
                }
                else {
                    // line
                    h = 15;
                    cb.saveState();
                    cb.setColorFill(COLOR_EXPERIAN_GREY_10);
                    cb.rectangle(MARGIN_OUTSIDE_L_R, y - h - 5, document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R,
                            h);
                    cb.fill();
                    cb.restoreState();
                    x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
                    y -= 10;
                    cb.saveState();
                    cb.setColorStroke(COLOR_EXPERIAN_GREY_40);
                    cb.moveTo(x, y);
                    cb.lineTo(document.getPageSize().getWidth() - x, y);
                    cb.stroke();
                    cb.restoreState();
                    x = MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR;
                    y -= 15;
                    float maxX = document.getPageSize().getWidth() - 2 * (MARGIN_OUTSIDE_L_R + FIELD_SEPARATOR);
                    float pw = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
                    float[] uv;
                    // Gray background
                    cb.saveState();
                    cb.setColorFill(COLOR_EXPERIAN_GREY_10);
                    cb.rectangle(MARGIN_OUTSIDE_L_R, y - 5, pw, 15);
                    cb.fill();
                    cb.restoreState();

                    if (reportCustomLabels !=null && !CollectionUtils.isEmpty(reportCustomLabels.getLabelMapper())) {
                        Map<String, String> labelMapper = reportCustomLabels.getLabelMapper();
                        TreeMap<String, String> sorted_map = new TreeMap<>(labelMapper);
                        for (Map.Entry<String, String> entry : sorted_map.entrySet()) {

                            uv = testAndPut(FONT_8_BOLD, entry.getKey() + ":", FONT_8_NORMAL, entry.getValue(), x, y, 12, maxX,
                                    MARGIN_OUTSIDE_L_R + 10, true);
                            w = uv[0];
                            if (uv[2] != y) {
                                // one line is needed
                                x = uv[1];
                                y = uv[2];
                            }
                            x += w + FIELD_SEPARATOR;
                        }
                    }
                    y -= 15;
                }
            }
        }
    }

    private ApplicantInformationEnhanced mapToApplicationInformationEnhanced(ApplicantInformation applicantInformation1) {
        ApplicantInformationEnhanced applicantInformationEnhanced = new ApplicantInformationEnhanced();
        applicantInformationEnhanced.setDob(null);
        applicantInformationEnhanced.setName(applicantInformation1.getName());
        applicantInformationEnhanced.setCurrentAddress(applicantInformation1.getCurrentAddress());
        applicantInformationEnhanced.setCurrentAddress(applicantInformation1.getCurrentAddress());
        applicantInformationEnhanced.setPhone(applicantInformation1.getPhone());
        applicantInformationEnhanced.setSsn(applicantInformation1.getSsn());

        return applicantInformationEnhanced;
    }

    private String returnValueOrNA(String value) {
        return value != null ? value : JsonUtil.NULL_VALUE;
    }


    /**
     * Overload to support optional weights
     *
     * @param x
     * @param w
     * @param header
     * @param values
     * @throws DocumentException
     * @throws NotFitException
     */
    private void writeTable(float x, float w, String[] header, List<String[]> values)
            throws DocumentException, NotFitException {
        writeTable(x, w, header, null, values);
    }

    /**
     * Render a table (all columns will have same width)
     *
     * @param x      Leftmost coordinate
     * @param header Table header
     * @param weight percent weight for all columns. Should sum 1.
     * @param values A list for rows to add
     * @throws DocumentException
     * @throws NotFitException   If table does not fit in the current page
     */
    private void writeTable(float x, float width, String[] header, float[] weight, List<String[]> values)
            throws DocumentException, NotFitException {
        // background
        float bx = MARGIN_OUTSIDE_L_R;
        float bw = document.getPageSize().getWidth() - 2 * MARGIN_OUTSIDE_L_R;
        float h = 20 + 15 * (values.size() + 1);
        if ((y - h) < PAGE_CUT) {
            if(!CollectionUtils.isEmpty(executedYear)) {
                int i = executedYear.size() - 1;
                executedYear.remove(i);
            }
            throw new NotFitException();
        }
        // Gray
        cb.saveState();
        cb.setColorFill(COLOR_EXPERIAN_GREY_10);
        cb.rectangle(bx, y - h, bw, h);
        cb.fill();
        // White
        bw -= 2 * MARGIN_INSIDE_L_R;
        bx += MARGIN_INSIDE_L_R;
        cb.setColorFill(COLOR_EXPERIAN_WHITE);
        cb.rectangle(bx, y - h, bw, h);
        cb.fill();
        cb.restoreState();
        // lines
        cb.saveState();
        cb.setColorStroke(COLOR_EXPERIAN_GREY_10);
        cb.moveTo(x, y);
        cb.lineTo(x + width, y);
        cb.stroke();
        cb.moveTo(x, y - 15);
        cb.lineTo(x + width, y - 15);
        cb.stroke();
        cb.restoreState();
        // header
        y -= 15;
        bx = x;
        if (weight == null || weight.length == 0) {
            weight = new float[header.length];
            float space = width / header.length;
            for (int i = 0; i < header.length; i++) {
                weight[i] = space;
            }
        } else {
            for (int i = 0; i < weight.length; i++) {
                weight[i] = weight[i] * width;
            }
        }
        int i = 0;
        for (String s : header) {
            writeText(FONT_8_BLUE, s, bx, y, bx + 200, y + 15);
            bx += weight[i];
            i++;
        }
        float w;
        // values
        for (String[] row : values) {
            y -= 15;
            bx = x;
            i = 0;
            for (String s : row) {
                if ("Title".equals(header[i])) {
                    float m = x + width;
                    if (i < (weight.length - 1)) {
                        m = bx + weight[i + 1];
                    }
                    String backup = s;
                    float by = y;
                    int nlines = 1;
                    do {
                        String line = backup;
                        backup = "";
                        while (true) {
                            w = writeText(FONT_8_NORMAL, line, bx, y, bx + 200, y + 15, true);
                            if (bx + w <= m) {
                                // line fit
                                break;
                            }
                            if (nlines == 3) {
                                // Write rest of title
                                line = line + backup;
                                break;
                            }
                            if (line.lastIndexOf(" ") != -1) {
                                backup = line.substring(line.lastIndexOf(" ")) + backup;
                                line = line.substring(0, line.lastIndexOf(" "));
                            } else {
                                break;
                            }
                        }
                        ;
                        writeText(FONT_8_NORMAL, line, bx, y, width, y + 15);
                        nlines++;
                        y -= 8;
                        if (backup.length() > 1) {
                            backup = backup.substring(1);
                        }
                    } while (!backup.isEmpty());
                    y = by;
                } else {
                    w = writeText(FONT_8_NORMAL, s, bx, y, bx + 200, y + 15);
                }
                bx += weight[i];
                i++;
            }
        }
        y -= 15;
    }
    /**
     * Write a text to the document at specific location using specific font.
     *
     * @param font Font to use
     * @param text Text to write
     * @param llx  Lower left X position
     * @param lly  Lower left Y position
     * @param urx  Upper right X position
     * @param ury  Upper right Y position
     * @return The width of the text, in points
     * @throws DocumentException
     */
    private float writeText(Font font, String text, float llx, float lly, float urx, float ury)
            throws DocumentException {
        return writeText(font, text, llx, lly, urx, ury, false);
    }

    /**
     * Write text with specific font using the rectangle coordinates
     *
     * @param font     Font to use
     * @param text     Text to write
     * @param llx      Lower left X position
     * @param lly      Lower left Y position
     * @param urx      Upper right X position
     * @param ury      Upper right Y position
     * @param simulate {@code true} to simulate output
     * @throws DocumentException
     */
    private float writeText(Font font, String text, float llx, float lly, float urx, float ury, boolean simulate)
            throws DocumentException {
        ColumnText ct = new ColumnText(cb);
        ct.setSimpleColumn(llx, lly, urx, ury, 0, Element.ALIGN_LEFT);
        Paragraph p = new Paragraph(text, font);
        ct.addElement(p);
        ct.go(simulate);
        return ColumnText.getWidth(p);
    }
}
