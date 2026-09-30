package com.playerhud;

public enum JobType {

    MINER(
            "광부",
            "광물을 채굴해 직업 경험치를 얻습니다.",
            "Lv.40 광맥 채굴 · Lv.50 광물 드롭 2배 · Lv.90 자동 제련 · Lv.100 채굴 가속"
    ),

    FARMER(
            "농부",
            "작물을 수확해 직업 경험치를 얻습니다.",
            "Lv.40 3×3 풍요 수확 · Lv.50 작물 드롭 2배 · Lv.90 자동 재심기 · Lv.100 풍요의 계절"
    ),

    FISHER(
            "어부",
            "물고기를 낚아 직업 경험치를 얻습니다.",
            "Lv.30 입질 시간 20% 감소 · Lv.50 물고기 포획량 2배 · Lv.60 입질 시간 40% 감소 · Lv.100 입질 시간 60% 감소 및 전설 어획"
    );

    private final String displayName;
    private final String description;
    private final String levelDescription;

    JobType(
            String displayName,
            String description,
            String levelDescription
    ) {

        this.displayName =
                displayName;

        this.description =
                description;

        this.levelDescription =
                levelDescription;
    }

    public String getDisplayName() {

        return displayName;
    }

    public String getDescription() {

        return description;
    }

    public String getLevelDescription() {

        return levelDescription;
    }

    public String getMasterTitle() {
        return switch (this) {
            case MINER -> "심층의 정복자";
            case FARMER -> "풍요의 주인";
            case FISHER -> "전설의 낚시꾼";
        };
    }
}
