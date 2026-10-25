package com.codominator.angusmasail.backend.dto;

public class SurahHeaderDto {
    private int number;
    private String name;
    private String englishName;
    private String englishNameTranslation;
    private int numberOfAyahs;
    private String revelationType;

    public SurahHeaderDto() {}

    public SurahHeaderDto(int number, String name, String englishName, String englishNameTranslation, int numberOfAyahs, String revelationType) {
        this.number = number;
        this.name = name;
        this.englishName = englishName;
        this.englishNameTranslation = englishNameTranslation;
        this.numberOfAyahs = numberOfAyahs;
        this.revelationType = revelationType;
    }

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
}
