# CLAUDE.md

YuruCra（Paper プラグイン）。旧RealisticSurvival（`../Release/RealisticSurvival`、Paper 1.20.1）の機能を Paper 26.x 向けに作り直して移植している。
GitHub: https://github.com/kotyabuchi/YuruCra （旧YuruCra は `legacy` ブランチ、ローカルは `../Release/YuruCra-legacy`）

旧RealisticSurvival の保存データ（DB・スキルのオン状態・墓石）とは互換を取らない方針。

## 対象バージョン

| 項目 | バージョン |
|---|---|
| サーバー | Paper 26.3（`paper-api:26.3.build.244-stable`） |
| Java | 25 |
| Kotlin | 2.4.21 |
| Gradle | 9.9.0（Wrapper） |

バージョンを上げるときは `build.gradle.kts` の `minecraftVersion` / `paperApiVersion` を合わせて変える。
26.1 以降、paper-api のバージョン表記は `{VERSION}.build.{N}-stable`（旧: `-R0.1-SNAPSHOT`）。

## コマンド

```bash
./gradlew build       # テストを実行して build/libs/YuruCra-*.jar と build/resourcepack/PumpkingPack.zip（.sha1 付き）を作る
./gradlew test        # テストだけ実行（サーバーなしで動く純粋なロジックが対象）
./gradlew runServer   # run/ にテストサーバーを立てて起動（初回は run/eula.txt の同意が必要）
```

## リソースパック

- `resourcepack/` が配布用のリソースパック（形式 97.1 = 26.3）。`./gradlew build` で `build/resourcepack/PumpkingPack.zip` になる
- アイテムの見た目は custom_model_data の**文字列**で切り替える。プラグイン側は `util/ItemModels.kt` の `ItemModel`、パック側は `assets/minecraft/items` の `"when"`。**両方を必ず一致させる**
- パックなしの人には元のアイテムの見た目（矢印・看板など）がそのまま見えるようにする（`fallback` にバニラのモデルを書く）
- 独自のモデル・テクスチャは名前空間 `pumpking` に置く。アイテムのテクスチャは `textures/item/` 以下でないと読み込まれない。ファイル名は小文字のみ
- 26.x の定義ファイルの書き方が不明なときは、`%APPDATA%\.minecraft\versions\26.3\26.3.jar` の `assets/minecraft/items` を見本にする
- 配布: GitHub Releases に `PumpkingPack.zip` を添付する。プラグインは `config.yml` の `resource-pack.url`（`releases/latest/download/PumpkingPack.zip`）から起動時にダウンロードしてSHA-1を計算し、参加時に送る。リリースを更新したら `/yc resourcepack reload`

## 構成

- `YuruCra.kt` … プラグイン本体。各機能のインスタンスを持つ。**DIライブラリは使わない**（各クラスはコンストラクタで `plugin` を受け取る）
- `data/` … SQLite。**SQL は必ず `Database.submit {}` の中（DB専用スレッド）で実行する**。メインスレッドで実行しない
- `player/` … プレイヤーデータ（ジョブ経験値・ホーム・リソースストレージ）
- `job/` … ジョブ、経験値付与（`JobProgress`）、スキル詠唱（`SkillCaster`）
- `skill/` … スキル。基本クラスは `Skill` → `ToggleSkill` → `TimedToggleSkill` / `PassiveSkill`
- `block/BreakContext.kt` … スキルによる一括破壊。`Player#breakBlock` で1つずつ壊し、破壊中は `current(player)` で「どのスキルによる破壊か」を判定できる。まとめて壊した分のドロップは起点に集め、経験値は起点の破壊イベントの最後（HIGHEST）でオーブ1つにまとめる（スキルから呼ぶときは `originEvent` に起点の BlockBreakEvent を渡す）
- `feature/` … 他と独立した小さな便利機能（エレベーター・ワールド保護・石生成機・スポナー強化・農地・チャット音・初期アイテム・リソースパック配布・チェストソート・追加レシピ・ダメージ表示）
- `monster/` … 強化モンスター。戦闘中の行動は `FightBehavior` を `FightTracker` に登録すると、1つのタイマーで標的のいるモブだけ確認される。ほかに帽子をかぶったゾンビ・スケルトン、モンスターの爆発で壊れた地形を元に戻す処理（TNTなどプレイヤーの爆発はバニラどおり）
- `item/` … アイテムの仕組み。`ItemIdentity` は1つずつしか持てないアイテムに、見分け用のUUIDを一度だけ付ける（`ItemIdentity.uuidOf` で読む）
- `tombstone/` … 墓石。中身は死亡直後に `tombstones` テーブルへ保存し、メモリには位置だけ持つ
- `command/` … `/yc`（別名 `/yurucra`）。Brigadier で登録する
- `menu/` … チェストGUI。`MenuPage` を `InventoryHolder` にして判別する。開閉は `MenuService` 経由（次のtickで行う）

