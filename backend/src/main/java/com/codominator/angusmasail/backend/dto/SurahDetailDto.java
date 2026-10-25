package com.codominator.angusmasail.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SurahDetailDto {
    private int number;
    private String name;
    private String englishName;
    private String englishNameTranslation;
    private int numberOfAyahs;
    private String revelationType;
    private String bismillahArabic;
    private String bismillahTranslation; // Kanzul Iman translation
    private List<AyahDto> ayahs = new ArrayList<>();

    public SurahDetailDto() {}

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public String getEnglishNameTranslation() { return englishNameTranslation; }
    public void setEnglishNameTranslation(String englishNameTranslation) { this.englishNameTranslation = englishNameTranslation; }

    public int getNumberOfAyahs() { return numberOfAyahs; }
    public void setNumberOfAyahs(int numberOfAyahs) { this.numberOfAyahs = numberOfAyahs; }

    public String getRevelationType() { return revelationType; }
    public void setRevelationType(String revelationType) { this.revelationType = revelationType; }

    public String getBismillahArabic() { return bismillahArabic; }
    public void setBismillahArabic(String bismillahArabic) { this.bismillahArabic = bismillahArabic; }

    public String getBismillahTranslation() { return bismillahTranslation; }
    public void setBismillahTranslation(String bismillahTranslation) { this.bismillahTranslation = bismillahTranslation; }

    public List<AyahDto> getAyahs() { return ayahs; }
    public void setAyahs(List<AyahDto> ayahs) { this.ayahs = ayahs; }
}
