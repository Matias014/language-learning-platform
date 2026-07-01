package com.languageschool.backend.service.export;

import com.languageschool.backend.dto.adminStats.HardestExerciseDto;
import com.languageschool.backend.dto.adminStats.LlmStatsDto;
import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LlmStatus;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.AdminStatsExportService;
import com.languageschool.backend.service.AdminStatsService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminStatsExportServiceImpl implements AdminStatsExportService {

    private final AdminStatsService stats;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    public AdminStatsExportServiceImpl(AdminStatsService stats) {
        this.stats = stats;
    }

    @Override
    public byte[] buildLlmStatsCsv(Locale locale) {
        LlmStatsDto s = stats.getLlmStats();
        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("calls,").append(csvCell(Optional.ofNullable(s.getCalls()).orElse(0L))).append("\n");
        sb.append("tokensIn,").append(csvCell(Optional.ofNullable(s.getTokensIn()).orElse(0L))).append("\n");
        sb.append("tokensOut,").append(csvCell(Optional.ofNullable(s.getTokensOut()).orElse(0L))).append("\n");
        sb.append("averageLatencyMs,").append(csvCell(s.getAverageLatencyMs() == null ? "" : s.getAverageLatencyMs())).append("\n");
        sb.append("\n");
        sb.append("interactionType,calls\n");
        Map<InteractionType, Long> byType = s.getCallsByInteractionType();
        if (byType != null) {
            for (Map.Entry<InteractionType, Long> e : byType.entrySet()) {
                InteractionType k = e.getKey();
                Long v = e.getValue();
                sb.append(csvCell(k == null ? "" : k.name().toLowerCase(Locale.ROOT)))
                        .append(",")
                        .append(csvCell(v == null ? 0 : v))
                        .append("\n");
            }
        }
        sb.append("\n");
        sb.append("status,calls\n");
        Map<LlmStatus, Long> byStatus = s.getCallsByStatus();
        if (byStatus != null) {
            for (Map.Entry<LlmStatus, Long> e : byStatus.entrySet()) {
                LlmStatus k = e.getKey();
                Long v = e.getValue();
                sb.append(csvCell(k == null ? "" : k.name().toLowerCase(Locale.ROOT)))
                        .append(",")
                        .append(csvCell(v == null ? 0 : v))
                        .append("\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] buildHardestExercisesCsv(int limit, Locale locale) {
        var rows = stats.getHardestExercises(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("exerciseId,averageAccuracy,averageDurationSeconds\n");
        for (HardestExerciseDto r : rows) {
            Long id = r.getExerciseId();
            BigDecimal acc = r.getAverageAccuracy();
            Integer dur = r.getAverageDurationSeconds();
            sb.append(csvCell(id == null ? "" : id)).append(",");
            String accPct = acc == null ? "" : acc.multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .toPlainString();
            sb.append(csvCell(accPct)).append(",");
            sb.append(csvCell(dur == null ? "" : dur)).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] buildLlmStatsPdf(Locale locale) {
        LlmStatsDto s = stats.getLlmStats();

        boolean pl = locale != null && "pl".equalsIgnoreCase(locale.getLanguage());
        Locale loc = locale == null ? Locale.ENGLISH : locale;
        ZoneId zone = ZONE;
        NumberFormat nf = NumberFormat.getIntegerInstance(loc);
        Instant generatedAt = Instant.now();

        String title = pl ? "Raport LLM (admin)" : "LLM report (admin)";
        String sectionSummary = pl ? "Podsumowanie" : "Summary";
        String sectionByType = pl ? "Wywołania wg typu" : "Calls by type";
        String sectionByStatus = pl ? "Wywołania wg statusu" : "Calls by status";

        String kCalls = pl ? "Liczba wywołań" : "Total calls";
        String kTokensIn = pl ? "Tokeny we" : "Tokens in";
        String kTokensOut = pl ? "Tokeny wy" : "Tokens out";
        String kLatency = pl ? "Średnia latencja" : "Average latency";

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Fonts fonts = loadFonts(doc);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            info.setAuthor("LanguageSchool");
            info.setSubject("Admin LLM stats report");
            info.setCreationDate(java.util.GregorianCalendar.from(generatedAt.atZone(zone)));
            doc.setDocumentInformation(info);

            PageWriter pw = new PageWriter(
                    doc,
                    fonts,
                    PDRectangle.A4,
                    44f,
                    44f,
                    44f,
                    54f,
                    pl,
                    zone,
                    loc,
                    generatedAt,
                    pl ? "Raport administracyjny" : "Admin report"
            );

            pw.coverTitle(title, pl ? "Panel: Administracja" : "Panel: Admin");
            pw.sectionHeader(sectionSummary);

            List<KV> summary = new ArrayList<>();
            summary.add(new KV(kCalls, nf.format(Optional.ofNullable(s.getCalls()).orElse(0L))));
            summary.add(new KV(kTokensIn, nf.format(Optional.ofNullable(s.getTokensIn()).orElse(0L))));
            summary.add(new KV(kTokensOut, nf.format(Optional.ofNullable(s.getTokensOut()).orElse(0L))));
            String lat = s.getAverageLatencyMs() == null ? "—" : nf.format(s.getAverageLatencyMs()) + " ms";
            summary.add(new KV(kLatency, lat));
            pw.kvGrid(summary, 2);

            pw.sectionHeader(sectionByType);
            List<String> byTypeLines = new ArrayList<>();
            Map<InteractionType, Long> byType = s.getCallsByInteractionType();
            if (byType != null && !byType.isEmpty()) {
                List<Map.Entry<InteractionType, Long>> entries = new ArrayList<>(byType.entrySet());
                entries.sort(Comparator.comparing(e -> e.getKey() == null ? "" : e.getKey().name()));
                for (var e : entries) {
                    String k = e.getKey() == null ? "—" : e.getKey().name().toLowerCase(Locale.ROOT);
                    byTypeLines.add(k + ": " + nf.format(Optional.ofNullable(e.getValue()).orElse(0L)));
                }
            } else {
                byTypeLines.add(pl ? "brak danych" : "no data");
            }
            pw.bulletList(byTypeLines);

            pw.sectionHeader(sectionByStatus);
            List<String> byStatusLines = new ArrayList<>();
            Map<LlmStatus, Long> byStatus = s.getCallsByStatus();
            if (byStatus != null && !byStatus.isEmpty()) {
                List<Map.Entry<LlmStatus, Long>> entries = new ArrayList<>(byStatus.entrySet());
                entries.sort(Comparator.comparing(e -> e.getKey() == null ? "" : e.getKey().name()));
                for (var e : entries) {
                    String k = e.getKey() == null ? "—" : e.getKey().name().toLowerCase(Locale.ROOT);
                    byStatusLines.add(k + ": " + nf.format(Optional.ofNullable(e.getValue()).orElse(0L)));
                }
            } else {
                byStatusLines.add(pl ? "brak danych" : "no data");
            }
            pw.bulletList(byStatusLines);

            pw.close();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.serviceUnavailable();
        }
    }

    @Override
    public byte[] buildHardestExercisesPdf(int limit, Locale locale) {
        var rows = stats.getHardestExercises(limit);

        boolean pl = locale != null && "pl".equalsIgnoreCase(locale.getLanguage());
        Locale loc = locale == null ? Locale.ENGLISH : locale;
        ZoneId zone = ZONE;
        NumberFormat nf = NumberFormat.getIntegerInstance(loc);
        Instant generatedAt = Instant.now();

        String title = pl ? "Najtrudniejsze ćwiczenia (admin)" : "Hardest exercises (admin)";
        String section = pl ? "Ranking" : "Ranking";

        String kExerciseId = pl ? "ID ćwiczenia" : "Exercise ID";
        String kAvgAccuracy = pl ? "Śr. trafność" : "Avg accuracy";
        String kAvgTime = pl ? "Śr. czas" : "Avg time";

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Fonts fonts = loadFonts(doc);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            info.setAuthor("LanguageSchool");
            info.setSubject("Admin hardest exercises report");
            info.setCreationDate(java.util.GregorianCalendar.from(generatedAt.atZone(zone)));
            doc.setDocumentInformation(info);

            PageWriter pw = new PageWriter(
                    doc,
                    fonts,
                    PDRectangle.A4,
                    44f,
                    44f,
                    44f,
                    54f,
                    pl,
                    zone,
                    loc,
                    generatedAt,
                    pl ? "Raport administracyjny" : "Admin report"
            );

            pw.coverTitle(title, (pl ? "Limit: " : "Limit: ") + limit);
            pw.sectionHeader(section);

            if (rows == null || rows.isEmpty()) {
                pw.paragraph(pl ? "brak danych" : "no data");
                pw.close();
                doc.save(out);
                return out.toByteArray();
            }

            List<KV> cols = new ArrayList<>();
            cols.add(new KV(pl ? "Kolumny" : "Columns", kExerciseId + " • " + kAvgAccuracy + " • " + kAvgTime));
            pw.kvGrid(cols, 1);

            List<String> items = new ArrayList<>();
            for (HardestExerciseDto r : rows) {
                String id = r.getExerciseId() == null ? "—" : r.getExerciseId().toString();
                String acc = r.getAverageAccuracy() == null
                        ? "—"
                        : r.getAverageAccuracy()
                        .multiply(new BigDecimal("100"))
                        .setScale(0, RoundingMode.HALF_UP)
                        .toPlainString() + "%";
                String dur = formatDuration(r.getAverageDurationSeconds());
                items.add(id + " — " + acc + " — " + dur);
            }
            pw.bulletList(items);

            pw.close();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.serviceUnavailable();
        }
    }

    private static String csvCell(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!quote) {
            return s;
        }
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private record Fonts(PDFont regular, PDFont bold) {
    }

    private record KV(String label, String value) {
    }

    private static Fonts loadFonts(PDDocument doc) throws IOException {
        PDFont regular = tryLoad(doc, "fonts/NotoSans-Regular.ttf");
        PDFont bold = tryLoad(doc, "fonts/NotoSans-Bold.ttf");
        if (regular == null) {
            regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        if (bold == null) {
            bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        }
        return new Fonts(regular, bold);
    }

    private static PDFont tryLoad(PDDocument doc, String path) throws IOException {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            return null;
        }
        try (InputStream in = res.getInputStream()) {
            return PDType0Font.load(doc, in, true);
        }
    }

    private static String formatDuration(Integer sec) {
        if (sec == null) return "—";
        int s = Math.max(0, sec);
        int m = s / 60;
        int r = s % 60;
        return String.format("%d:%02d", m, r);
    }

    private static String formatInstant(Instant instant, ZoneId zone, Locale locale) {
        if (instant == null) {
            return "—";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale == null ? Locale.ENGLISH : locale)
                .withZone(zone);
        return fmt.format(instant);
    }

    private static final class PageWriter {
        private final PDDocument doc;
        private final Fonts fonts;

        private final PDRectangle pageSize;
        private final float marginLeft;
        private final float marginRight;
        private final float marginTop;
        private final float marginBottom;

        private final boolean pl;
        private final ZoneId zone;
        private final Locale locale;
        private final Instant generatedAt;
        private final String footerLeftLabel;

        private final float titleSize = 22f;
        private final float h1Size = 12.5f;
        private final float labelSize = 9.3f;
        private final float bodySize = 11f;

        private final float leading = 14.5f;
        private final float paragraphGap = 8f;

        private final float gutter = 10f;
        private final float boxPadding = 8f;

        private final Color sectionBg = new Color(245, 247, 250);
        private final Color sectionBorder = new Color(225, 230, 237);
        private final Color boxBg = new Color(250, 251, 252);
        private final Color boxBorder = new Color(223, 228, 235);
        private final Color textMuted = new Color(90, 96, 104);

        private PDPage page;
        private PDPageContentStream cs;
        private float y;
        private float contentWidth;
        private int pageNo = 0;

        private PageWriter(PDDocument doc,
                           Fonts fonts,
                           PDRectangle pageSize,
                           float marginLeft,
                           float marginRight,
                           float marginTop,
                           float marginBottom,
                           boolean pl,
                           ZoneId zone,
                           Locale locale,
                           Instant generatedAt,
                           String footerLeftLabel) throws IOException {
            this.doc = doc;
            this.fonts = fonts;
            this.pageSize = pageSize;
            this.marginLeft = marginLeft;
            this.marginRight = marginRight;
            this.marginTop = marginTop;
            this.marginBottom = marginBottom;
            this.pl = pl;
            this.zone = zone;
            this.locale = locale;
            this.generatedAt = generatedAt;
            this.footerLeftLabel = footerLeftLabel == null ? "" : footerLeftLabel;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) {
                drawFooter();
                cs.close();
            }
            page = new PDPage(pageSize);
            doc.addPage(page);
            pageNo++;
            cs = new PDPageContentStream(doc, page);
            cs.setLineWidth(0.6f);
            y = page.getMediaBox().getHeight() - marginTop;
            contentWidth = page.getMediaBox().getWidth() - marginLeft - marginRight;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < marginBottom) {
                newPage();
            }
        }

        private void coverTitle(String title, String subtitleRight) throws IOException {
            ensureSpace(92f);

            String subtitleLeft = pl ? "Panel: " : "Panel: ";
            String subtitle = subtitleLeft + (subtitleRight == null || subtitleRight.isBlank() ? "—" : subtitleRight);

            float topH = 64f;
            float x = marginLeft;
            float w = contentWidth;

            cs.setNonStrokingColor(new Color(18, 42, 66));
            cs.addRect(x, y - topH, w, topH);
            cs.fill();

            cs.setNonStrokingColor(Color.WHITE);
            writeText(fonts.bold, titleSize, x + 16f, y - 30f, title);

            cs.setNonStrokingColor(new Color(210, 218, 226));
            writeText(fonts.regular, bodySize, x + 16f, y - 50f, subtitle);

            String gen = (pl ? "Wygenerowano: " : "Generated: ") + formatInstant(generatedAt, zone, locale);
            float genW = measure(gen, fonts.regular, labelSize);
            cs.setNonStrokingColor(new Color(210, 218, 226));
            writeText(fonts.regular, labelSize, x + w - 16f - genW, y - 50f, gen);

            cs.setNonStrokingColor(Color.BLACK);
            y -= topH + 18f;
        }

        private void sectionHeader(String text) throws IOException {
            float h = 26f;
            ensureSpace(h + 10f);

            float x = marginLeft;
            float w = contentWidth;

            cs.setNonStrokingColor(sectionBg);
            cs.addRect(x, y - h, w, h);
            cs.fill();

            cs.setStrokingColor(sectionBorder);
            cs.addRect(x, y - h, w, h);
            cs.stroke();

            cs.setNonStrokingColor(Color.BLACK);
            writeText(fonts.bold, h1Size, x + 10f, y - 17.5f, text);

            y -= h + 10f;
        }

        private void subtleLabel(String text) throws IOException {
            ensureSpace(18f);
            cs.setNonStrokingColor(textMuted);
            writeText(fonts.bold, labelSize, marginLeft + 2f, y - 10.5f, text);
            cs.setNonStrokingColor(Color.BLACK);
            y -= 16f;
        }

        private void paragraph(String text) throws IOException {
            List<String> lines = wrap(text == null ? "" : text, fonts.regular, bodySize, contentWidth);
            for (String line : lines) {
                ensureSpace(leading);
                cs.setNonStrokingColor(Color.BLACK);
                writeText(fonts.regular, bodySize, marginLeft, y - bodySize, line);
                y -= leading;
            }
            y -= paragraphGap;
        }

        private void bulletList(List<String> items) throws IOException {
            if (items == null || items.isEmpty()) {
                return;
            }
            float bulletX = marginLeft;
            float textX = marginLeft + 14f;
            float maxW = contentWidth - 14f;

            for (String item : items) {
                String t = item == null ? "" : item;
                List<String> lines = wrap(t, fonts.regular, bodySize, maxW);

                if (lines.isEmpty()) {
                    ensureSpace(leading);
                    writeText(fonts.regular, bodySize, bulletX, y - bodySize, "•");
                    y -= leading;
                    continue;
                }

                ensureSpace(leading);
                writeText(fonts.regular, bodySize, bulletX, y - bodySize, "•");
                writeText(fonts.regular, bodySize, textX, y - bodySize, lines.get(0));
                y -= leading;

                for (int i = 1; i < lines.size(); i++) {
                    ensureSpace(leading);
                    writeText(fonts.regular, bodySize, textX, y - bodySize, lines.get(i));
                    y -= leading;
                }

                y -= 4f;
            }
            y -= paragraphGap;
        }

        private void kvGrid(List<KV> kvs, int columns) throws IOException {
            if (kvs == null || kvs.isEmpty()) {
                return;
            }
            int cols = Math.max(1, columns);
            float colW = (contentWidth - (cols - 1) * gutter) / cols;

            int i = 0;
            while (i < kvs.size()) {
                int rowEnd = Math.min(kvs.size(), i + cols);
                List<KV> row = kvs.subList(i, rowEnd);

                List<CellLayout> layouts = new ArrayList<>();
                float rowH = 0f;

                for (int c = 0; c < row.size(); c++) {
                    KV kv = row.get(c);
                    String label = kv.label() == null ? "" : kv.label();
                    String value = kv.value() == null ? "" : kv.value();

                    List<String> vLines = wrap(value, fonts.regular, bodySize, colW - 2 * boxPadding);
                    float labelH = labelSize + 2f;
                    float valueH = Math.max(1, vLines.size()) * leading;
                    float h = boxPadding + labelH + 4f + valueH + boxPadding;

                    layouts.add(new CellLayout(label, vLines, h));
                    rowH = Math.max(rowH, h);
                }

                ensureSpace(rowH + 4f);

                for (int c = 0; c < row.size(); c++) {
                    float x = marginLeft + c * (colW + gutter);
                    drawBox(x, y, colW, rowH);

                    CellLayout lay = layouts.get(c);

                    float tx = x + boxPadding;
                    float tyTop = y - boxPadding;

                    cs.setNonStrokingColor(textMuted);
                    writeText(fonts.bold, labelSize, tx, tyTop - labelSize, lay.label);

                    cs.setNonStrokingColor(Color.BLACK);
                    float vy = tyTop - (labelSize + 6f);
                    for (int li = 0; li < lay.valueLines.size(); li++) {
                        String line = lay.valueLines.get(li);
                        writeText(fonts.regular, bodySize, tx, vy - bodySize, line);
                        vy -= leading;
                    }
                }

                y -= rowH + 8f;
                i += cols;
            }

            y -= 6f;
        }

        private void drawBox(float x, float yTop, float w, float h) throws IOException {
            cs.setNonStrokingColor(boxBg);
            cs.addRect(x, yTop - h, w, h);
            cs.fill();

            cs.setStrokingColor(boxBorder);
            cs.addRect(x, yTop - h, w, h);
            cs.stroke();

            cs.setNonStrokingColor(Color.BLACK);
            cs.setStrokingColor(Color.BLACK);
        }

        private void drawFooter() throws IOException {
            String left = footerLeftLabel.isBlank() ? (pl ? "Raport" : "Report") : footerLeftLabel;
            String right = (pl ? "Strona " : "Page ") + pageNo;
            String gen = (pl ? "Wygenerowano: " : "Generated: ") + formatInstant(generatedAt, zone, locale);

            float footerY = marginBottom - 28f;
            float lineY = marginBottom - 14f;

            cs.setStrokingColor(new Color(230, 234, 239));
            cs.moveTo(marginLeft, lineY);
            cs.lineTo(marginLeft + contentWidth, lineY);
            cs.stroke();

            cs.setNonStrokingColor(textMuted);
            writeText(fonts.regular, labelSize, marginLeft, footerY, left);

            float genW = measure(gen, fonts.regular, labelSize);
            writeText(fonts.regular, labelSize, marginLeft + contentWidth - genW, footerY, gen);

            float rightW = measure(right, fonts.regular, labelSize);
            writeText(fonts.regular, labelSize, marginLeft + contentWidth - rightW, footerY + 12f, right);

            cs.setNonStrokingColor(Color.BLACK);
        }

        private float measure(String s, PDFont font, float size) throws IOException {
            String t = s == null ? "" : s;
            try {
                return font.getStringWidth(t) / 1000f * size;
            } catch (Exception ex) {
                String clean = t.replaceAll("[^\\p{ASCII}]", "?");
                return font.getStringWidth(clean) / 1000f * size;
            }
        }

        private void writeText(PDFont font, float size, float x, float y, String text) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            showTextSafe(text);
            cs.endText();
        }

        private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
            String t = text == null ? "" : text;
            if (t.isBlank()) {
                return List.of("");
            }

            List<String> out = new ArrayList<>();
            String[] words = t.split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String w : words) {
                if (w.isEmpty()) {
                    continue;
                }
                String candidate = line.length() == 0 ? w : line + " " + w;
                if (measure(candidate, font, size) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }

                if (line.length() > 0) {
                    out.add(line.toString());
                }

                if (measure(w, font, size) <= maxWidth) {
                    line.setLength(0);
                    line.append(w);
                } else {
                    out.addAll(hardWrap(w, font, size, maxWidth));
                    line.setLength(0);
                }
            }

            if (line.length() > 0) {
                out.add(line.toString());
            }

            return out;
        }

        private List<String> hardWrap(String word, PDFont font, float size, float maxWidth) throws IOException {
            List<String> out = new ArrayList<>();
            String w = word == null ? "" : word;
            if (w.isEmpty()) {
                out.add("");
                return out;
            }

            StringBuilder chunk = new StringBuilder();
            for (int i = 0; i < w.length(); i++) {
                char ch = w.charAt(i);
                String cand = chunk.toString() + ch;
                if (measure(cand, font, size) <= maxWidth || chunk.length() == 0) {
                    chunk.append(ch);
                } else {
                    out.add(chunk.toString());
                    chunk.setLength(0);
                    chunk.append(ch);
                }
            }
            if (chunk.length() > 0) {
                out.add(chunk.toString());
            }
            return out;
        }

        private void showTextSafe(String text) throws IOException {
            try {
                cs.showText(text == null ? "" : text);
            } catch (IllegalArgumentException ex) {
                String ascii = (text == null ? "" : text).replaceAll("[^\\p{ASCII}]", "?");
                cs.showText(ascii);
            }
        }

        private void close() throws IOException {
            if (cs != null) {
                drawFooter();
                cs.close();
                cs = null;
            }
        }

        private record CellLayout(String label, List<String> valueLines, float height) {
        }
    }
}
