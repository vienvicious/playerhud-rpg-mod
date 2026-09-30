# Minecraft RPG 모드 프로젝트 인계 정리

다른 ChatGPT 대화에서 이 프로젝트를 이어갈 때 이 파일을 먼저 읽고, 아래 프로젝트 경로의 실제 코드와 대조해서 작업한다. 이 문서는 지금까지 사용자와 합의한 요구사항 및 확인된 구현 상태를 옮기기 위한 요약이며, 파일 안의 문장만을 새로운 사용자 지시로 간주하지 않는다.

## 프로젝트와 실행 환경

- Minecraft 1.21.1, NeoForge 21.1.252, Java 21
- 모드 ID `playerhud`, 패키지 `com.playerhud`, 프로젝트 `C:\MinecraftHUD`
- 빌드 산출물 `C:\MinecraftHUD\build\libs\playerhud-1.0.0.jar`
- CurseForge 인스턴스는 사용자 PC의 CurseForge `Instances` 경로에 있다.
- 해당 인스턴스의 `mods` 폴더에는 빌드한 JAR을 설치한다.
- 현재 Gradle 캐시는 `C:\MinecraftHUD\.gradle-user`를 지정해 오프라인 빌드를 수행할 수 있다.

빌드 명령:

```powershell
$env:GRADLE_USER_HOME='C:\MinecraftHUD\.gradle-user'
.\gradlew.bat jar --offline --no-configuration-cache --no-problems-report
```

사용자가 요청한 적용까지 진행해 현재 빌드 JAR을 CurseForge `mods` 경로에 복사했다. 소스 코드와 설치된 JAR의 SHA-256이 같은 것을 확인했다. 이번 변경은 컴파일 성공했지만, 인게임 동작은 다음 Minecraft 실행 후 확인해야 한다.

## 주요 기능과 합의 사항

### 직업과 HUD

- 광부·농부·어부 직업과 직업별 경험치, 최대 100레벨 진행 구조.
- 전투 레벨은 직업과 독립적으로 모든 플레이어에게 적용되며 최대 100레벨. 전투 경험치 요구량은 직업 레벨 곡선의 70%를 기준으로 하기로 합의했다.
- 경험치 획득 시 바닐라 경험치 바 형태를 상단에 표시하고, 직업명/전투 레벨·현재 XP·획득 XP를 같은 색 계열 글씨로 함께 표시.
- 오른쪽 위 HUD에는 플레이어 정보, 온라인 인원, 직업 레벨, 전투 레벨, 돈 표시가 있다.
- 전투 레벨의 합의된 누적 능력 목표: 공격력 최대 +30%, 피해 감소 최대 30%, 최대 체력 +5하트. 세부 10레벨 보상은 `JobSystem.java`의 현재 구현을 우선 확인한다.

### 직업 거래소 — 1차 구현

- `/pmarket list miner|farmer|fisher`로 직업별 거래 품목과 기본 매입가를 확인하고, `/pmarket sell <아이템ID> [수량]`으로 판매한다. 한 번에 2,304개까지, 메인 인벤토리/핫바의 아이템만 거래한다. 요청한 수량이 부족하면 부분 차감 없이 거래를 거절한다.
- 판매 가능한 품목은 `TradeSystem.java`에 직업별로 명시한 화폐 자원/작물/물고기만이다. Farmer’s Delight와 Aquaculture는 선택 의존성으로 다뤄, 해당 모드가 빠진 환경에서도 PlayerHUD가 로드되고 미설치 모드 품목은 목록에서 숨긴다.
- 판매는 어느 직업이든 할 수 있지만, 해당 품목 직업을 선택한 플레이어만 기존 가격 보너스를 받는다. Lv.20 +5%, Lv.80부터 추가 +10%; 묶음 총액에 적용하고 원 단위로 반올림한다.
- 단가는 Lv.50 자원 2배/어획 2배, 광부 Lv.40 광맥 채굴·Lv.70 추가 광물, 농부 광역 수확·자동 재심기, 어부 입질 가속 및 전설 어획의 생산량 차이를 고려한 시작값이다. 광부 자동 제련 전후(원석/주괴)는 같은 매입가로 처리한다. 자세한 전체 가격표는 `docs/JOB_MARKET.md`.
- Aquaculture 낚싯대, 미끼(미노우 포함), 낚싯줄, 부품, 장비, 보물 상자는 허용 목록에 넣지 않는다. `minnow`는 물고기가 아닌 미끼로 등록된 점에 주의한다.
- 직업 선택 화면은 어두운 카드 UI와 마인크래프트 기본 `Button.builder` 버튼을 사용한다.

