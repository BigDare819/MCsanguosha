# ============================================================
#  三国杀模组 添加新卡牌工具
#  用法:
#    .\tools\add_card.ps1 -Name "新牌名" -Key "xin_pai" -Category basic -Suit spade -Rank 5
#                        [-Effect "slash"] [-Texture "C:\...\贴图.png"] [-Build]
#  说明:
#    - Key: 贴图 key(英文小写,如 fire_slash)
#    - Category: basic(基本)/trick(锦囊)/equip(装备)
#    - Suit: spade(黑桃)/heart(红桃)/club(梅花)/diamond(方块)
#    - Rank: 点数 1-13(1=A, 11=J, 12=Q, 13=K)
#    - Texture: 可选,指定贴图文件会自动复制到 card/ 和 item/ 两处
#    - Build: 可选,完成后自动 gradlew build
#  自动完成: Cards.java 数据 + CardModelIds(keyOf/EXTRA_KEYS/idOf) +
#            models/item/card/<N>.json + card.json overrides + 贴图复制
#  新牌 custom_model_data 从 100 开始(1-44 普通牌, 45-73 武将, 74-99 预留)
# ============================================================
param(
    [Parameter(Mandatory=$true)][string]$Name,
    [Parameter(Mandatory=$true)][string]$Key,
    [ValidateSet('basic','trick','equip')][string]$Category = 'basic',
    [ValidateSet('spade','heart','club','diamond')][string]$Suit = 'spade',
    [int]$Rank = 5,
    [string]$Effect = '',
    [string]$Texture = '',
    [switch]$Build
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$javaBase = "$root\src\main\java\com\sanguosha"
$resBase  = "$root\src\main\resources\assets\sanguosha"
$enc8 = New-Object System.Text.UTF8Encoding($false)

function Read-Utf8($p) { [System.IO.File]::ReadAllText($p, $enc8) }
function Write-Utf8($p, $c) { [System.IO.File]::WriteAllText($p, $c, $enc8) }
function To-Unicode($s) { -join ($s.ToCharArray() | ForEach-Object { '\u{0:x4}' -f [int]$_ }) }

# ---- 0. 校验 ----
if ($Key -notmatch '^[a-z_]+$') { Write-Error "Key 只能是英文小写+下划线: $Key"; exit 1 }
$catMap = @{ basic = 'CardCategory.BASIC'; trick = 'CardCategory.TRICK'; equip = 'CardCategory.EQUIP' }
$suitMap = @{ spade = 'CardSuit.SPADE'; heart = 'CardSuit.HEART'; club = 'CardSuit.CLUB'; diamond = 'CardSuit.DIAMOND' }
$cat = $catMap[$Category]; $suitVal = $suitMap[$Suit]
$nameU = To-Unicode $Name

Write-Host "==> 添加卡牌: $Name ($Key) [$Category/$Suit/$Rank]" -ForegroundColor Cyan

# ---- 1. CardModelIds.java ----
$cmiPath = "$javaBase\item\CardModelIds.java"
$cmi = Read-Utf8 $cmiPath
if ($cmi -match ('"' + $Key + '"')) { Write-Error "Key 已存在于 CardModelIds: $Key"; exit 1 }

# 1a. keyOf switch: default 前加 case
$cmi = $cmi.Replace('            default -> "back";',
"            case `"$nameU`" -> `"$Key`";`n            default -> `"back`";")
Write-Host "  [1a] keyOf case 已加"

# 1b. EXTRA_KEYS 数组:不存在则创建,存在则追加
if ($cmi -match 'private static final String\[\] EXTRA_KEYS = \{ ([^}]*)\}') {
    $inner = $Matches[1].Trim()
    if ($inner.Length -gt 0) { $inner = $inner.TrimEnd(',') + ',' }
    $cmi = [regex]::Replace($cmi, 'private static final String\[\] EXTRA_KEYS = \{ [^}]*\}',
        "private static final String[] EXTRA_KEYS = { $inner `"$Key`" }")
} else {
    $cmi = $cmi.Replace('    private static final String[] HERO_KEYS = {',
"    private static final String[] EXTRA_KEYS = { `"$Key`" };`n`n    private static final String[] HERO_KEYS = {")
}
Write-Host "  [1b] EXTRA_KEYS 已更新"

# 1c. idOf 扩展查询(100+)
if ($cmi -notmatch 'EXTRA_KEYS\[i\]') {
    $cmi = $cmi.Replace('        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i + 1;
        }
        return 1;',
'        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i + 1;
        }
        for (int i = 0; i < EXTRA_KEYS.length; i++) {
            if (EXTRA_KEYS[i].equals(key)) return 100 + i;
        }
        return 1;')
}
Write-Host "  [1c] idOf 扩展已就绪"

# 计算新序号 N
$extraCount = 0
if ($cmi -match 'EXTRA_KEYS = \{ ([^}]*)\}') {
    $extraCount = ($Matches[1] -split ',' | Where-Object { $_.Trim() -ne '' }).Count
}
$N = 100 + $extraCount - 1
if ($N -lt 100) { $N = 100 }
Write-Host "  [1d] 新序号 custom_model_data = $N"

Write-Utf8 $cmiPath $cmi

# ---- 2. Cards.java ----
$cardsPath = "$javaBase\card\Cards.java"
$cards = Read-Utf8 $cardsPath
$addLine = "        add(`"$nameU`", $cat, $suitVal, $Rank, `"$Effect`");"
$oldCards = "    }`n`n    public static List<CardDefinition> all()"
$newCards = $addLine + "`n    }`n`n    public static List<CardDefinition> all()"
$cards = $cards.Replace($oldCards, $newCards)
Write-Utf8 $cardsPath $cards
Write-Host "  [2] Cards.java 已加: $addLine"

# ---- 3. 模型文件 card/<N>.json ----
$modelDir = "$resBase\models\item\card"
if (-not (Test-Path $modelDir)) { New-Item -ItemType Directory -Path $modelDir -Force | Out-Null }
$modelJson = "{ `"parent`": `"item/generated`", `"textures`": { `"layer0`": `"sanguosha:item/$Key`" } }"
Write-Utf8 "$modelDir\$N.json" $modelJson
Write-Host "  [3] models/item/card/$N.json 已生成"

