package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.AyahDto;
import com.codominator.angusmasail.backend.dto.SurahDetailDto;
import com.codominator.angusmasail.backend.dto.SurahHeaderDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuranService {

    private static final Logger log = LoggerFactory.getLogger(QuranService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final Map<Integer, SurahDetailDto> memoryCache = new ConcurrentHashMap<>();
    private final Path cacheDir = Paths.get("./quran_cache");

    private static final List<SurahHeaderDto> ALL_SURAHS = new ArrayList<>();

    private static final List<AyahDto> ROTATING_DAILY_AYAHS = new ArrayList<>();

    static {
        // Initialize 114 Surahs catalog
        initSurahCatalog();
        initRotatingDailyAyahs();
    }

    private static void initRotatingDailyAyahs() {
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            255, 262,
            "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ",
            "اللہ ہے جس کے سوا کسی کی پوجا نہیں، آپ زندہ اور اوروں کا قائم رکھنے والا، اسے نہ اونگھ آئے نہ نیند (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3",
            3, 42, 2, "Sureh Baqarah"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            103, 396,
            "وَاعْتَصِمُوا بِحَبْلِ اللَّهِ جَمِيعًا وَلَا تَفَرَّقُوا",
            "اور اللہ کی رسی مضبوط تھام لو سب مل کر اور آپس میں پھوٹ نہ ڈالو (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/396.mp3",
            4, 63, 3, "Sureh Aale Imraan"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            5, 6095,
            "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ﴿٥﴾ إِنَّ مَعَ الْعُسْرِ يُسْرًا ﴿٦﴾",
            "تو بیشک دشواری کے ساتھ آسانی ہے، بیشک دشواری کے ساتھ آسانی ہے (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6095.mp3",
            30, 596, 94, "Sureh Inshirah"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            56, 3589,
            "إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ ۚ يَا أَيُّهَا الَّذِينَ آمَنُوا صَلُّوا عَلَيْهِ وَسَلِّمُوا تَسْلِيمًا",
            "بیشک اللہ اور اس کے فرشتے درود بھیجتے ہیں اس غیب بتانے والے (نبی) پر، اے ایمان والو! ان پر درود اور خوب سلام بھیجو (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/3589.mp3",
            22, 426, 33, "Sureh Ahzab"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            286, 293,
            "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
            "اللہ کسی جان پر بوجھ نہیں ڈالتا مگر اس کی طاقت بھر (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/293.mp3",
            3, 49, 2, "Sureh Baqarah"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            13, 4914,
            "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
            "تو تم دونوں اپنے رب کی کون کون سی نعمتوں کو جھٹلاؤ گے (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/4914.mp3",
            27, 531, 55, "Sureh Rahman"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            87, 2570,
            "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            "کوئی معبود نہیں مگر تو، پاکی ہے تجھے بیشک مجھ سے بے جا ہوا (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2570.mp3",
            17, 329, 21, "Sureh Ambiya"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            2, 2,
            "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ﴿٢﴾ الرَّحْمَٰنِ الرَّحِيمِ ﴿٣﴾",
            "سب خوبیاں اللہ کو جو مالک سارے جہان والوں کا، بہت مہربان رحمت والا (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2.mp3",
            1, 1, 1, "Sureh Faatiha"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            152, 159,
            "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            "تو میری یاد کرو میں تمہارا چرچا کروں گا اور میرا حق مانو اور میری ناشکری نہ کرو (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/159.mp3",
            2, 23, 2, "Sureh Baqarah"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            5, 6084,
            "وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ",
            "اور بیشک قریب ہے کہ تمہارا رب تمہیں اتنا دے گا کہ تم راضی ہو جاؤ گے (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6084.mp3",
            30, 596, 93, "Sureh Duha"
        ));
        ROTATING_DAILY_AYAHS.add(new AyahDto(
            10, 4622,
            "إِنَّمَا الْمُؤْمِنُونَ إِخْوَةٌ فَأَصْلِحُوا بَيْنَ أَخَوَيْكُمْ ۚ وَاتَّقُوا اللَّهَ لَعَلَّكُمْ تُرْحَمُونَ",
            "مسلمان مسلمان بھائی ہیں تو اپنے دو بھائیوں میں صلح کرو اور اللہ سے ڈرو کہ تم پر رحمت ہو (کنز الایمان - امام احمد رضا خان)",
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/4622.mp3",
            26, 516, 49, "Sureh Hujuraat"
        ));
    }

    public AyahDto getAyahOfTheDay() {
        int dayOfYear = java.time.LocalDate.now().getDayOfYear();
        if (ROTATING_DAILY_AYAHS.isEmpty()) {
            initRotatingDailyAyahs();
        }
        int idx = Math.abs(dayOfYear % ROTATING_DAILY_AYAHS.size());
        return ROTATING_DAILY_AYAHS.get(idx);
    }

    public QuranService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(requestFactory);

        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
        } catch (Exception e) {
            log.error("Failed to initialize quran_cache dir: {}", e.getMessage());
        }
    }

    public List<SurahHeaderDto> getAllSurahs() {
        return ALL_SURAHS;
    }

    public SurahDetailDto getSurah(int surahNumber) {
        if (surahNumber < 1 || surahNumber > 114) {
            surahNumber = 1;
        }

        // 1. Check memory cache
        if (memoryCache.containsKey(surahNumber)) {
            return memoryCache.get(surahNumber);
        }

        // 2. Check disk cache
        File cacheFile = cacheDir.resolve("surah_" + surahNumber + ".json").toFile();
        if (cacheFile.exists()) {
            try {
                SurahDetailDto dto = objectMapper.readValue(cacheFile, SurahDetailDto.class);
                memoryCache.put(surahNumber, dto);
                return dto;
            } catch (Exception e) {
                log.warn("Error reading cached surah {}: {}", surahNumber, e.getMessage());
            }
        }

        // 3. Fetch from API with Kanzul Iman translation
        try {
            String url = "https://api.alquran.cloud/v1/surah/" + surahNumber + "/editions/quran-uthmani,ur.kanzuliman";
            String jsonResp = restTemplate.getForObject(url, String.class);
            if (jsonResp != null) {
                JsonNode root = objectMapper.readTree(jsonResp);
                if (root.has("code") && root.get("code").asInt() == 200 && root.has("data")) {
                    JsonNode dataArray = root.get("data");
                    if (dataArray.isArray() && dataArray.size() >= 2) {
                        JsonNode uthmaniSurah = dataArray.get(0);
                        JsonNode kanzulImanSurah = dataArray.get(1);

                        SurahDetailDto dto = new SurahDetailDto();
                        dto.setNumber(surahNumber);
                        dto.setName(uthmaniSurah.path("name").asText());
                        dto.setEnglishName(uthmaniSurah.path("englishName").asText());
                        dto.setEnglishNameTranslation(uthmaniSurah.path("englishNameTranslation").asText());
                        dto.setNumberOfAyahs(uthmaniSurah.path("numberOfAyahs").asInt());
                        dto.setRevelationType(uthmaniSurah.path("revelationType").asText());

                        dto.setBismillahArabic("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ");
                        dto.setBismillahTranslation("Allaah Ke Naam Se Shuru Jo Bahot Maherbaan Rehmat Waala.");

                        JsonNode uthmaniAyahs = uthmaniSurah.path("ayahs");
                        JsonNode kanzulAyahs = kanzulImanSurah.path("ayahs");

                        List<AyahDto> ayahList = new ArrayList<>();
                        int count = Math.min(uthmaniAyahs.size(), kanzulAyahs.size());
                        for (int i = 0; i < count; i++) {
                            JsonNode uAyah = uthmaniAyahs.get(i);
                            JsonNode kAyah = kanzulAyahs.get(i);

                            AyahDto ayah = new AyahDto();
                            ayah.setNumberInSurah(uAyah.path("numberInSurah").asInt());
                            int globalNum = uAyah.path("number").asInt();
                            ayah.setGlobalNumber(globalNum);
                            ayah.setArabicText(uAyah.path("text").asText());
                            ayah.setUrduTranslation(kAyah.path("text").asText());
                            ayah.setAudioUrl("https://cdn.islamic.network/quran/audio/128/ar.alafasy/" + globalNum + ".mp3");
                            ayah.setJuz(uAyah.path("juz").asInt());
                            ayah.setPage(uAyah.path("page").asInt());

                            ayahList.add(ayah);
                        }
                        dto.setAyahs(ayahList);

                        // Save to disk & memory
                        try {
                            objectMapper.writeValue(cacheFile, dto);
                        } catch (Exception ex) {
                            log.error("Failed to write to quran cache: {}", ex.getMessage());
                        }
                        memoryCache.put(surahNumber, dto);
                        return dto;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch surah {} from alquran.cloud: {}", surahNumber, e.getMessage());
        }

        // Fallback Surah Faatiha if network unavailable
        return createFallbackSurah(surahNumber);
    }

    public AyahDto getAyah(int surahNumber, int ayahNumber) {
        SurahDetailDto surah = getSurah(surahNumber);
        for (AyahDto ayah : surah.getAyahs()) {
            if (ayah.getNumberInSurah() == ayahNumber) {
                return ayah;
            }
        }
        if (!surah.getAyahs().isEmpty()) {
            return surah.getAyahs().get(0);
        }
        return new AyahDto(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "اللہ کے نام سے شروع جو بہت مہربان رحمت والا", null, 1, 1);
    }


    private SurahDetailDto createFallbackSurah(int surahNumber) {
        SurahDetailDto dto = new SurahDetailDto();
        dto.setNumber(1);
        dto.setName("سُورَةُ ٱلْفَاتِحَةِ");
        dto.setEnglishName("Sureh Faatiha");
        dto.setEnglishNameTranslation("The Opening");
        dto.setNumberOfAyahs(7);
        dto.setRevelationType("Meccan");
        dto.setBismillahArabic("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ");
        dto.setBismillahTranslation("Allaah Ke Naam Se Shuru Jo Bahot Maherbaan Rehmat Waala.");

        List<AyahDto> ayahs = new ArrayList<>();
        ayahs.add(new AyahDto(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "اللہ کے نام سے شروع جو بہت مہربان رحمت والا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3", 1, 1));
        ayahs.add(new AyahDto(2, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "سب خوبیاں اللہ کو جو مالک سارے جہان والوں کا،", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2.mp3", 1, 1));
        ayahs.add(new AyahDto(3, 3, "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "بہت مہربان رحمت والا،", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/3.mp3", 1, 1));
        ayahs.add(new AyahDto(4, 4, "مَٰلِكِ يَوْمِ ٱلدِّينِ", "روز جزا کا مالک،", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/4.mp3", 1, 1));
        ayahs.add(new AyahDto(5, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "ہم تجھی کو پوجیں اور تجھی سے مدد چاہیں،", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5.mp3", 1, 1));
        ayahs.add(new AyahDto(6, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "ہم کو سیدھا راستہ چلا،", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6.mp3", 1, 1));
        ayahs.add(new AyahDto(7, 7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "راستہ ان کا جن پر تو نے احسان کیا، نہ ان کا جن پر غضب ہوا اور نہ بہکے ہوؤں کا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/7.mp3", 1, 1));

        dto.setAyahs(ayahs);
        return dto;
    }

    private static void initSurahCatalog() {
        ALL_SURAHS.add(new SurahHeaderDto(1, "سورة الفَاتِحَة", "Sureh Faatiha", "The Opening", 7, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(2, "سورة البَقَرَة", "Sureh Baqarah", "The Cow", 286, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(3, "سورة آل عِمْرَان", "Sureh Aale Imraan", "The Family of Imran", 200, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(4, "سورة النِّسَاء", "Sureh Nisa", "The Women", 176, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(5, "سورة المَائِدَة", "Sureh Maa'idah", "The Table Spread", 120, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(6, "سورة الأَنْعَام", "Sureh An'aam", "The Cattle", 165, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(7, "سورة الأَعْرَاف", "Sureh A'raaf", "The Heights", 206, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(8, "سورة الأَنْفَال", "Surah Anfaal", "The Spoils of War", 75, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(9, "سورة التَّوْبَة", "Surah Tauba", "The Repentance", 129, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(10, "سورة يُونُس", "Surah Yunus", "Jonah", 109, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(11, "سورة هُود", "Surah Hud", "Hud", 123, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(12, "سورة يُوسُف", "Surah Yusuf", "Joseph", 111, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(13, "سورة الرَّعْد", "Surah Ar-Ra'd", "The Thunder", 43, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(14, "سورة إِبْرَاهِيم", "Surah Ibrahim", "Abraham", 52, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(15, "سورة الحِجْر", "Surah Al-Hijr", "The Rocky Tract", 99, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(16, "سورة النَّحْل", "Surah An-Nahl", "The Bee", 128, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(17, "سورة الإِسْرَاء", "Surah Al-Isra", "The Night Journey", 111, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(18, "سورة الكَهْف", "Surah Al-Kahf", "The Cave", 110, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(19, "سورة مَرْيَم", "Surah Maryam", "Mary", 98, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(20, "سورة طٰهٰ", "Surah Ta-Ha", "Ta-Ha", 135, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(21, "سورة الأَنْبِيَاء", "Surah Al-Anbiya", "The Prophets", 112, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(22, "سورة الحَجّ", "Surah Al-Hajj", "The Pilgrimage", 78, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(23, "سورة المُؤْمِنُون", "Surah Al-Mu'minun", "The Believers", 118, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(24, "سورة النُّور", "Surah An-Nur", "The Light", 64, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(25, "سورة الفُرْقَان", "Surah Al-Furqan", "The Criterion", 77, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(26, "سورة الشُّعَرَاء", "Surah Ash-Shu'ara", "The Poets", 227, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(27, "سورة النَّمْل", "Surah An-Naml", "The Ant", 93, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(28, "سورة القَصَص", "Surah Al-Qasas", "The Stories", 88, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(29, "سورة العَنْكَبُوت", "Surah Al-Ankabut", "The Spider", 69, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(30, "سورة الرُّوم", "Surah Ar-Rum", "The Romans", 60, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(31, "سورة لُقْمَان", "Surah Luqman", "Luqman", 34, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(32, "سورة السَّجْدَة", "Surah As-Sajdah", "The Prostration", 30, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(33, "سورة الأَحْزَاب", "Surah Al-Ahzab", "The Combined Forces", 73, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(34, "سورة سَبَأ", "Surah Saba", "Sheba", 54, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(35, "سورة فَاطِر", "Surah Fatir", "Originator", 45, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(36, "سورة يٰسٓ", "Surah Ya-Sin", "Ya-Sin", 83, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(37, "سورة الصَّافَّات", "Surah As-Saffat", "Those who set the Ranks", 182, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(38, "سورة صٓ", "Surah Sad", "The Letter Sad", 88, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(39, "سورة الزُّمَر", "Surah Az-Zumar", "The Troops", 75, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(40, "سورة غَافِر", "Surah Ghafir", "The Forgiver", 85, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(41, "سورة فُصِّلَت", "Surah Fussilat", "Explained in Detail", 54, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(42, "سورة الشُّورَىٰ", "Surah Ash-Shura", "The Consultation", 53, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(43, "سورة الزُّخْرُف", "Surah Az-Zukhruf", "The Ornaments of Gold", 89, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(44, "سورة الدُّخَان", "Surah Ad-Dukhan", "The Smoke", 59, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(45, "سورة الجَاثِيَة", "Surah Al-Jathiyah", "The Crouching", 37, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(46, "سورة الأَحْقَاف", "Surah Al-Ahqaf", "The Wind-Curved Sandhills", 35, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(47, "سورة مُحَمَّد", "Surah Muhammad", "Muhammad", 38, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(48, "سورة الفَتْح", "Surah Al-Fath", "The Victory", 29, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(49, "سورة الحُجُرَات", "Surah Al-Hujurat", "The Rooms", 18, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(50, "سورة قٓ", "Surah Qaf", "The Letter Qaf", 45, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(51, "سورة الذَّارِيَات", "Surah Adh-Dhariyat", "The Winnowing Winds", 60, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(52, "سورة الطُّور", "Surah At-Tur", "The Mount", 49, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(53, "سورة النَّجْم", "Surah An-Najm", "The Star", 62, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(54, "سورة القَمَر", "Surah Al-Qamar", "The Moon", 55, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(55, "سورة الرَّحْمٰن", "Surah Ar-Rahman", "The Beneficent", 78, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(56, "سورة الوَاقِعَة", "Surah Al-Waqi'ah", "The Inevitable", 96, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(57, "سورة الحَدِيد", "Surah Al-Hadid", "The Iron", 29, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(58, "سورة المُجَادَلَة", "Surah Al-Mujadila", "The Pleading Woman", 22, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(59, "سورة الحَشْر", "Surah Al-Hashr", "The Exile", 24, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(60, "سورة المُمْتَحَنَة", "Surah Al-Mumtahanah", "She that is to be examined", 13, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(61, "سورة الصَّفّ", "Surah As-Saff", "The Ranks", 14, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(62, "سورة الجُمُعَة", "Surah Al-Jumu'ah", "The Congregation", 11, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(63, "سورة المُنَافِقُون", "Surah Al-Munafiqun", "The Hypocrites", 11, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(64, "سورة التَّغَابُن", "Surah At-Taghabun", "The Mutual Disillusion", 18, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(65, "سورة الطَّلَاق", "Surah At-Talaq", "The Divorce", 12, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(66, "سورة التَّحْرِيم", "Surah At-Tahrim", "The Prohibition", 12, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(67, "سورة المُلْك", "Surah Al-Mulk", "The Sovereignty", 30, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(68, "سورة القَلَم", "Surah Al-Qalam", "The Pen", 52, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(69, "سورة الحَاقَّة", "Surah Al-Haqqah", "The Reality", 52, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(70, "سورة المَعَارِج", "Surah Al-Ma'arij", "The Ascending Stairways", 44, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(71, "سورة نُوح", "Surah Nuh", "Noah", 28, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(72, "سورة الجِنّ", "Surah Al-Jinn", "The Jinn", 28, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(73, "سورة المُزَّمِّل", "Surah Al-Muzzammil", "The Enshrouded One", 20, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(74, "سورة المُدَّثِّر", "Surah Al-Muddaththir", "The Cloaked One", 56, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(75, "سورة القِيَامَة", "Surah Al-Qiyamah", "The Resurrection", 40, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(76, "سورة الإِنْسَان", "Surah Al-Insan", "Man", 31, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(77, "سورة المُرْسَلَات", "Surah Al-Mursalat", "The Emissaries", 50, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(78, "سورة النَّبَأ", "Surah An-Naba", "The Tidings", 40, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(79, "سورة النَّازِعَات", "Surah An-Nazi'at", "Those who drag forth", 46, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(80, "سورة عَبَسَ", "Surah Abasa", "He frowned", 42, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(81, "سورة التَّكْوِير", "Surah At-Takwir", "The Overthrowing", 29, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(82, "سورة الانفِطَار", "Surah Al-Infitar", "The Cleaving", 19, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(83, "سورة المُطَفِّفِين", "Surah Al-Mutaffifin", "Defrauding", 36, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(84, "سورة الانشِقَاق", "Surah Al-Inshiqaq", "The Splitting Open", 25, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(85, "سورة البُرُوج", "Surah Al-Buruj", "The Mansions of the Stars", 22, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(86, "سورة الطَّارِق", "Surah At-Tariq", "The Morning Star", 17, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(87, "سورة الأَعْلَىٰ", "Surah Al-A'la", "The Most High", 19, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(88, "سورة الغَاشِيَة", "Surah Al-Ghashiyah", "The Overwhelming", 26, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(89, "سورة الفَجْر", "Surah Al-Fajr", "The Dawn", 30, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(90, "سورة البَلَد", "Surah Al-Balad", "The City", 20, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(91, "سورة الشَّمْس", "Surah Ash-Shams", "The Sun", 15, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(92, "سورة اللَّيْل", "Surah Al-Layl", "The Night", 21, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(93, "سورة الضُّحَىٰ", "Surah Ad-Duha", "The Morning Hours", 11, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(94, "سورة الشَّرْح", "Surah Ash-Sharh", "The Relief", 8, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(95, "سورة التِّين", "Surah At-Tin", "The Fig", 8, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(96, "سورة العَلَق", "Surah Al-Alaq", "The Clot", 19, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(97, "سورة القَدْر", "Surah Al-Qadr", "The Power", 5, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(98, "سورة البَيِّنَة", "Surah Al-Bayyinah", "The Clear Proof", 8, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(99, "سورة الزَّلْزَلَة", "Surah Az-Zalzalah", "The Earthquake", 8, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(100, "سورة العَادِيَات", "Surah Al-Adiyat", "The Courser", 11, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(101, "سورة القَارِعَة", "Surah Al-Qari'ah", "The Calamity", 11, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(102, "سورة التَّكَاثُر", "Surah At-Takathur", "The Rivalry in world increase", 8, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(103, "سورة العَصْر", "Surah Al-Asr", "The Declining Day", 3, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(104, "سورة الهُمَزَة", "Surah Al-Humazah", "The Traducer", 9, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(105, "سورة الفِيل", "Surah Al-Fil", "The Elephant", 5, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(106, "سورة قُرَيْش", "Surah Quraysh", "Quraysh", 4, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(107, "سورة المَاعُون", "Surah Al-Ma'un", "Small Kindnesses", 7, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(108, "سورة الكَوْثَر", "Surah Al-Kawthar", "Abundance", 3, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(109, "سورة الكَافِرُون", "Surah Al-Kafirun", "The Disbelievers", 6, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(110, "سورة النَّصْر", "Surah An-Nasr", "The Divine Support", 3, "Medinan"));
        ALL_SURAHS.add(new SurahHeaderDto(111, "سورة المَسَد", "Surah Al-Masad", "The Palm Fiber", 5, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(112, "سورة الإِخْلَاص", "Surah Al-Ikhlas", "The Sincerity", 4, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(113, "سورة الفَلَق", "Surah Al-Falaq", "The Daybreak", 5, "Meccan"));
        ALL_SURAHS.add(new SurahHeaderDto(114, "سورة النَّاس", "Surah An-Nas", "Mankind", 6, "Meccan"));
    }
}