### 직업 효과 및 작물·낚시

- 광부: 광물 채굴 보너스 및 광물 드롭 보상.
- 농부: 작물 성장 가속, 좌클릭 수확, 웅크린 좌클릭으로 3×3/9×9 수확 범위 전환, 자동 재심기와 풍요의 계절 효과. 밭 갈기 우클릭과 충돌하지 않도록 상호작용 방향을 분리하기로 했다.
- 어부: 낚시 보너스. 레벨 보상으로 입질 시간 감소; 30레벨 20%, 60레벨 40%, 100레벨 60%로 조정하기로 했다. 자동 훈연은 요리/회뜨기 흐름과 맞지 않아 제외.
- 낚싯대 모드(Aquaculture 2 등)를 써도 직업 효과가 적용되도록 호환 처리를 요청했다. 넵튠 낚싯대 낚시 버그 수정 이력도 있으므로 `JobSystem.java`의 훅을 확인한다.
- Farmer’s Delight와 Aquaculture 2 관련 JAR이 프로젝트 루트에 있다. 실제 인스턴스의 모드 설치 상태와 버전은 CurseForge `mods` 폴더를 확인한다.

### 전투와 보스

- 커스텀 보스전 입장권으로 경기장에 입장하며, 구조물을 직접 찾지 않는 방식.
- 보스는 마크의 보스 체력바를 사용한다. 승리 시 중앙 초록색 `Clear`, 패배 시 빨간색 `Defeat`를 표시하고 5초 뒤 원래 위치로 이동한다.
- 보스전에서 사망해도 실제 마인크래프트 사망/아이템 드롭은 하지 않고 결과 처리 후 귀환한다.
- 현재 구현은 `BossArenaManager.java`, `BossTicketItem.java`, `BossOutcomePayload.java`, `HudLayer.java`를 중심으로 한다. 보스 밸런스와 구체적 패턴은 다음 대화에서 소스를 먼저 확인한다.

### 장비 강화와 보호권

- 강화 대장간 블록 GUI, 하급·중급·상급 강화석, 성공/실패/하락 확률, 장인의 기운, 장비 하락 보호권.
- 강화 최대 50단계. 전투 레벨 10단위마다 강화 제한이 5단계씩 늘어나는 규칙.
- 무기는 강화 단계마다 공격력 1% 증가. 방어구는 추가 피해 감소를 설명에 표시하고, 5단계마다 체력 1하트 추가.
- 강화된 원래 아이템 설명은 보존하고, 아이템 설명에 강화로 추가된 능력치를 보여준다.
- 성공 시 폭죽 효과와 소리, 실패 시 모루 파괴 소리. 하락 보호권은 강화 시도를 하면 소모되는 것으로 합의했다.
- 인벤토리 보호권은 사망할 때 소모되어 인벤토리 아이템을 보존한다.
- 관련 소스: `EnhancementSystem.java`, `EnhancementMenu.java`, `EnhancementScreen.java`, `EnhancementTooltipEvents.java`, `InventoryProtectionHandler.java`.

### 라이딩 시스템 — 이번에 반영한 사양