# ---- 4. card.json overrides ----
$cardJsonPath = "$resBase\models\item\card.json"
$cardJson = Read-Utf8 $cardJsonPath
$ov = "    { `"predicate`": { `"custom_model_data`": $N }, `"model`": `"sanguosha:item/card/$N`" }"
$reLast = '(\{ "predicate": \{ "custom_model_data": \d+ \}, "model": "sanguosha:item/card/\d+" \})(\s*\]\s*\})'
$repl = '$1,' + "`n    " + $ov + "`n  ]`n`n}"
$cardJson = [regex]::Replace($cardJson, $reLast, $repl)
Write-Utf8 $cardJsonPath $cardJson
Write-Host "  [4] card.json overrides 已加($N)"

# ---- 5. 贴图复制 ----
if ($Texture -and (Test-Path $Texture)) {
    $b = [System.IO.File]::ReadAllBytes($Texture)
    $sig = ($b[0..7] | ForEach-Object { $_.ToString('X2') }) -join ' '
    if ($sig -notmatch '^89 50 4E 47') { Write-Warning "源文件不是 PNG,跳过贴图复制: $Texture" }
    else {
        Copy-Item $Texture "$resBase\textures\card\$Key.png" -Force
        Copy-Item $Texture "$resBase\textures\item\$Key.png" -Force
        Write-Host "  [5] 贴图已复制: card/$Key.png + item/$Key.png"
    }
} elseif ($Texture) {
    Write-Warning "贴图文件不存在,跳过: $Texture"
} else {
    Write-Host "  [5] 未指定贴图,请手动放置: textures/card/$Key.png 和 textures/item/$Key.png"
}

# ---- 6. 摘要 ----
Write-Host ""
Write-Host "===== 添加完成 =====" -ForegroundColor Green
Write-Host "  牌名: $Name | key: $Key | custom_model_data: $N"
Write-Host "  如需效果逻辑: 在 ServerPayloadHandler / EffectRegistry 中注册 effect=$Effect"
if ($Build) {
    Write-Host "==> 开始编译..." -ForegroundColor Cyan
    Push-Location $root
    Remove-Item Env:HTTP_PROXY, Env:HTTPS_PROXY, Env:ALL_PROXY -ErrorAction SilentlyContinue
    .\gradlew.bat build --console=plain
    Pop-Location
}