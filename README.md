# PlayerHUD RPG Mod

Minecraft 1.21.1과 NeoForge 21.1.252용 커스텀 RPG 모드입니다. 직업·전투 진행도와 HUD, 보스전, 장비 강화, 라이딩 시스템을 구현합니다.

## 주요 기능

- 광부·농부·어부 직업과 최대 100레벨 진행, 전투 직업 XP HUD
- 채굴·농사·낚시 직업 보너스와 모드 작물/낚시 호환 처리
- 입장권으로 시작하는 보스전, 보스 체력바, Clear/Defeat 처리 및 귀환
- 강화 대장간 GUI, 50단계 장비 강화, 강화 재료와 하락 보호권
- 인벤토리 보호권
- 10종 라이딩 소환 아이템과 추첨권, 확률·등급 표시, JEI/크리에이티브 탭 등록
- 명령어로 보스/보호권/라이딩 아이템 지급

현재 구현과 합의된 설정의 상세 인계 기록은 [`docs/PROJECT_CONTEXT_HANDOFF.md`](docs/PROJECT_CONTEXT_HANDOFF.md)를 참고하세요.

## 개발 환경

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.252
- 모드 ID: `playerhud`

## 빌드

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
bash ./gradlew build
```

출력 JAR은 `build/libs/`에 생성됩니다. 저장소에는 생성물, 로컬 Gradle 캐시, 다운로드한 모드 JAR을 포함하지 않습니다.

## 게임에서 사용

모드를 NeoForge 1.21.1 인스턴스의 `mods` 폴더에 넣으세요. Farmer’s Delight, Aquaculture 2, Lightman's Currency 등 모드 연동 기능을 사용할 때는 해당 모드를 각 모드 배포처에서 별도로 설치하세요. 이 저장소는 해당 모드의 JAR을 재배포하지 않습니다.

관리자 지급 명령 예시:

```text
/priding give @p riding_ticket 1
/priding give @p riding_white_horse 1
```

## 라이선스

`TEMPLATE_LICENSE.txt`는 NeoForge MDK 템플릿에서 제공된 파일에만 적용됩니다. 모드의 프로젝트 소스 코드는 `gradle.properties` 기준 All Rights Reserved이며, 공개 저장소라는 사실만으로 복제·수정·재배포 권한이 부여되지는 않습니다.