## BlockDropItemEvent の優先度

ドロップを書き換える処理は、登録順に頼らず優先度で順番を決めている。追加するときはこの表に入れること。MONITOR では書き換えない。

| 優先度 | 処理 |
|---|---|
| LOW | `SafeCropAndReplant` 収穫時に種を1つ使って植え直す |
| NORMAL | `GatheringJob` ドロップ増加と経験値付与 |
| HIGH | `StoneReplacer` ドロップを手元に送って跡を石で埋める |
| HIGHEST | `BreakContext` まとめて壊した分のドロップを1か所に集める |

## 規約・注意点

- 人数が増えても負荷が増えにくい書き方にする:
  - ブロック1個・経験値1回ごとにタスクを予約しない（同じtick内だけ使う印は `TickScopedMap`、時間で消すものは時刻を記録して1つのタイマーで確認）
  - 毎tick・クリックごとに呼ばれる処理は、軽い判定（素材の比較など）を先にする
  - ブロックの探索は上限付きの幅優先にする（再帰の深さ優先にしない）

- 使わない古いAPI: `ChatColor`・`§`/`&` の色コード（→ Adventure の `Component`）、`setCustomModelData(Int)`（→ データコンポーネント）、`Material.values()`（→ `Material.entries`）、`Block#isValidTool`（→ `canHarvestWith`）、`ParticleBuilder#extra`（→ `speed`）、AnvilGUI（→ Dialog API の `TextPrompt`）、`plugin.yml` の `commands:`（→ Brigadier の `YcCommand`）
- サバイバルのクライアントはスロットの中クリックをサーバーへ送らない（`ClickType.MIDDLE` はクリエイティブでしか来ない）。操作の割り当てに使わない
- 道具の判定は `Tag.ITEMS_PICKAXES` などのタグを使う（`ToolType`）。素材名の文字列判定は新ブロック追加で漏れるので避ける
- `JobType` の名前・スキルの `id`・DBのテーブル構成は保存データと紐付いている。変えると既存のデータが読めなくなる
- 依存ライブラリは jar に同梱せず `plugin.yml` の `libraries` で読み込む（`compileOnly` にする）
- APIの使い方が不明なときは Context7（`/papermc/docs`）か、`~/.gradle/caches` の paper-api jar を `javap` で確認する

## 未移植（旧RealisticSurvival にあってまだ移していない機能）

独自耐久値（ItemExtension）・クラフト拡張、グローバル修繕、サウンドサンプルメニュー

旧RealisticSurvival でも無効だったもの（移すかは都度決める）: 戦闘系ジョブ、ペット、体力バー、自動植林、独自ポーション効果、ワールドメニュー

移さないと決めたもの: 羊毛刈り（AnimalShearing）、ブロック設置ディスペンサー（BlockPlacer）、金床プレス（AnvilPressCraft）、カスタムモブ（CursedEye）、多機能ディスペンサー（OmniDispenser）、強化エンダーマン（CursedEye を呼ぶだけだったため）