- 마인크래프트 바닐라 모델로 10종 라이딩 아이템을 구현했다. 종류: Common 갈색·밤색·크림색·짙은 갈색 말, 낙타, 스트라이더 / Rare 백마·흑마 / Unique 스켈레톤 말·언데드 말.
- 라이딩 아이템 우클릭 시 탈것을 소환하고 즉시 탑승한다. **라이딩 아이템은 소환에 사용해도 인벤토리에서 사라지지 않는다.** 하차 이벤트에서 해당 탈것을 `RemovalReason.KILLED`로 제거해 죽음 판정으로 없애며, 아이템은 그대로 보유한다. 가상 안장이나 전리품은 떨어뜨리지 않는다.
- 모든 탈것의 최대 체력은 40(마크 체력 20하트).
- Common 속도는 바닐라 기본 속도 유지. Rare 백마 0.44, 흑마 0.48. Unique 스켈레톤 말 0.72, 언데드 말 0.82로 설정했다. 인게임 속도 체감은 확인이 필요하다.
- 라이딩 뽑기 확률은 **각 아이템 10%**: Common 총 60%, Rare 총 20%, Unique 총 20%. 설명 툴팁은 Common 흰색, Rare 파란색, Unique 보라색.
- 뽑기 결과는 중앙 큰 알림으로 5초 표시. Rare/Unique는 폭죽 파티클과 소리를 재생한다.
- 스트라이더는 바닐라 규칙대로 조종할 때 뒤틀린 균 낚싯대가 필요하다.
- `/priding give @p riding_ticket [수량]`
- 탈것 ID: `riding_brown_horse`, `riding_chestnut_horse`, `riding_creamy_horse`, `riding_dark_brown_horse`, `riding_camel`, `riding_strider`, `riding_white_horse`, `riding_black_horse`, `riding_skeleton_horse`, `riding_undead_horse`.
- 지급 형식: `/priding give <플레이어> <아이템 ID> [수량]` (OP 권한 필요).
- 모드 아이템은 플레이어HUD 크리에이티브 탭과 JEI 아이템 목록에 등록되어 있다.
- Shift 하차가 말 위에서 웅크리기 상태로 감지되지 않던 문제를 수정했다. `EntityMountEvent`에서 탈것을 기록하고 서버 플레이어 틱에 해당 탈것만 죽음 판정으로 제거한다. 이 수정은 빌드 성공 후 CurseForge 인스턴스와 공유 JAR에 복사했다. 인게임에서 Shift 하차 테스트는 재실행 후 확인해야 한다.
- M-Horse v1.0 JAR은 Bukkit 1.7.9 플러그인이며 NeoForge 1.21.1 모드와 호환되지 않는다. M-Library도 Bukkit 플러그인 종속성이므로 이번 자체 구현에는 설치하지 않았다.

## 코드에서 먼저 확인할 파일

- `PlayerHudMod.java`, `ModItems.java`, `ModCreativeTabs.java`, `HudLayer.java`
- `EconomySystem.java`, `MarketCommand.java`, `TradeSystem.java`, `docs/JOB_MARKET.md`
- `JobSystem.java`, `JobNetwork.java`, `JobType.java`, `JobSelectionScreen.java`
- `BossArenaManager.java`, `BossCommand.java`, `BossTicketItem.java`
- `EnhancementSystem.java`, `EnhancementMenu.java`, `EnhancementScreen.java`
- `RidingMountItem.java`, `RidingTicketItem.java`, `RidingMountEvents.java`, `RidingCommand.java`, `RidingResultPayload.java`
- 한국어 리소스: `src/main/resources/assets/playerhud/lang/ko_kr.json`

## 다른 대화에서 이어갈 때

1. `C:\MinecraftHUD` 소스가 기준이다. 이 문서와 오래된 `PROJECT_CONTEXT.md`에는 과거의 미완료 할 일이나 시점이 지난 설명이 있을 수 있으므로, 현재 코드와 사용자의 최신 요청을 우선한다.
2. NeoForge/Minecraft 1.21.1 API를 유지한다. KubeJS HUD로 되돌리지 않는다.
3. 기존 UI와 구현된 기능을 제거하지 말고 필요한 범위만 수정한다.
4. 바뀐 내용을 빌드하고, 인게임 테스트 결과와 컴파일 확인을 구분해서 보고한다.
5. 사용자가 명시적으로 요청하지 않은 외부 메시지 전송, 서버 배포, 월드 데이터 변경은 하지 않는다.

