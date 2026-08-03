# ============================================================
#  三国杀模组 添加新武将工具(只加将盒数据+贴图+模型,不碰技能逻辑)
#  用法:
#    .\tools\add_hero.ps1 -Name "新武将" -Id "xin_id" -Faction wei -Hp 4
#                        [-Texture "C:\...\贴图.png"] [-Build]
#  说明:
#    - Id: 拼音 id(英文小写,如 caocao)
#    - Faction: wei(魏)/shu(蜀)/wu(吴)/qun(群)/shen(神)/lao(牢)
#    - Hp: 体力值(整数)
#    - Texture: 可选,自动复制到 textures/hero/<id>.png(实体)和 textures/item/hero_<id>.png(手持/HUD)
#    - Build: 可选,完成后自动 gradlew build
#  自动完成: Heroes.java(nameToId case + add) + CardModelIds(HERO_KEYS 追加) +
#            models/item/card/<N>.json + card.json overrides + 贴图复制
#  武将 custom_model_data 自动接续(当前 45-73,新武将从 74 起)
#  技能占位后续手动加(StandardSkills.java),本工具不涉及
# ============================================================
param(
    [Parameter(Mandatory=$true)][string]$Name,
    [Parameter(Mandatory=$true)][string]$Id,
    [ValidateSet('wei','shu','wu','qun','shen','lao')][string]$Faction = 'qun',
    [int]$Hp = 4,
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
if ($Id -notmatch '^[a-z_]+$') { Write-Error "Id 只能是英文小写+下划线: $Id"; exit 1 }
$factionMap = @{ wei = 'Faction.WEI'; shu = 'Faction.SHU'; wu = 'Faction.WU'; qun = 'Faction.QUN'; shen = 'Faction.SHEN'; lao = 'Faction.LAO' }
$factionVal = $factionMap[$Faction]
$nameU = To-Unicode $Name

Write-Host "==> 添加武将: $Name ($Id) [$Faction / $Hp 血]" -ForegroundColor Cyan

# ---- 1. Heroes.java ----
$heroesPath = "$javaBase\hero\Heroes.java"
$heroes = Read-Utf8 $heroesPath
if ($heroes -match ('"' + $Id + '"')) { Write-Error "Id 已存在于 Heroes.java: $Id"; exit 1 }

# 1a. nameToId: default 前加 case
$heroes = $heroes.Replace('            default -> name;',
"            case `"$nameU`" -> `"$Id`";`n            default -> name;")
Write-Host "  [1a] nameToId case 已加"

# 1b. static 块末尾加 add(锚点 all() 方法前,无技能)
$oldAnchor = "    }`n`n    public static List<HeroDefinition> all()"
$addLine = "        add(`"$nameU`", $factionVal, $Hp);"
$heroes = $heroes.Replace($oldAnchor, $addLine + "`n    }`n`n    public static List<HeroDefinition> all()")
Write-Host "  [1b] Heroes.java 已加: $addLine"

Write-Utf8 $heroesPath $heroes

# ---- 2. CardModelIds.java ----
$cmiPath = "$javaBase\item\CardModelIds.java"
$cmi = Read-Utf8 $cmiPath
if ($cmi -match ('"' + $Id + '"')) { Write-Error "Id 已存在于 CardModelIds: $Id"; exit 1 }

# 2a. HERO_KEYS 追加
$reHero = '(HERO_KEYS = \{[^}]*")([a-z_]+)"(\s*\};)'
if ($cmi -match $reHero) {
    $repl = '$1$2","' + $Id + '"$3'
    $cmi = [regex]::Replace($cmi, $reHero, $repl)
    Write-Host "  [2a] HERO_KEYS 追加 $Id"
} else {
    Write-Error "未找到 HERO_KEYS 数组,请检查 CardModelIds.java"; exit 1
}

# 计算新序号 N
$mHeroBody = [regex]::Match($cmi, 'HERO_KEYS = \{(.*?)\};', 'Singleline')
$heroCount = 0
if ($mHeroBody.Success) {
    $heroCount = ($mHeroBody.Groups[1].Value -split ',' | Where-Object { $_.Trim().Trim('"') -ne '' }).Count
}
$N = 45 + $heroCount - 1
if ($N -lt 45) { $N = 45 }
Write-Host "  [2b] 新序号 custom_model_data = $N"

Write-Utf8 $cmiPath $cmi

# ---- 3. 模型文件 card/<N>.json ----
$modelDir = "$resBase\models\item\card"
if (-not (Test-Path $modelDir)) { New-Item -ItemType Directory -Path $modelDir -Force | Out-Null }
$modelJson = "{ `"parent`": `"item/generated`", `"textures`": { `"layer0`": `"sanguosha:item/hero_$Id`" } }"
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
        Copy-Item $Texture "$resBase\textures\hero\$Id.png" -Force
        Copy-Item $Texture "$resBase\textures\item\hero_$Id.png" -Force
        Write-Host "  [5] 贴图已复制: hero/$Id.png + item/hero_$Id.png"
    }
} elseif ($Texture) {
    Write-Warning "贴图文件不存在,跳过: $Texture"
} else {
    Write-Host "  [5] 未指定贴图,请手动放置: textures/hero/$Id.png 和 textures/item/hero_$Id.png"
}

# ---- 6. 摘要 ----
Write-Host ""
Write-Host "===== 添加完成 =====" -ForegroundColor Green
Write-Host "  武将: $Name | id: $Id | custom_model_data: $N"
Write-Host "  技能逻辑未加(StandardSkills 占位后续手动);如需显示技能描述请在 StandardSkills.java 注册"
if ($Build) {
    Write-Host "==> 开始编译..." -ForegroundColor Cyan
    Push-Location $root
    Remove-Item Env:HTTP_PROXY, Env:HTTPS_PROXY, Env:ALL_PROXY -ErrorAction SilentlyContinue
    .\gradlew.bat build --console=plain
    Pop-Location
}