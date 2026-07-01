package com.languageschool.backend.service.export;

import com.languageschool.backend.dto.achievement.AchievementDto;
import com.languageschool.backend.dto.courseEnrollment.CourseEnrollmentDto;
import com.languageschool.backend.dto.exerciseAttempt.ExerciseAttemptDto;
import com.languageschool.backend.dto.exerciseAward.ExerciseAwardDto;
import com.languageschool.backend.dto.llmLog.LlmLogDto;
import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.dto.userAchievement.UserAchievementDto;
import com.languageschool.backend.dto.userLessonProgress.UserLessonProgressDto;
import com.languageschool.backend.entity.CourseStatus;
import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LessonStatus;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.AchievementService;
import com.languageschool.backend.service.CourseEnrollmentService;
import com.languageschool.backend.service.ExerciseAwardService;
import com.languageschool.backend.service.ExerciseAttemptService;
import com.languageschool.backend.service.LlmLogService;
import com.languageschool.backend.service.PdfReportExportService;
import com.languageschool.backend.service.UserAchievementService;
import com.languageschool.backend.service.UserLessonProgressService;
import com.languageschool.backend.service.UserLevelService;
import com.languageschool.backend.service.UserService;
import com.languageschool.backend.util.SecurityUtils;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PdfReportExportServiceImpl implements PdfReportExportService {

    private final UserService userService;
    private final UserAchievementService userAchievementService;
    private final AchievementService achievementService;
    private final CourseEnrollmentService enrollmentService;
    private final UserLessonProgressService lessonProgressService;
    private final ExerciseAttemptService attemptService;
    private final ExerciseAwardService awardService;
    private final LlmLogService llmLogService;
    private final UserLevelService userLevelService;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    public PdfReportExportServiceImpl(UserService userService,
                                      UserAchievementService userAchievementService,
                                      AchievementService achievementService,
                                      CourseEnrollmentService enrollmentService,
                                      UserLessonProgressService lessonProgressService,
                                      ExerciseAttemptService attemptService,
                                      ExerciseAwardService awardService,
                                      LlmLogService llmLogService,
                                      UserLevelService userLevelService) {
        this.userService = userService;
        this.userAchievementService = userAchievementService;
        this.achievementService = achievementService;
        this.enrollmentService = enrollmentService;
        this.lessonProgressService = lessonProgressService;
        this.attemptService = attemptService;
        this.awardService = awardService;
        this.llmLogService = llmLogService;
        this.userLevelService = userLevelService;
    }

    @Override
    public byte[] buildUserReportForLogin(String login, Locale locale) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, login);

        UserDto user = userService.findByLogin(login).orElseThrow(ApiException::notFound);
        List<UserAchievementDto> achievements = userAchievementService.findByUser(user.getId()).stream()
                .sorted(Comparator.comparing(UserAchievementDto::getEarnedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<CourseEnrollmentDto> enrollments = enrollmentService.findByUser(user.getId());
        List<UserLessonProgressDto> progresses = lessonProgressService.findByUser(user.getId());
        List<ExerciseAttemptDto> attempts = attemptService.findByUser(user.getId());
        List<ExerciseAwardDto> awards = awardService.findByUser(user.getId());
        List<LlmLogDto> logs = llmLogService.findByUser(user.getId());

        Map<Long, String> achievementTitles = achievementService.findAll().stream()
                .collect(Collectors.toMap(AchievementDto::getId, AchievementDto::getTitle, (a, b) -> a, HashMap::new));

        boolean pl = locale != null && "pl".equalsIgnoreCase(locale.getLanguage());
        Locale loc = locale == null ? Locale.ENGLISH : locale;
        ZoneId zone = ZONE;
        NumberFormat nf = NumberFormat.getIntegerInstance(loc);

        String title = pl ? "Raport użytkownika" : "User report";
        String sectionUser = pl ? "Użytkownik" : "User";
        String sectionStats = pl ? "Statystyki" : "Statistics";
        String sectionActivity = pl ? "Aktywność" : "Activity";
        String sectionProgress = pl ? "Postępy" : "Progress";
        String sectionAchievements = pl ? "Osiągnięcia" : "Achievements";

        String fLogin = "Login";
        String fEmail = "Email";
        String fName = pl ? "Imię" : "Name";
        String fSurname = pl ? "Nazwisko" : "Surname";
        String fRole = pl ? "Rola" : "Role";
        String fLevel = pl ? "Poziom" : "UserLevel";
        String fXp = pl ? "Suma XP" : "Total XP";
        String fCreated = pl ? "Konto utworzono" : "Account created";
        String fLastLogin = pl ? "Ostatnie logowanie" : "Last login";
        String none = pl ? "brak" : "none";

        long enrollTotal = enrollments.size();
        long enrollInProgress = enrollments.stream().filter(e -> e.getStatus() == CourseStatus.in_progress).count();
        long enrollCompleted = enrollments.stream().filter(e -> e.getStatus() == CourseStatus.completed).count();
        Instant enrollLast = enrollments.stream().map(CourseEnrollmentDto::getLastActivityAt).filter(i -> i != null).max(Instant::compareTo).orElse(null);

        int lessonsTracked = progresses.size();
        long lessonsCompleted = progresses.stream().filter(p -> p.getStatus() == LessonStatus.completed).count();
        Instant lessonsLast = progresses.stream().map(UserLessonProgressDto::getLastActivityAt).filter(i -> i != null).max(Instant::compareTo).orElse(null);

        int attemptsTotal = attempts.size();
        long attemptsCorrect = attempts.stream().filter(a -> Boolean.TRUE.equals(a.isCorrect())).count();
        BigDecimal avgScore = averageScore(attempts);
        int totalDurationSec = attempts.stream().map(a -> Optional.ofNullable(a.getDurationSeconds()).orElse(0)).reduce(0, Integer::sum);
        Instant attemptsLast = attempts.stream().map(ExerciseAttemptDto::getSubmittedAt).filter(i -> i != null).max(Instant::compareTo).orElse(null);

        int awardsCount = awards.size();
        int awardsTotalXp = awards.stream().map(a -> Optional.ofNullable(a.getAwardedXp()).orElse(0)).reduce(0, Integer::sum);
        Instant awardLast = awards.stream().map(ExerciseAwardDto::getAwardedAt).filter(i -> i != null).max(Instant::compareTo).orElse(null);

        int logsTotal = logs.size();
        Map<InteractionType, Long> logsByType = logs.stream().collect(Collectors.groupingBy(LlmLogDto::getInteractionType, Collectors.counting()));
        Instant logsLast = logs.stream().map(LlmLogDto::getCreatedAt).filter(i -> i != null).max(Instant::compareTo).orElse(null);

        Instant lastActivity = maxInstant(enrollLast, lessonsLast, attemptsLast, awardLast, logsLast);

        int totalXp = Optional.ofNullable(user.getTotalXp()).orElse(0);
        int level = Optional.ofNullable(userLevelService.resolveLevelForXp(totalXp)).orElse(1);

        Instant generatedAt = Instant.now();

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Fonts fonts = loadFonts(doc);

            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            info.setAuthor("LanguageSchool");
            info.setSubject("User report");
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
                    generatedAt
            );

            pw.coverTitle(title, safe(user.getLogin()));
            pw.sectionHeader(sectionUser);

            List<KV> userKvs = new ArrayList<>();
            userKvs.add(new KV(fLogin, safe(user.getLogin())));
            userKvs.add(new KV(fEmail, safe(user.getEmail())));
            userKvs.add(new KV(fName, safe(user.getName())));
            userKvs.add(new KV(fSurname, safe(user.getSurname())));
            userKvs.add(new KV(fRole, safe(String.valueOf(user.getRole()))));
            userKvs.add(new KV(fLevel, nf.format(level)));
            userKvs.add(new KV(fXp, nf.format(totalXp)));
            userKvs.add(new KV(fCreated, formatInstant(user.getCreatedAt(), zone, loc)));
            userKvs.add(new KV(fLastLogin, formatInstant(user.getLastLoginAt(), zone, loc)));
            pw.kvGrid(userKvs, 2);

            pw.sectionHeader(sectionStats);
            List<KV> stats = new ArrayList<>();
            stats.add(new KV(pl ? "Łącznie osiągnięć" : "Total achievements", nf.format(achievements.size())));
            stats.add(new KV(pl ? "Ostatnia aktywność" : "Last activity", formatInstant(lastActivity, zone, loc)));
            pw.kvGrid(stats, 2);

            pw.sectionHeader(sectionActivity);
            List<KV> activity = new ArrayList<>();
            activity.add(new KV(pl ? "Zapisy na kursy" : "Course enrollments", nf.format(enrollTotal)));
            activity.add(new KV(pl ? "Zapisy w trakcie" : "Enrollments in progress", nf.format(enrollInProgress)));
            activity.add(new KV(pl ? "Zapisy ukończone" : "Enrollments completed", nf.format(enrollCompleted)));
            activity.add(new KV(pl ? "Postępy w lekcjach (śledzone)" : "Lesson progress tracked", nf.format(lessonsTracked)));
            activity.add(new KV(pl ? "Lekcje ukończone" : "Lessons completed", nf.format(lessonsCompleted)));
            activity.add(new KV(pl ? "Próby ćwiczeń" : "Exercise attempts", nf.format(attemptsTotal)));
            activity.add(new KV(pl ? "Poprawne" : "Correct", nf.format(attemptsCorrect)));
            activity.add(new KV(pl ? "Średni wynik" : "Avg score", formatScore(avgScore)));
            activity.add(new KV(pl ? "Łączny czas rozwiązywania" : "Total solving time", formatDurationMinutes(totalDurationSec)));
            activity.add(new KV(pl ? "Nagrody XP (liczba)" : "XP awards (count)", nf.format(awardsCount)));
            activity.add(new KV(pl ? "Nagrody XP (suma)" : "XP awards (total)", nf.format(awardsTotalXp)));
            activity.add(new KV(pl ? "Interakcje z asystentem" : "Assistant interactions", nf.format(logsTotal)));
            pw.kvGrid(activity, 3);

            List<String> breakdownLines = logsBreakdownLines(logsByType, pl);
            if (!breakdownLines.isEmpty()) {
                pw.subtleLabel(pl ? "Rozkład interakcji" : "Interaction breakdown");
                pw.bulletList(breakdownLines);
            }

            pw.sectionHeader(sectionProgress);
            List<KV> progress = new ArrayList<>();
            progress.add(new KV(pl ? "Ostatnia aktywność w zapisach" : "Last enrollment activity", formatInstant(enrollLast, zone, loc)));
            progress.add(new KV(pl ? "Ostatnia aktywność w lekcjach" : "Last lesson activity", formatInstant(lessonsLast, zone, loc)));
            progress.add(new KV(pl ? "Ostatnia próba ćwiczenia" : "Last exercise attempt", formatInstant(attemptsLast, zone, loc)));
            progress.add(new KV(pl ? "Ostatnia nagroda XP" : "Last XP award", formatInstant(awardLast, zone, loc)));
            progress.add(new KV(pl ? "Ostatnia interakcja z AI" : "Last AI interaction", formatInstant(logsLast, zone, loc)));
            pw.kvGrid(progress, 2);

            pw.sectionHeader(sectionAchievements);
            if (achievements.isEmpty()) {
                pw.paragraph(none);
            } else {
                List<String> achItems = new ArrayList<>();
                for (UserAchievementDto a : achievements) {
                    String titleTxt = achievementTitles.getOrDefault(a.getAchievementId(), "#" + a.getAchievementId());
                    String whenTxt = formatInstant(a.getEarnedAt(), zone, loc);
                    achItems.add(safe(titleTxt) + " — " + whenTxt);
                }
                pw.bulletList(achItems);
            }

            pw.close();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.serviceUnavailable();
        }
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

    private static String safe(String s) {
        return s == null ? "" : s;
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

    private static BigDecimal averageScore(List<ExerciseAttemptDto> attempts) {
        List<BigDecimal> xs = attempts.stream().map(ExerciseAttemptDto::getScore).filter(x -> x != null).toList();
        if (xs.isEmpty()) {
            return null;
        }
        BigDecimal sum = xs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(xs.size()), 2, RoundingMode.HALF_UP);
    }

    private static String formatScore(BigDecimal score) {
        if (score == null) {
            return "—";
        }
        return score.stripTrailingZeros().toPlainString();
    }

    private static String formatDurationMinutes(int totalSec) {
        int min = totalSec / 60;
        int sec = totalSec % 60;
        String ss = sec < 10 ? "0" + sec : String.valueOf(sec);
        return min + "m " + ss + "s";
    }

    private static Instant maxInstant(Instant... xs) {
        Instant best = null;
        for (Instant i : xs) {
            if (i != null) {
                best = best == null ? i : (i.isAfter(best) ? i : best);
            }
        }
        return best;
    }

    private static List<String> logsBreakdownLines(Map<InteractionType, Long> map, boolean pl) {
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        long chat = map.getOrDefault(InteractionType.chat, 0L);
        long grading = map.getOrDefault(InteractionType.grading, 0L);
        long generation = map.getOrDefault(InteractionType.generation, 0L);
        long hint = map.getOrDefault(InteractionType.hint, 0L);

        String k1 = pl ? "Czat" : "Chat";
        String k2 = pl ? "Ocena" : "Grading";
        String k3 = pl ? "Generacja" : "Generation";
        String k4 = pl ? "Podpowiedzi" : "Hints";

        List<String> out = new ArrayList<>();
        out.add(k1 + ": " + chat);
        out.add(k2 + ": " + grading);
        out.add(k3 + ": " + generation);
        out.add(k4 + ": " + hint);
        return out;
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
                           Instant generatedAt) throws IOException {
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

        private void coverTitle(String title, String userLogin) throws IOException {
            ensureSpace(92f);

            String subtitleLeft = pl ? "Użytkownik: " : "User: ";
            String subtitle = subtitleLeft + (userLogin == null || userLogin.isBlank() ? "—" : userLogin);

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
            String left = pl ? "Raport użytkownika" : "User report";
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
